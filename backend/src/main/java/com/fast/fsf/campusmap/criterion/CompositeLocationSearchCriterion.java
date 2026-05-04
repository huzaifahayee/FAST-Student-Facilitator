package com.fast.fsf.campusmap.criterion;

import com.fast.fsf.campusmap.domain.CampusLocation;
import java.util.List;

/**
 * Composite Strategy — holds multiple LocationSearchCriterion.
 * OR logic: matches if ANY criterion matches (for keyword search
 * across multiple fields). AND logic variant also available for
 * type + category combined filtering.
 *
 * Mirror: search/criterion/CompositeRideSearchCriterion.java
 */
public class CompositeLocationSearchCriterion implements LocationSearchCriterion {
    private final List<LocationSearchCriterion> criteria;
    private final boolean useOrLogic; // true=OR, false=AND

    public CompositeLocationSearchCriterion(List<LocationSearchCriterion> criteria, boolean useOrLogic) {
        this.criteria = criteria;
        this.useOrLogic = useOrLogic;
    }

    @Override
    public boolean matches(CampusLocation location) {
        if (useOrLogic) {
            return criteria.stream().anyMatch(c -> c.matches(location));
        } else {
            return criteria.stream().allMatch(c -> c.matches(location));
        }
    }
}
