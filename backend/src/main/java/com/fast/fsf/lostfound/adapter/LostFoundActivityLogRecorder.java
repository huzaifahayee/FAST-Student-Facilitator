package com.fast.fsf.lostfound.adapter;

/**
 * Adapter pattern (GoF): <strong>Target</strong> interface — the behaviour our Lost &amp; Found
 * domain wants to speak to when recording audit activity.
 * <p>
 * The real persistence layer uses {@link com.fast.fsf.shared.persistence.ActivityLogRepository},
 * which exposes a generic {@code save(ActivityLog)} and knows nothing about "recording listing
 * audit lines". A concrete {@link LostFoundActivityLogRepositoryAdapter Adapter} translates
 * simple record calls into {@link com.fast.fsf.shared.model.ActivityLog} entities so the
 * Observer and service layers stay decoupled from JPA types.
 * <p>
 * Mirrors {@code ActivityLogRecorder} from the carpool feature.
 */
public interface LostFoundActivityLogRecorder {

    /**
     * Persist one human-readable audit line plus its machine-facing category
     * (e.g. {@code ITEM_REPORTED}, {@code ITEM_RESOLVED}).
     */
    void record(String message, String logType);
}
