package com.fast.fsf.reminders.factory;

import com.fast.fsf.reminders.domain.Reminder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Factory Method pattern (GoF): centralises construction and validation rules for reminders (UC-25).
 * <p>
 * Ensures that all new reminders have mandatory fields and are forced to "PENDING" status.
 */
@Component
public class ReminderFactory {

    private static final Set<String> ALLOWED_CATEGORIES =
            Set.of("ASSIGNMENT", "EXAM", "QUIZ", "PROJECT", "OTHER");

    /**
     * Validates and normalises an incoming reminder.
     *
     * @throws IllegalArgumentException if validation fails.
     */
    public Reminder createReminder(Reminder incoming) {
        validate(incoming);
        
        incoming.setId(null);
        incoming.setStatus("PENDING");
        incoming.setCategory(incoming.getCategory().toUpperCase());
        
        return incoming;
    }

    /**
     * Validates an existing reminder for updates.
     */
    public void validate(Reminder r) {
        if (r.getTitle() == null || r.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (r.getReminderTime() == null) {
            throw new IllegalArgumentException("Date & Time is required");
        }
        if (r.getCategory() == null || r.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Category is required");
        }
        if (!ALLOWED_CATEGORIES.contains(r.getCategory().toUpperCase())) {
            throw new IllegalArgumentException("Category must be one of: Assignment, Exam, Quiz, Project, Other");
        }
        if (r.getStudentEmail() == null || r.getStudentEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Student email is required");
        }
    }
}
