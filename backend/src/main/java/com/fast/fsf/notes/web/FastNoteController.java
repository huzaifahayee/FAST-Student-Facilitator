package com.fast.fsf.notes.web;

import com.fast.fsf.notes.domain.FastNote;
import com.fast.fsf.notes.service.FastNoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import org.springframework.http.ContentDisposition;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import java.util.List;

/**
 * Singleton pattern (GoF): Spring's default {@code @RestController} scope is singleton — one shared
 * instance per JVM context, satisfying the Singleton intent without manual locking.
 * <p>
 * This controller delegates all business logic to {@link com.fast.fsf.notes.service.FastNoteService},
 * keeping HTTP concerns (request mapping, multipart handling, response codes, CORS) strictly
 * separated from domain logic.
 */
@RestController
@RequestMapping("/api/notes")
@CrossOrigin(origins = "*")
public class FastNoteController {


    @Autowired
    private FastNoteService service;

    @GetMapping
    public List<FastNote> getNotes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String studentEmail) {
        return service.getNotes(keyword, subject, studentEmail);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FastNote> uploadNote(
            @RequestParam("title") String title,
            @RequestParam("subjectName") String subjectName,
            @RequestParam("courseCode") String courseCode,
            @RequestParam("studentEmail") String studentEmail,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.uploadNote(title, subjectName, courseCode, studentEmail, file));
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadNoteFile(@PathVariable String fileName) {
        try {
            Path file = service.getNoteFile(fileName);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                String extension = fileName.substring(fileName.lastIndexOf("."));

                com.fast.fsf.notes.domain.FastNote note = service.getNoteByFileName(fileName);
                String finalName;
                if (note != null) {
                    // Build: COURSECODE_SUBJECTNAME_Title.pdf
                    String safe = note.getCourseCode().toUpperCase().replaceAll("[^a-zA-Z0-9\\-]", "_")
                            + "_" + note.getSubjectName().toUpperCase().replaceAll("[^a-zA-Z0-9 ]", "_")
                            + "_" + note.getTitle().replaceAll("[^a-zA-Z0-9 ]", "_")
                            + extension.toLowerCase();
                    finalName = safe;
                } else {
                    finalName = fileName;
                }

                ContentDisposition contentDisposition = ContentDisposition.attachment()
                        .filename(finalName, StandardCharsets.UTF_8)
                        .build();

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    @PutMapping("/{id}/vote")
    public ResponseEntity<FastNote> voteNote(
            @PathVariable Long id,
            @RequestParam String studentEmail,
            @RequestParam String type) { // type = "UPVOTE" or "DOWNVOTE"
        return ResponseEntity.ok(service.voteNote(id, studentEmail, type));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        service.deleteNote(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Catches Spring's multipart size guard before it becomes an opaque 500.
     * Returns HTTP 413 with a clear JSON body the frontend can read.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleSizeExceeded(MaxUploadSizeExceededException ex) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("message", "File is too large. Maximum allowed size is 20 MB."));
    }
}
