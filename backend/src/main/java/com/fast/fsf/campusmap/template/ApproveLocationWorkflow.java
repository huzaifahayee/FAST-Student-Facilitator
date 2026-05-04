package com.fast.fsf.campusmap.template;

import com.fast.fsf.campusmap.domain.CampusLocation;
import com.fast.fsf.campusmap.observer.MapEventPublisher;
import com.fast.fsf.campusmap.persistence.CampusLocationRepository;
import com.fast.fsf.campusmap.state.LocationModerationContext;
import org.springframework.stereotype.Service;

/**
 * Concrete Workflow for approving a location.
 */
@Service
public class ApproveLocationWorkflow extends AbstractLocationModerationWorkflow {

    public ApproveLocationWorkflow(CampusLocationRepository locationRepository, MapEventPublisher eventPublisher) {
        super(locationRepository, eventPublisher);
    }

    @Override
    protected void validateTransition(CampusLocation location, String reason) {
        new LocationModerationContext(location).approve(location, "Validation Check");
    }

    @Override
    protected void applyChange(CampusLocation location, String reason) {
        new LocationModerationContext(location).approve(location, reason);
    }

    @Override
    protected void publishEvent(CampusLocation location, String reason) {
        eventPublisher.publishLocationApproved(location);
    }
}
