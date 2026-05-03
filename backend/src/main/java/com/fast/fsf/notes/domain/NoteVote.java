package com.fast.fsf.notes.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "note_vote", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"noteId", "studentEmail"})
})
public class NoteVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long noteId;

    @Column(nullable = false)
    private String studentEmail;

    @Column(nullable = false)
    private String voteType; // "UPVOTE" or "DOWNVOTE"

    public NoteVote() {}

    public NoteVote(Long noteId, String studentEmail, String voteType) {
        this.noteId = noteId;
        this.studentEmail = studentEmail;
        this.voteType = voteType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNoteId() { return noteId; }
    public void setNoteId(Long noteId) { this.noteId = noteId; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getVoteType() { return voteType; }
    public void setVoteType(String voteType) { this.voteType = voteType; }
}
