package com.fast.fsf.search.observer;

import com.fast.fsf.search.event.RideSearchPerformedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observer pattern (GoF): reacts to {@link RideSearchPerformedEvent} publications synchronously on the publishing thread.
 * <p>
 * Default behaviour is DEBUG-only logging — zero functional drift vs pre-observer flows while demonstrating extensibility.
 */
@Component
public class SearchAnalyticsObserver {

    private static final Logger log = LoggerFactory.getLogger(SearchAnalyticsObserver.class);

    @EventListener
    public void onRideSearchPerformed(RideSearchPerformedEvent event) {
        log.debug("[Search feature] criterion={} matches={}", event.getCriterionKindSummary(), event.getMatchCount());
    }
}
