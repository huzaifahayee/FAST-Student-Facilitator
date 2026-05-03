package com.fast.fsf.search.event;

import org.springframework.context.ApplicationEvent;

/**
 * Domain notification emitted after a UC‑05 search completes — lets Observer subscribers attach analytics without coupling the executor.
 */
public class RideSearchPerformedEvent extends ApplicationEvent {

    private final String criterionKindSummary;
    private final int matchCount;

    public RideSearchPerformedEvent(Object source, String criterionKindSummary, int matchCount) {
        super(source);
        this.criterionKindSummary = criterionKindSummary;
        this.matchCount = matchCount;
    }

    public String getCriterionKindSummary() {
        return criterionKindSummary;
    }

    public int getMatchCount() {
        return matchCount;
    }
}
