package com.fast.fsf.reminders.web;

import com.fast.fsf.reminders.domain.Reminder;
import com.fast.fsf.reminders.event.ReminderCreatedEvent;
import com.fast.fsf.reminders.event.ReminderDeletedEvent;
import com.fast.fsf.reminders.factory.ReminderFactory;
import com.fast.fsf.reminders.persistence.ReminderRepository;
import com.fast.fsf.reminders.template.CompleteReminderWorkflow;
import com.fast.fsf.reminders.template.UpdateReminderWorkflow;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

/**
 * ReminderController — Pop Reminder feature.
 * <p>
 * <strong>Design Patterns:</strong>
 * <ul>
 *   <li><strong>Singleton</strong>: This controller is a Spring-managed bean, ensuring a single shared instance (GoF).</li>
 *   <li><strong>Factory Method</strong>: Creation and validation logic is delegated to {@link ReminderFactory}.</li>
 *   <li><strong>Template Method</strong>: Mutation workflows (complete/update) use the {@code AbstractReminderMutationWorkflow} skeleton.</li>
 *   <li><strong>State</strong>: Status transitions are handled via {@code ReminderStatusState} implementations inside workflows.</li>
 *   <li><strong>Observer</strong>: Lifecycle changes trigger {@code ApplicationEvent}s handled by {@code ReminderActivityLogObserver}.</li>
 * </ul>
 * <p>
 * Implements:
 * UC-24 View Reminders and Login Pop-up
 * UC-25 Add a Reminder
 * UC-26 Manage Own Reminders (mark completed / edit / delete)
 */
@RestController
@RequestMapping("/api/reminders")
@CrossOrigin(origins = "http://localhost:5173")
public class ReminderController {

    private final ReminderRepository reminderRepository;
    private final ReminderFactory reminderFactory;
    private final CompleteReminderWorkflow completeWorkflow;
    private final UpdateReminderWorkflow updateWorkflow;
    private final ApplicationEventPublisher eventPublisher;

    public ReminderController(ReminderRepository reminderRepository,
                              ReminderFactory reminderFactory,
                              CompleteReminderWorkflow completeWorkflow,
                              UpdateReminderWorkflow updateWorkflow,
                              ApplicationEventPublisher eventPublisher) {
        this.reminderRepository = reminderRepository;
        this.reminderFactory = reminderFactory;
        this.completeWorkflow = completeWorkflow;
        this.updateWorkflow = updateWorkflow;
        this.eventPublisher = eventPublisher;
    }

    /**
     * UC-26: Returns every reminder for the student, ordered so that
     * PENDING entries appear first and COMPLETED follow.
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
     * UC-25: Add a new reminder. Uses {@link ReminderFactory} for validation and normalisation.
     */
    @PostMapping
    public ResponseEntity<?> addReminder(@RequestBody Reminder reminder) {
        try {
            Reminder prepared = reminderFactory.createReminder(reminder);
            Reminder saved = reminderRepository.save(prepared);
            eventPublisher.publishEvent(new ReminderCreatedEvent(this, saved));
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * UC-26: Edit an existing reminder. Uses {@link UpdateReminderWorkflow} (Template Method).
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateReminder(@PathVariable Long id, @RequestBody Reminder updated) {
        return reminderRepository.findById(id)
                .map(existing -> {
                    if (!existing.getStudentEmail().equalsIgnoreCase(updated.getStudentEmail())) {
                        return ResponseEntity.status(403).body("Not allowed to edit this reminder");
                    }
                    updateWorkflow.setUpdateSource(updated);
                    return updateWorkflow.execute(id);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * UC-26 typical: Mark as Completed. Uses {@link CompleteReminderWorkflow} (Template Method).
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeReminder(@PathVariable Long id) {
        return completeWorkflow.execute(id);
    }

    /**
     * UC-26 alt 2: Delete a reminder permanently.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReminder(@PathVariable Long id) {
        if (reminderRepository.existsById(id)) {
            reminderRepository.deleteById(id);
            eventPublisher.publishEvent(new ReminderDeletedEvent(this, id));
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}

