package com.fast.fsf.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "past_papers")
public class PastPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String courseName;

    @Column(nullable = false)
    private String courseCode;

    @Column(nullable = false)
    private String semesterYear;

    @Column(nullable = false)
    private String examType; // "MIDTERM" | "FINAL" | "QUIZ"

    @Column(nullable = false)
    private String instructorName;

    @Column(nullable = false)
    private String googleDriveLink;

    @Column(nullable = false)
    private String ownerEmail;

    @Column(nullable = false)
    private String ownerName;

    private double averageRating = 0.0;
    private int ratingCount = 0;
    private LocalDateTime uploadedAt;

    private boolean approved = false;
    private boolean flagged = false;
    private String moderationReason;

    public PastPaper() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getSemesterYear() { return semesterYear; }
    public void setSemesterYear(String semesterYear) { this.semesterYear = semesterYear; }

    public String getExamType() { return examType; }
    public void setExamType(String examType) {
        if (!"MIDTERM".equals(examType) && !"FINAL".equals(examType) && !"QUIZ".equals(examType)) {
            throw new IllegalArgumentException("Exam type must be MIDTERM, FINAL, or QUIZ");
        }
        this.examType = examType;
    }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public String getGoogleDriveLink() { return googleDriveLink; }
    public void setGoogleDriveLink(String googleDriveLink) {
        if (googleDriveLink == null || !googleDriveLink.startsWith("https://")) {
            throw new IllegalArgumentException("Google Drive link must start with https://");
        }
        this.googleDriveLink = googleDriveLink;
    }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }

    public String getModerationReason() { return moderationReason; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }
}
