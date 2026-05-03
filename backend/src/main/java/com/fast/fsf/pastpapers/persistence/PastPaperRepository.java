package com.fast.fsf.pastpapers.persistence;

import com.fast.fsf.pastpapers.domain.PastPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PastPaperRepository extends JpaRepository<PastPaper, Long> {

    // mirrors RideRepository naming so admin/stats/search work automatically
    long countByApprovedTrue();
    long countByApprovedFalse();
    long countByFlaggedTrue();

    List<PastPaper> findByApprovedFalse();
    List<PastPaper> findByApprovedTrue();
    List<PastPaper> findByFlaggedTrue();

    List<PastPaper> findByApprovedTrueAndExamType(String examType);

    List<PastPaper> findByCourseNameContainingIgnoreCaseAndApprovedTrue(String name);
    List<PastPaper> findByCourseCodeContainingIgnoreCaseAndApprovedTrue(String code);

    // combined keyword search used by GET /search?query=
    @Query("SELECT p FROM PastPaper p WHERE p.approved = true AND " +
           "(LOWER(p.courseName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           " LOWER(p.courseCode) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<PastPaper> searchApproved(@Param("q") String query);
}
