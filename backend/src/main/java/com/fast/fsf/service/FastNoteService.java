package com.fast.fsf.service;

import com.fast.fsf.model.FastNote;
import com.fast.fsf.model.NoteVote;
import com.fast.fsf.repository.FastNoteRepository;
import com.fast.fsf.repository.NoteVoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class FastNoteService {

    @Autowired
    private FastNoteRepository repository;

    @Autowired
    private NoteVoteRepository voteRepository;

    public List<FastNote> getNotes(String keyword, String subject, String studentEmail) {
        List<FastNote> notes;
        if (keyword != null && !keyword.isEmpty() && subject != null && !subject.isEmpty()) {
            notes = repository.searchAndFilterOrdered(keyword, subject);
        } else if (keyword != null && !keyword.isEmpty()) {
            notes = repository.searchByKeywordOrdered(keyword);
        } else if (subject != null && !subject.isEmpty()) {
            notes = repository.filterBySubjectOrdered(subject);
        } else {
            notes = repository.findAllActiveOrdered();
        }

        // Populate userVoteType if email is provided
        if (studentEmail != null && !studentEmail.isEmpty()) {
            for (FastNote note : notes) {
                voteRepository.findByNoteIdAndStudentEmail(note.getId(), studentEmail)
                    .ifPresent(v -> note.setUserVoteType(v.getVoteType()));
            }
        }
        return notes;
    }

    private final String UPLOAD_DIR = "uploads/notes/";

    public FastNote uploadNote(String title, String subjectName, String courseCode, String studentEmail, MultipartFile file) {
        try {
            // Create directory if it doesn't exist
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalName = file.getOriginalFilename();
            if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files are accepted.");
            }
            
            String extension = originalName.substring(originalName.lastIndexOf("."));
            
            // Sanitize title but keep it recognizable
            String sanitizedTitle = title.replaceAll("[^a-zA-Z0-9.-]", "_");
            String fileName = UUID.randomUUID().toString() + "_" + sanitizedTitle + extension;
            Path filePath = uploadPath.resolve(fileName);
            
            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Create Entity
            FastNote note = new FastNote(title, subjectName, courseCode, fileName, studentEmail, LocalDate.now());
            return repository.save(note);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public Path getNoteFile(String fileName) {
        return Paths.get(UPLOAD_DIR).resolve(fileName);
    }

    public FastNote voteNote(Long id, String studentEmail, String voteType) {
        FastNote note = repository.findById(id).orElseThrow(() -> new RuntimeException("Note not found"));
        
        // Find existing vote for this user on this note
        var existingVote = voteRepository.findByNoteIdAndStudentEmail(id, studentEmail);

        if (existingVote.isPresent()) {
            NoteVote vote = existingVote.get();
            if (vote.getVoteType().equalsIgnoreCase(voteType)) {
                // If same vote type, user is clicking same button -> Remove vote (Toggle off)
                if ("UPVOTE".equalsIgnoreCase(voteType)) {
                    note.setUpvotes(Math.max(0, note.getUpvotes() - 1));
                } else {
                    note.setDownvotes(Math.max(0, note.getDownvotes() - 1));
                }
                voteRepository.delete(vote);
            } else {
                // If opposite vote type -> Switch vote
                if ("UPVOTE".equalsIgnoreCase(voteType)) {
                    note.setUpvotes(note.getUpvotes() + 1);
                    note.setDownvotes(Math.max(0, note.getDownvotes() - 1));
                } else {
                    note.setDownvotes(note.getDownvotes() + 1);
                    note.setUpvotes(Math.max(0, note.getUpvotes() - 1));
                }
                vote.setVoteType(voteType);
                voteRepository.save(vote);
            }
        } else {
            // New vote
            if ("UPVOTE".equalsIgnoreCase(voteType)) {
                note.setUpvotes(note.getUpvotes() + 1);
            } else {
                note.setDownvotes(note.getDownvotes() + 1);
            }
            voteRepository.save(new NoteVote(id, studentEmail, voteType));
        }
        
        return repository.save(note);
    }

    public FastNote getNoteByFileName(String fileName) {
        return repository.findByFileUrl(fileName).orElse(null);
    }
    public void deleteNote(Long id) {
        FastNote note = repository.findById(id).orElseThrow(() -> new RuntimeException("Note not found"));
        note.setStatus("Removed");
        repository.save(note);
    }
}
