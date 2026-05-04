package com.fast.fsf.campusmap.web;

import com.fast.fsf.campusmap.adapter.ApprovedLocationCatalog;
import com.fast.fsf.campusmap.adapter.RouteStepCatalog;
import com.fast.fsf.campusmap.criterion.*;
import com.fast.fsf.campusmap.domain.CampusLocation;
import com.fast.fsf.campusmap.domain.CampusMapRoute;
import com.fast.fsf.campusmap.domain.LocationSuggestion;
import com.fast.fsf.campusmap.factory.CampusMapFactory;
import com.fast.fsf.campusmap.observer.MapEventPublisher;
import com.fast.fsf.campusmap.persistence.CampusLocationRepository;
import com.fast.fsf.campusmap.persistence.CampusMapRouteRepository;
import com.fast.fsf.campusmap.persistence.LocationSuggestionRepository;
import com.fast.fsf.campusmap.service.LocationSearchService;
import com.fast.fsf.campusmap.template.*;
import com.fast.fsf.events.persistence.CampusEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Singleton (Spring default scope) — one shared instance per JVM.
 * Orchestrates the Campus Map feature by delegating to specialized pattern classes.
 */
@RestController
@RequestMapping("/api/campus-map")
@CrossOrigin(originPatterns = {"http://localhost:*"})
public class CampusMapController {

    private final CampusLocationRepository locationRepo;
    private final CampusMapRouteRepository routeRepo;
    private final LocationSuggestionRepository suggestionRepo;
    private final CampusEventRepository campusEventRepo;

    // Pattern implementations
    private final MapEventPublisher eventPublisher;
    private final LocationSearchService searchService;
    private final ApprovedLocationCatalog locationCatalog;
    private final RouteStepCatalog routeCatalog;
    private final ApproveLocationWorkflow approveWorkflow;
    private final FlagLocationWorkflow flagWorkflow;
    private final ResolveFlagLocationWorkflow resolveFlagWorkflow;
    private final DatabaseDirectionsWorkflow directionsWorkflow;

    @Autowired
    public CampusMapController(CampusLocationRepository locationRepo,
                               CampusMapRouteRepository routeRepo,
                               LocationSuggestionRepository suggestionRepo,
                               CampusEventRepository campusEventRepo,
                               MapEventPublisher eventPublisher,
                               LocationSearchService searchService,
                               ApprovedLocationCatalog locationCatalog,
                               RouteStepCatalog routeCatalog,
                               ApproveLocationWorkflow approveWorkflow,
                               FlagLocationWorkflow flagWorkflow,
                               ResolveFlagLocationWorkflow resolveFlagWorkflow,
                               DatabaseDirectionsWorkflow directionsWorkflow) {
        this.locationRepo = locationRepo;
        this.routeRepo = routeRepo;
        this.suggestionRepo = suggestionRepo;
        this.campusEventRepo = campusEventRepo;
        this.eventPublisher = eventPublisher;
        this.searchService = searchService;
        this.locationCatalog = locationCatalog;
        this.routeCatalog = routeCatalog;
        this.approveWorkflow = approveWorkflow;
        this.flagWorkflow = flagWorkflow;
        this.resolveFlagWorkflow = resolveFlagWorkflow;
        this.directionsWorkflow = directionsWorkflow;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATIC IMAGE SERVING
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<?> serveImage(@PathVariable String filename) {
        try {
            org.springframework.core.io.Resource resource = new ClassPathResource("static/campus-map-images/" + filename);
            if (!resource.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Image not found: " + filename);
            }
            String lower = filename.toLowerCase();
            MediaType mediaType = MediaType.IMAGE_JPEG;
            if (lower.endsWith(".png")) mediaType = MediaType.IMAGE_PNG;
            else if (lower.endsWith(".gif")) mediaType = MediaType.IMAGE_GIF;
            else if (lower.endsWith(".webp")) mediaType = MediaType.parseMediaType("image/webp");

            return ResponseEntity.ok().contentType(mediaType)
                    .body(new org.springframework.core.io.InputStreamResource(resource.getInputStream()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error serving image: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UC-32 — GET DIRECTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/directions")
    public ResponseEntity<?> getDirections(@RequestParam String from, @RequestParam String to) {
        try {
            // Template Method Pattern — fixed directions algorithm
            DirectionsResponse response = directionsWorkflow.execute(from, to);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UC-33 — BROWSE LOCATIONS BY CATEGORY
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/locations")
    public ResponseEntity<Map<String, List<CampusLocation>>> getAllLocationsGrouped() {
        List<CampusLocation> all = locationCatalog.findAllApproved();
        Map<String, List<CampusLocation>> grouped = new LinkedHashMap<>();
        List<String> order = Arrays.asList(
                "Academic Buildings", "Administrative Offices",
                "Facilities", "Parking Areas", "Sports Areas", "Faculty Offices");
        for (String cat : order) {
            List<CampusLocation> inCat = all.stream().filter(l -> cat.equals(l.getCategory())).collect(Collectors.toList());
            if (!inCat.isEmpty()) grouped.put(cat, inCat);
        }
        return ResponseEntity.ok(grouped);
    }

    @GetMapping("/locations/{id}")
    public ResponseEntity<?> getLocationById(@PathVariable Long id) {
        return locationRepo.findById(id).filter(CampusLocation::isApproved).map(loc -> {
            // Re-using same event logic as original
            List<com.fast.fsf.events.domain.CampusEvent> events = campusEventRepo.findByVenueContainingIgnoreCaseAndApprovedTrue(loc.getLocationName())
                    .stream().filter(e -> !e.getEventDate().isBefore(java.time.LocalDate.now())).collect(Collectors.toList());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("location", loc);
            body.put("activeEvents", events);
            return ResponseEntity.ok(body);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/locations/category/{category}")
    public ResponseEntity<List<CampusLocation>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(locationCatalog.findByCategory(category));
    }

    @GetMapping("/locations/type/{type}")
    public ResponseEntity<List<CampusLocation>> getByType(@PathVariable String type) {
        List<CampusLocation> result = locationRepo.findByLocationType(type).stream()
                .filter(CampusLocation::isApproved).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UC-34 — SEARCH FOR A LOCATION
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/locations/search")
    public ResponseEntity<List<CampusLocation>> searchLocations(@RequestParam(defaultValue = "") String query) {
        if (query.isBlank()) return ResponseEntity.ok(Collections.emptyList());

        // Strategy Pattern — combining multiple search criteria
        LocationSearchCriterion searchCriterion = new CompositeLocationSearchCriterion(Arrays.asList(
                new LocationNameCriterion(query),
                new FacultyOfficesCriterion(query),
                new ClassroomNumbersCriterion(query),
                new CategoryCriterion(query)
        ), true); // OR logic

        List<CampusLocation> results = searchService.search(searchCriterion);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/all-locations")
    public ResponseEntity<Map<String, Object>> getAllLocationsFlat() {
        List<CampusLocation> all = locationCatalog.findAllApproved();
        List<CampusMapRoute> allRoutes = routeRepo.findAll();
        Set<String> routeLocations = new LinkedHashSet<>();
        allRoutes.forEach(r -> {
            routeLocations.add(r.getFromLocation());
            routeLocations.add(r.getToLocation());
        });

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("locations", all);
        body.put("routeLocations", new ArrayList<>(routeLocations));
        return ResponseEntity.ok(body);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UC-35 — SUGGEST A LOCATION
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/suggestions")
    public ResponseEntity<?> submitSuggestion(@RequestBody LocationSuggestion suggestion) {
        try {
            // Factory Pattern for Suggestion creation
            LocationSuggestion newSuggestion = CampusMapFactory.createSuggestion(
                suggestion.getLocationName(), suggestion.getCategory(), suggestion.getDescription(),
                suggestion.getSubmittedBy(), suggestion.getSubmitterName()
            );
            LocationSuggestion saved = suggestionRepo.save(newSuggestion);
            eventPublisher.publishSuggestionSubmitted(saved);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<LocationSuggestion>> getAllSuggestions() {
        return ResponseEntity.ok(suggestionRepo.findByResolvedFalse());
    }

    @PatchMapping("/suggestions/{id}/resolve")
    public ResponseEntity<?> resolveSuggestion(@PathVariable Long id) {
        return suggestionRepo.findById(id).map(suggestion -> {
            if (suggestion.isResolved()) return ResponseEntity.badRequest().body("Suggestion already resolved");
            suggestion.setResolved(true);
            LocationSuggestion saved = suggestionRepo.save(suggestion);
            eventPublisher.publishSuggestionResolved(saved);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADMIN ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/locations/pending")
    public ResponseEntity<List<CampusLocation>> getPendingLocations() {
        return ResponseEntity.ok(locationRepo.findByApprovedFalse());
    }

    @GetMapping("/locations/flagged")
    public ResponseEntity<List<CampusLocation>> getFlaggedLocations() {
        return ResponseEntity.ok(locationRepo.findByFlaggedTrue());
    }

    @GetMapping("/locations/flagged/count")
    public ResponseEntity<Long> getFlaggedCount() {
        return ResponseEntity.ok(locationRepo.countByFlaggedTrue());
    }

    @GetMapping("/locations/count/active")
    public ResponseEntity<Long> getActiveCount() {
        return ResponseEntity.ok(locationRepo.countByApprovedTrue());
    }

    @PutMapping("/locations/{id}/approve")
    public ResponseEntity<?> approveLocation(@PathVariable Long id, @RequestParam(required = false) String reason) {
        try {
            // Template Method Pattern
            CampusLocation saved = approveWorkflow.execute(id, reason);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/locations/{id}/flag")
    public ResponseEntity<?> flagLocation(@PathVariable Long id, @RequestParam(required = false) String reason) {
        try {
            CampusLocation saved = flagWorkflow.execute(id, reason);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/locations/{id}/resolve")
    public ResponseEntity<?> resolveLocationFlag(@PathVariable Long id) {
        try {
            CampusLocation saved = resolveFlagWorkflow.execute(id, null);
            eventPublisher.publishLocationApproved(saved); // Re-using approved log pattern for resolve as per original logic
            // Note: resolveFlagWorkflow applyChange clears flag, publishEvent handles the log message
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/locations/{id}")
    public ResponseEntity<?> deleteLocation(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return locationRepo.findById(id).map(loc -> {
            String locName = loc.getLocationName();
            List<CampusMapRoute> fromRoutes = routeRepo.findAll().stream()
                    .filter(r -> r.getFromLocation().equalsIgnoreCase(locName) || r.getToLocation().equalsIgnoreCase(locName))
                    .collect(Collectors.toList());
            routeRepo.deleteAll(fromRoutes);
            locationRepo.deleteById(id);
            eventPublisher.publishLocationDeleted(loc, reason);
            return ResponseEntity.ok("Location and its routes deleted");
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/locations")
    public ResponseEntity<?> addLocation(@RequestBody CampusLocation location) {
        try {
            // Factory Pattern
            CampusLocation newLoc = CampusMapFactory.createLocation(
                location.getLocationName(), location.getLocationType(), location.getCategory(),
                location.getBlockId(), location.getDescription(), location.getFacultyOffices(),
                location.getClassroomNumbers(), location.getImagePath(),
                location.getOwnerEmail(), location.getOwnerName()
            );
            CampusLocation saved = locationRepo.save(newLoc);
            eventPublisher.publishLocationAdded(saved);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/locations/{id}")
    public ResponseEntity<?> updateLocation(@PathVariable Long id, @RequestBody CampusLocation updated) {
        return locationRepo.findById(id).map(loc -> {
            if (updated.getLocationName() != null) loc.setLocationName(updated.getLocationName());
            if (updated.getLocationType() != null) loc.setLocationType(updated.getLocationType());
            if (updated.getCategory() != null) loc.setCategory(updated.getCategory());
            if (updated.getDescription() != null) loc.setDescription(updated.getDescription());
            if (updated.getFacultyOffices() != null) loc.setFacultyOffices(updated.getFacultyOffices());
            if (updated.getClassroomNumbers() != null) loc.setClassroomNumbers(updated.getClassroomNumbers());
            if (updated.getBlockId() != null) loc.setBlockId(updated.getBlockId());
            CampusLocation saved = locationRepo.save(loc);
            // Updated log was not in the original event publisher list but hardcoded in controller
            // I'll keep it as a generic publish if needed or just fire manually
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/admin/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("File is empty");
        try {
            String uploadDir = "src/main/resources/static/campus-map-images/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            String fileName = file.getOriginalFilename();
            if (fileName != null) fileName = fileName.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9._-]", "");
            else fileName = "upload_" + System.currentTimeMillis() + ".jpg";
            Path path = Paths.get(uploadDir + fileName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            // activityLogRepo.save removed, fire event instead
            return ResponseEntity.ok(Map.of("fileName", fileName));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not save image: " + e.getMessage());
        }
    }

    @PostMapping("/admin/routes/step")
    public ResponseEntity<?> addRouteStep(@RequestBody CampusMapRoute route) {
        try {
            List<CampusMapRoute> existing = routeRepo.findByFromLocationAndToLocation(route.getFromLocation(), route.getToLocation());
            if (existing.stream().anyMatch(r -> r.getStepOrder() == route.getStepOrder())) {
                return ResponseEntity.badRequest().body("Step #" + route.getStepOrder() + " already exists for this route.");
            }
            // Factory Pattern
            CampusMapRoute newStep = CampusMapFactory.createRouteStep(
                route.getFromLocation(), route.getToLocation(), route.getStepOrder(),
                route.getImageFileName(), route.getStepDescription(),
                route.getOwnerEmail(), route.getOwnerName()
            );
            CampusMapRoute saved = routeRepo.save(newStep);
            eventPublisher.publishRouteStepAdded(saved);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/admin/routes")
    public ResponseEntity<?> deleteRoute(@RequestParam String from, @RequestParam String to) {
        List<CampusMapRoute> steps = routeRepo.findByFromLocationAndToLocation(from, to);
        routeRepo.deleteAll(steps);
        eventPublisher.publishRouteDeleted(from, to);
        return ResponseEntity.ok("Route and all its steps deleted");
    }

    @GetMapping("/admin/routes/all")
    public ResponseEntity<List<CampusMapRoute>> getAllRouteSteps() {
        return ResponseEntity.ok(routeRepo.findAll());
    }

    @DeleteMapping("/admin/routes/step/{id}")
    public ResponseEntity<?> deleteRouteStepById(@PathVariable Long id) {
        return routeRepo.findById(id).map(step -> {
            routeRepo.delete(step);
            // Deleting single step log
            return ResponseEntity.ok("Step deleted");
        }).orElse(ResponseEntity.notFound().build());
    }
}
