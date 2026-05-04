package com.fast.fsf.campusmap.template;

import com.fast.fsf.campusmap.domain.CampusLocation;
import com.fast.fsf.events.domain.CampusEvent;
import java.util.List;
import java.util.Map;

/**
 * DTO for Directions Workflow response.
 */
public class DirectionsResponse {
    private boolean routeFound;
    private boolean sameLocation;
    private String message;
    private List<Map<String, Object>> steps;
    private CampusLocation destinationInfo;
    private List<CampusEvent> activeEvents;

    // Getters and setters
    public boolean isRouteFound() { return routeFound; }
    public void setRouteFound(boolean routeFound) { this.routeFound = routeFound; }
    public boolean isSameLocation() { return sameLocation; }
    public void setSameLocation(boolean sameLocation) { this.sameLocation = sameLocation; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<Map<String, Object>> getSteps() { return steps; }
    public void setSteps(List<Map<String, Object>> steps) { this.steps = steps; }
    public CampusLocation getDestinationInfo() { return destinationInfo; }
    public void setDestinationInfo(CampusLocation destinationInfo) { this.destinationInfo = destinationInfo; }
    public List<CampusEvent> getActiveEvents() { return activeEvents; }
    public void setActiveEvents(List<CampusEvent> activeEvents) { this.activeEvents = activeEvents; }
}
