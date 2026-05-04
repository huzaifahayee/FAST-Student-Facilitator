package com.fast.fsf.campusmap.criterion;

import com.fast.fsf.campusmap.domain.CampusLocation;

/**
 * Strategy for filtering by location type.
 */
public class LocationTypeCriterion implements LocationSearchCriterion {
    private final String locationType;

    public LocationTypeCriterion(String locationType) {
        this.locationType = locationType;
    }

    @Override
    public boolean matches(CampusLocation location) {
        return locationType == null || locationType.isEmpty() || 
               (location.getLocationType() != null && location.getLocationType().equalsIgnoreCase(locationType));
    }
}
