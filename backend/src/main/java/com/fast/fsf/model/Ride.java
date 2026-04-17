package com.fast.fsf.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ride Entity
 * 
 * What is this?
 * This class represents a single Carpool listing in our database.
 * We are following Feature #1 (Carpool) requirements from the SRS/Phase 2.
 */
@Entity
@Table(name = "rides")
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The name of the student offering the ride
    @Column(nullable = false)
    private String driverName;

    @Column(nullable = false)
    private String driverEmail;

    // Origin and Destination (FAST Lahore is usually one of these)
    @Column(nullable = false)
    private String origin;

    @Column(nullable = false)
    private String destination;

    // Planned departure time (e.g., "08:30 AM")
    @Column(nullable = false)
    private String departureTime;

    // How many seats are available in the car?
    private int availableSeats;

    /**
     * Checkpoints
     * SRS NFR 4.2.3: Maximum of 5 checkpoint fields.
     */
    @ElementCollection
    @CollectionTable(name = "ride_checkpoints", joinColumns = @JoinColumn(name = "ride_id"))
    @Column(name = "checkpoint_name")
    private List<String> checkpoints = new ArrayList<>();

    // Contact details (Email/WhatsApp) as per SRS rules
    @Column(nullable = false)
    private String contactInfo;

    // The type of vehicle (Car, Bike, etc.)
    @Column(nullable = false)
    private String vehicleType;

    @Column(nullable = false)
    private boolean flagged = false; // For Admin moderation

    @Column(nullable = false)
    private boolean approved = false; // Approval hurdle

    private String moderationReason; // Admin provided reason for flags/deletions

    // Default constructor for JPA
    public Ride() {
        this.flagged = false;
        this.approved = false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getDriverEmail() { return driverEmail; }
    public void setDriverEmail(String driverEmail) { this.driverEmail = driverEmail; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public List<String> getCheckpoints() { return checkpoints; }
    public void setCheckpoints(List<String> checkpoints) { 
        if (checkpoints != null && checkpoints.size() > 5) {
            throw new IllegalArgumentException("Maximum of 5 checkpoints allowed.");
        }
        this.checkpoints = checkpoints; 
    }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getModerationReason() { return moderationReason; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }
}
