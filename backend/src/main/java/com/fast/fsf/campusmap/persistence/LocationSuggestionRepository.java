package com.fast.fsf.campusmap.persistence;

import com.fast.fsf.campusmap.domain.LocationSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * LocationSuggestionRepository  (UC-35)
 *
 * Manages student-submitted location suggestions.
 * Admin Panel / Stats discover the moderation triplet methods automatically.
 */
@Repository
public interface LocationSuggestionRepository extends JpaRepository<LocationSuggestion, Long> {

    // ── Admin inbox / resolved queue ──────────────────────────────────────────
    List<LocationSuggestion> findByResolvedFalse();
    List<LocationSuggestion> findByResolvedTrue();
    long countByResolvedFalse();

    // ── Student's own suggestions ─────────────────────────────────────────────
    List<LocationSuggestion> findBySubmittedBy(String email);

    // ── Admin Panel / Stats support ───────────────────────────────────────────
    long countByApprovedTrue();
    long countByFlaggedTrue();
    List<LocationSuggestion> findByApprovedFalse();
    List<LocationSuggestion> findByFlaggedTrue();
}
