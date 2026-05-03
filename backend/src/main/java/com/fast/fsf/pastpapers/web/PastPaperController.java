package com.fast.fsf.pastpapers.web;

import com.fast.fsf.pastpapers.domain.PaperComment;
import com.fast.fsf.pastpapers.domain.PaperRating;
import com.fast.fsf.pastpapers.domain.PaperReport;
import com.fast.fsf.pastpapers.domain.PastPaper;
import com.fast.fsf.pastpapers.persistence.PaperCommentRepository;
import com.fast.fsf.pastpapers.persistence.PaperRatingRepository;
import com.fast.fsf.pastpapers.persistence.PaperReportRepository;
import com.fast.fsf.pastpapers.persistence.PastPaperRepository;
import com.fast.fsf.shared.model.ActivityLog;
import com.fast.fsf.shared.persistence.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/past-papers")
@CrossOrigin(originPatterns = {"http://localhost:*"})
public class PastPaperController {

    private final PastPaperRepository pastPaperRepository;
    private final PaperRatingRepository paperRatingRepository;
    private final PaperCommentRepository paperCommentRepository;
    private final PaperReportRepository paperReportRepository;
    private final ActivityLogRepository activityLogRepository;

    private static final Map<String, String> GOOGLE_DRIVE_LINKS = new HashMap<>();

    static {
        GOOGLE_DRIVE_LINKS.put("Database Systems", "https://drive.google.com/drive/folders/1b8syVaHAJ1jCM70t8LvxRqeaAoGeHyK9");
        GOOGLE_DRIVE_LINKS.put("Applied Physics", "https://drive.google.com/drive/folders/1Iy6uJGHFmvTd3pMe1jkKuEFUkCOc0IJN");
        GOOGLE_DRIVE_LINKS.put("Calculus", "https://drive.google.com/drive/folders/1PvyVrVdYE5DaMN1LGM-Zk5UmECXbcPvd");
        GOOGLE_DRIVE_LINKS.put("Discrete Structures", "https://drive.google.com/drive/folders/1VhK2MaXjLo-O5oGzOM6v5-kDYg94Ry54");
        GOOGLE_DRIVE_LINKS.put("Cloud Computing", "https://drive.google.com/drive/folders/1qHoYQsuz-jkgLdozkh1HQb_DcTbPdWBR");
        GOOGLE_DRIVE_LINKS.put("Digital Logic Design", "https://drive.google.com/drive/folders/1SZ2HkZJ02xq9oy5_RdFOeAur7IiSvHaN");
        GOOGLE_DRIVE_LINKS.put("Digital Logic Design Lab", "https://drive.google.com/drive/folders/1MtjPz-sLc0WhQFeQHmsnRUUxwpBdfjAv");
        GOOGLE_DRIVE_LINKS.put("Islamic Studies", "https://drive.google.com/drive/folders/1mw8pSWsPhIFM9rRcSQQWF-OfYKvqz8WE");
        GOOGLE_DRIVE_LINKS.put("Linear Algebra", "https://drive.google.com/drive/folders/1SUkRnSiQkyVHohHoIDXOZ6T_gWkFHyrF");
        GOOGLE_DRIVE_LINKS.put("Probability and Statistics", "https://drive.google.com/drive/folders/1knOsNuexBD1a86aFrgHUp4gym6U6ja1V");
    }

    @Autowired
    public PastPaperController(PastPaperRepository pastPaperRepository,
                               PaperRatingRepository paperRatingRepository,
                               PaperCommentRepository paperCommentRepository,
                               PaperReportRepository paperReportRepository,
                               ActivityLogRepository activityLogRepository) {
        this.pastPaperRepository = pastPaperRepository;
        this.paperRatingRepository = paperRatingRepository;
        this.paperCommentRepository = paperCommentRepository;
        this.paperReportRepository = paperReportRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @GetMapping
    public ResponseEntity<List<PastPaper>> getAllApproved(@RequestParam(required = false) String examType) {
        List<PastPaper> list;
        if (examType != null && !examType.isEmpty()) {
            list = pastPaperRepository.findByApprovedTrueAndExamType(examType);
        } else {
            list = pastPaperRepository.findByApprovedTrue();
        }
        if (list == null) list = Collections.emptyList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PastPaper>> search(@RequestParam String query) {
        List<PastPaper> results = pastPaperRepository.searchApproved(query);
        if (results == null) results = Collections.emptyList();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/count/active")
    public long getActiveCount() {
        return pastPaperRepository.countByApprovedTrue();
    }

    @GetMapping("/pending")
    public List<PastPaper> getPendingPapers() {
        return pastPaperRepository.findByApprovedFalse();
    }

    @GetMapping("/flagged")
    public List<PastPaper> getFlaggedPapers() {
        return pastPaperRepository.findByFlaggedTrue();
    }

    @GetMapping("/flagged/count")
    public long getFlaggedCount() {
        return pastPaperRepository.countByFlaggedTrue();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPaperDetails(@PathVariable Long id) {
        return pastPaperRepository.findById(id).map(paper -> {
            if (!paper.isApproved()) {
                return ResponseEntity.notFound().<Map<String, Object>>build();
            }
            List<PaperComment> comments = paperCommentRepository.findByPaperIdOrderByPostedAtAsc(id);
            Map<String, Object> response = new HashMap<>();
            response.put("paper", paper);
            response.put("comments", comments);
            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Map<String, String>> downloadPaper(@PathVariable Long id) {
        return pastPaperRepository.findById(id).map(paper -> {
            if (!paper.isApproved()) {
                return ResponseEntity.notFound().<Map<String, String>>build();
            }
            String driveLink = GOOGLE_DRIVE_LINKS.getOrDefault(paper.getCourseName(), paper.getGoogleDriveLink());
            
            activityLogRepository.save(new ActivityLog(
                "Paper #" + id + " (" + paper.getCourseName() + ") downloaded",
                "PAPER_DOWNLOAD"
            ));
            
            return ResponseEntity.ok(Map.of("googleDriveLink", driveLink));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> uploadPaper(@RequestBody PastPaper paper) {
        if (paper.getCourseName() == null || paper.getCourseName().trim().isEmpty() ||
            paper.getCourseCode() == null || paper.getCourseCode().trim().isEmpty() ||
            paper.getSemesterYear() == null || paper.getSemesterYear().trim().isEmpty() ||
            paper.getExamType() == null || paper.getExamType().trim().isEmpty() ||
            paper.getInstructorName() == null || paper.getInstructorName().trim().isEmpty() ||
            paper.getGoogleDriveLink() == null || paper.getGoogleDriveLink().trim().isEmpty() ||
            paper.getOwnerEmail() == null || paper.getOwnerEmail().trim().isEmpty() ||
            paper.getOwnerName() == null || paper.getOwnerName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("All fields must be provided and cannot be blank.");
        }

        try {
            paper.setGoogleDriveLink(paper.getGoogleDriveLink());
            paper.setExamType(paper.getExamType());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        paper.setApproved(false);
        paper.setFlagged(false);
        paper.setUploadedAt(LocalDateTime.now());
        paper.setAverageRating(0.0);
        paper.setRatingCount(0);

        PastPaper saved = pastPaperRepository.save(paper);

        activityLogRepository.save(new ActivityLog(
            paper.getOwnerName() + " uploaded paper: " + paper.getCourseName(),
            "PAPER_UPLOADED"
        ));

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approvePaper(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return pastPaperRepository.findById(id).map(paper -> {
            if (paper.isApproved()) {
                return ResponseEntity.badRequest().body("Paper is already approved");
            }
            paper.setApproved(true);
            paper.setModerationReason(reason);
            PastPaper saved = pastPaperRepository.save(paper);

            activityLogRepository.save(new ActivityLog(
                "Paper #" + id + " approved",
                "PAPER_APPROVED"
            ));
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/flag")
    public ResponseEntity<PastPaper> flagPaper(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return pastPaperRepository.findById(id).map(paper -> {
            paper.setFlagged(true);
            paper.setModerationReason(reason);
            PastPaper saved = pastPaperRepository.save(paper);

            activityLogRepository.save(new ActivityLog(
                "Paper #" + id + " flagged: " + reason,
                "PAPER_FLAGGED"
            ));
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<PastPaper> resolvePaper(@PathVariable Long id) {
        return pastPaperRepository.findById(id).map(paper -> {
            paper.setFlagged(false);
            paper.setModerationReason(null);
            PastPaper saved = pastPaperRepository.save(paper);

            activityLogRepository.save(new ActivityLog(
                "Flag on Paper #" + id + " resolved",
                "PAPER_FLAG_RESOLVED"
            ));
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePaper(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return pastPaperRepository.findById(id).map(paper -> {
            paperRatingRepository.deleteAll(paperRatingRepository.findByPaperId(id));
            paperCommentRepository.deleteAll(paperCommentRepository.findByPaperId(id));
            paperReportRepository.deleteAll(paperReportRepository.findByPaperId(id));
            
            pastPaperRepository.delete(paper);

            activityLogRepository.save(new ActivityLog(
                "Paper #" + id + " deleted. Reason: " + (reason != null ? reason : "None"),
                "PAPER_DELETED"
            ));
            return ResponseEntity.ok("Paper and all associated data deleted");
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/reject")
    public ResponseEntity<?> rejectPaper(@PathVariable Long id) {
        return pastPaperRepository.findById(id).map(paper -> {
            if (paper.isApproved()) {
                return ResponseEntity.badRequest().body("Cannot reject an approved paper. Use delete instead.");
            }
            pastPaperRepository.delete(paper);

            activityLogRepository.save(new ActivityLog(
                "Paper #" + id + " rejected",
                "PAPER_REJECTED"
            ));
            return ResponseEntity.ok("Paper rejected");
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<?> ratePaper(@PathVariable Long id, @RequestBody PaperRating ratingRequest) {
        return pastPaperRepository.findById(id).map(paper -> {
            if (!paper.isApproved()) {
                return ResponseEntity.notFound().build();
            }
            try {
                PaperRating existing = paperRatingRepository
                    .findByPaperIdAndStudentEmail(id, ratingRequest.getStudentEmail()).orElse(null);

                if (existing != null) {
                    existing.setRating(ratingRequest.getRating());
                    existing.setRatedAt(LocalDateTime.now());
                    paperRatingRepository.save(existing);
                } else {
                    PaperRating newRating = new PaperRating();
                    newRating.setPaperId(id);
                    newRating.setStudentEmail(ratingRequest.getStudentEmail());
                    newRating.setRating(ratingRequest.getRating());
                    newRating.setRatedAt(LocalDateTime.now());
                    paperRatingRepository.save(newRating);
                }

                List<PaperRating> allRatings = paperRatingRepository.findByPaperId(id);
                double avg = allRatings.stream().mapToInt(PaperRating::getRating).average().orElse(0.0);
                paper.setAverageRating(Math.round(avg * 10.0) / 10.0);
                paper.setRatingCount(allRatings.size());
                PastPaper savedPaper = pastPaperRepository.save(paper);

                activityLogRepository.save(new ActivityLog(
                    ratingRequest.getStudentEmail() + " rated Paper #" + id + ": " + ratingRequest.getRating() + " stars",
                    "PAPER_RATED"
                ));

                return ResponseEntity.ok(savedPaper);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long id, @RequestBody PaperComment commentRequest) {
        return pastPaperRepository.findById(id).map(paper -> {
            if (!paper.isApproved()) {
                return ResponseEntity.notFound().build();
            }
            if (commentRequest.getContent() == null || commentRequest.getContent().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Comment content cannot be empty");
            }
            
            PaperComment newComment = new PaperComment();
            newComment.setPaperId(id);
            newComment.setStudentEmail(commentRequest.getStudentEmail());
            newComment.setContent(commentRequest.getContent());
            newComment.setPostedAt(LocalDateTime.now());
            
            PaperComment saved = paperCommentRepository.save(newComment);

            activityLogRepository.save(new ActivityLog(
                "Comment on Paper #" + id + " by " + commentRequest.getStudentEmail(),
                "PAPER_COMMENT_ADDED"
            ));
            
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId, @RequestParam String studentEmail) {
        return paperCommentRepository.findById(commentId).map(comment -> {
            if (!comment.getStudentEmail().equals(studentEmail)) {
                return ResponseEntity.status(403).body("You can only delete your own comments");
            }
            paperCommentRepository.delete(comment);

            activityLogRepository.save(new ActivityLog(
                "Comment #" + commentId + " deleted by " + studentEmail,
                "PAPER_COMMENT_DELETED"
            ));
            
            return ResponseEntity.ok("Comment deleted");
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<?> reportPaper(@PathVariable Long id, @RequestBody PaperReport reportRequest) {
        return pastPaperRepository.findById(id).map(paper -> {
            if (!paper.isApproved()) {
                return ResponseEntity.notFound().build();
            }
            if (reportRequest.getReason() == null || reportRequest.getReason().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Report reason cannot be empty");
            }

            PaperReport newReport = new PaperReport();
            newReport.setPaperId(id);
            newReport.setReporterEmail(reportRequest.getReporterEmail());
            newReport.setReason(reportRequest.getReason());
            newReport.setResolved(false);
            newReport.setReportedAt(LocalDateTime.now());

            PaperReport saved = paperReportRepository.save(newReport);

            paper.setFlagged(true);
            pastPaperRepository.save(paper);

            activityLogRepository.save(new ActivityLog(
                "Paper #" + id + " reported by " + reportRequest.getReporterEmail(),
                "PAPER_REPORTED"
            ));

            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/reports")
    public ResponseEntity<List<PaperReport>> getReports(@PathVariable Long id) {
        return pastPaperRepository.findById(id).map(paper -> {
            List<PaperReport> reports = paperReportRepository.findByPaperId(id);
            return ResponseEntity.ok(reports);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/reports/{reportId}/resolve")
    public ResponseEntity<?> resolveReport(@PathVariable Long reportId) {
        return paperReportRepository.findById(reportId).map(report -> {
            if (report.isResolved()) {
                return ResponseEntity.badRequest().body("Report already resolved");
            }
            report.setResolved(true);
            PaperReport saved = paperReportRepository.save(report);

            activityLogRepository.save(new ActivityLog(
                "Report #" + reportId + " resolved",
                "REPORT_RESOLVED"
            ));

            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

}
