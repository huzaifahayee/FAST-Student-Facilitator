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

    // Event Board feed: every approved event whose organizer is NOT one of the
    // calendar-import organizers. A user-posted event with semesterPlan=true
    // therefore shows in BOTH the Event Board and the Semester Plan.
    List<CampusEvent> findByApprovedTrueAndOrganizerNotInOrderByEventDateAsc(List<String> organizers);
    
    // UC-23: Semester Plan (Items marked as semester plan)
    List<CampusEvent> findByApprovedTrueAndSemesterPlanTrueOrderByEventDateAsc();
    
    List<CampusEvent> findByApprovedFalse();
    
    List<CampusEvent> findByOwnerEmail(String email);
    
    List<CampusEvent> findByEventDateAfterAndApprovedTrue(LocalDate date);

    // Calendar-imported semester-plan items (organizer set by the parser).
    // Used to wipe only previously-imported items on re-upload, leaving
    // user-posted semester-plan items untouched.
    List<CampusEvent> findBySemesterPlanTrueAndOrganizerIn(List<String> organizers);
}
