package com.fast.fsf.events.web;

import com.fast.fsf.shared.model.ActivityLog;
import com.fast.fsf.events.domain.CampusEvent;
import com.fast.fsf.shared.persistence.ActivityLogRepository;
import com.fast.fsf.events.persistence.CampusEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CampusEventController (UC-21, 22, 23)
 * 
 * Manages Campus Events and Semester Plan.
 */
@RestController
@RequestMapping("/api/events")
@CrossOrigin(originPatterns = {"http://localhost:*"})
public class CampusEventController {

    private final CampusEventRepository eventRepository;
    private final ActivityLogRepository activityLogRepository;

    public CampusEventController(CampusEventRepository eventRepository, ActivityLogRepository activityLogRepository) {
        this.eventRepository = eventRepository;
        this.activityLogRepository = activityLogRepository;
    }

    // Organizers our parser stamps on calendar-imported entries.
    // Used to keep imported items out of the Event Board feed.
    private static final java.util.List<String> CALENDAR_ORGANIZERS =
            java.util.Arrays.asList("Academic Office", "Administration");

    // UC-22: Event Board feed.
    // Returns every approved user-posted event. An event with semesterPlan=true
    // also appears in the Semester Plan endpoint, so it shows in both lists.
    @GetMapping
    public List<CampusEvent> getAllApprovedEvents() {
        return eventRepository.findByApprovedTrueAndOrganizerNotInOrderByEventDateAsc(CALENDAR_ORGANIZERS);
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
    //
    // Supports two layouts:
    //   1. FAST Academic Calendar  -> Week No. | From | To | Quizzes | Sessional Exams
    //      (followed by a "Holidays" section: Name | Day & Date)
    //   2. FAST Exam Schedule      -> Date | ... | Course | Time | Venue
    @PostMapping("/upload-plan")
    public ResponseEntity<?> uploadPlan(
            @RequestParam("file") MultipartFile file,
            @RequestParam String ownerEmail) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            List<CampusEvent> entries = new ArrayList<>();

            int defaultYear = detectYear(sheet, formatter);
            int headerRow = findCalendarHeaderRow(sheet, formatter);

            System.out.println("--- DEBUG: STARTING PLAN PARSE (year=" + defaultYear
                    + ", calendarHeaderRow=" + headerRow + ") ---");

            if (headerRow >= 0) {
                parseAcademicCalendar(sheet, formatter, headerRow, defaultYear, ownerEmail, entries);
            } else {
                parseExamSchedule(sheet, formatter, defaultYear, ownerEmail, entries);
            }

            System.out.println("--- DEBUG: PLAN PARSE COMPLETE. FOUND: " + entries.size() + " ---");

            if (!entries.isEmpty()) {
                // Wipe only previously-imported calendar entries — user-posted
                // semester-plan items keep their place.
                eventRepository.deleteAll(eventRepository.findBySemesterPlanTrueAndOrganizerIn(
                        java.util.Arrays.asList("Academic Office", "Administration")));
                eventRepository.saveAll(entries);
                activityLogRepository.save(new ActivityLog(
                        "Semester Plan uploaded with " + entries.size() + " items.",
                        "PLAN_UPLOADED"));
            }

            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error parsing plan: " + e.getMessage());
        }
    }

    // ---------- Helpers for the upload parser ----------

    private static final Pattern SEMESTER_YEAR = Pattern.compile("(?i)(?:spring|fall|summer|autumn|winter)\\s*(\\d{4})");
    private static final Pattern HOLIDAY_DATE = Pattern.compile(
            "(\\d{1,2})\\s*(?:[-\u2013]\\s*\\d{0,2})?\\s*([A-Za-z]+)\\s*[,\\s]\\s*(\\d{4})");
    private static final Pattern HOLIDAY_DATE_MONTH_FIRST = Pattern.compile(
            "([A-Za-z]+)\\s*(\\d{1,2})\\s*(?:[-\u2013]\\s*\\d{0,2})?\\s*[,\\s]\\s*(\\d{4})");
    private static final Pattern WEEK_NUMBER = Pattern.compile("^\\s*\\d+[A-Za-z]?(?:\\s*-\\s*\\d+[A-Za-z]?)?\\s*$");

    private int detectYear(Sheet sheet, DataFormatter formatter) {
        int scan = Math.min(10, sheet.getLastRowNum());
        for (int r = 0; r <= scan; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (Cell c : row) {
                Matcher m = SEMESTER_YEAR.matcher(formatter.formatCellValue(c));
                if (m.find()) return Integer.parseInt(m.group(1));
            }
        }
        return LocalDate.now().getYear();
    }

    private int findCalendarHeaderRow(Sheet sheet, DataFormatter formatter) {
        int scan = Math.min(15, sheet.getLastRowNum());
        for (int r = 0; r <= scan; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String c0 = formatter.formatCellValue(row.getCell(0)).trim().toLowerCase(Locale.ROOT);
            String c1 = formatter.formatCellValue(row.getCell(1)).trim().toLowerCase(Locale.ROOT);
            if (c0.startsWith("week no") && (c1.contains("from") || c1.contains("date"))) return r;
        }
        return -1;
    }

    private void parseAcademicCalendar(Sheet sheet, DataFormatter formatter, int headerRow,
                                       int defaultYear, String ownerEmail, List<CampusEvent> out) {
        int last = sheet.getLastRowNum();
        boolean inHolidays = false;

        for (int r = headerRow + 1; r <= last; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String col0 = formatter.formatCellValue(row.getCell(0)).trim();
            String col1 = formatter.formatCellValue(row.getCell(1)).trim();
            String col2 = formatter.formatCellValue(row.getCell(2)).trim();
            String col3 = formatter.formatCellValue(row.getCell(3)).trim();
            String col4 = formatter.formatCellValue(row.getCell(4)).trim();

            if (col0.isEmpty() && col1.isEmpty() && col2.isEmpty() && col3.isEmpty() && col4.isEmpty()) continue;

            if (col0.toLowerCase(Locale.ROOT).startsWith("holiday")) {
                inHolidays = true;
                continue;
            }

            if (!inHolidays) {
                if (!WEEK_NUMBER.matcher(col0).matches()) continue;

                boolean hasQuiz = !col3.isEmpty();
                boolean hasSessional = !col4.isEmpty();
                if (!hasQuiz && !hasSessional) continue;

                LocalDate fromDate = parseShortDate(col1, defaultYear);
                if (fromDate == null) {
                    System.err.println("DEBUG: Skipping week row " + r + " - cannot parse date '" + col1 + "'");
                    continue;
                }

                String title;
                if (hasQuiz && hasSessional) title = col3 + " / " + col4;
                else title = hasQuiz ? col3 : col4;

                String desc = "Week " + col0 + " (" + col1 + (col2.isEmpty() ? "" : " to " + col2) + ")";
                addEntry(out, title, desc, fromDate, "See Dept Notice Board",
                        "Academic Office", "ACADEMIC", ownerEmail);
                System.out.println("DEBUG: Parsed academic week row " + r + ": " + title + " on " + fromDate);
            } else {
                if (col0.startsWith("*") || col0.toLowerCase(Locale.ROOT).contains("subject to appearance")) continue;
                if (col1.isEmpty()) continue;

                LocalDate holidayDate = parseHolidayDate(col1, defaultYear);
                if (holidayDate == null) {
                    System.err.println("DEBUG: Skipping holiday row " + r + " - cannot parse '" + col1 + "'");
                    continue;
                }
                addEntry(out, col0.replace("*", "").trim(), col1, holidayDate,
                        "Campus-wide", "Administration", "HOLIDAY", ownerEmail);
                System.out.println("DEBUG: Parsed holiday row " + r + ": " + col0 + " on " + holidayDate);
            }
        }
    }

    private void parseExamSchedule(Sheet sheet, DataFormatter formatter,
                                   int defaultYear, String ownerEmail, List<CampusEvent> out) {
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String dateStr = formatter.formatCellValue(row.getCell(0)).trim();
            String courseStr = formatter.formatCellValue(row.getCell(2)).trim();
            String timeStr = formatter.formatCellValue(row.getCell(3)).trim();
            String venue = formatter.formatCellValue(row.getCell(4)).trim();
            if (dateStr.isEmpty() || courseStr.isEmpty()) continue;

            LocalDate eventDate = null;
            Cell dateCell = row.getCell(0);
            if (dateCell != null && dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                eventDate = dateCell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else {
                eventDate = parseShortDate(dateStr, defaultYear);
            }
            if (eventDate == null) {
                System.err.println("DEBUG: Skipping exam row " + r + " - cannot parse date '" + dateStr + "'");
                continue;
            }

            addEntry(out, courseStr, "Exam/Test - " + timeStr, eventDate,
                    venue.isEmpty() ? "See Dept Notice Board" : venue,
                    "Academic Office", "ACADEMIC", ownerEmail);
        }
    }

    private LocalDate parseShortDate(String raw, int defaultYear) {
        if (raw == null) return null;
        String cleaned = raw.replaceAll("\\(.*?\\)", "").trim();
        if (cleaned.isEmpty()) return null;

        if (cleaned.matches(".*\\d{4}\\s*$")) {
            for (String pat : new String[] {"d-MMM-yyyy", "dd-MMM-yyyy", "d MMM yyyy", "dd MMM yyyy"}) {
                try { return LocalDate.parse(cleaned, DateTimeFormatter.ofPattern(pat, Locale.ENGLISH)); }
                catch (Exception ignored) {}
            }
        }
        for (String pat : new String[] {"d-MMM", "dd-MMM", "d MMM", "dd MMM"}) {
            try {
                return LocalDate.parse(cleaned + "-" + defaultYear,
                        DateTimeFormatter.ofPattern(pat + "-yyyy", Locale.ENGLISH));
            } catch (Exception ignored) {
                try {
                    return LocalDate.parse(cleaned + " " + defaultYear,
                            DateTimeFormatter.ofPattern(pat + " yyyy", Locale.ENGLISH));
                } catch (Exception ignored2) {}
            }
        }
        return null;
    }

    private LocalDate parseHolidayDate(String raw, int defaultYear) {
        if (raw == null) return null;
        String cleaned = raw.replace('\u2013', '-').replaceAll("\\s+", " ").trim();

        Matcher m = HOLIDAY_DATE.matcher(cleaned);
        if (m.find()) {
            try {
                int day = Integer.parseInt(m.group(1));
                String month = m.group(2);
                int year = Integer.parseInt(m.group(3));
                return LocalDate.parse(day + "-" + month + "-" + year,
                        DateTimeFormatter.ofPattern("d-MMMM-yyyy", Locale.ENGLISH));
            } catch (Exception e) {
                try {
                    int day = Integer.parseInt(m.group(1));
                    String month = m.group(2);
                    int year = Integer.parseInt(m.group(3));
                    return LocalDate.parse(day + "-" + month + "-" + year,
                            DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH));
                } catch (Exception ignored) {}
            }
        }

        Matcher m2 = HOLIDAY_DATE_MONTH_FIRST.matcher(cleaned);
        if (m2.find()) {
            try {
                String month = m2.group(1);
                int day = Integer.parseInt(m2.group(2));
                int year = Integer.parseInt(m2.group(3));
                return LocalDate.parse(day + "-" + month + "-" + year,
                        DateTimeFormatter.ofPattern("d-MMMM-yyyy", Locale.ENGLISH));
            } catch (Exception e) {
                try {
                    String month = m2.group(1);
                    int day = Integer.parseInt(m2.group(2));
                    int year = Integer.parseInt(m2.group(3));
                    return LocalDate.parse(day + "-" + month + "-" + year,
                            DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH));
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private void addEntry(List<CampusEvent> out, String title, String desc, LocalDate date,
                          String venue, String organizer, String category, String ownerEmail) {
        CampusEvent ev = new CampusEvent();
        ev.setTitle(title);
        ev.setDescription(desc);
        ev.setEventDate(date);
        ev.setVenue(venue);
        ev.setOrganizer(organizer);
        ev.setCategory(category);
        ev.setSemesterPlan(true);
        ev.setOwnerEmail(ownerEmail);
        ev.setApproved(true);
        out.add(ev);
    }

    // Admin: Get pending events for approval
    @GetMapping("/pending")
    public List<CampusEvent> getPendingEvents() {
        return eventRepository.findByApprovedFalse();
    }

    // Edit an existing event. Owner or Admin only.
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id,
                                         @RequestBody CampusEvent updated,
                                         @RequestParam(required = false) String requesterEmail) {
        return eventRepository.findById(id).<ResponseEntity<?>>map(existing -> {
            String requester = requesterEmail == null ? "" : requesterEmail;
            boolean isOwner = existing.getOwnerEmail() != null && existing.getOwnerEmail().equalsIgnoreCase(requester);
            boolean isAdmin = requester.toLowerCase().contains("admin");
            if (!isOwner && !isAdmin) {
                return ResponseEntity.status(403).body("Only the owner or an admin can edit this event.");
            }

            if (updated.getTitle() != null)       existing.setTitle(updated.getTitle());
            if (updated.getDescription() != null) existing.setDescription(updated.getDescription());
            if (updated.getEventDate() != null)   existing.setEventDate(updated.getEventDate());
            if (updated.getVenue() != null)       existing.setVenue(updated.getVenue());
            if (updated.getOrganizer() != null)   existing.setOrganizer(updated.getOrganizer());
            if (updated.getCategory() != null)    existing.setCategory(updated.getCategory());
            existing.setSemesterPlan(updated.getSemesterPlan());

            CampusEvent saved = eventRepository.save(existing);
            activityLogRepository.save(new ActivityLog(
                    "Campus event edited: " + existing.getTitle(), "EVENT_EDITED"));
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
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
