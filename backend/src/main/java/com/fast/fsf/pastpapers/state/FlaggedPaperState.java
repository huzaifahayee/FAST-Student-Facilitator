package com.fast.fsf.pastpapers.state;

import com.fast.fsf.pastpapers.domain.PastPaper;

/**
 * Concrete State for papers that have been flagged for review.
 */
public class FlaggedPaperState implements PaperModerationState {

    @Override
    public void approve(PastPaper paper, String reason) {
        paper.setApproved(true);
        paper.setModerationReason(reason);
    }

    @Override
    public void reject(PastPaper paper) {
        throw new IllegalStateException("Cannot reject a flagged paper directly. Resolve flag first.");
    }

    @Override
    public void flag(PastPaper paper, String reason) {
        throw new IllegalStateException("Paper is already flagged");
    }

    @Override
    public void resolveFlag(PastPaper paper) {
        paper.setFlagged(false);
        paper.setModerationReason(null);
    }

    @Override
    public String getStateName() {
        return "FLAGGED";
    }
}
