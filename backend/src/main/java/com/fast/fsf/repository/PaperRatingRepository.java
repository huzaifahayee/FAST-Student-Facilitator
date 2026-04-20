package com.fast.fsf.repository;

import com.fast.fsf.model.PaperRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaperRatingRepository extends JpaRepository<PaperRating, Long> {
    Optional<PaperRating> findByPaperIdAndStudentEmail(Long paperId, String email);
    List<PaperRating> findByPaperId(Long paperId);
}
