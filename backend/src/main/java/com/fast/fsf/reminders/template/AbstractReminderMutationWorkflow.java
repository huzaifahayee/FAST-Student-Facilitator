package com.fast.fsf.reminders.template;

import com.fast.fsf.reminders.domain.Reminder;
import com.fast.fsf.reminders.persistence.ReminderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;

/**
 * Template Method pattern (GoF): defines the invariant skeleton for “load → mutate → save → notify observers”.
 */
public abstract class AbstractReminderMutationWorkflow {

    protected final ReminderRepository reminderRepository;
    protected final ApplicationEventPublisher eventPublisher;

    protected AbstractReminderMutationWorkflow(ReminderRepository reminderRepository, ApplicationEventPublisher eventPublisher) {
        this.reminderRepository = reminderRepository;
        this.eventPublisher = eventPublisher;
    }

    public final ResponseEntity<?> execute(Long id) {
        return reminderRepository.findById(id)
                .map(reminder -> {
                    mutate(reminder);
                    Reminder saved = reminderRepository.save(reminder);
                    publishEvent(saved);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    protected abstract void mutate(Reminder reminder);
    protected abstract void publishEvent(Reminder saved);
}
