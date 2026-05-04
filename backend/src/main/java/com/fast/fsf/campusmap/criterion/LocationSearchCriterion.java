package com.fast.fsf.campusmap.criterion;

import com.fast.fsf.campusmap.domain.CampusLocation;

/**
 * Strategy pattern — each criterion encapsulates one filter rule
 * for CampusLocation search. Composed via
 * CompositeLocationSearchCriterion for complex queries.
 *
 * Mirror: search/criterion/RideSearchCriterion.java (hypothetical)
 */
public interface LocationSearchCriterion {
    boolean matches(CampusLocation location);
}
