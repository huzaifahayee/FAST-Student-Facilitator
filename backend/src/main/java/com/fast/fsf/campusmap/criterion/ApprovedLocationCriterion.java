package com.fast.fsf.campusmap.criterion;

import com.fast.fsf.campusmap.domain.CampusLocation;

/**
 * Strategy for ensuring only approved locations are matched.
 */
public class ApprovedLocationCriterion implements LocationSearchCriterion {
    @Override
    public boolean matches(CampusLocation location) {
        return location.isApproved();
    }
}
