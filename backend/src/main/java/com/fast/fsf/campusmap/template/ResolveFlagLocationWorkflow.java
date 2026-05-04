package com.fast.fsf.campusmap.template;

import com.fast.fsf.campusmap.domain.CampusLocation;
import com.fast.fsf.campusmap.observer.MapEventPublisher;
import com.fast.fsf.campusmap.persistence.CampusLocationRepository;
import com.fast.fsf.campusmap.state.LocationModerationContext;
import org.springframework.stereotype.Service;

/**
 * Concrete Workflow for resolving a flag on a location.
 */
@Service
public class ResolveFlagLocationWorkflow extends AbstractLocationModerationWorkflow {

    public ResolveFlagLocationWorkflow(CampusLocationRepository locationRepository, MapEventPublisher eventPublisher) {
        super(locationRepository, eventPublisher);
    }

    @Override
    protected void validateTransition(CampusLocation location, String reason) {
        new LocationModerationContext(location).resolveFlag(location);
    }

    @Override
    protected void applyChange(CampusLocation location, String reason) {
        new LocationModerationContext(location).resolveFlag(location);
    }

    @Override
    protected void publishEvent(CampusLocation location, String reason) {
        // Clearing flag is logged as "Flag on Location #id resolved"
        // Mirroring logic in controller
    }
}
