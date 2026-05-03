package com.fast.fsf.carpool.state;

import com.fast.fsf.carpool.domain.Ride;

/**
 * State pattern (GoF): lightweight <strong>Context</strong> object holding the mutable {@link Ride} aggregate while
 * delegating transitions to whichever {@link RideModerationState} matches current {@code approved}/{@code flagged} tuple.
 */
public final class RideModerationContext {

    private final Ride ride;

    public RideModerationContext(Ride ride) {
        this.ride = ride;
    }

    public Ride getRide() {
        return ride;
    }

    public void approve(String moderationReasonOrNull) {
        RideModerationStates.fromRide(ride).approve(this, moderationReasonOrNull);
    }

    public void flag(String flagReasonOrNull) {
        RideModerationStates.fromRide(ride).flag(this, flagReasonOrNull);
    }

    public void resolve() {
        RideModerationStates.fromRide(ride).resolve(this);
    }
}
