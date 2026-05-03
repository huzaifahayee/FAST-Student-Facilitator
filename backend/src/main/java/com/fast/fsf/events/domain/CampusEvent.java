package com.fast.fsf.events.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * CampusEvent Entity (UC-21, 22, 23)
 * 
 * Represents an event on the Campus Event Board or an item in the Semester
 * Plan.
 */
@Entity
@Table(name = "campus_events")
public class CampusEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate eventDate;

    @Column(nullable = false)
    private String venue;

    @Column(nullable = false)
    private String organizer;

    @Column(nullable = false)
    private String category; // e.g., "ACADEMIC", "SOCIAL", "SPORTS", "HOLIDAY"

    @Column(name = "semester_plan", nullable = false)
    private boolean semesterPlan = false; // UC-23: Distinguishes semester plan items (like exams/holidays)

    @Column(nullable = false)
    private String ownerEmail;

    @Column(nullable = false)
    private boolean approved = false;

    public CampusEvent() {
    }

    public CampusEvent(String title, String description, LocalDate eventDate, String venue, String organizer,
            String category, boolean semesterPlan, String ownerEmail) {
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
        this.venue = venue;
        this.organizer = organizer;
        this.category = category;
        this.semesterPlan = semesterPlan;
        this.ownerEmail = ownerEmail;
        this.approved = false;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean getSemesterPlan() {
        return semesterPlan;
    }

    public void setSemesterPlan(boolean semesterPlan) {
        this.semesterPlan = semesterPlan;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }
}
