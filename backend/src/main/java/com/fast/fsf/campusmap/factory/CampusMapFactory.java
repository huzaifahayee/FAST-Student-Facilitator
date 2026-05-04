package com.fast.fsf.campusmap.factory;

import com.fast.fsf.campusmap.domain.CampusLocation;
import com.fast.fsf.campusmap.domain.CampusMapRoute;
import com.fast.fsf.campusmap.domain.LocationSuggestion;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Factory Method pattern — centralises construction and creation-time
 * validation for all Campus Map domain objects.
 * Controllers call factory methods instead of using constructors
 * directly, ensuring every object is created in a valid state.
 *
 * Mirror: carpool/factory/RideOfferFactory.java
 */
public class CampusMapFactory {

    public static CampusLocation createLocation(
            String locationName, String locationType, String category,
            String blockId, String description, String facultyOffices,
            String classroomNumbers, String imagePath,
            String ownerEmail, String ownerName) {

        validateNotBlank(locationName, "Location Name");
        validateNotBlank(locationType, "Location Type");
        validateNotBlank(category, "Category");
        validateNotBlank(ownerEmail, "Owner Email");

        List<String> validTypes = Arrays.asList("BLOCK", "FACULTY_OFFICE", "ROOM");
        if (!validTypes.contains(locationType)) {
            throw new IllegalArgumentException("Location type must be one of: BLOCK, FACULTY_OFFICE, ROOM");
        }

        List<String> validCats = Arrays.asList(
                "Academic Buildings", "Administrative Offices",
                "Facilities", "Parking Areas", "Sports Areas", "Faculty Offices");
        if (!validCats.contains(category)) {
            throw new IllegalArgumentException("Category must be one of the 6 valid values");
        }

        CampusLocation location = new CampusLocation();
        location.setLocationName(locationName);
        location.setLocationType(locationType);
        location.setCategory(category);
        location.setBlockId(blockId);
        location.setDescription(description);
        location.setFacultyOffices(facultyOffices);
        location.setClassroomNumbers(classroomNumbers);
        location.setImagePath(imagePath);
        location.setOwnerEmail(ownerEmail);
        location.setOwnerName(ownerName);
        
        location.setApproved(false);
        location.setFlagged(false);
        
        return location;
    }

    public static CampusMapRoute createRouteStep(
            String fromLocation, String toLocation, int stepOrder,
            String imageFileName, String stepDescription,
            String ownerEmail, String ownerName) {

        validateNotBlank(fromLocation, "From location");
        validateNotBlank(toLocation, "To location");
        validateNotBlank(stepDescription, "Step description");
        validateNotBlank(ownerEmail, "Owner email");

        if (fromLocation.equalsIgnoreCase(toLocation)) {
            throw new IllegalArgumentException("From and To cannot be the same location");
        }
        if (stepOrder < 1) {
            throw new IllegalArgumentException("Step order must be 1 or greater");
        }

        CampusMapRoute route = new CampusMapRoute();
        route.setFromLocation(fromLocation);
        route.setToLocation(toLocation);
        route.setStepOrder(stepOrder);
        route.setImageFileName(imageFileName);
        route.setStepDescription(stepDescription);
        route.setOwnerEmail(ownerEmail);
        route.setOwnerName(ownerName);
        
        route.setApproved(true); // admin-submitted steps are pre-approved in original logic
        route.setFlagged(false);
        
        return route;
    }

    public static LocationSuggestion createSuggestion(
            String locationName, String category, String description,
            String submittedBy, String submitterName) {

        validateNotBlank(locationName, "Location name");
        validateNotBlank(category, "Category");
        validateNotBlank(description, "Description");
        validateNotBlank(submittedBy, "User email");

        LocationSuggestion suggestion = new LocationSuggestion();
        suggestion.setLocationName(locationName);
        suggestion.setCategory(category);
        suggestion.setDescription(description);
        suggestion.setSubmittedBy(submittedBy);
        suggestion.setSubmitterName(submitterName);
        
        suggestion.setSubmittedAt(LocalDateTime.now());
        suggestion.setResolved(false);
        suggestion.setApproved(false);
        
        return suggestion;
    }

    private static void validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
    }
}
