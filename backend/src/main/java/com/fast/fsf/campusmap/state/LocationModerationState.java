package com.fast.fsf.campusmap.state;

import com.fast.fsf.campusmap.domain.CampusLocation;

/**
 * State pattern — encapsulates which moderation transitions are
 * legal from each state of a CampusLocation's lifecycle.
 *
 * Mirror: carpool/state/RideModerationState.java (hypothetical)
 */
public interface LocationModerationState {
    void approve(CampusLocation location, String reason);
    void flag(CampusLocation location, String reason);
    void resolveFlag(CampusLocation location);
    String getStateName();
}
