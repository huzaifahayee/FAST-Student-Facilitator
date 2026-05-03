package com.fast.fsf.search.template;

import com.fast.fsf.carpool.domain.Ride;
import com.fast.fsf.search.catalog.ApprovedRideSource;
import com.fast.fsf.search.criterion.RideSearchCriterion;

import java.util.List;

/**
 * Template Method pattern (GoF): fixes the algorithm “load candidates → apply criterion → materialise results” while allowing
 * hooks to evolve (pagination, caching, alternate candidate sources) without copying controller code.
 */
public abstract class AbstractRideSearchExecutor {

    private final ApprovedRideSource catalog;

    protected AbstractRideSearchExecutor(ApprovedRideSource catalog) {
        this.catalog = catalog;
    }

    /**
     * Template method — {@code final} so every search honours the same sequencing guarantees.
     */
    public final List<Ride> search(RideSearchCriterion criterion) {
        List<Ride> pool = loadCandidatePool();
        return refine(pool, criterion);
    }

    /** Hook #1 — default: approved rides exposed through {@link ApprovedRideSource}. */
    protected List<Ride> loadCandidatePool() {
        return catalog.loadApprovedCandidates();
    }

    /** Hook #2 — default: linear filter via Composite / leaf criteria. */
    protected List<Ride> refine(List<Ride> pool, RideSearchCriterion criterion) {
        return pool.stream().filter(criterion::matches).toList();
    }
}
