package com.fast.fsf.repository;

import com.fast.fsf.model.NoteVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoteVoteRepository extends JpaRepository<NoteVote, Long> {
    Optional<NoteVote> findByNoteIdAndStudentEmail(Long noteId, String studentEmail);
}
