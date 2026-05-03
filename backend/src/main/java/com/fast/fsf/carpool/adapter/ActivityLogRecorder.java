package com.fast.fsf.carpool.adapter;

/**
 * Adapter pattern (GoF): <strong>Target</strong> interface — the behaviour our Carpool domain wants to speak to.
 * <p>
 * The real persistence layer uses {@link com.fast.fsf.shared.persistence.ActivityLogRepository},
 * which exposes generic {@code save(ActivityLog)} and knows nothing about “recording ride audit lines”.
 * An {@linkplain ActivityLogRepositoryAdapter Adapter implementation} translates simple record calls into
 * {@link com.fast.fsf.shared.model.ActivityLog} entities so controllers/workflows stay decoupled from JPA types.
 * <p>
 * Why here? UC-06 / UC-08 flows must append moderation audit rows without tying those workflows directly to Spring Data.
 */
public interface ActivityLogRecorder {

    /**
     * Persist one human-readable audit line plus its machine-facing category (e.g. {@code RIDE_POSTED}).
     */
    void record(String message, String logType);
}
