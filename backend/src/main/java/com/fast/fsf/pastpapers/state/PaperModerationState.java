package com.fast.fsf.pastpapers.state;

import com.fast.fsf.pastpapers.domain.PastPaper;

/**
 * State pattern — each concrete state encapsulates what transitions
 * are legal from that state and what happens during them.
 * PaperModerationContext delegates approve/reject/flag/resolve calls
 * to the current state object, which either executes the transition
 * or throws IllegalStateException if disallowed.
 *
 * Mirror: carpool/state/RideModerationState.java (hypothetical)
 */
public interface PaperModerationState {
    void approve(PastPaper paper, String reason);
    void reject(PastPaper paper);
    void flag(PastPaper paper, String reason);
    void resolveFlag(PastPaper paper);
    String getStateName();
}
