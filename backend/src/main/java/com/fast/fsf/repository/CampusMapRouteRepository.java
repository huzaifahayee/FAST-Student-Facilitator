package com.fast.fsf.repository;

import com.fast.fsf.model.CampusMapRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CampusMapRouteRepository  (UC-32)
 *
 * Spring Data JPA query derivation is used throughout —
 * no manual SQL is needed.
 */
@Repository
public interface CampusMapRouteRepository extends JpaRepository<CampusMapRoute, Long> {

    // ── Route lookup — ordered (UC-32) ────────────────────────────────────────
    List<CampusMapRoute> findByFromLocationAndToLocationOrderByStepOrderAsc(
            String fromLocation, String toLocation);

    // ── Route existence check (UC-32) ────────────────────────────────────────
    boolean existsByFromLocationAndToLocation(String fromLocation, String toLocation);

    // ── Admin route management ────────────────────────────────────────────────
    List<CampusMapRoute> findByFromLocationAndToLocation(
            String fromLocation, String toLocation);

    // ── Admin Panel / Stats support ───────────────────────────────────────────
    long countByApprovedTrue();
    long countByFlaggedTrue();
    List<CampusMapRoute> findByApprovedFalse();
    List<CampusMapRoute> findByFlaggedTrue();
}
