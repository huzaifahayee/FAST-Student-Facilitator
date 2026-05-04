package com.fast.fsf.campusmap.observer;

import com.fast.fsf.campusmap.domain.CampusLocation;
import com.fast.fsf.campusmap.domain.CampusMapRoute;
import com.fast.fsf.campusmap.domain.LocationSuggestion;
import com.fast.fsf.shared.model.ActivityLog;
import com.fast.fsf.shared.persistence.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Observer pattern — decouples ActivityLog writes from the HTTP layer.
 * The controller fires map events; this observer reacts by persisting
 * audit entries without the controller knowing about logging at all.
 *
 * Mirror: carpool/observer/RideActivityLogObserver.java
 */
@Component
public class MapActivityLogObserver implements MapEventListener {

    private final ActivityLogRepository activityLogRepository;

    @Autowired
    public MapActivityLogObserver(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @Override
    public void onLocationAdded(CampusLocation location) {
        activityLogRepository.save(new ActivityLog(
            location.getOwnerName() + " added location: " + location.getLocationName(),
            "LOCATION_ADDED"
        ));
    }

    @Override
    public void onLocationApproved(CampusLocation location) {
        activityLogRepository.save(new ActivityLog(
            "Location #" + location.getId() + " approved",
            "LOCATION_APPROVED"
        ));
    }

    @Override
    public void onLocationFlagged(CampusLocation location, String reason) {
        activityLogRepository.save(new ActivityLog(
            "Location #" + location.getId() + " flagged: " + reason,
            "LOCATION_FLAGGED"
        ));
    }

    @Override
    public void onLocationDeleted(CampusLocation location, String reason) {
        activityLogRepository.save(new ActivityLog(
            "Location #" + location.getId() + " deleted. Reason: " + (reason != null ? reason : "None"),
            "LOCATION_DELETED"
        ));
    }

    @Override
    public void onRouteStepAdded(CampusMapRoute step) {
        activityLogRepository.save(new ActivityLog(
            "Route step added: " + step.getFromLocation() + " → " + step.getToLocation()
                    + " step " + step.getStepOrder(),
            "ROUTE_STEP_ADDED"
        ));
    }

    @Override
    public void onRouteDeleted(String from, String to) {
        activityLogRepository.save(new ActivityLog(
            "Route deleted: " + from + " → " + to,
            "ROUTE_DELETED"
        ));
    }

    @Override
    public void onDirectionsRequested(String from, String to) {
        activityLogRepository.save(new ActivityLog(
            "Directions requested: " + from + " → " + to,
            "MAP_DIRECTIONS_REQUESTED"
        ));
    }

    @Override
    public void onSuggestionSubmitted(LocationSuggestion suggestion) {
        activityLogRepository.save(new ActivityLog(
            suggestion.getSubmitterName() + " suggested location: " + suggestion.getLocationName(),
            "LOCATION_SUGGESTED"
        ));
    }

    @Override
    public void onSuggestionResolved(LocationSuggestion suggestion) {
        activityLogRepository.save(new ActivityLog(
            "Location suggestion #" + suggestion.getId() + " resolved",
            "SUGGESTION_RESOLVED"
        ));
    }
}
