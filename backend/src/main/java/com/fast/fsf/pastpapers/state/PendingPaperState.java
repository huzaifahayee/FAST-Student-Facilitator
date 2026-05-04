package com.fast.fsf.pastpapers.state;

import com.fast.fsf.pastpapers.domain.PastPaper;

/**
 * Concrete State for papers that are freshly uploaded and pending admin approval.
 */
public class PendingPaperState implements PaperModerationState {

    @Override
    public void approve(PastPaper paper, String reason) {
        paper.setApproved(true);
        paper.setModerationReason(reason);
    }

    @Override
    public void reject(PastPaper paper) {
        // Handled by repository deletion in the workflow
    }

    @Override
    public void flag(PastPaper paper, String reason) {
        paper.setFlagged(true);
        paper.setModerationReason(reason);
    }

    @Override
    public void resolveFlag(PastPaper paper) {
        throw new IllegalStateException("Cannot resolve flag on a pending paper");
    }

    @Override
    public String getStateName() {
        return "PENDING";
    }
}
