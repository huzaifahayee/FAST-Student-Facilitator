package com.fast.fsf.pastpapers.adapter;

import com.fast.fsf.pastpapers.domain.PastPaper;

import java.util.List;
import java.util.Optional;

/**
 * Port that the search and listing service depends on.
 * Decouples business logic from Spring Data JPA specifics.
 *
 * Mirror: search/catalog/ApprovedRideCatalog.java (hypothetical)
 */
public interface ApprovedPaperCatalog {
    List<PastPaper> findAllApproved();
    List<PastPaper> searchApproved(String keyword);
    Optional<PastPaper> findApprovedById(Long id);
}
