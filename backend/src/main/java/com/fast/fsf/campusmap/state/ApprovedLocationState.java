package com.fast.fsf.campusmap.state;

import com.fast.fsf.campusmap.domain.CampusLocation;

/**
 * Concrete State for locations that have been approved by an admin.
 */
public class ApprovedLocationState implements LocationModerationState {

    @Override
    public void approve(CampusLocation location, String reason) {
        throw new IllegalStateException("Location is already approved");
    }

    @Override
    public void flag(CampusLocation location, String reason) {
        location.setFlagged(true);
        location.setModerationReason(reason);
    }

    @Override
    public void resolveFlag(CampusLocation location) {
        location.setFlagged(false);
        location.setModerationReason(null);
    }

    @Override
    public String getStateName() {
        return "APPROVED";
    }
}
