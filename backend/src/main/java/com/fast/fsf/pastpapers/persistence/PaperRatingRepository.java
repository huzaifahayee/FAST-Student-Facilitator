package com.fast.fsf.pastpapers.persistence;

import com.fast.fsf.pastpapers.domain.PaperRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaperRatingRepository extends JpaRepository<PaperRating, Long> {
    Optional<PaperRating> findByPaperIdAndStudentEmail(Long paperId, String email);
    List<PaperRating> findByPaperId(Long paperId);
}
