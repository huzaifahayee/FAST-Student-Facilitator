package com.fast.fsf.pastpapers.criterion;

import com.fast.fsf.pastpapers.domain.PastPaper;

/**
 * Strategy pattern — each criterion encapsulates one filter rule.
 * Criteria can be composed via CompositePaperSearchCriterion to
 * build complex queries from simple, testable pieces.
 *
 * Mirror: search/criterion/RideSearchCriterion.java (hypothetical)
 */
public interface PaperSearchCriterion {
    boolean matches(PastPaper paper);
}
