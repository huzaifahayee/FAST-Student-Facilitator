package com.fast.fsf.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Reminder Entity (UC-24, 25, 26)
 * 
 * Represents a personal pop reminder for a student.
 */
@Entity
@Table(name = "reminders")
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime reminderTime;

    @Column(nullable = false)
    private String category; // e.g., "ASSIGNMENT", "QUIZ", "GENERAL"

    @Column(nullable = false)
    private String studentEmail;

    public Reminder() {}

    public Reminder(String title, LocalDateTime reminderTime, String category, String studentEmail) {
        this.title = title;
        this.reminderTime = reminderTime;
        this.category = category;
        this.studentEmail = studentEmail;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getReminderTime() { return reminderTime; }
    public void setReminderTime(LocalDateTime reminderTime) { this.reminderTime = reminderTime; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
}
