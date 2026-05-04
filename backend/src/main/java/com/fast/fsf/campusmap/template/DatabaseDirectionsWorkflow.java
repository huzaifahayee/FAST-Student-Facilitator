package com.fast.fsf.campusmap.template;

import com.fast.fsf.campusmap.adapter.ApprovedLocationCatalog;
import com.fast.fsf.campusmap.adapter.RouteStepCatalog;
import com.fast.fsf.campusmap.domain.CampusMapRoute;
import com.fast.fsf.campusmap.observer.MapEventPublisher;
import com.fast.fsf.events.persistence.CampusEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Concrete Workflow for fetching directions from the database.
 */
@Service
public class DatabaseDirectionsWorkflow extends AbstractDirectionsWorkflow {

    private final RouteStepCatalog routeCatalog;

    public DatabaseDirectionsWorkflow(ApprovedLocationCatalog locationCatalog,
                                      CampusEventRepository campusEventRepo,
                                      MapEventPublisher eventPublisher,
                                      RouteStepCatalog routeCatalog) {
        super(locationCatalog, campusEventRepo, eventPublisher);
        this.routeCatalog = routeCatalog;
    }

    @Override
    protected List<CampusMapRoute> fetchRouteSteps(String from, String to) {
        return routeCatalog.findStepsInOrder(from, to);
    }

    @Override
    protected boolean routeExists(String from, String to) {
        return routeCatalog.routeExists(from, to);
    }
}
