package com.fast.fsf.campusmap.criterion;

import com.fast.fsf.campusmap.domain.CampusLocation;

/**
 * Strategy for filtering by category.
 */
public class CategoryCriterion implements LocationSearchCriterion {
    private final String category;

    public CategoryCriterion(String category) {
        this.category = category;
    }

    @Override
    public boolean matches(CampusLocation location) {
        return category == null || category.isEmpty() || 
               (location.getCategory() != null && location.getCategory().equalsIgnoreCase(category));
    }
}
