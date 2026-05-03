package com.fast.fsf.reminders.state;

import com.fast.fsf.reminders.domain.Reminder;

/**
 * State pattern (GoF): handles lifecycle transitions for a Reminder.
 */
public interface ReminderStatusState {
    void handle(Reminder reminder);
    String getStatusName();
}
