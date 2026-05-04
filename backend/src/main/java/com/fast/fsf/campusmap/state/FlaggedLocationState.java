package com.fast.fsf.campusmap.state;

import com.fast.fsf.campusmap.domain.CampusLocation;

/**
 * Concrete State for locations that have been flagged for review.
 */
public class FlaggedLocationState implements LocationModerationState {

    @Override
    public void approve(CampusLocation location, String reason) {
        location.setApproved(true);
        location.setModerationReason(reason);
    }

    @Override
    public void flag(CampusLocation location, String reason) {
        throw new IllegalStateException("Location is already flagged");
    }

    @Override
    public void resolveFlag(CampusLocation location) {
        location.setFlagged(false);
        location.setModerationReason(null);
    }

    @Override
    public String getStateName() {
        return "FLAGGED";
    }
}
