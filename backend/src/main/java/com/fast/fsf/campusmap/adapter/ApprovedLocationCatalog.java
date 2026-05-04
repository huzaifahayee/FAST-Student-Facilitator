package com.fast.fsf.campusmap.adapter;

import com.fast.fsf.campusmap.domain.CampusLocation;
import java.util.List;
import java.util.Optional;

/**
 * Port that search and directions services depend on.
 * Decouples business logic from Spring Data JPA.
 *
 * Mirror: search/catalog/ApprovedRideCatalog.java (hypothetical)
 */
public interface ApprovedLocationCatalog {
    List<CampusLocation> findAllApproved();
    List<CampusLocation> searchLocations(String query);
    Optional<CampusLocation> findApprovedByName(String name);
    List<CampusLocation> findByCategory(String category);
}
