package com.fast.fsf.pastpapers.template;

import com.fast.fsf.pastpapers.domain.PastPaper;
import com.fast.fsf.pastpapers.observer.PaperEventPublisher;
import com.fast.fsf.pastpapers.persistence.PaperCommentRepository;
import com.fast.fsf.pastpapers.persistence.PaperRatingRepository;
import com.fast.fsf.pastpapers.persistence.PaperReportRepository;
import com.fast.fsf.pastpapers.persistence.PastPaperRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Concrete Workflow for deleting a paper and its associated data.
 * Follows the Template Method orchestration logic even though it results in deletion.
 */
@Service
public class DeletePaperWorkflow {

    private final PastPaperRepository paperRepository;
    private final PaperRatingRepository ratingRepository;
    private final PaperCommentRepository commentRepository;
    private final PaperReportRepository reportRepository;
    private final PaperEventPublisher eventPublisher;

    public DeletePaperWorkflow(PastPaperRepository paperRepository,
                               PaperRatingRepository ratingRepository,
                               PaperCommentRepository commentRepository,
                               PaperReportRepository reportRepository,
                               PaperEventPublisher eventPublisher) {
        this.paperRepository = paperRepository;
        this.ratingRepository = ratingRepository;
        this.commentRepository = commentRepository;
        this.reportRepository = reportRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(Long paperId, String reason) {
        PastPaper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found with ID: " + paperId));
        
        // Cascade delete children (enforced in service layer here)
        ratingRepository.deleteAll(ratingRepository.findByPaperId(paperId));
        commentRepository.deleteAll(commentRepository.findByPaperId(paperId));
        reportRepository.deleteAll(reportRepository.findByPaperId(paperId));
        
        paperRepository.delete(paper);
        
        eventPublisher.publishPaperDeleted(paper, reason);
    }
}
