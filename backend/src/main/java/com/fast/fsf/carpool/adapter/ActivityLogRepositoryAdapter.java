package com.fast.fsf.carpool.adapter;

import com.fast.fsf.shared.model.ActivityLog;
import com.fast.fsf.shared.persistence.ActivityLogRepository;
import org.springframework.stereotype.Component;

/**
 * Adapter pattern (GoF): concrete <strong>Adapter</strong> that wraps {@link ActivityLogRepository}
 * (the <strong>Adaptee</strong>) behind {@link ActivityLogRecorder} (the <strong>Target</strong>).
 * <p>
 * Observers / workflows depend only on {@code ActivityLogRecorder}, so we could swap implementations later
 * (e.g. noop adapter in tests, remote logging service) without touching ride moderation logic.
 */
@Component
public class ActivityLogRepositoryAdapter implements ActivityLogRecorder {

    private final ActivityLogRepository delegate;

    public ActivityLogRepositoryAdapter(ActivityLogRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public void record(String message, String logType) {
        delegate.save(new ActivityLog(message, logType));
    }
}
