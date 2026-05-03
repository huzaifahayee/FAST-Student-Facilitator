package com.fast.fsf.reminders.event;

import com.fast.fsf.reminders.domain.Reminder;
import org.springframework.context.ApplicationEvent;

/**
 * Domain event published when a reminder is updated.
 */
public class ReminderUpdatedEvent extends ApplicationEvent {
    private final Reminder reminder;

    public ReminderUpdatedEvent(Object source, Reminder reminder) {
        super(source);
        this.reminder = reminder;
    }

    public Reminder getReminder() {
        return reminder;
    }
}
