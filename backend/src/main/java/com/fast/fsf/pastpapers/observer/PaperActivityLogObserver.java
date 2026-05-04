package com.fast.fsf.pastpapers.observer;

import com.fast.fsf.pastpapers.domain.PaperComment;
import com.fast.fsf.pastpapers.domain.PastPaper;
import com.fast.fsf.shared.model.ActivityLog;
import com.fast.fsf.shared.persistence.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Observer pattern — decouples ActivityLog writes from the HTTP layer.
 * The controller fires events via PaperEventPublisher; this observer reacts
 * by persisting audit entries, keeping the controller free of logging concerns.
 *
 * Mirror: carpool/observer/RideActivityLogObserver.java
 */
@Component
public class PaperActivityLogObserver implements PaperEventListener {

    private final ActivityLogRepository activityLogRepository;

    @Autowired
    public PaperActivityLogObserver(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @Override
    public void onPaperUploaded(PastPaper paper) {
        activityLogRepository.save(new ActivityLog(
            paper.getOwnerName() + " uploaded paper: " + paper.getCourseName(),
            "PAPER_UPLOADED"
        ));
    }

    @Override
    public void onPaperApproved(PastPaper paper) {
        activityLogRepository.save(new ActivityLog(
            "Paper #" + paper.getId() + " approved",
            "PAPER_APPROVED"
        ));
    }

    @Override
    public void onPaperFlagged(PastPaper paper, String reason) {
        activityLogRepository.save(new ActivityLog(
            "Paper #" + paper.getId() + " flagged: " + reason,
            "PAPER_FLAGGED"
        ));
    }

    @Override
    public void onPaperFlagResolved(PastPaper paper) {
        activityLogRepository.save(new ActivityLog(
            "Flag on Paper #" + paper.getId() + " resolved",
            "PAPER_FLAG_RESOLVED"
        ));
    }

    @Override
    public void onPaperDeleted(PastPaper paper, String reason) {
        activityLogRepository.save(new ActivityLog(
            "Paper #" + paper.getId() + " deleted. Reason: " + (reason != null ? reason : "None"),
            "PAPER_DELETED"
        ));
    }

    @Override
    public void onPaperRejected(PastPaper paper) {
        activityLogRepository.save(new ActivityLog(
            "Paper #" + paper.getId() + " rejected",
            "PAPER_REJECTED"
        ));
    }

    @Override
    public void onPaperRated(PastPaper paper, String studentEmail, int rating) {
        activityLogRepository.save(new ActivityLog(
            studentEmail + " rated Paper #" + paper.getId() + ": " + rating + " stars",
            "PAPER_RATED"
        ));
    }

    @Override
    public void onPaperReported(PastPaper paper, String reporterEmail, String reason) {
        activityLogRepository.save(new ActivityLog(
            "Paper #" + paper.getId() + " reported by " + reporterEmail,
            "PAPER_REPORTED"
        ));
    }

    @Override
    public void onCommentPosted(PaperComment comment) {
        activityLogRepository.save(new ActivityLog(
            "Comment on Paper #" + comment.getPaperId() + " by " + comment.getStudentEmail(),
            "PAPER_COMMENT_ADDED"
        ));
    }

    @Override
    public void onCommentDeleted(PaperComment comment, String studentEmail) {
        activityLogRepository.save(new ActivityLog(
            "Comment #" + comment.getId() + " deleted by " + studentEmail,
            "PAPER_COMMENT_DELETED"
        ));
    }

    @Override
    public void onReportResolved(Long reportId) {
        activityLogRepository.save(new ActivityLog(
            "Report #" + reportId + " resolved",
            "REPORT_RESOLVED"
        ));
    }

    @Override
    public void onPaperDownloaded(PastPaper paper) {
        activityLogRepository.save(new ActivityLog(
            "Paper #" + paper.getId() + " (" + paper.getCourseName() + ") downloaded",
            "PAPER_DOWNLOAD"
        ));
    }
}
