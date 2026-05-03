package com.fast.fsf.search.criterion;

import com.fast.fsf.carpool.domain.Ride;

/**
 * Composite pattern — participates as leaf OR composite component: implementors plug into {@link CompositeRideSearchCriterion}.
 * Every criterion knows how to evaluate a {@link Ride}.
 * <p>
 * Lives under the dedicated <strong>Search</strong> feature package (Approved Project Proposal), separate from Carpool
 * listings/moderation. Carpool supplies the domain entity; this feature supplies reusable query predicates (UC‑05 style).
 */
@FunctionalInterface
public interface RideSearchCriterion {

    boolean matches(Ride ride);
}
