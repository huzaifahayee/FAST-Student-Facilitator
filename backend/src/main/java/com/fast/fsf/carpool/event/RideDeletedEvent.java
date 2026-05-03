package com.fast.fsf.carpool.event;

import org.springframework.context.ApplicationEvent;

/**
 * Published <strong>before</strong> the physical DELETE so synchronous observers can still read FK-linked rows if needed.
 * (Today we only append an ActivityLog row — identical ordering to the legacy controller.)
 */
public class RideDeletedEvent extends ApplicationEvent {

    private final Long rideId;
    private final String deletionReasonOrNull;

    public RideDeletedEvent(Object source, Long rideId, String deletionReasonOrNull) {
        super(source);
        this.rideId = rideId;
        this.deletionReasonOrNull = deletionReasonOrNull;
    }

    public Long getRideId() {
        return rideId;
    }

    public String getDeletionReasonOrNull() {
        return deletionReasonOrNull;
    }
}
