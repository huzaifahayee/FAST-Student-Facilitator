package com.fast.fsf.reminders.persistence;

import com.fast.fsf.reminders.domain.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    
    // UC-26: All reminders for a student, ordered by time
    List<Reminder> findByStudentEmailOrderByReminderTimeAsc(String email);

    // UC-24: Only PENDING reminders for the login pop-up
    List<Reminder> findByStudentEmailAndStatusOrderByReminderTimeAsc(String email, String status);

    void deleteByStudentEmail(String email);
}
