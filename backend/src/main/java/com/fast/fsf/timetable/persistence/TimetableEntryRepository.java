package com.fast.fsf.timetable.persistence;

import com.fast.fsf.timetable.domain.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * TimetableEntryRepository
 */
@Repository
public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {
    
    // Custom moderation queries
    long countByFlaggedTrue();
    List<TimetableEntry> findByFlaggedTrue();
    
    // Approval filtering
    long countByApprovedTrue();
    List<TimetableEntry> findByApprovedTrue();
    List<TimetableEntry> findByApprovedFalse();
    long countByApprovedFalse();
    
    // Search queries
    List<TimetableEntry> findByCourseNameContainingIgnoreCaseAndApprovedTrue(String courseName);
    
    // Specific queries for Timetable Manager UI
    List<TimetableEntry> findByDepartmentAndBatchAndSectionAndApprovedTrue(String department, String batch, String section);
}
