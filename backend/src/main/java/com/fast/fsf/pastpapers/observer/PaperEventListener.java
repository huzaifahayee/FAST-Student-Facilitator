package com.fast.fsf.pastpapers.observer;

import com.fast.fsf.pastpapers.domain.PaperComment;
import com.fast.fsf.pastpapers.domain.PastPaper;

/**
 * Observer Pattern — Part of the PaperEventPublisher system.
 * Defines the contract for any component that needs to react to
 * Past Paper domain events without being directly coupled to the controller.
 *
 * Mirror: carpool/observer/RideActivityLogObserver.java (indirectly via listener interface)
 */
public interface PaperEventListener {
    void onPaperUploaded(PastPaper paper);
    void onPaperApproved(PastPaper paper);
    void onPaperFlagged(PastPaper paper, String reason);
    void onPaperFlagResolved(PastPaper paper);
    void onPaperDeleted(PastPaper paper, String reason);
    void onPaperRejected(PastPaper paper);
    void onPaperRated(PastPaper paper, String studentEmail, int rating);
    void onPaperReported(PastPaper paper, String reporterEmail, String reason);
    void onCommentPosted(PaperComment comment);
    void onCommentDeleted(PaperComment comment, String studentEmail);
    void onReportResolved(Long reportId);
    void onPaperDownloaded(PastPaper paper);
}
