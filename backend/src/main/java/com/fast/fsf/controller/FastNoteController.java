package com.fast.fsf.controller;

import com.fast.fsf.model.FastNote;
import com.fast.fsf.service.FastNoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import org.springframework.http.ContentDisposition;
import java.nio.charset.StandardCharsets;

import java.util.List;

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

                com.fast.fsf.model.FastNote note = service.getNoteByFileName(fileName);
                String finalName;
                if (note != null) {
                    // Build: courseCode_subjectName_title.pdf
                    String safe = note.getCourseCode().replaceAll("[^a-zA-Z0-9\\-]", "_")
                            + "_" + note.getSubjectName().replaceAll("[^a-zA-Z0-9 ]", "_")
                            + "_" + note.getTitle().replaceAll("[^a-zA-Z0-9 ]", "_")
                            + extension;
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
}
