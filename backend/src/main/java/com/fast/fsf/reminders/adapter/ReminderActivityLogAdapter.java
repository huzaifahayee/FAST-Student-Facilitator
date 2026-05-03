package com.fast.fsf.reminders.adapter;

import com.fast.fsf.shared.model.ActivityLog;
import com.fast.fsf.shared.persistence.ActivityLogRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Adapter pattern (GoF): adapts the shared {@link ActivityLogRepository} to the feature's recording needs.
 */
@Component
public class ReminderActivityLogAdapter {

    private final ActivityLogRepository activityLogRepository;

    public ReminderActivityLogAdapter(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    public void record(String description, String type) {
        ActivityLog log = new ActivityLog(description, type);
        activityLogRepository.save(log);
    }
}
