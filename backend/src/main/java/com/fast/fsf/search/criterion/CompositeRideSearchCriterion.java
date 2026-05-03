package com.fast.fsf.search.criterion;

import com.fast.fsf.carpool.domain.Ride;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Composite pattern (GoF): treats many {@link RideSearchCriterion} leaves as one predicate via logical AND.
 * <p>
 * Extensible Search feature: add leaves (origin, time window, …) without rewriting controllers—aligned with the proposal’s
 * Search capability layered on top of approved listings data.
 */
public final class CompositeRideSearchCriterion implements RideSearchCriterion {

    private final List<RideSearchCriterion> children;

    public CompositeRideSearchCriterion(Collection<RideSearchCriterion> criteria) {
        this.children = Collections.unmodifiableList(new ArrayList<>(criteria));
    }

    public CompositeRideSearchCriterion(RideSearchCriterion... criteria) {
        this(Arrays.asList(criteria));
    }

    public List<RideSearchCriterion> getChildren() {
        return children;
    }

    @Override
    public boolean matches(Ride ride) {
        for (RideSearchCriterion child : children) {
            if (!child.matches(ride)) {
                return false;
            }
        }
        return true;
    }
}
