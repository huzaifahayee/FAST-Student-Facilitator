package com.fast.fsf.pastpapers.adapter;

import com.fast.fsf.pastpapers.domain.PastPaper;
import com.fast.fsf.pastpapers.persistence.PastPaperRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter pattern — adapts PastPaperRepository (Spring Data JPA)
 * to the ApprovedPaperCatalog port. The service depends only on
 * ApprovedPaperCatalog; the adapter translates calls to repository
 * methods. Swapping the persistence layer only requires a new adapter.
 *
 * Mirror: search/catalog/RideRepositoryApprovedRideAdapter.java
 */
@Component
public class PastPaperRepositoryAdapter implements ApprovedPaperCatalog {

    private final PastPaperRepository paperRepository;

    @Autowired
    public PastPaperRepositoryAdapter(PastPaperRepository paperRepository) {
        this.paperRepository = paperRepository;
    }

    @Override
    public List<PastPaper> findAllApproved() {
        return paperRepository.findByApprovedTrue();
    }

    @Override
    public List<PastPaper> searchApproved(String keyword) {
        return paperRepository.searchApproved(keyword);
    }

    @Override
    public Optional<PastPaper> findApprovedById(Long id) {
        return paperRepository.findById(id)
                .filter(PastPaper::isApproved);
    }
}
