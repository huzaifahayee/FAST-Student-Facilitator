package com.fast.fsf.campusmap.criterion;

import com.fast.fsf.campusmap.domain.CampusLocation;

/**
 * Strategy for filtering by classroom numbers keyword.
 */
public class ClassroomNumbersCriterion implements LocationSearchCriterion {
    private final String keyword;

    public ClassroomNumbersCriterion(String keyword) {
        this.keyword = keyword != null ? keyword.toLowerCase() : "";
    }

    @Override
    public boolean matches(CampusLocation location) {
        return keyword.isEmpty() || 
               (location.getClassroomNumbers() != null && location.getClassroomNumbers().toLowerCase().contains(keyword));
    }
}
