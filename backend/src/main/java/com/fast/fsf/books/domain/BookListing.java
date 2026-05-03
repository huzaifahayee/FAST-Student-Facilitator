package com.fast.fsf.books.domain;

import jakarta.persistence.*;

/**
 * BookListing Entity
 * 
 * Represents a listing for buying, selling, or exchanging books.
 */
@Entity
@Table(name = "book_listings")
public class BookListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String bookTitle;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private String courseCode;

    @Column(nullable = false)
    private String bookCondition;

    private Double price;

    @Column(columnDefinition = "TEXT")
    private String frontCoverImage;

    @Column(columnDefinition = "TEXT")
    private String backCoverImage;

    @Column(nullable = false)
    private String listingType; // SELL, BUY, EXCHANGE

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, CLOSED

    // Owner info
    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private String ownerEmail;

    // Moderation triplet
    @Column(nullable = false)
    private boolean flagged = false;

    @Column(nullable = false)
    private boolean approved = false;

    private String moderationReason;

    public BookListing() {
        this.flagged = false;
        this.approved = false;
        this.status = "ACTIVE";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getBookCondition() { return bookCondition; }
    public void setBookCondition(String bookCondition) { this.bookCondition = bookCondition; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getFrontCoverImage() { return frontCoverImage; }
    public void setFrontCoverImage(String frontCoverImage) { this.frontCoverImage = frontCoverImage; }

    public String getBackCoverImage() { return backCoverImage; }
    public void setBackCoverImage(String backCoverImage) { this.backCoverImage = backCoverImage; }

    public String getListingType() { return listingType; }
    public void setListingType(String listingType) { this.listingType = listingType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getModerationReason() { return moderationReason; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }
}
