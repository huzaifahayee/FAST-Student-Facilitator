package com.fast.fsf.notes.template;

import com.fast.fsf.notes.domain.FastNote;
import com.fast.fsf.notes.persistence.FastNoteRepository;
import com.fast.fsf.notes.persistence.NoteVoteRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Template Method pattern (GoF): concrete implementation of {@link AbstractNoteQueryWorkflow}
 * that implements the {@link #fetchFromRepo} hook with the same four-branch filter logic
 * previously inline inside {@link com.fast.fsf.notes.service.FastNoteService#getNotes}.
 * <p>
 * The branch semantics are byte-for-byte identical to the original service method, so no
 * query behaviour or result ordering changes.
 * <p>
 * Spring manages this as a singleton {@code @Component} (consistent with the Singleton
 * documentation on the service bean).
 */
@Component
public class StandardNoteQueryWorkflow extends AbstractNoteQueryWorkflow {

    public StandardNoteQueryWorkflow(FastNoteRepository noteRepository,
                                     NoteVoteRepository voteRepository) {
        super(noteRepository, voteRepository);
    }

    /**
     * Hook implementation: selects the repository query that matches the active filter
     * combination. Logic is identical to the original {@code FastNoteService.getNotes} branches.
     */
    @Override
    protected List<FastNote> fetchFromRepo(String keyword, String subject) {
        if (keyword != null && !keyword.isEmpty() && subject != null && !subject.isEmpty()) {
            return noteRepository.searchAndFilterOrdered(keyword, subject);
        } else if (keyword != null && !keyword.isEmpty()) {
            return noteRepository.searchByKeywordOrdered(keyword);
        } else if (subject != null && !subject.isEmpty()) {
            return noteRepository.filterBySubjectOrdered(subject);
        } else {
            return noteRepository.findAllActiveOrdered();
        }
    }
}
