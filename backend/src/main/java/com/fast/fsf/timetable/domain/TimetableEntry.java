package com.fast.fsf.timetable.domain;

import jakarta.persistence.*;

/**
 * TimetableEntry Entity
 * 
 * Represents a single class session in the weekly timetable.
 */
@Entity
@Table(name = "timetable_entries")
public class TimetableEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String batch;

    @Column(nullable = false)
    private String section;

    @Column(nullable = false)
    private String dayOfWeek;

    @Column(nullable = false)
    private String startTime;

    @Column(nullable = false)
    private String endTime;

    @Column(nullable = false)
    private String courseName;

    @Column(nullable = false)
    private String instructorName;

    @Column(nullable = false)
    private String roomNumber;

    // Owner (Admin who uploaded)
    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private String ownerEmail;

    // Moderation triplet required by architecture
    @Column(nullable = false)
    private boolean flagged = false;

    @Column(nullable = false)
    private boolean approved = false;

    private String moderationReason;

    public TimetableEntry() {
        this.flagged = false;
        this.approved = false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getModerationReason() { return moderationReason; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }

    @Override
    public String toString() {
        return "TimetableEntry{" +
                "dept='" + department + '\'' +
                ", batch='" + batch + '\'' +
                ", section='" + section + '\'' +
                ", day='" + dayOfWeek + '\'' +
                ", time='" + startTime + "-" + endTime + '\'' +
                ", course='" + courseName + '\'' +
                ", room='" + roomNumber + '\'' +
                '}';
    }
}
