package com.fast.fsf.pastpapers.criterion;

import com.fast.fsf.pastpapers.domain.PastPaper;
import java.util.List;

/**
 * Composite Strategy — holds a list of PaperSearchCriterion.
 * matches() returns true only if ALL criteria match (AND logic).
 * Used by PaperSearchService to build keyword + examType + approved
 * filters from individual criterion objects.
 *
 * Mirror: search/criterion/CompositeRideSearchCriterion.java
 */
public class CompositePaperSearchCriterion implements PaperSearchCriterion {
    private final List<PaperSearchCriterion> criteria;

    public CompositePaperSearchCriterion(List<PaperSearchCriterion> criteria) {
        this.criteria = criteria;
    }

    @Override
    public boolean matches(PastPaper paper) {
        return criteria.stream().allMatch(c -> c.matches(paper));
    }
}
