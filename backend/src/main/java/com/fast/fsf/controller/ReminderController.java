package com.fast.fsf.controller;

import com.fast.fsf.model.Reminder;
import com.fast.fsf.repository.ReminderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ReminderController (UC-24, 25, 26)
 * 
 * Manages personal pop reminders for students.
 */
@RestController
@RequestMapping("/api/reminders")
@CrossOrigin(origins = "http://localhost:5173")
public class ReminderController {

    private final ReminderRepository reminderRepository;

    public ReminderController(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    // UC-25: Get all reminders for a specific student
    @GetMapping
    public List<Reminder> getMyReminders(@RequestParam String email) {
        return reminderRepository.findByStudentEmailOrderByReminderTimeAsc(email);
    }

    // UC-24: Add a new reminder
    @PostMapping
    public ResponseEntity<Reminder> addReminder(@RequestBody Reminder reminder) {
        Reminder saved = reminderRepository.save(reminder);
        return ResponseEntity.ok(saved);
    }

    // UC-26: Delete/Dismiss a reminder
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReminder(@PathVariable Long id) {
        if (reminderRepository.existsById(id)) {
            reminderRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
