package com.fast.fsf.controller;

import com.fast.fsf.model.ActivityLog;
import com.fast.fsf.model.TimetableEntry;
import com.fast.fsf.repository.ActivityLogRepository;
import com.fast.fsf.repository.TimetableEntryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/timetable")
@CrossOrigin(origins = "http://localhost:5173")
public class TimetableEntryController {

    private final TimetableEntryRepository timetableRepository;
    private final ActivityLogRepository activityLogRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public TimetableEntryController(TimetableEntryRepository timetableRepository, ActivityLogRepository activityLogRepository) {
        this.timetableRepository = timetableRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @GetMapping
    public List<TimetableEntry> getAllEntries() {
        return timetableRepository.findByApprovedTrue();
    }

    @GetMapping("/section")
    public List<TimetableEntry> getSectionTimetable(
            @RequestParam String department,
            @RequestParam String batch,
            @RequestParam String section) {
        String d = department.trim();
        String b = batch.trim();
        String s = section.trim();
        System.out.println("DEBUG: Querying timetable for Dept: [" + d + "], Batch: [" + b + "], Section: [" + s + "]");
        return timetableRepository.findByDepartmentAndBatchAndSectionAndApprovedTrue(d, b, s);
    }

    @PostMapping
    public ResponseEntity<TimetableEntry> createEntry(@RequestBody TimetableEntry entry) {
        entry.setApproved(false);
        TimetableEntry saved = timetableRepository.save(entry);
        activityLogRepository.save(new ActivityLog(
            entry.getOwnerName() + " proposed a timetable entry for " + entry.getCourseName(),
            "TIMETABLE_PROPOSED"
        ));
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadTimetable(
            @RequestParam String url,
            @RequestParam String ownerName,
            @RequestParam String ownerEmail) {
        
        try {
            // Fetch CSV data
            String csvData = restTemplate.getForObject(url, String.class);
            if (csvData == null) {
                return ResponseEntity.badRequest().body("Failed to fetch data from URL.");
            }
            
            System.out.println("DEBUG: Raw data preview (first 200 chars): " + 
                (csvData.length() > 200 ? csvData.substring(0, 200) : csvData));

            // Remove all existing approved entries
            List<TimetableEntry> existing = timetableRepository.findByApprovedTrue();
            timetableRepository.deleteAll(existing);

            List<TimetableEntry> entries = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new StringReader(csvData));
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false; // Skip header
                    continue;
                }
                
                // Improved regex to split CSV by comma while ignoring commas inside double quotes
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (parts.length >= 7) {
                    TimetableEntry entry = new TimetableEntry();
                    
                    // Helper to clean quotes and trim
                    java.util.function.Function<String, String> clean = (s) -> {
                        String res = s.trim();
                        if (res.startsWith("\"") && res.endsWith("\"")) {
                            res = res.substring(1, res.length() - 1).trim();
                        }
                        return res;
                    };

                    String dept = clean.apply(parts[0]);
                    // Normalize Department: Remove common prefixes like "BS " or "MS "
                    if (dept.toUpperCase().startsWith("BS ")) dept = dept.substring(3).trim();
                    if (dept.toUpperCase().startsWith("MS ")) dept = dept.substring(3).trim();
                    
                    entry.setDepartment(dept);
                    
                    String rawBatch = clean.apply(parts[1]);
                    // Normalize batch to 2 digits (e.g. 2024 -> 24)
                    entry.setBatch(rawBatch.length() > 2 ? rawBatch.substring(rawBatch.length() - 2) : rawBatch);
                    entry.setSection(clean.apply(parts[2]));
                    entry.setDayOfWeek(clean.apply(parts[3]));
                    
                    String timeSlot = clean.apply(parts[4]);
                    String[] times = timeSlot.split("-");
                    entry.setStartTime(times[0].trim());
                    entry.setEndTime(times.length > 1 ? times[1].trim() : "");
                    
                    entry.setCourseName(clean.apply(parts[5]));
                    entry.setRoomNumber(parts.length >= 7 ? clean.apply(parts[6]) : "");
                    entry.setInstructorName(parts.length >= 8 ? clean.apply(parts[7]) : "TBD");
                    
                    entry.setOwnerName(ownerName);
                    entry.setOwnerEmail(ownerEmail);
                    entry.setApproved(true);
                    
                    if (entries.isEmpty()) {
                        System.out.println("DEBUG: First parsed entry - Dept: [" + entry.getDepartment() + "], Batch: [" + entry.getBatch() + "], Section: [" + entry.getSection() + "]");
                    }
                    System.out.println("PARSING LINE: " + line);
                    System.out.println("RESULTING ENTRY: " + entry.toString());
                    
                    entries.add(entry);
                } else {
                    if (!line.trim().isEmpty()) {
                        System.out.println("SKIPPING INVALID LINE (parts < 7): " + line);
                    }
                }
            }
            
            System.out.println("DEBUG: Successfully parsed and saved " + entries.size() + " timetable entries.");
            
            timetableRepository.saveAll(entries);
            activityLogRepository.save(new ActivityLog(
                ownerName + " uploaded a new global timetable with " + entries.size() + " entries.",
                "TIMETABLE_UPLOADED"
            ));
            
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error parsing CSV: " + e.getMessage());
        }
    }

    @GetMapping("/pending")
    public List<TimetableEntry> getPendingEntries() {
        return timetableRepository.findByApprovedFalse();
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<TimetableEntry> approveEntry(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return timetableRepository.findById(id).map(entry -> {
            entry.setApproved(true);
            entry.setModerationReason(reason);
            TimetableEntry saved = timetableRepository.save(entry);
            activityLogRepository.save(new ActivityLog("TimetableEntry #" + id + " approved.", "TIMETABLE_APPROVED"));
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/flagged/count")
    public long getFlaggedCount() {
        return timetableRepository.countByFlaggedTrue();
    }

    @GetMapping("/flagged")
    public List<TimetableEntry> getFlaggedEntries() {
        return timetableRepository.findByFlaggedTrue();
    }

    @PutMapping("/{id}/flag")
    public ResponseEntity<TimetableEntry> flagEntry(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return timetableRepository.findById(id).map(entry -> {
            entry.setFlagged(true);
            entry.setModerationReason(reason);
            TimetableEntry saved = timetableRepository.save(entry);
            activityLogRepository.save(new ActivityLog("TimetableEntry #" + id + " flagged.", "TIMETABLE_FLAGGED"));
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<TimetableEntry> resolveEntry(@PathVariable Long id) {
        return timetableRepository.findById(id).map(entry -> {
            entry.setFlagged(false);
            entry.setModerationReason(null);
            TimetableEntry saved = timetableRepository.save(entry);
            activityLogRepository.save(new ActivityLog("TimetableEntry #" + id + " resolved.", "FLAG_RESOLVED"));
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/count/active")
    public long getActiveCount() {
        return timetableRepository.countByApprovedTrue();
    }

    @GetMapping("/search")
    public List<TimetableEntry> searchEntries(@RequestParam String query) {
        return timetableRepository.findByCourseNameContainingIgnoreCaseAndApprovedTrue(query);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return timetableRepository.findById(id).map(entry -> {
            activityLogRepository.save(new ActivityLog("TimetableEntry #" + id + " deleted.", "CONTENT_DELETED"));
            timetableRepository.deleteById(id);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
