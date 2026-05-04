package com.fast.fsf.campusmap.criterion;

import com.fast.fsf.campusmap.domain.CampusLocation;

/**
 * Strategy for filtering by faculty offices keyword.
 */
public class FacultyOfficesCriterion implements LocationSearchCriterion {
    private final String keyword;

    public FacultyOfficesCriterion(String keyword) {
        this.keyword = keyword != null ? keyword.toLowerCase() : "";
    }

    @Override
    public boolean matches(CampusLocation location) {
        return keyword.isEmpty() || 
               (location.getFacultyOffices() != null && location.getFacultyOffices().toLowerCase().contains(keyword));
    }
}
