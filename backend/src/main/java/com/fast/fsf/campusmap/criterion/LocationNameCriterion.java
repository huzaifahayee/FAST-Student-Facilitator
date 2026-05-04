package com.fast.fsf.campusmap.criterion;

import com.fast.fsf.campusmap.domain.CampusLocation;

/**
 * Strategy for filtering by location name keyword.
 */
public class LocationNameCriterion implements LocationSearchCriterion {
    private final String keyword;

    public LocationNameCriterion(String keyword) {
        this.keyword = keyword != null ? keyword.toLowerCase() : "";
    }

    @Override
    public boolean matches(CampusLocation location) {
        return keyword.isEmpty() || 
               (location.getLocationName() != null && location.getLocationName().toLowerCase().contains(keyword));
    }
}
