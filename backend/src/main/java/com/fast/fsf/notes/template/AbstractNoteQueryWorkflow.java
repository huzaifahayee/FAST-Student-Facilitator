package com.fast.fsf.notes.template;

import com.fast.fsf.notes.domain.FastNote;
import com.fast.fsf.notes.persistence.FastNoteRepository;
import com.fast.fsf.notes.persistence.NoteVoteRepository;

import java.util.List;

/**
 * Template Method pattern (GoF): defines the invariant skeleton for the "query notes" use case:
 * <ol>
 *   <li><strong>{@link #fetchFromRepo}</strong> — subclass decides which repository query to call
 *       based on the active filters.</li>
 *   <li><strong>Populate votes</strong> — fixed step: if a student email is provided, annotate
 *       each note with the user's current vote type (UPVOTE / DOWNVOTE / null).</li>
 *   <li><strong>Return</strong> — hand the enriched list back to the service.</li>
 * </ol>
 * <p>
 * The template is {@code final} so the control flow cannot drift in subclasses. Only the
 * {@link #fetchFromRepo} hook may vary, keeping conditional filter logic out of the service.
 * <p>
 * Mirrors {@code AbstractRideMutationWorkflow} from the carpool feature.
 */
public abstract class AbstractNoteQueryWorkflow {

    protected final FastNoteRepository noteRepository;
    protected final NoteVoteRepository voteRepository;

    protected AbstractNoteQueryWorkflow(FastNoteRepository noteRepository,
                                        NoteVoteRepository voteRepository) {
        this.noteRepository = noteRepository;
        this.voteRepository = voteRepository;
    }

    /**
     * Template method — {@code final} so the two-step skeleton (fetch → enrich) cannot drift.
     *
     * @param keyword      optional search keyword (may be {@code null}).
     * @param subject      optional subject filter (may be {@code null}).
     * @param studentEmail optional email for vote-type annotation (may be {@code null}).
     * @return enriched list of active notes matching the supplied criteria.
     */
    public final List<FastNote> query(String keyword, String subject, String studentEmail) {
        // Step 1 — Hook: subclass selects the right repository query
        List<FastNote> notes = fetchFromRepo(keyword, subject);

        // Step 2 — Fixed: populate the transient userVoteType field if an email was provided
        if (studentEmail != null && !studentEmail.isEmpty()) {
            for (FastNote note : notes) {
                voteRepository.findByNoteIdAndStudentEmail(note.getId(), studentEmail)
                        .ifPresent(v -> note.setUserVoteType(v.getVoteType()));
            }
        }

        return notes;
    }

    /**
     * Hook — subclass returns the appropriate repository result set for the given filter
     * combination. Called by the template method; never call directly.
     *
     * @param keyword optional search keyword (may be {@code null}).
     * @param subject optional subject filter (may be {@code null}).
     * @return raw (un-enriched) list from the repository.
     */
    protected abstract List<FastNote> fetchFromRepo(String keyword, String subject);
}
