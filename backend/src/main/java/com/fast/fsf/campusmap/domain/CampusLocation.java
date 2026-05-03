package com.fast.fsf.campusmap.domain;

import jakarta.persistence.*;

/**
 * CampusLocation Entity  (UC-32, UC-33, UC-34, UC-35)
 *
 * Represents a named place on the FAST-NUCES Lahore campus —
 * a Block, a Faculty Office, or a specific Room.
 *
 * Convention notes:
 *  - No Lombok: all getters/setters/default constructor written explicitly.
 *  - Moderation triplet (approved, flagged, moderationReason) is REQUIRED
 *    so the existing AdminPanel works with this entity automatically.
 *  - Seeded admin locations start approved=true; student-added ones start approved=false.
 */
@Entity
@Table(name = "campus_locations")
public class CampusLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable name: "Block C", "Library", "Main Gate" */
    @Column(nullable = false)
    private String locationName;

    /** BLOCK | FACULTY_OFFICE | ROOM */
    @Column(nullable = false)
    private String locationType;

    /**
     * Broad grouping for the accordion browser:
     * "Academic Buildings" | "Administrative Offices" | "Facilities"
     * | "Parking Areas" | "Sports Areas" | "Faculty Offices"
     */
    @Column(nullable = false)
    private String category;

    /** Which block this entity belongs to, e.g. "BLOCK_C".  Nullable for top-level blocks. */
    @Column(nullable = true)
    private String blockId;

    @Column(nullable = true, length = 1000)
    private String description;

    /** Comma-separated: "Dr. Naveed (CS), Dr. Irfan (CS)" */
    @Column(nullable = true, length = 2000)
    private String facultyOffices;

    /** Comma-separated: "CR-F1, CR-F2, Lab-F1" */
    @Column(nullable = true, length = 1000)
    private String classroomNumbers;

    /** Filename of the block-entrance photo served via /api/campus-map/images/{filename} */
    @Column(nullable = true)
    private String imagePath;

    @Column(nullable = false)
    private String ownerEmail;

    @Column(nullable = false)
    private String ownerName;

    // ── Moderation triplet ────────────────────────────────────────────────────
    // REQUIRED so the existing Admin Panel picks this entity up automatically.
    // Admin-seeded locations start approved=true; student suggestions start false.
    @Column(nullable = false)
    private boolean approved = true;

    @Column(nullable = false)
    private boolean flagged = false;

    /** Admin-provided reason for a flag or rejection.  Nullable. */
    private String moderationReason;

    // ── Default no-arg constructor (required by JPA) ──────────────────────────
    public CampusLocation() {
        this.approved = true;
        this.flagged = false;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBlockId() { return blockId; }
    public void setBlockId(String blockId) { this.blockId = blockId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFacultyOffices() { return facultyOffices; }
    public void setFacultyOffices(String facultyOffices) { this.facultyOffices = facultyOffices; }

    public String getClassroomNumbers() { return classroomNumbers; }
    public void setClassroomNumbers(String classroomNumbers) { this.classroomNumbers = classroomNumbers; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }

    public String getModerationReason() { return moderationReason; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }
}
