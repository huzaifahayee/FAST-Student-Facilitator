package com.fast.fsf.pastpapers.web;

import com.fast.fsf.pastpapers.adapter.ApprovedPaperCatalog;
import com.fast.fsf.pastpapers.criterion.*;
import com.fast.fsf.pastpapers.domain.PaperComment;
import com.fast.fsf.pastpapers.domain.PaperRating;
import com.fast.fsf.pastpapers.domain.PaperReport;
import com.fast.fsf.pastpapers.domain.PastPaper;
import com.fast.fsf.pastpapers.factory.PastPaperFactory;
import com.fast.fsf.pastpapers.observer.PaperEventPublisher;
import com.fast.fsf.pastpapers.persistence.PaperCommentRepository;
import com.fast.fsf.pastpapers.persistence.PaperRatingRepository;
import com.fast.fsf.pastpapers.persistence.PaperReportRepository;
import com.fast.fsf.pastpapers.persistence.PastPaperRepository;
import com.fast.fsf.pastpapers.service.PaperSearchService;
import com.fast.fsf.pastpapers.template.ApprovePaperWorkflow;
import com.fast.fsf.pastpapers.template.DeletePaperWorkflow;
import com.fast.fsf.pastpapers.template.FlagPaperWorkflow;
import com.fast.fsf.pastpapers.template.ResolveFlagWorkflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Singleton (Spring default scope) — one shared instance per JVM.
 * Orchestrates the Past Papers feature by delegating to specialized pattern classes.
 */
@RestController
@RequestMapping("/api/past-papers")
@CrossOrigin(originPatterns = {"http://localhost:*"})
public class PastPaperController {

    private final PastPaperRepository pastPaperRepository;
    private final PaperRatingRepository paperRatingRepository;
    private final PaperCommentRepository paperCommentRepository;
    private final PaperReportRepository paperReportRepository;
    
    // Pattern implementations
    private final PaperEventPublisher eventPublisher;
    private final PaperSearchService searchService;
    private final ApprovedPaperCatalog paperCatalog;
    private final ApprovePaperWorkflow approveWorkflow;
    private final FlagPaperWorkflow flagWorkflow;
    private final ResolveFlagWorkflow resolveFlagWorkflow;
    private final DeletePaperWorkflow deleteWorkflow;

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
                               PaperEventPublisher eventPublisher,
                               PaperSearchService searchService,
                               ApprovedPaperCatalog paperCatalog,
                               ApprovePaperWorkflow approveWorkflow,
                               FlagPaperWorkflow flagWorkflow,
                               ResolveFlagWorkflow resolveFlagWorkflow,
                               DeletePaperWorkflow deleteWorkflow) {
        this.pastPaperRepository = pastPaperRepository;
        this.paperRatingRepository = paperRatingRepository;
        this.paperCommentRepository = paperCommentRepository;
        this.paperReportRepository = paperReportRepository;
        this.eventPublisher = eventPublisher;
        this.searchService = searchService;
        this.paperCatalog = paperCatalog;
        this.approveWorkflow = approveWorkflow;
        this.flagWorkflow = flagWorkflow;
        this.resolveFlagWorkflow = resolveFlagWorkflow;
        this.deleteWorkflow = deleteWorkflow;
    }

    @GetMapping
    public ResponseEntity<List<PastPaper>> getAllApproved(@RequestParam(required = false) String examType) {
        List<PaperSearchCriterion> criteria = new ArrayList<>();
        criteria.add(new ApprovedOnlyCriterion());
        if (examType != null && !examType.isEmpty()) {
            criteria.add(new ExamTypeCriterion(examType));
        }
        
        List<PastPaper> list = searchService.search(new CompositePaperSearchCriterion(criteria));
        return ResponseEntity.ok(list);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PastPaper>> search(@RequestParam String query) {
        // Strategy Pattern — combining keyword search with approval status
        List<PaperSearchCriterion> criteria = Arrays.asList(
            new ApprovedOnlyCriterion(),
            new CompositePaperSearchCriterion(Arrays.asList(
                new CourseNameCriterion(query),
                new CourseCodeCriterion(query)
            )) {
                @Override
                public boolean matches(PastPaper paper) {
                    // Overriding for OR logic between Name and Code as per original search logic
                    return new CourseNameCriterion(query).matches(paper) || 
                           new CourseCodeCriterion(query).matches(paper);
                }
            }
        );
        
        List<PastPaper> results = searchService.search(new CompositePaperSearchCriterion(criteria));
        return ResponseEntity.ok(results);
    }

    @GetMapping("/count/active")
    public long getActiveCount() {
        return paperCatalog.findAllApproved().size();
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
        return paperCatalog.findApprovedById(id).map(paper -> {
            List<PaperComment> comments = paperCommentRepository.findByPaperIdOrderByPostedAtAsc(id);
            Map<String, Object> response = new HashMap<>();
            response.put("paper", paper);
            response.put("comments", comments);
            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Map<String, String>> downloadPaper(@PathVariable Long id) {
        return paperCatalog.findApprovedById(id).map(paper -> {
            String driveLink = GOOGLE_DRIVE_LINKS.getOrDefault(paper.getCourseName(), paper.getGoogleDriveLink());
            eventPublisher.publishPaperDownloaded(paper);
            return ResponseEntity.ok(Map.of("googleDriveLink", driveLink));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> uploadPaper(@RequestBody PastPaper paper) {
        try {
            // Factory Method Pattern — centralization of creation and validation
            PastPaper newPaper = PastPaperFactory.createPaper(
                paper.getCourseName(), paper.getCourseCode(), paper.getSemesterYear(),
                paper.getExamType(), paper.getInstructorName(), paper.getGoogleDriveLink(),
                paper.getOwnerEmail(), paper.getOwnerName()
            );

            PastPaper saved = pastPaperRepository.save(newPaper);
            eventPublisher.publishPaperUploaded(saved);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approvePaper(@PathVariable Long id, @RequestParam(required = false) String reason) {
        try {
            // Template Method Pattern — fixed moderation sequence
            PastPaper saved = approveWorkflow.execute(id, reason);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/flag")
    public ResponseEntity<?> flagPaper(@PathVariable Long id, @RequestParam(required = false) String reason) {
        try {
            PastPaper saved = flagWorkflow.execute(id, reason);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolvePaper(@PathVariable Long id) {
        try {
            PastPaper saved = resolveFlagWorkflow.execute(id, null);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePaper(@PathVariable Long id, @RequestParam(required = false) String reason) {
        try {
            deleteWorkflow.execute(id, reason);
            return ResponseEntity.ok("Paper and all associated data deleted");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/reject")
    public ResponseEntity<?> rejectPaper(@PathVariable Long id) {
        return pastPaperRepository.findById(id).map(paper -> {
            if (paper.isApproved()) {
                return ResponseEntity.badRequest().body("Cannot reject an approved paper. Use delete instead.");
            }
            pastPaperRepository.delete(paper);
            eventPublisher.publishPaperRejected(paper);
            return ResponseEntity.ok("Paper rejected");
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<?> ratePaper(@PathVariable Long id, @RequestBody PaperRating ratingRequest) {
        return paperCatalog.findApprovedById(id).map(paper -> {
            try {
                // Factory Pattern for Rating creation
                PaperRating rating = PastPaperFactory.createRating(id, ratingRequest.getStudentEmail(), ratingRequest.getRating());
                
                PaperRating existing = paperRatingRepository
                    .findByPaperIdAndStudentEmail(id, rating.getStudentEmail()).orElse(null);

                if (existing != null) {
                    existing.setRating(rating.getRating());
                    existing.setRatedAt(rating.getRatedAt());
                    paperRatingRepository.save(existing);
                } else {
                    paperRatingRepository.save(rating);
                }

                List<PaperRating> allRatings = paperRatingRepository.findByPaperId(id);
                double avg = allRatings.stream().mapToInt(PaperRating::getRating).average().orElse(0.0);
                paper.setAverageRating(Math.round(avg * 10.0) / 10.0);
                paper.setRatingCount(allRatings.size());
                PastPaper savedPaper = pastPaperRepository.save(paper);

                eventPublisher.publishPaperRated(savedPaper, rating.getStudentEmail(), rating.getRating());
                return ResponseEntity.ok(savedPaper);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long id, @RequestBody PaperComment commentRequest) {
        return paperCatalog.findApprovedById(id).map(paper -> {
            try {
                // Factory Pattern for Comment creation
                PaperComment newComment = PastPaperFactory.createComment(id, commentRequest.getStudentEmail(), commentRequest.getContent());
                PaperComment saved = paperCommentRepository.save(newComment);
                eventPublisher.publishCommentPosted(saved);
                return ResponseEntity.ok(saved);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId, @RequestParam String studentEmail) {
        return paperCommentRepository.findById(commentId).map(comment -> {
            if (!comment.getStudentEmail().equals(studentEmail)) {
                return ResponseEntity.status(403).body("You can only delete your own comments");
            }
            paperCommentRepository.delete(comment);
            eventPublisher.publishCommentDeleted(comment, studentEmail);
            return ResponseEntity.ok("Comment deleted");
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<?> reportPaper(@PathVariable Long id, @RequestBody PaperReport reportRequest) {
        return paperCatalog.findApprovedById(id).map(paper -> {
            try {
                // Factory Pattern for Report creation
                PaperReport newReport = PastPaperFactory.createReport(id, reportRequest.getReporterEmail(), reportRequest.getReason());
                PaperReport saved = paperReportRepository.save(newReport);

                paper.setFlagged(true);
                pastPaperRepository.save(paper);
                eventPublisher.publishPaperReported(paper, newReport.getReporterEmail(), newReport.getReason());

                return ResponseEntity.ok(saved);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
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
            eventPublisher.publishReportResolved(reportId);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

}
