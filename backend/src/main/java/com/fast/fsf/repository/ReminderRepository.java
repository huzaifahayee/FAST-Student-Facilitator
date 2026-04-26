package com.fast.fsf.repository;

import com.fast.fsf.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    
    // UC-25: View Reminders (Filtered by student email and ordered by time)
    List<Reminder> findByStudentEmailOrderByReminderTimeAsc(String email);
    
    void deleteByStudentEmail(String email);
}
