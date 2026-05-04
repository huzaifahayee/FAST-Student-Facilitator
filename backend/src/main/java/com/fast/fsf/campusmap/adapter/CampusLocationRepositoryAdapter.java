package com.fast.fsf.campusmap.adapter;

import com.fast.fsf.campusmap.domain.CampusLocation;
import com.fast.fsf.campusmap.persistence.CampusLocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter pattern — adapts CampusLocationRepository (Spring Data JPA)
 * to the ApprovedLocationCatalog port. Swapping persistence only
 * requires a new adapter — services remain untouched.
 *
 * Mirror: search/catalog/RideRepositoryApprovedRideAdapter.java
 */
@Component
public class CampusLocationRepositoryAdapter implements ApprovedLocationCatalog {

    private final CampusLocationRepository locationRepository;

    @Autowired
    public CampusLocationRepositoryAdapter(CampusLocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Override
    public List<CampusLocation> findAllApproved() {
        return locationRepository.findByApprovedTrue();
    }

    @Override
    public List<CampusLocation> searchLocations(String query) {
        return locationRepository.searchLocations(query);
    }

    @Override
    public Optional<CampusLocation> findApprovedByName(String name) {
        return locationRepository.findByLocationNameIgnoreCase(name)
                .filter(CampusLocation::isApproved);
    }

    @Override
    public List<CampusLocation> findByCategory(String category) {
        return locationRepository.findByCategory(category).stream()
                .filter(CampusLocation::isApproved)
                .collect(Collectors.toList());
    }
}
