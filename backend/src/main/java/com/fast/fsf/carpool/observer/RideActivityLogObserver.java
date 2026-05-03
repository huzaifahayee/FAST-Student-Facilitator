package com.fast.fsf.carpool.observer;

import com.fast.fsf.carpool.adapter.ActivityLogRecorder;
import com.fast.fsf.carpool.event.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observer pattern (GoF): this component is an <strong>Observer</strong> / subscriber reacting to ride lifecycle
 * {@linkplain org.springframework.context.ApplicationEvent ApplicationEvents}.
 * <p>
 * Spring's {@link EventListener} dispatches synchronously on the publishing thread by default, so database ordering
 * matches legacy behaviour (audit row inserted immediately after the transactional save/delete orchestration).
 * <p>
 * Each handler delegates persistence through {@link ActivityLogRecorder} — the Adapter target — keeping messages
 * identical byte-for-byte to the original {@code RideController} strings for regression safety.
 */
@Component
public class RideActivityLogObserver {

    private final ActivityLogRecorder activityLogRecorder;

    public RideActivityLogObserver(ActivityLogRecorder activityLogRecorder) {
        this.activityLogRecorder = activityLogRecorder;
    }

    @EventListener
    public void onRideOffered(RideOfferedEvent event) {
        var ride = event.getSavedRide();
        String msg = ride.getDriverName() + " offered a new ride to " + ride.getDestination() + ".";
        activityLogRecorder.record(msg, "RIDE_POSTED");
    }

    @EventListener
    public void onRideApproved(RideApprovedEvent event) {
        String reasonPart = event.getModerationReasonOrNull() != null ? event.getModerationReasonOrNull() : "None";
        String msg = "Ride #" + event.getRideId() + " was approved. Reason: " + reasonPart;
        activityLogRecorder.record(msg, "RIDE_APPROVED");
    }

    @EventListener
    public void onRideFlagged(RideFlaggedEvent event) {
        String msg = "Ride #" + event.getRideId() + " flagged. Reason: " + event.getFlagReasonOrNull();
        activityLogRecorder.record(msg, "RIDE_FLAGGED");
    }

    @EventListener
    public void onRideResolved(RideResolvedEvent event) {
        activityLogRecorder.record("Moderation flag cleared for Ride #" + event.getRideId() + ".", "FLAG_RESOLVED");
    }

    @EventListener
    public void onRideDeleted(RideDeletedEvent event) {
        String reasonPart = event.getDeletionReasonOrNull() != null
                ? event.getDeletionReasonOrNull()
                : "Compliance violation";
        String msg = "Ride #" + event.getRideId() + " deleted by Admin. Reason: " + reasonPart;
        activityLogRecorder.record(msg, "CONTENT_DELETED");
    }
}
