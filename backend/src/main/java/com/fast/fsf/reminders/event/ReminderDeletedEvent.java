package com.fast.fsf.reminders.event;

import org.springframework.context.ApplicationEvent;

/**
 * Domain event published when a reminder is deleted.
 */
public class ReminderDeletedEvent extends ApplicationEvent {
    private final Long reminderId;

    public ReminderDeletedEvent(Object source, Long reminderId) {
        super(source);
        this.reminderId = reminderId;
    }

    public Long getReminderId() {
        return reminderId;
    }
}
