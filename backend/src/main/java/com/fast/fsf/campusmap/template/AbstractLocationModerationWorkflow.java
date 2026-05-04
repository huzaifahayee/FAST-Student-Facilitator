package com.fast.fsf.campusmap.template;

import com.fast.fsf.campusmap.domain.CampusLocation;
import com.fast.fsf.campusmap.observer.MapEventPublisher;
import com.fast.fsf.campusmap.persistence.CampusLocationRepository;

/**
 * Template Method pattern — defines the fixed algorithm for any
 * location moderation action:
 *   1. findLocation(id)      — common: fetch or throw 404
 *   2. validateTransition()  — hook: subclass checks preconditions
 *   3. applyChange()         — hook: subclass mutates the location
 *   4. saveLocation()        — common: persist
 *   5. publishEvent()        — common: fire MapEventPublisher
 * Subclasses implement only the two hooks and fire the specific event.
 *
 * Mirror: carpool/template/AbstractRideModerationWorkflow.java (hypothetical)
 */
public abstract class AbstractLocationModerationWorkflow {

    protected final CampusLocationRepository locationRepository;
    protected final MapEventPublisher eventPublisher;

    protected AbstractLocationModerationWorkflow(CampusLocationRepository locationRepository, MapEventPublisher eventPublisher) {
        this.locationRepository = locationRepository;
        this.eventPublisher = eventPublisher;
    }

    public final CampusLocation execute(Long locationId, String reason) {
        CampusLocation loc = findLocation(locationId);
        validateTransition(loc, reason);
        applyChange(loc, reason);
        CampusLocation saved = locationRepository.save(loc);
        publishEvent(saved, reason);
        return saved;
    }

    protected CampusLocation findLocation(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found with ID: " + id));
    }

    protected abstract void validateTransition(CampusLocation location, String reason);
    protected abstract void applyChange(CampusLocation location, String reason);
    protected abstract void publishEvent(CampusLocation location, String reason);
}
