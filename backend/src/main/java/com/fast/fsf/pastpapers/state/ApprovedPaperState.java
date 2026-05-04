package com.fast.fsf.pastpapers.state;

import com.fast.fsf.pastpapers.domain.PastPaper;

/**
 * Concrete State for papers that have been approved by an admin.
 */
public class ApprovedPaperState implements PaperModerationState {

    @Override
    public void approve(PastPaper paper, String reason) {
        throw new IllegalStateException("Paper is already approved");
    }

    @Override
    public void reject(PastPaper paper) {
        throw new IllegalStateException("Cannot reject an approved paper. Use delete instead.");
    }

    @Override
    public void flag(PastPaper paper, String reason) {
        paper.setFlagged(true);
        paper.setModerationReason(reason);
    }

    @Override
    public void resolveFlag(PastPaper paper) {
        paper.setFlagged(false);
        paper.setModerationReason(null);
    }

    @Override
    public String getStateName() {
        return "APPROVED";
    }
}
