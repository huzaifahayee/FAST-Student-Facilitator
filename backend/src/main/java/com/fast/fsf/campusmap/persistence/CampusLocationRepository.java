package com.fast.fsf.campusmap.persistence;

import com.fast.fsf.campusmap.domain.CampusLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CampusLocationRepository  (UC-33, UC-34)
 *
 * Follows the same naming conventions as RideRepository so that
 * Admin Panel / Stats endpoints discover this entity automatically
 * (they call countByApprovedTrue, findByApprovedFalse, etc. by naming contract).
 */
@Repository
public interface CampusLocationRepository extends JpaRepository<CampusLocation, Long> {

    // ── Admin Panel / Stats support ───────────────────────────────────────────
    long countByApprovedTrue();
    long countByApprovedFalse();
    long countByFlaggedTrue();

    List<CampusLocation> findByApprovedTrue();
    List<CampusLocation> findByApprovedFalse();
    List<CampusLocation> findByFlaggedTrue();

    // ── Category / type browsing  (UC-33) ────────────────────────────────────
    List<CampusLocation> findByCategory(String category);
    List<CampusLocation> findByLocationType(String locationType);
    List<CampusLocation> findByBlockId(String blockId);

    // ── Full-text search  (UC-34) ─────────────────────────────────────────────
    // Searches locationName, facultyOffices, classroomNumbers, and category.
    // Only returns approved locations to prevent leaking pending content.
    @Query("SELECT l FROM CampusLocation l WHERE l.approved = true AND (" +
           "LOWER(l.locationName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(l.facultyOffices) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(l.classroomNumbers) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(l.category) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<CampusLocation> searchLocations(@Param("q") String query);

    // ── Directions endpoint — exact name lookup (UC-32) ───────────────────────
    Optional<CampusLocation> findByLocationNameIgnoreCase(String locationName);
}
