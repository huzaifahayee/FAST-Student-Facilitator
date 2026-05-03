package com.fast.fsf.events.web;

import com.fast.fsf.events.domain.CampusEvent;
import com.fast.fsf.events.event.*;
import com.fast.fsf.events.factory.EventFactory;
import com.fast.fsf.events.persistence.CampusEventRepository;
import com.fast.fsf.events.state.ApprovedEventState;
import com.fast.fsf.events.template.AbstractPlanParser;
import com.fast.fsf.events.template.AcademicCalendarParser;
import com.fast.fsf.events.template.ExamScheduleParser;
import org.apache.poi.ss.usermodel.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CampusEventController — Campus Event Board & Semester Plan feature.
 * <p>
 * <strong>Design Patterns:</strong>
 * <ul>
 *   <li><strong>Singleton</strong>: Managed by Spring as a shared bean instance.</li>
 *   <li><strong>Factory Method</strong>: Centralises construction via {@link EventFactory}.</li>
 *   <li><strong>Template Method</strong>: Logic for parsing Academic Calendar vs Exam Schedule follows a fixed skeleton in {@link AbstractPlanParser}.</li>
 *   <li><strong>State</strong>: Lifecycle (approved status) transitions use {@code EventModerationState}.</li>
 *   <li><strong>Observer</strong>: Reacts to events (create/approve/delete/upload) via {@code ApplicationEvent}s.</li>
 *   <li><strong>Adapter</strong>: Delegated activity logging through {@code EventActivityLogAdapter}.</li>
 * </ul>
 * <p>
 * Implements:
 * UC-21 Post a new event
 * UC-22 Event Board feed
 * UC-23 View Semester Plan
 */
@RestController
@RequestMapping("/api/events")
@CrossOrigin(originPatterns = {"http://localhost:*"})
public class CampusEventController {

    private final CampusEventRepository eventRepository;
    private final EventFactory eventFactory;
    private final ApplicationEventPublisher eventPublisher;

    public CampusEventController(CampusEventRepository eventRepository,
                                 EventFactory eventFactory,
                                 ApplicationEventPublisher eventPublisher) {
        this.eventRepository = eventRepository;
        this.eventFactory = eventFactory;
        this.eventPublisher = eventPublisher;
    }

    private static final List<String> CALENDAR_ORGANIZERS =
            java.util.Arrays.asList("Academic Office", "Administration");

    @GetMapping
    public List<CampusEvent> getAllApprovedEvents() {
        return eventRepository.findByApprovedTrueAndOrganizerNotInOrderByEventDateAsc(CALENDAR_ORGANIZERS);
    }

    @GetMapping("/semester-plan")
    public List<CampusEvent> getSemesterPlan() {
        return eventRepository.findByApprovedTrueAndSemesterPlanTrueOrderByEventDateAsc();
    }

    @PostMapping
    public ResponseEntity<CampusEvent> postEvent(@RequestBody CampusEvent event) {
        try {
            CampusEvent prepared = eventFactory.createEvent(event);
            CampusEvent saved = eventRepository.save(prepared);
            eventPublisher.publishEvent(new EventCreatedEvent(this, saved));
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

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

            AbstractPlanParser parser;
            if (headerRow >= 0) {
                parser = new AcademicCalendarParser(eventFactory, headerRow);
            } else {
                parser = new ExamScheduleParser(eventFactory);
            }

            parser.parse(sheet, formatter, defaultYear, ownerEmail, entries);

            if (!entries.isEmpty()) {
                eventRepository.deleteAll(eventRepository.findBySemesterPlanTrueAndOrganizerIn(CALENDAR_ORGANIZERS));
                eventRepository.saveAll(entries);
                eventPublisher.publishEvent(new PlanUploadedEvent(this, entries.size()));
            }

            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error parsing plan: " + e.getMessage());
        }
    }

    private static final Pattern SEMESTER_YEAR = Pattern.compile("(?i)(?:spring|fall|summer|autumn|winter)\\s*(\\d{4})");

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

    @GetMapping("/pending")
    public List<CampusEvent> getPendingEvents() {
        return eventRepository.findByApprovedFalse();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id,
                                         @RequestBody CampusEvent updated,
                                         @RequestParam(required = false) String requesterEmail,
                                         @RequestParam(required = false) String requesterRole) {
        return eventRepository.findById(id).map(existing -> {
            String requester = requesterEmail == null ? "" : requesterEmail.toLowerCase();
            String role = requesterRole == null ? "" : requesterRole.toUpperCase();
            
            boolean isAdmin = requester.contains("admin") || role.equals("ADMIN");
            
            if (!isAdmin) {
                return ResponseEntity.status(403).body("Only an admin can edit events.");
            }

            if (updated.getTitle() != null)       existing.setTitle(updated.getTitle());
            if (updated.getDescription() != null) existing.setDescription(updated.getDescription());
            if (updated.getEventDate() != null)   existing.setEventDate(updated.getEventDate());
            if (updated.getVenue() != null)       existing.setVenue(updated.getVenue());
            if (updated.getOrganizer() != null)   existing.setOrganizer(updated.getOrganizer());
            if (updated.getCategory() != null)    existing.setCategory(updated.getCategory());
            existing.setSemesterPlan(updated.getSemesterPlan());

            CampusEvent saved = eventRepository.save(existing);
            eventPublisher.publishEvent(new EventEditedEvent(this, saved));
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<CampusEvent> approveEvent(@PathVariable Long id) {
        return eventRepository.findById(id).map(event -> {
            new ApprovedEventState().handle(event);
            CampusEvent saved = eventRepository.save(event);
            eventPublisher.publishEvent(new EventApprovedEvent(this, saved));
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        if (eventRepository.existsById(id)) {
            eventRepository.deleteById(id);
            eventPublisher.publishEvent(new EventDeletedEvent(this, id));
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}

