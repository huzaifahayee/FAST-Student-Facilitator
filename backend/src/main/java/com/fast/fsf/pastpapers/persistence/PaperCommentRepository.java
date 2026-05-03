package com.fast.fsf.pastpapers.persistence;

import com.fast.fsf.pastpapers.domain.PaperComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaperCommentRepository extends JpaRepository<PaperComment, Long> {
    List<PaperComment> findByPaperIdOrderByPostedAtAsc(Long paperId);
    Optional<PaperComment> findByIdAndStudentEmail(Long id, String email);
    List<PaperComment> findByPaperId(Long paperId);
}
