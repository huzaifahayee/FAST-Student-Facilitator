package com.fast.fsf.reminders.state;

import com.fast.fsf.reminders.domain.Reminder;

public class CompletedState implements ReminderStatusState {
    @Override
    public void handle(Reminder reminder) {
        reminder.setStatus("COMPLETED");
    }

    @Override
    public String getStatusName() {
        return "COMPLETED";
    }
}
