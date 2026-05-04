package com.fast.fsf.pastpapers.observer;

import com.fast.fsf.pastpapers.domain.PaperComment;
import com.fast.fsf.pastpapers.domain.PastPaper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Acts as the subject in the Observer pattern.
 * Controllers call publish methods; registered listeners react independently.
 *
 * Singleton (Spring default scope) — one shared instance per JVM.
 * All controllers and services that publish paper events share this
 * single publisher, ensuring all observers are consistently notified.
 *
 * Mirror: carpool/observer/RideEventPublisher.java (hypothetical, matches pattern description)
 */
@Service
public class PaperEventPublisher {

    private final List<PaperEventListener> listeners = new ArrayList<>();

    @Autowired
    public PaperEventPublisher(List<PaperEventListener> listeners) {
        this.listeners.addAll(listeners);
    }

    public void publishPaperUploaded(PastPaper paper) {
        listeners.forEach(l -> l.onPaperUploaded(paper));
    }

    public void publishPaperApproved(PastPaper paper) {
        listeners.forEach(l -> l.onPaperApproved(paper));
    }

    public void publishPaperFlagged(PastPaper paper, String reason) {
        listeners.forEach(l -> l.onPaperFlagged(paper, reason));
    }

    public void publishPaperFlagResolved(PastPaper paper) {
        listeners.forEach(l -> l.onPaperFlagResolved(paper));
    }

    public void publishPaperDeleted(PastPaper paper, String reason) {
        listeners.forEach(l -> l.onPaperDeleted(paper, reason));
    }

    public void publishPaperRejected(PastPaper paper) {
        listeners.forEach(l -> l.onPaperRejected(paper));
    }

    public void publishPaperRated(PastPaper paper, String studentEmail, int rating) {
        listeners.forEach(l -> l.onPaperRated(paper, studentEmail, rating));
    }

    public void publishPaperReported(PastPaper paper, String reporterEmail, String reason) {
        listeners.forEach(l -> l.onPaperReported(paper, reporterEmail, reason));
    }

    public void publishCommentPosted(PaperComment comment) {
        listeners.forEach(l -> l.onCommentPosted(comment));
    }

    public void publishCommentDeleted(PaperComment comment, String studentEmail) {
        listeners.forEach(l -> l.onCommentDeleted(comment, studentEmail));
    }

    public void publishReportResolved(Long reportId) {
        listeners.forEach(l -> l.onReportResolved(reportId));
    }

    public void publishPaperDownloaded(PastPaper paper) {
        listeners.forEach(l -> l.onPaperDownloaded(paper));
    }
}
