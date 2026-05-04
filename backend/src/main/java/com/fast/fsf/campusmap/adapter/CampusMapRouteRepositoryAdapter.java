package com.fast.fsf.campusmap.adapter;

import com.fast.fsf.campusmap.domain.CampusMapRoute;
import com.fast.fsf.campusmap.persistence.CampusMapRouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter pattern — adapts CampusMapRouteRepository to the
 * RouteStepCatalog port used by DatabaseDirectionsWorkflow.
 *
 * Mirror: search/catalog/RideRepositoryApprovedRideAdapter.java (variant)
 */
@Component
public class CampusMapRouteRepositoryAdapter implements RouteStepCatalog {

    private final CampusMapRouteRepository routeRepository;

    @Autowired
    public CampusMapRouteRepositoryAdapter(CampusMapRouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Override
    public List<CampusMapRoute> findStepsInOrder(String from, String to) {
        return routeRepository.findByFromLocationAndToLocationOrderByStepOrderAsc(from, to);
    }

    @Override
    public boolean routeExists(String from, String to) {
        return routeRepository.existsByFromLocationAndToLocation(from, to);
    }
}
