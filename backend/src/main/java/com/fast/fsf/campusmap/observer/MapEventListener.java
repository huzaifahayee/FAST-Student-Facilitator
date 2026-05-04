package com.fast.fsf.campusmap.observer;

import com.fast.fsf.campusmap.domain.CampusLocation;
import com.fast.fsf.campusmap.domain.CampusMapRoute;
import com.fast.fsf.campusmap.domain.LocationSuggestion;

/**
 * Observer Pattern — Part of the MapEventPublisher system.
 * Defines the contract for any component that needs to react to
 * Campus Map domain events without being directly coupled to the controller.
 *
 * Mirror: carpool/observer/RideActivityLogObserver.java (indirectly via listener interface)
 */
public interface MapEventListener {
    void onLocationAdded(CampusLocation location);
    void onLocationApproved(CampusLocation location);
    void onLocationFlagged(CampusLocation location, String reason);
    void onLocationDeleted(CampusLocation location, String reason);
    void onRouteStepAdded(CampusMapRoute step);
    void onRouteDeleted(String from, String to);
    void onDirectionsRequested(String from, String to);
    void onSuggestionSubmitted(LocationSuggestion suggestion);
    void onSuggestionResolved(LocationSuggestion suggestion);
}
