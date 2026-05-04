package com.fast.fsf.pastpapers.state;

import com.fast.fsf.pastpapers.domain.PastPaper;

/**
 * Context in the State pattern. Wraps a PastPaper and routes
 * moderation method calls to the appropriate state object based on the
 * paper's current approved/flagged fields.
 *
 * Mirror: carpool/state/RideModerationContext.java
 */
public class PaperModerationContext {

    private final PaperModerationState state;

    public PaperModerationContext(PastPaper paper) {
        if (paper.isFlagged()) {
            this.state = new FlaggedPaperState();
        } else if (paper.isApproved()) {
            this.state = new ApprovedPaperState();
        } else {
            this.state = new PendingPaperState();
        }
    }

    public void approve(PastPaper paper, String reason) {
        state.approve(paper, reason);
    }

    public void reject(PastPaper paper) {
        state.reject(paper);
    }

    public void flag(PastPaper paper, String reason) {
        state.flag(paper, reason);
    }

    public void resolveFlag(PastPaper paper) {
        state.resolveFlag(paper);
    }

    public String getStateName() {
        return state.getStateName();
    }
}
