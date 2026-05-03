package com.fast.fsf.lostfound.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "lost_found_listings")
public class LostFoundListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String itemName;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String status; // "Active", "Resolved"

    @Column(nullable = false)
    private String type; // "Lost", "Found"

    @Column(nullable = false)
    private String studentEmail;

    public LostFoundListing() {}

    public LostFoundListing(String itemName, String category, String description, String location, LocalDate date, String status, String type, String studentEmail) {
        this.itemName = itemName;
        this.category = category;
        this.description = description;
        this.location = location;
        this.date = date;
        this.status = status;
        this.type = type;
        this.studentEmail = studentEmail;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
}
