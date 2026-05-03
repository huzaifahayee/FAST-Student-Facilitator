package com.fast.fsf.reminders.observer;

import com.fast.fsf.reminders.adapter.ReminderActivityLogAdapter;
import com.fast.fsf.reminders.event.ReminderCompletedEvent;
import com.fast.fsf.reminders.event.ReminderCreatedEvent;
import com.fast.fsf.reminders.event.ReminderDeletedEvent;
import com.fast.fsf.reminders.event.ReminderUpdatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observer pattern (GoF): reacts to reminder domain events to record activity logs.
 */
@Component
public class ReminderActivityLogObserver {

    private final ReminderActivityLogAdapter activityLogAdapter;

    public ReminderActivityLogObserver(ReminderActivityLogAdapter activityLogAdapter) {
        this.activityLogAdapter = activityLogAdapter;
    }

    @EventListener
    public void onReminderCreated(ReminderCreatedEvent event) {
        var r = event.getReminder();
        activityLogAdapter.record("New reminder created: " + r.getTitle(), "REMINDER_CREATED");
    }

    @EventListener
    public void onReminderUpdated(ReminderUpdatedEvent event) {
        var r = event.getReminder();
        activityLogAdapter.record("Reminder updated: " + r.getTitle(), "REMINDER_UPDATED");
    }

    @EventListener
    public void onReminderCompleted(ReminderCompletedEvent event) {
        var r = event.getReminder();
        activityLogAdapter.record("Reminder completed: " + r.getTitle(), "REMINDER_COMPLETED");
    }

    @EventListener
    public void onReminderDeleted(ReminderDeletedEvent event) {
        activityLogAdapter.record("Reminder deleted: #" + event.getReminderId(), "REMINDER_DELETED");
    }
}
