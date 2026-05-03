package com.fast.fsf.carpool.event;

import org.springframework.context.ApplicationEvent;

/** Published after moderation clears the flag from a ride listing. */
public class RideResolvedEvent extends ApplicationEvent {

    private final Long rideId;

    public RideResolvedEvent(Object source, Long rideId) {
        super(source);
        this.rideId = rideId;
    }

    public Long getRideId() {
        return rideId;
    }
}
