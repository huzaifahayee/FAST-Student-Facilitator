package com.fast.fsf.reminders.state;

import com.fast.fsf.reminders.domain.Reminder;

public class PendingState implements ReminderStatusState {
    @Override
    public void handle(Reminder reminder) {
        reminder.setStatus("PENDING");
    }

    @Override
    public String getStatusName() {
        return "PENDING";
    }
}
