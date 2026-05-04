package com.fast.fsf.pastpapers.template;

import com.fast.fsf.pastpapers.domain.PastPaper;
import com.fast.fsf.pastpapers.observer.PaperEventPublisher;
import com.fast.fsf.pastpapers.persistence.PastPaperRepository;
import com.fast.fsf.pastpapers.state.PaperModerationContext;
import org.springframework.stereotype.Service;

/**
 * Concrete Workflow for flagging a paper.
 */
@Service
public class FlagPaperWorkflow extends AbstractPaperModerationWorkflow {

    public FlagPaperWorkflow(PastPaperRepository paperRepository, PaperEventPublisher eventPublisher) {
        super(paperRepository, eventPublisher);
    }

    @Override
    protected void validateTransition(PastPaper paper) {
        new PaperModerationContext(paper).flag(paper, "Validation Check");
    }

    @Override
    protected void applyChange(PastPaper paper, String reason) {
        new PaperModerationContext(paper).flag(paper, reason);
    }

    @Override
    protected void publishEvent(PastPaper paper, String reason) {
        eventPublisher.publishPaperFlagged(paper, reason);
    }
}
