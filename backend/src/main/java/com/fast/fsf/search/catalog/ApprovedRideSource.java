package com.fast.fsf.search.catalog;

import com.fast.fsf.carpool.domain.Ride;

import java.util.List;

/**
 * Adapter pattern — <strong>Target</strong> port: read-only API the Search feature wants (“give me candidate rides”).
 * <p>
 * {@link RideRepositoryApprovedRideAdapter} adapts {@link com.fast.fsf.carpool.persistence.RideRepository} so search logic
 * never depends directly on Spring Data types — swap implementations for caching, tests, or alternate backends.
 */
public interface ApprovedRideSource {

    /** Candidates eligible for UC‑05 matching (today: approved listings only). */
    List<Ride> loadApprovedCandidates();
}
