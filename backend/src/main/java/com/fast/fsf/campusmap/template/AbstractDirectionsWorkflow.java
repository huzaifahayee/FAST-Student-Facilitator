package com.fast.fsf.campusmap.template;

import com.fast.fsf.campusmap.adapter.ApprovedLocationCatalog;
import com.fast.fsf.campusmap.domain.CampusLocation;
import com.fast.fsf.campusmap.domain.CampusMapRoute;
import com.fast.fsf.campusmap.observer.MapEventPublisher;
import com.fast.fsf.events.domain.CampusEvent;
import com.fast.fsf.events.persistence.CampusEventRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Template Method pattern — fixed steps for fetching directions:
 *   1. validateInputs(from, to)   — common
 *   2. resolveSubLocations()      — common
 *   3. checkSameLocation()        — common
 *   4. fetchRouteSteps()          — hook: subclass determines source
 *   5. fetchDestinationInfo()     — common
 *   6. fetchActiveEvents()        — common
 *   7. buildResponse()            — common
 */
public abstract class AbstractDirectionsWorkflow {

    protected final ApprovedLocationCatalog locationCatalog;
    protected final CampusEventRepository campusEventRepo;
    protected final MapEventPublisher eventPublisher;

    protected AbstractDirectionsWorkflow(ApprovedLocationCatalog locationCatalog,
                                         CampusEventRepository campusEventRepo,
                                         MapEventPublisher eventPublisher) {
        this.locationCatalog = locationCatalog;
        this.campusEventRepo = campusEventRepo;
        this.eventPublisher = eventPublisher;
    }

    public final DirectionsResponse execute(String from, String to) {
        validateInputs(from, to);

        String originalTo = to;
        String finalSubDestinationMessage = null;

        // Resolve rooms to blocks
        Optional<CampusLocation> primaryDest = locationCatalog.findApprovedByName(to);
        if (primaryDest.isEmpty()) {
            for (CampusLocation loc : locationCatalog.findAllApproved()) {
                if (loc.getClassroomNumbers() != null &&
                        Arrays.stream(loc.getClassroomNumbers().split(","))
                                .map(String::trim)
                                .anyMatch(r -> r.equalsIgnoreCase(originalTo))) {
                    to = loc.getLocationName();
                    finalSubDestinationMessage = "You have arrived at " + loc.getLocationName() + " (" + loc.getCategory() + ")! Please locate room " + originalTo + " inside this building.";
                    break;
                }
            }
        }

        DirectionsResponse response = new DirectionsResponse();

        if (from.equalsIgnoreCase(to)) {
            response.setSameLocation(true);
            if (finalSubDestinationMessage != null) {
                response.setMessage("You are already at " + to + "! Please locate room " + originalTo + " inside.");
            } else {
                response.setMessage("You are already at your destination. No directions needed.");
            }
            response.setSteps(Collections.emptyList());
            CampusLocation destInfo = locationCatalog.findApprovedByName(to).orElse(null);
            response.setDestinationInfo(destInfo);
            response.setActiveEvents(fetchActiveEvents(to));
            eventPublisher.publishDirectionsRequested(from, to);
            return response;
        }

        List<CampusMapRoute> steps = fetchRouteSteps(from, to);
        CampusLocation destInfo = locationCatalog.findApprovedByName(to).orElse(null);
        List<CampusEvent> events = fetchActiveEvents(to);

        response.setDestinationInfo(destInfo);
        response.setActiveEvents(events);

        if (steps.isEmpty() && !routeExists(from, to)) {
            response.setRouteFound(false);
            response.setMessage("Directions for this route are not yet available. Please ask at the reception.");
            response.setSteps(Collections.emptyList());
            eventPublisher.publishDirectionsRequested(from, to);
            return response;
        }

        List<Map<String, Object>> stepDTOs = buildStepDTOs(steps);
        if (finalSubDestinationMessage != null) {
            Map<String, Object> extraStep = new LinkedHashMap<>();
            extraStep.put("stepOrder", steps.size() + 1);
            extraStep.put("imageFileName", null);
            extraStep.put("imageUrl", null);
            extraStep.put("stepDescription", finalSubDestinationMessage);
            extraStep.put("hasImage", false);
            stepDTOs.add(extraStep);
        }

        response.setRouteFound(true);
        response.setSameLocation(false);
        response.setSteps(stepDTOs);
        
        eventPublisher.publishDirectionsRequested(from, to);
        return response;
    }

    private void validateInputs(String from, String to) {
        if (from == null || from.isBlank()) throw new IllegalArgumentException("Please select your current location");
        if (to == null || to.isBlank()) throw new IllegalArgumentException("Please select a destination");
    }

    protected abstract List<CampusMapRoute> fetchRouteSteps(String from, String to);

    protected abstract boolean routeExists(String from, String to);

    private List<CampusEvent> fetchActiveEvents(String venueName) {
        if (venueName == null || venueName.isBlank()) return Collections.emptyList();
        List<CampusEvent> events = campusEventRepo.findByVenueContainingIgnoreCaseAndApprovedTrue(venueName);
        LocalDate today = LocalDate.now();
        return events.stream()
                .filter(e -> !e.getEventDate().isBefore(today))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildStepDTOs(List<CampusMapRoute> steps) {
        return steps.stream().map(s -> {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("stepOrder", s.getStepOrder());
            dto.put("imageFileName", s.getImageFileName());
            dto.put("imageUrl", s.getImageFileName() != null
                    ? "/api/campus-map/images/" + s.getImageFileName()
                    : null);
            dto.put("stepDescription", s.getStepDescription());
            dto.put("hasImage", s.getImageFileName() != null);
            return dto;
        }).collect(Collectors.toList());
    }
}
