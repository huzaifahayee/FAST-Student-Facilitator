package com.fast.fsf.controller;

import com.fast.fsf.model.ActivityLog;
import com.fast.fsf.model.CampusEvent;
import com.fast.fsf.repository.ActivityLogRepository;
import com.fast.fsf.repository.CampusEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * CampusEventController (UC-21, 22, 23)
 * 
 * Manages Campus Events and Semester Plan.
 */
@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:5173")
public class CampusEventController {

    private final CampusEventRepository eventRepository;
    private final ActivityLogRepository activityLogRepository;

    public CampusEventController(CampusEventRepository eventRepository, ActivityLogRepository activityLogRepository) {
        this.eventRepository = eventRepository;
        this.activityLogRepository = activityLogRepository;
    }

    // UC-22: View all approved events
    @GetMapping
    public List<CampusEvent> getAllApprovedEvents() {
        return eventRepository.findByApprovedTrueAndSemesterPlanFalseOrderByEventDateAsc();
    }

    // UC-23: View Semester Plan
    @GetMapping("/semester-plan")
    public List<CampusEvent> getSemesterPlan() {
        return eventRepository.findByApprovedTrueAndSemesterPlanTrueOrderByEventDateAsc();
    }

    // UC-21: Post a new event
    @PostMapping
    public ResponseEntity<CampusEvent> postEvent(@RequestBody CampusEvent event) {
        System.out.println("DEBUG: Receiving Event Post Request - Title: " + event.getTitle());
        try {
            // Keep the incoming 'approved' status (the frontend sets it to true if admin)
            CampusEvent saved = eventRepository.save(event);
            
            activityLogRepository.save(new ActivityLog(
                "Campus event added: " + event.getTitle() + (event.isApproved() ? " (Auto-approved)" : " (Pending)"),
                event.isApproved() ? "EVENT_ADDED" : "EVENT_PROPOSED"
            ));
            
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to save event - " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // NEW: Upload Semester Plan (Excel Parser)
    @PostMapping("/upload-plan")
    public ResponseEntity<?> uploadPlan(
            @RequestParam("file") MultipartFile file,
            @RequestParam String ownerEmail) {
        try {
            Workbook workbook = WorkbookFactory.create(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            List<CampusEvent> entries = new ArrayList<>();

            // FAST Exam Schedule Parser logic
            System.out.println("--- DEBUG: STARTING PLAN PARSE ---");
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                // Log all cells in the row for debugging
                System.out.print("DEBUG: Row " + r + ": ");
                for (int c = 0; c < 10; c++) {
                    System.out.print("[" + formatter.formatCellValue(row.getCell(c)) + "] ");
                }
                System.out.println();

                String dateStr = formatter.formatCellValue(row.getCell(0)).trim();
                String courseStr = formatter.formatCellValue(row.getCell(2)).trim();
                String timeStr = formatter.formatCellValue(row.getCell(3)).trim();
                String venue = formatter.formatCellValue(row.getCell(4)).trim();

                if (dateStr.isEmpty() || courseStr.isEmpty()) continue;

                try {
                    LocalDate eventDate;
                    Cell dateCell = row.getCell(0);
                    if (dateCell != null && dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                        eventDate = dateCell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    } else {
                        // Try various formats
                        String cleanDate = dateStr.replaceAll("[^a-zA-Z0-9-]", "-");
                        try {
                            eventDate = LocalDate.parse(cleanDate, DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
                        } catch (Exception e1) {
                            eventDate = LocalDate.parse(cleanDate, DateTimeFormatter.ofPattern("d-MMM-yyyy"));
                        }
                    }

                    CampusEvent event = new CampusEvent();
                    event.setTitle(courseStr);
                    event.setDescription("Exam/Test - " + timeStr);
                    event.setEventDate(eventDate);
                    event.setVenue(venue.isEmpty() ? "See Dept Notice Board" : venue);
                    event.setOrganizer("Academic Office");
                    event.setCategory("ACADEMIC");
                    event.setSemesterPlan(true);
                    event.setOwnerEmail(ownerEmail);
                    event.setApproved(true);
                    entries.add(event);
                    System.out.println("DEBUG: Successfully parsed " + courseStr + " on " + eventDate);
                } catch (Exception e) {
                    System.err.println("DEBUG: Skipping row " + r + " due to parse error: " + e.getMessage());
                }
            }
            System.out.println("--- DEBUG: PLAN PARSE COMPLETE. FOUND: " + entries.size() + " ---");

            if (!entries.isEmpty()) {
                // Clear old plan items before saving new ones
                eventRepository.deleteAll(eventRepository.findByApprovedTrueAndSemesterPlanTrueOrderByEventDateAsc());
                eventRepository.saveAll(entries);
                activityLogRepository.save(new ActivityLog(
                    "Semester Plan uploaded with " + entries.size() + " items.",
                    "PLAN_UPLOADED"
                ));
            }

            workbook.close();
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error parsing plan: " + e.getMessage());
        }
    }

    // Admin: Get pending events for approval
    @GetMapping("/pending")
    public List<CampusEvent> getPendingEvents() {
        return eventRepository.findByApprovedFalse();
    }

    // Admin: Approve event
    @PutMapping("/{id}/approve")
    public ResponseEntity<CampusEvent> approveEvent(@PathVariable Long id) {
        return eventRepository.findById(id).map(event -> {
            event.setApproved(true);
            CampusEvent saved = eventRepository.save(event);
            activityLogRepository.save(new ActivityLog("Campus event approved: " + event.getTitle(), "EVENT_APPROVED"));
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        if (eventRepository.existsById(id)) {
            eventRepository.deleteById(id);
            activityLogRepository.save(new ActivityLog("Campus event deleted: #" + id, "EVENT_DELETED"));
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
