package com.fast.fsf.repository;

import com.fast.fsf.model.CampusEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CampusEventRepository extends JpaRepository<CampusEvent, Long> {
    
    // UC-22: View Event Board (Approved events, filtered by date or category)
    List<CampusEvent> findByApprovedTrueOrderByEventDateAsc();
    
    List<CampusEvent> findByApprovedTrueAndSemesterPlanFalseOrderByEventDateAsc();
    
    // UC-23: Semester Plan (Items marked as semester plan)
    List<CampusEvent> findByApprovedTrueAndSemesterPlanTrueOrderByEventDateAsc();
    
    List<CampusEvent> findByApprovedFalse();
    
    List<CampusEvent> findByOwnerEmail(String email);
    
    List<CampusEvent> findByEventDateAfterAndApprovedTrue(LocalDate date);
}
