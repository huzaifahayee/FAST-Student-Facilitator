package com.fast.fsf.campusmap.state;

import com.fast.fsf.campusmap.domain.CampusLocation;

/**
 * Concrete State for locations that are freshly added and pending admin approval.
 */
public class PendingLocationState implements LocationModerationState {

    @Override
    public void approve(CampusLocation location, String reason) {
        location.setApproved(true);
        location.setModerationReason(reason);
    }

    @Override
    public void flag(CampusLocation location, String reason) {
        location.setFlagged(true);
        location.setModerationReason(reason);
    }

    @Override
    public void resolveFlag(CampusLocation location) {
        throw new IllegalStateException("Cannot resolve flag on a pending location");
    }

    @Override
    public String getStateName() {
        return "PENDING";
    }
}
