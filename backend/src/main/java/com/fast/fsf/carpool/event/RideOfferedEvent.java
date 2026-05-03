package com.fast.fsf.carpool.event;

import com.fast.fsf.carpool.domain.Ride;
import org.springframework.context.ApplicationEvent;

/**
 * Domain event published after UC-06 “Offer a Ride” persists a listing (still pending approval).
 * Observer subscribers react without the HTTP controller touching {@code ActivityLogRepository} directly.
 */
public class RideOfferedEvent extends ApplicationEvent {

    private final Ride savedRide;

    public RideOfferedEvent(Object source, Ride savedRide) {
        super(source);
        this.savedRide = savedRide;
    }

    public Ride getSavedRide() {
        return savedRide;
    }
}
