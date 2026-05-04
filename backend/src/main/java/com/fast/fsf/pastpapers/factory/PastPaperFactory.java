package com.fast.fsf.pastpapers.factory;

import com.fast.fsf.pastpapers.domain.PaperComment;
import com.fast.fsf.pastpapers.domain.PaperRating;
import com.fast.fsf.pastpapers.domain.PaperReport;
import com.fast.fsf.pastpapers.domain.PastPaper;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Factory Method pattern — centralises PastPaper construction and
 * enforces all creation-time validation rules in one place.
 * The controller delegates object creation here instead of using
 * new PastPaper() inline, ensuring business rules (non-blank fields,
 * valid examType, valid Drive link) are always applied consistently.
 *
 * Mirror: carpool/factory/RideOfferFactory.java
 */
public class PastPaperFactory {

    public static PastPaper createPaper(
            String courseName, String courseCode, String semesterYear,
            String examType, String instructorName, String googleDriveLink,
            String ownerEmail, String ownerName) {

        validateNotBlank(courseName, "Course Name");
        validateNotBlank(courseCode, "Course Code");
        validateNotBlank(semesterYear, "Semester/Year");
        validateNotBlank(examType, "Exam Type");
        validateNotBlank(instructorName, "Instructor Name");
        validateNotBlank(googleDriveLink, "Google Drive Link");
        validateNotBlank(ownerEmail, "Owner Email");
        validateNotBlank(ownerName, "Owner Name");

        if (!Arrays.asList("MIDTERM", "FINAL", "QUIZ").contains(examType.toUpperCase())) {
            throw new IllegalArgumentException("Exam Type must be MIDTERM, FINAL, or QUIZ");
        }

        if (!googleDriveLink.toLowerCase().startsWith("https://")) {
            throw new IllegalArgumentException("Google Drive Link must start with https://");
        }

        PastPaper paper = new PastPaper();
        paper.setCourseName(courseName);
        paper.setCourseCode(courseCode);
        paper.setSemesterYear(semesterYear);
        paper.setExamType(examType.toUpperCase());
        paper.setInstructorName(instructorName);
        paper.setGoogleDriveLink(googleDriveLink);
        paper.setOwnerEmail(ownerEmail);
        paper.setOwnerName(ownerName);
        
        paper.setApproved(false);
        paper.setFlagged(false);
        paper.setUploadedAt(LocalDateTime.now());
        paper.setAverageRating(0.0);
        paper.setRatingCount(0);
        
        return paper;
    }

    public static PaperComment createComment(
            Long paperId, String studentEmail, String content) {
        validateNotBlank(content, "Comment content");
        validateNotBlank(studentEmail, "Student Email");

        PaperComment comment = new PaperComment();
        comment.setPaperId(paperId);
        comment.setStudentEmail(studentEmail);
        comment.setContent(content);
        comment.setPostedAt(LocalDateTime.now());
        return comment;
    }

    public static PaperReport createReport(
            Long paperId, String reporterEmail, String reason) {
        validateNotBlank(reason, "Report reason");
        validateNotBlank(reporterEmail, "Reporter Email");

        PaperReport report = new PaperReport();
        report.setPaperId(paperId);
        report.setReporterEmail(reporterEmail);
        report.setReason(reason);
        report.setResolved(false);
        report.setReportedAt(LocalDateTime.now());
        return report;
    }

    public static PaperRating createRating(
            Long paperId, String studentEmail, int rating) {
        validateNotBlank(studentEmail, "Student Email");
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        PaperRating paperRating = new PaperRating();
        paperRating.setPaperId(paperId);
        paperRating.setStudentEmail(studentEmail);
        paperRating.setRating(rating);
        paperRating.setRatedAt(LocalDateTime.now());
        return paperRating;
    }

    private static void validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
    }
}
