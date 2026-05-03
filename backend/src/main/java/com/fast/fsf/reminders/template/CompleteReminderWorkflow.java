package com.fast.fsf.reminders.template;

import com.fast.fsf.reminders.domain.Reminder;
import com.fast.fsf.reminders.event.ReminderCompletedEvent;
import com.fast.fsf.reminders.persistence.ReminderRepository;
import com.fast.fsf.reminders.state.CompletedState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class CompleteReminderWorkflow extends AbstractReminderMutationWorkflow {

    public CompleteReminderWorkflow(ReminderRepository reminderRepository, ApplicationEventPublisher eventPublisher) {
        super(reminderRepository, eventPublisher);
    }

    @Override
    protected void mutate(Reminder reminder) {
        new CompletedState().handle(reminder);
    }

    @Override
    protected void publishEvent(Reminder saved) {
        eventPublisher.publishEvent(new ReminderCompletedEvent(this, saved));
    }
}
