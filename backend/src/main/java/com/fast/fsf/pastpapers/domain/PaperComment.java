package com.fast.fsf.pastpapers.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "paper_comments")
public class PaperComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long paperId;

    @Column(nullable = false)
    private String studentEmail;

    @Column(nullable = false, length = 2000)
    private String content;

    private LocalDateTime postedAt;

    public PaperComment() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPaperId() { return paperId; }
    public void setPaperId(Long paperId) { this.paperId = paperId; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }
}
