package com.fast.fsf.shared.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ActivityLog Entity
 * 
 * Stores system events for Admin visibility.
 * Example: "M. Huzaifa offered a new ride to Faisal Town."
 */
@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String logType; // e.g., "RIDE_POSTED", "RIDE_APPROVED", "RIDE_FLAGGED"

    public ActivityLog() {}

    public ActivityLog(String message, String logType) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.logType = logType;
    }

    // Getters
    public Long getId() { return id; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getLogType() { return logType; }
}
