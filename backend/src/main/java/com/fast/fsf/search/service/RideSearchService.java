package com.fast.fsf.search.service;

import com.fast.fsf.carpool.domain.Ride;
import com.fast.fsf.search.catalog.ApprovedRideSource;
import com.fast.fsf.search.criterion.RideSearchCriterion;
import com.fast.fsf.search.event.RideSearchPerformedEvent;
import com.fast.fsf.search.template.AbstractRideSearchExecutor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Search feature façade built on {@link AbstractRideSearchExecutor} (Template Method).
 * <p>
 * <strong>Singleton (GoF / Spring)</strong>: {@code @Service} beans default to singleton scope — one executor coordinates every search request.
 */
@Service
public class RideSearchService extends AbstractRideSearchExecutor {

    private final ApplicationEventPublisher eventPublisher;

    public RideSearchService(ApprovedRideSource catalog, ApplicationEventPublisher eventPublisher) {
        super(catalog);
        this.eventPublisher = eventPublisher;
    }

    /**
     * Executes Composite criteria against approved rides and notifies observers (DEBUG analytics only by default).
     */
    public List<Ride> searchApproved(RideSearchCriterion criterion) {
        List<Ride> matches = search(criterion);
        eventPublisher.publishEvent(new RideSearchPerformedEvent(
                this,
                criterion.getClass().getSimpleName(),
                matches.size()));
        return matches;
    }
}
