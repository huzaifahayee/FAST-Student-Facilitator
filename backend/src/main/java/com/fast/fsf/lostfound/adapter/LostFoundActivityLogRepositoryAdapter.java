package com.fast.fsf.lostfound.adapter;

import com.fast.fsf.shared.model.ActivityLog;
import com.fast.fsf.shared.persistence.ActivityLogRepository;
import org.springframework.stereotype.Component;

/**
 * Adapter pattern (GoF): concrete <strong>Adapter</strong> that wraps
 * {@link ActivityLogRepository} (the <strong>Adaptee</strong>) behind
 * {@link LostFoundActivityLogRecorder} (the <strong>Target</strong>).
 * <p>
 * The {@link com.fast.fsf.lostfound.observer.LostFoundActivityLogObserver Observer} and
 * any future workflows depend only on {@code LostFoundActivityLogRecorder}, so we can swap
 * implementations (e.g. no-op adapter in tests, remote logging) without touching listing logic.
 * <p>
 * Mirrors {@code ActivityLogRepositoryAdapter} from the carpool feature.
 */
@Component
public class LostFoundActivityLogRepositoryAdapter implements LostFoundActivityLogRecorder {

    private final ActivityLogRepository delegate;

    public LostFoundActivityLogRepositoryAdapter(ActivityLogRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public void record(String message, String logType) {
        delegate.save(new ActivityLog(message, logType));
    }
}
