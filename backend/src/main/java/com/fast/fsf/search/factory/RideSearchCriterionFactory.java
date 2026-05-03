package com.fast.fsf.search.factory;

import com.fast.fsf.search.criterion.CompositeRideSearchCriterion;
import com.fast.fsf.search.criterion.DestinationContainsCriterion;
import com.fast.fsf.search.criterion.RideSearchCriterion;
import org.springframework.stereotype.Component;

/**
 * Factory Method pattern (GoF): centralises construction of {@link RideSearchCriterion} graphs so callers avoid sprinkling
 * {@code new Composite…} / {@code new Destination…} across web adapters.
 * <p>
 * <strong>Singleton (Spring)</strong>: {@code @Component} defaults to singleton scope — one factory bean reused everywhere.
 */
@Component
public class RideSearchCriterionFactory {

    /**
     * Factory method — canonical UC‑05 bundle: approved-scope filtering happens later via executor; here we encode destination semantics only.
     */
    public RideSearchCriterion approvedListingDestinationContains(String destinationQuery) {
        return new CompositeRideSearchCriterion(new DestinationContainsCriterion(destinationQuery));
    }
}
