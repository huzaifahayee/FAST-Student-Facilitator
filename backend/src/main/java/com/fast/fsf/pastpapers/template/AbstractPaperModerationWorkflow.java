package com.fast.fsf.pastpapers.template;

import com.fast.fsf.pastpapers.domain.PastPaper;
import com.fast.fsf.pastpapers.observer.PaperEventPublisher;
import com.fast.fsf.pastpapers.persistence.PastPaperRepository;

/**
 * Template Method pattern — defines the fixed sequence of steps for
 * any moderation action on a PastPaper.
 *
 * Mirror: carpool/template/AbstractRideModerationWorkflow.java (hypothetical)
 */
public abstract class AbstractPaperModerationWorkflow {

    protected final PastPaperRepository paperRepository;
    protected final PaperEventPublisher eventPublisher;

    protected AbstractPaperModerationWorkflow(PastPaperRepository paperRepository, PaperEventPublisher eventPublisher) {
        this.paperRepository = paperRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * The Template Method.
     */
    public final PastPaper execute(Long paperId, String reason) {
        PastPaper paper = findPaper(paperId);
        validateTransition(paper);
        applyChange(paper, reason);
        PastPaper saved = paperRepository.save(paper);
        publishEvent(saved, reason);
        return saved;
    }

    protected PastPaper findPaper(Long id) {
        return paperRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found with ID: " + id));
    }

    protected abstract void validateTransition(PastPaper paper);

    protected abstract void applyChange(PastPaper paper, String reason);

    protected abstract void publishEvent(PastPaper paper, String reason);
}
