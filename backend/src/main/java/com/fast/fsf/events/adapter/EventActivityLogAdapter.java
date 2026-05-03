package com.fast.fsf.events.adapter;

import com.fast.fsf.shared.model.ActivityLog;
import com.fast.fsf.shared.persistence.ActivityLogRepository;
import org.springframework.stereotype.Component;

/**
 * Adapter pattern (GoF): adapts the shared ActivityLogRepository to the events feature recording needs.
 */
@Component
public class EventActivityLogAdapter {

    private final ActivityLogRepository activityLogRepository;

    public EventActivityLogAdapter(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    public void record(String description, String type) {
        ActivityLog log = new ActivityLog(description, type);
        activityLogRepository.save(log);
    }
}
