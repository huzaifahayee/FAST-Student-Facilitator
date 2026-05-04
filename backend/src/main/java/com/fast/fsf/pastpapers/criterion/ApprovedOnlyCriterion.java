package com.fast.fsf.pastpapers.criterion;

import com.fast.fsf.pastpapers.domain.PastPaper;

/**
 * Strategy for ensuring only approved papers are matched.
 */
public class ApprovedOnlyCriterion implements PaperSearchCriterion {
    @Override
    public boolean matches(PastPaper paper) {
        return paper.isApproved();
    }
}
