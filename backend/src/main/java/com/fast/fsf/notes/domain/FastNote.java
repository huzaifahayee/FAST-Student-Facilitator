package com.fast.fsf.notes.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "fast_notes")
public class FastNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String subjectName;

    @Column(nullable = false)
    private String courseCode;

    @Column(nullable = false, length = 2000)
    private String fileUrl;

    @Column(nullable = false)
    private String studentEmail;

    @Column(nullable = false)
    private LocalDate uploadDate;

    @Column(nullable = false)
    private int upvotes = 0;

    @Column(nullable = false)
    private int downvotes = 0;

    @Column(nullable = false)
    private String status = "Active"; // "Active", "Removed"

    @Transient
    private String userVoteType; // "UPVOTE", "DOWNVOTE", or null

    public FastNote() {}

    public FastNote(String title, String subjectName, String courseCode, String fileUrl, String studentEmail, LocalDate uploadDate) {
        this.title = title;
        this.subjectName = subjectName;
        this.courseCode = courseCode;
        this.fileUrl = fileUrl;
        this.studentEmail = studentEmail;
        this.uploadDate = uploadDate;
        this.upvotes = 0;
        this.downvotes = 0;
        this.status = "Active";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public LocalDate getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDate uploadDate) { this.uploadDate = uploadDate; }

    public int getUpvotes() { return upvotes; }
    public void setUpvotes(int upvotes) { this.upvotes = upvotes; }

    public int getDownvotes() { return downvotes; }
    public void setDownvotes(int downvotes) { this.downvotes = downvotes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Transient
    public int getVoteScore() {
        return upvotes - downvotes;
    }

    public String getUserVoteType() { return userVoteType; }
    public void setUserVoteType(String userVoteType) { this.userVoteType = userVoteType; }
}
