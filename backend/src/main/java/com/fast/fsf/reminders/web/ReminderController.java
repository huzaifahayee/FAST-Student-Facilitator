package com.fast.fsf.reminders.web;

import com.fast.fsf.reminders.domain.Reminder;
import com.fast.fsf.reminders.persistence.ReminderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * ReminderController — Pop Reminder feature.
 *
 * Implements:
 *   UC-24 View Reminders and Login Pop-up
 *   UC-25 Add a Reminder
 *   UC-26 Manage Own Reminders (mark completed / edit / delete)
 */
@RestController
@RequestMapping("/api/reminders")
@CrossOrigin(origins = "http://localhost:5173")
public class ReminderController {

    private static final Set<String> ALLOWED_CATEGORIES =
            Set.of("ASSIGNMENT", "EXAM", "QUIZ", "PROJECT", "OTHER");

    private final ReminderRepository reminderRepository;

    public ReminderController(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    /**
     * UC-26: Returns every reminder for the student, ordered so that
     * PENDING entries (sorted by Date & Time ascending) appear first
     * and COMPLETED entries follow at the bottom.
     */
    @GetMapping
    public List<Reminder> getMyReminders(@RequestParam String email) {
        List<Reminder> all = reminderRepository.findByStudentEmailOrderByReminderTimeAsc(email);
        return all.stream()
                .sorted(Comparator
                        .comparing((Reminder r) -> "COMPLETED".equalsIgnoreCase(r.getStatus()))
                        .thenComparing(Reminder::getReminderTime))
                .toList();
    }

    /**
     * UC-24: Pending reminders only — used by the login pop-up.
     */
    @GetMapping("/pending")
    public List<Reminder> getPendingReminders(@RequestParam String email) {
        return reminderRepository.findByStudentEmailAndStatusOrderByReminderTimeAsc(email, "PENDING");
    }

    /**
     * UC-25: Add a new reminder. Mandatory fields: title, reminderTime, category.
     * Status is forced to PENDING regardless of input.
     */
    @PostMapping
    public ResponseEntity<?> addReminder(@RequestBody Reminder reminder) {
        String validation = validate(reminder);
        if (validation != null) {
            return ResponseEntity.badRequest().body(validation);
        }
        reminder.setId(null);
        reminder.setStatus("PENDING");
        reminder.setCategory(reminder.getCategory().toUpperCase());
        Reminder saved = reminderRepository.save(reminder);
        return ResponseEntity.ok(saved);
    }

    /**
     * UC-26 alt 1: Edit an existing reminder (title / category / reminderTime).
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateReminder(@PathVariable Long id, @RequestBody Reminder updated) {
        return reminderRepository.findById(id)
                .<ResponseEntity<?>>map(existing -> {
                    if (!existing.getStudentEmail().equalsIgnoreCase(updated.getStudentEmail())) {
                        return ResponseEntity.status(403).body("Not allowed to edit this reminder");
                    }
                    String validation = validate(updated);
                    if (validation != null) {
                        return ResponseEntity.badRequest().body(validation);
                    }
                    existing.setTitle(updated.getTitle());
                    existing.setReminderTime(updated.getReminderTime());
                    existing.setCategory(updated.getCategory().toUpperCase());
                    return ResponseEntity.ok(reminderRepository.save(existing));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * UC-26 typical: Mark a pending reminder as Completed.
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeReminder(@PathVariable Long id) {
        return reminderRepository.findById(id)
                .<ResponseEntity<?>>map(r -> {
                    r.setStatus("COMPLETED");
                    return ResponseEntity.ok(reminderRepository.save(r));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * UC-26 alt 2: Delete a reminder permanently.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReminder(@PathVariable Long id) {
        if (reminderRepository.existsById(id)) {
            reminderRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    private String validate(Reminder r) {
        if (r.getTitle() == null || r.getTitle().trim().isEmpty()) {
            return "Title is required";
        }
        if (r.getReminderTime() == null) {
            return "Date & Time is required";
        }
        if (r.getCategory() == null || r.getCategory().trim().isEmpty()) {
            return "Category is required";
        }
        if (!ALLOWED_CATEGORIES.contains(r.getCategory().toUpperCase())) {
            return "Category must be one of: Assignment, Exam, Quiz, Project, Other";
        }
        if (r.getStudentEmail() == null || r.getStudentEmail().trim().isEmpty()) {
            return "Student email is required";
        }
        return null;
    }
}
