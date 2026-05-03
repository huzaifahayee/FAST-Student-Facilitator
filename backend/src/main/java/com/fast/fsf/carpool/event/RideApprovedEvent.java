package com.fast.fsf.carpool.event;

import org.springframework.context.ApplicationEvent;

/** Published after an Admin approves a ride listing (moderation workflow). */
public class RideApprovedEvent extends ApplicationEvent {

    private final Long rideId;
    private final String moderationReasonOrNull;

    public RideApprovedEvent(Object source, Long rideId, String moderationReasonOrNull) {
        super(source);
        this.rideId = rideId;
        this.moderationReasonOrNull = moderationReasonOrNull;
    }

    public Long getRideId() {
        return rideId;
    }

    public String getModerationReasonOrNull() {
        return moderationReasonOrNull;
    }
}
