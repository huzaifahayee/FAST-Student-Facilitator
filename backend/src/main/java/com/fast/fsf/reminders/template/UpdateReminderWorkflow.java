package com.fast.fsf.reminders.template;

import com.fast.fsf.reminders.domain.Reminder;
import com.fast.fsf.reminders.event.ReminderUpdatedEvent;
import com.fast.fsf.reminders.factory.ReminderFactory;
import com.fast.fsf.reminders.persistence.ReminderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class UpdateReminderWorkflow extends AbstractReminderMutationWorkflow {

    private final ReminderFactory reminderFactory;
    private Reminder updateSource;

    public UpdateReminderWorkflow(ReminderRepository reminderRepository, 
                                  ApplicationEventPublisher eventPublisher,
                                  ReminderFactory reminderFactory) {
        super(reminderRepository, eventPublisher);
        this.reminderFactory = reminderFactory;
    }

    public void setUpdateSource(Reminder updateSource) {
        this.updateSource = updateSource;
    }

    @Override
    protected void mutate(Reminder existing) {
        if (updateSource == null) return;
        
        reminderFactory.validate(updateSource);
        
        existing.setTitle(updateSource.getTitle());
        existing.setReminderTime(updateSource.getReminderTime());
        existing.setCategory(updateSource.getCategory().toUpperCase());
    }

    @Override
    protected void publishEvent(Reminder saved) {
        eventPublisher.publishEvent(new ReminderUpdatedEvent(this, saved));
    }
}
