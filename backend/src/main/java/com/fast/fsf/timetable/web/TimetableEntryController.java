package com.fast.fsf.timetable.web;

import com.fast.fsf.shared.model.ActivityLog;
import com.fast.fsf.timetable.domain.TimetableEntry;
import com.fast.fsf.shared.persistence.ActivityLogRepository;
import com.fast.fsf.timetable.persistence.TimetableEntryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;

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
            String csvData = restTemplate.getForObject(url, String.class);
            if (csvData == null) return ResponseEntity.badRequest().body("Failed to fetch data from URL.");
            return processCSV(new BufferedReader(new StringReader(csvData)), ownerName, ownerEmail);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/upload-file")
    public ResponseEntity<?> uploadTimetableFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam String ownerName,
            @RequestParam String ownerEmail) {
        try {
            String fileName = file.getOriginalFilename();
            if (fileName != null && (fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
                return processExcel(file.getInputStream(), ownerName, ownerEmail);
            }
            return processCSV(new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)), ownerName, ownerEmail);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error parsing file: " + e.getMessage());
        }
    }

    private ResponseEntity<?> processExcel(InputStream is, String ownerName, String ownerEmail) throws Exception {
        Workbook workbook = WorkbookFactory.create(is);
        Sheet sheet = workbook.getSheetAt(0);
        DataFormatter formatter = new DataFormatter();
        
        System.out.println("--- DEBUG: STARTING SPECIALIZED FAST GRID PARSE ---");

        // 1. Detect Time Slots in the top rows (usually Row 2 or 3)
        Map<Integer, String> timeSlots = new HashMap<>();
        for (int r = 0; r < 10; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < 50; c++) {
                String val = formatter.formatCellValue(row.getCell(c)).trim();
                // Match 08:30-10:00 or similar
                if (val.matches("\\d{2}:\\d{2}-\\d{2}:\\d{2}") || val.matches("\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2}")) {
                    timeSlots.put(c, val);
                }
            }
        }
        
        if (timeSlots.isEmpty()) {
            System.out.println("DEBUG: No time slots detected in header.");
            workbook.close();
            return ResponseEntity.badRequest().body("Could not detect time slots (e.g. 08:30-10:00) in the first 10 rows.");
        }

        List<TimetableEntry> entries = new ArrayList<>();
        String currentDay = "";
        List<String> dayNamesShort = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        List<String> dayNamesFull = Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");

        // 2. Iterate through rows (Data usually starts around Row 5)
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            // Update current day if Column 0 is a day name
            String dayCol = formatter.formatCellValue(row.getCell(0)).trim();
            if (!dayCol.isEmpty()) {
                String tempDay = dayCol.split("\\s+")[0].trim(); // Get "Mon" from "Mon Classes" etc.
                for (int i = 0; i < dayNamesShort.size(); i++) {
                    if (tempDay.equalsIgnoreCase(dayNamesShort.get(i)) || tempDay.toUpperCase().startsWith(dayNamesShort.get(i).toUpperCase())) {
                        currentDay = dayNamesFull.get(i);
                        break;
                    }
                }
            }

            if (currentDay.isEmpty()) continue;

            // Room Number usually in Column 1
            String room = formatter.formatCellValue(row.getCell(1)).trim();
            if (room.isEmpty()) continue;
            if (room.equalsIgnoreCase("Room") || room.equalsIgnoreCase("Periods")) continue;

            // 3. Scan each detected time slot column in this row
            for (Map.Entry<Integer, String> slot : timeSlots.entrySet()) {
                int col = slot.getKey();
                String time = slot.getValue();
                String cellContent = formatter.formatCellValue(row.getCell(col)).trim();

                if (!cellContent.isEmpty()) {
                    // Specialized Regex for FAST Timetable: Course (Dept-SemSection): Instructor
                    // Example: OOP (BSE-2A): Hina I
                    Pattern p = Pattern.compile("(.+)\\s*\\((.+)-(\\d)([A-Z])\\)\\s*:\\s*(.+)");
                    Matcher m = p.matcher(cellContent);
                    
                    if (m.find()) {
                        TimetableEntry entry = new TimetableEntry();
                        entry.setCourseName(m.group(1).trim());
                        
                        String rawDept = m.group(2).trim();
                        String dept = "CS";
                        if (rawDept.contains("SE")) dept = "SE";
                        else if (rawDept.contains("AI")) dept = "AI";
                        else if (rawDept.contains("DS")) dept = "DS";
                        else if (rawDept.contains("CYS")) dept = "CYS";
                        entry.setDepartment(dept);
                        
                        // Calculate Batch Year from Semester (e.g. Sem 2 in Spring 2026 -> Batch 2025)
                        try {
                            int sem = Integer.parseInt(m.group(3));
                            int batchYear = 2026 - (sem + 1) / 2;
                            entry.setBatch(String.valueOf(batchYear).substring(2));
                        } catch (Exception e) {
                            entry.setBatch("24"); // Fallback
                        }
                        
                        entry.setSection(m.group(4));
                        entry.setInstructorName(m.group(5).trim());
                        entry.setDayOfWeek(currentDay);
                        entry.setRoomNumber(room);
                        
                        String[] times = time.split("-");
                        entry.setStartTime(times[0].trim());
                        entry.setEndTime(times.length > 1 ? times[1].trim() : "");
                        
                        entry.setOwnerName(ownerName);
                        entry.setOwnerEmail(ownerEmail);
                        entry.setApproved(true);
                        entries.add(entry);
                    }
                }
            }
        }
        
        System.out.println("--- DEBUG: GRID PARSE COMPLETE. FOUND " + entries.size() + " VALID ENTRIES ---");

        if (!entries.isEmpty()) {
            timetableRepository.deleteAll(timetableRepository.findByApprovedTrue());
            timetableRepository.saveAll(entries);
            activityLogRepository.save(new ActivityLog(
                ownerName + " uploaded the FAST Spring 2026 timetable with " + entries.size() + " entries parsed.",
                "TIMETABLE_UPLOADED"
            ));
        }
        
        workbook.close();
        
        if (entries.isEmpty()) {
            return ResponseEntity.badRequest().body("No valid classes found in the grid. Please ensure it follows the format: Course (Dept-SemSection): Instructor");
        }
        
        return ResponseEntity.ok(entries);
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        if (row == null) return true;
        for (int i = 0; i < 8; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && !formatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private ResponseEntity<?> processCSV(BufferedReader reader, String ownerName, String ownerEmail) throws Exception {
        // Remove all existing approved entries
        timetableRepository.deleteAll(timetableRepository.findByApprovedTrue());

        List<TimetableEntry> entries = new ArrayList<>();
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
                java.util.function.Function<String, String> clean = (s) -> {
                    String res = s.trim();
                    if (res.startsWith("\"") && res.endsWith("\"")) res = res.substring(1, res.length() - 1).trim();
                    return res;
                };

                String dept = clean.apply(parts[0]);
                if (dept.toUpperCase().startsWith("BS ")) dept = dept.substring(3).trim();
                if (dept.toUpperCase().startsWith("MS ")) dept = dept.substring(3).trim();
                entry.setDepartment(dept);
                
                String rawBatch = clean.apply(parts[1]);
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
                entries.add(entry);
            }
        }
        
        timetableRepository.saveAll(entries);
        activityLogRepository.save(new ActivityLog(
            ownerName + " uploaded a new global timetable with " + entries.size() + " entries.",
            "TIMETABLE_UPLOADED"
        ));
        
        return ResponseEntity.ok(entries);
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
