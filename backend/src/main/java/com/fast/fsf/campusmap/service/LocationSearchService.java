package com.fast.fsf.campusmap.service;

import com.fast.fsf.campusmap.adapter.ApprovedLocationCatalog;
import com.fast.fsf.campusmap.criterion.LocationSearchCriterion;
import com.fast.fsf.campusmap.domain.CampusLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Singleton (Spring default scope). Stateless search service that
 * composes criteria objects at query time.
 * Thread-safe: no mutable state, criteria objects are constructed
 * fresh per query and discarded after use.
 *
 * Mirror: search/service/RideSearchService.java (hypothetical)
 */
@Service
public class LocationSearchService {

    private final ApprovedLocationCatalog locationCatalog;

    @Autowired
    public LocationSearchService(ApprovedLocationCatalog locationCatalog) {
        this.locationCatalog = locationCatalog;
    }

    public List<CampusLocation> search(LocationSearchCriterion criterion) {
        return locationCatalog.findAllApproved().stream()
                .filter(criterion::matches)
                .collect(Collectors.toList());
    }
}
