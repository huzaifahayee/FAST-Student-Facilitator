package com.fast.fsf.controller;

import com.fast.fsf.model.ActivityLog;
import com.fast.fsf.model.CampusEvent;
import com.fast.fsf.model.CampusLocation;
import com.fast.fsf.model.CampusMapRoute;
import com.fast.fsf.model.LocationSuggestion;
import com.fast.fsf.repository.ActivityLogRepository;
import com.fast.fsf.repository.CampusEventRepository;
import com.fast.fsf.repository.CampusLocationRepository;
import com.fast.fsf.repository.CampusMapRouteRepository;
import com.fast.fsf.repository.LocationSuggestionRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CampusMapController  (UC-32, UC-33, UC-34, UC-35)
 *
 * All business logic lives directly here — no service layer (convention).
 * Constructor injection only (@Autowired on constructor).
 * Every mutation is logged to ActivityLog.
 * No Lombok.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * HOW TO ADD MORE DIRECTION IMAGES IN THE FUTURE
 * ─────────────────────────────────────────────────────────────────────────
 * 1. Place your image file in:
 *    backend/src/main/resources/static/campus-map-images/
 *
 * 2. Name the file using this convention:
 *    {from}_{to}_step{N}.jpg
 *    Example: block_c_block_f_step1.jpg
 *             block_c_block_f_step2.jpg
 *             main_gate_block_a_step1.jpg
 *
 * 3. Call this endpoint to register it in the database:
 *    POST /api/campus-map/admin/routes/step
 *    Body: { fromLocation, toLocation, stepOrder,
 *            imageFileName, stepDescription,
 *            ownerEmail, ownerName }
 *
 * 4. The frontend fetches the image at:
 *    GET /api/campus-map/images/{filename}
 *    which serves it from the static folder above.
 *
 * 5. No code change is ever needed to add new routes or images.
 *    It is 100% data-driven.
 *
 * CURRENT ROUTES IN THE SYSTEM (update this comment when you add more):
 *   BLOCK_A  → BLOCK_B        (3 steps, text only)
 *   BLOCK_A  → BLOCK_C        (3 steps, text only)
 *   BLOCK_A  → CAFETERIA      (2 steps, text only)
 *   BLOCK_C  → BLOCK_F        (4 steps, text only)
 *   BLOCK_C  → LIBRARY        (3 steps, text only)
 *   MAIN_GATE → BLOCK_A       (2 steps, text only)
 *   MAIN_GATE → CAFETERIA     (3 steps, text only)
 *   PARKING   → BLOCK_A       (2 steps, text only)
 * ─────────────────────────────────────────────────────────────────────────
 */
@RestController
@RequestMapping("/api/campus-map")
@CrossOrigin(origins = "http://localhost:5173")
public class CampusMapController {

    private final CampusLocationRepository locationRepo;
    private final CampusMapRouteRepository routeRepo;
    private final LocationSuggestionRepository suggestionRepo;
    private final ActivityLogRepository activityLogRepo;
    private final CampusEventRepository campusEventRepo;

    @Autowired
    public CampusMapController(CampusLocationRepository locationRepo,
                               CampusMapRouteRepository routeRepo,
                               LocationSuggestionRepository suggestionRepo,
                               ActivityLogRepository activityLogRepo,
                               CampusEventRepository campusEventRepo) {
        this.locationRepo = locationRepo;
        this.routeRepo = routeRepo;
        this.suggestionRepo = suggestionRepo;
        this.activityLogRepo = activityLogRepo;
        this.campusEventRepo = campusEventRepo;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATIC IMAGE SERVING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/campus-map/images/{filename}
     *
     * Serves a direction photo from:
     *   backend/src/main/resources/static/campus-map-images/{filename}
     *
     * Returns 404 if the file does not exist.
     * Content-Type is inferred from extension (case-insensitive).
     * No ActivityLog entry — this is a static asset read, not a mutation.
     */
    @GetMapping("/images/{filename}")
    public ResponseEntity<byte[]> serveImage(@PathVariable String filename) {
        try {
            ClassPathResource resource = new ClassPathResource("static/campus-map-images/" + filename);
            if (!resource.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(("Image not found: " + filename).getBytes());
            }
            byte[] bytes = Files.readAllBytes(resource.getFile().toPath());

            // Determine Content-Type based on extension (case-insensitive)
            MediaType mediaType = MediaType.IMAGE_JPEG;
            String lower = filename.toLowerCase();
            if (lower.endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (lower.endsWith(".gif")) {
                mediaType = MediaType.IMAGE_GIF;
            } else if (lower.endsWith(".webp")) {
                mediaType = MediaType.parseMediaType("image/webp");
            }

            return ResponseEntity.ok().contentType(mediaType).body(bytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(("Image not found: " + filename).getBytes());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CAMPUS EVENT INTEGRATION (READ-ONLY)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Returns approved events whose venue matches (contains) the given name
     * and whose eventDate is today or in the future.
     * Pure read — no writes to the campus_events table ever.
     */
    private List<CampusEvent> getActiveEventsAtVenue(String venueName) {
        if (venueName == null || venueName.isBlank()) return Collections.emptyList();
        List<CampusEvent> events = campusEventRepo
                .findByVenueContainingIgnoreCaseAndApprovedTrue(venueName);
        LocalDate today = LocalDate.now();
        return events.stream()
                .filter(e -> !e.getEventDate().isBefore(today))
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UC-32 — GET DIRECTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/campus-map/directions?from={fromLocation}&to={toLocation}
     */
    @GetMapping("/directions")
    public ResponseEntity<Map<String, Object>> getDirections(
            @RequestParam String from,
            @RequestParam String to) {

        Map<String, Object> response = new LinkedHashMap<>();

        // ── Validation ────────────────────────────────────────────────────────
        if (from == null || from.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please select your current location"));
        }
        if (to == null || to.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please select a destination"));
        }

        // ── Resolve Sub-locations (Rooms) to Parent Blocks ─────────────────────
        String originalTo = to;
        String finalSubDestinationMessage = null;

        Optional<CampusLocation> primaryDest = locationRepo.findByLocationNameIgnoreCase(to);
        if (primaryDest.isEmpty()) {
            for (CampusLocation loc : locationRepo.findAll()) {
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

        // ── Same location ─────────────────────────────────────────────────────
        if (from.equalsIgnoreCase(to)) {
            Optional<CampusLocation> destInfo = locationRepo.findByLocationNameIgnoreCase(to);
            List<CampusEvent> events = destInfo
                    .map(l -> getActiveEventsAtVenue(l.getLocationName()))
                    .orElse(Collections.emptyList());

            response.put("sameLocation", true);
            if (finalSubDestinationMessage != null) {
                response.put("message", "You are already at " + to + "! Please locate room " + originalTo + " inside.");
            } else {
                response.put("message", "You are already at your destination. No directions needed.");
            }
            response.put("steps", Collections.emptyList());
            response.put("destinationInfo", destInfo.orElse(null));
            response.put("activeEvents", events);

            activityLogRepo.save(new ActivityLog(
                    "Directions requested: " + from + " → " + to,
                    "MAP_DIRECTIONS_REQUESTED"));
            return ResponseEntity.ok(response);
        }

        // ── Fetch route steps ─────────────────────────────────────────────────
        List<CampusMapRoute> steps =
                routeRepo.findByFromLocationAndToLocationOrderByStepOrderAsc(from, to);

        Optional<CampusLocation> destInfo = locationRepo.findByLocationNameIgnoreCase(to);
        List<CampusEvent> events = destInfo
                .map(l -> getActiveEventsAtVenue(l.getLocationName()))
                .orElse(Collections.emptyList());

        if (steps.isEmpty() && !routeRepo.existsByFromLocationAndToLocation(from, to)) {
            // Route genuinely not in the database
            response.put("routeFound", false);
            response.put("message",
                    "Directions for this route are not yet available. Please ask at the reception.");
            response.put("steps", Collections.emptyList());
            response.put("destinationInfo", destInfo.orElse(null));
            response.put("activeEvents", events);

            activityLogRepo.save(new ActivityLog(
                    "Directions requested: " + from + " → " + to,
                    "MAP_DIRECTIONS_REQUESTED"));
            return ResponseEntity.ok(response);
        }

        // ── Build step DTOs ───────────────────────────────────────────────────
        List<Map<String, Object>> stepDTOs = steps.stream().map(s -> {
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

        if (finalSubDestinationMessage != null) {
            Map<String, Object> extraStep = new LinkedHashMap<>();
            extraStep.put("stepOrder", steps.size() + 1);
            extraStep.put("imageFileName", null);
            extraStep.put("imageUrl", null);
            extraStep.put("stepDescription", finalSubDestinationMessage);
            extraStep.put("hasImage", false);
            stepDTOs.add(extraStep);
        }

        response.put("routeFound", true);
        response.put("sameLocation", false);
        response.put("totalSteps", steps.size());
        response.put("steps", stepDTOs);
        response.put("destinationInfo", destInfo.orElse(null));
        response.put("activeEvents", events);

        activityLogRepo.save(new ActivityLog(
                "Directions requested: " + from + " → " + to,
                "MAP_DIRECTIONS_REQUESTED"));

        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UC-33 — BROWSE LOCATIONS BY CATEGORY
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/campus-map/locations
     * Returns approved locations grouped by category.
     * Empty categories are omitted.
     */
    @GetMapping("/locations")
    public ResponseEntity<Map<String, List<CampusLocation>>> getAllLocationsGrouped() {
        List<CampusLocation> all = locationRepo.findByApprovedTrue();

        Map<String, List<CampusLocation>> grouped = new LinkedHashMap<>();
        // Preserve canonical order
        List<String> order = Arrays.asList(
                "Academic Buildings", "Administrative Offices",
                "Facilities", "Parking Areas", "Sports Areas", "Faculty Offices");
        for (String cat : order) {
            List<CampusLocation> inCat = all.stream()
                    .filter(l -> cat.equals(l.getCategory()))
                    .collect(Collectors.toList());
            if (!inCat.isEmpty()) {
                grouped.put(cat, inCat);
            }
        }
        return ResponseEntity.ok(grouped);
    }

    /**
     * GET /api/campus-map/locations/{id}
     * Returns single location + active events.
     */
    @GetMapping("/locations/{id}")
    public ResponseEntity<?> getLocationById(@PathVariable Long id) {
        Optional<CampusLocation> opt = locationRepo.findById(id);
        if (opt.isEmpty() || !opt.get().isApproved()) {
            return ResponseEntity.notFound().build();
        }
        CampusLocation loc = opt.get();
        List<CampusEvent> events = getActiveEventsAtVenue(loc.getLocationName());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("location", loc);
        body.put("activeEvents", events);
        return ResponseEntity.ok(body);
    }

    /**
     * GET /api/campus-map/locations/category/{category}
     */
    @GetMapping("/locations/category/{category}")
    public ResponseEntity<List<CampusLocation>> getByCategory(@PathVariable String category) {
        List<CampusLocation> result = locationRepo.findByCategory(category).stream()
                .filter(CampusLocation::isApproved)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/campus-map/locations/type/{type}
     */
    @GetMapping("/locations/type/{type}")
    public ResponseEntity<List<CampusLocation>> getByType(@PathVariable String type) {
        List<CampusLocation> result = locationRepo.findByLocationType(type).stream()
                .filter(CampusLocation::isApproved)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UC-34 — SEARCH FOR A LOCATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/campus-map/locations/search?query={q}
     * Case-insensitive partial match across name, faculty offices,
     * classroom numbers, and category.
     * Returns 200 empty list — never 404 — when nothing matches.
     */
    @GetMapping("/locations/search")
    public ResponseEntity<List<CampusLocation>> searchLocations(
            @RequestParam(defaultValue = "") String query) {
        if (query.isBlank()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(locationRepo.searchLocations(query.trim()));
    }

    /**
     * GET /api/campus-map/all-locations
     * Flat list of all approved locations + deduplicated route endpoint keys.
     * Used by the frontend dropdowns.
     */
    @GetMapping("/all-locations")
    public ResponseEntity<Map<String, Object>> getAllLocationsFlat() {
        List<CampusLocation> all = locationRepo.findByApprovedTrue();

        // Collect unique location keys that actually have route data
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

    /**
     * POST /api/campus-map/suggestions
     */
    @PostMapping("/suggestions")
    public ResponseEntity<?> submitSuggestion(@RequestBody LocationSuggestion suggestion) {
        if (suggestion.getLocationName() == null || suggestion.getLocationName().isBlank()) {
            return ResponseEntity.badRequest().body("Location name is required");
        }
        if (suggestion.getCategory() == null || suggestion.getCategory().isBlank()) {
            return ResponseEntity.badRequest().body("Please select a category");
        }
        if (suggestion.getDescription() == null || suggestion.getDescription().isBlank()) {
            return ResponseEntity.badRequest().body("Please describe the location");
        }
        if (suggestion.getSubmittedBy() == null || suggestion.getSubmittedBy().isBlank()) {
            return ResponseEntity.badRequest().body("User email is required");
        }

        suggestion.setSubmittedAt(LocalDateTime.now());
        suggestion.setResolved(false);
        suggestion.setApproved(false);
        LocationSuggestion saved = suggestionRepo.save(suggestion);

        activityLogRepo.save(new ActivityLog(
                suggestion.getSubmitterName() + " suggested location: " + suggestion.getLocationName(),
                "LOCATION_SUGGESTED"));

        return ResponseEntity.ok(saved);
    }

    /**
     * GET /api/campus-map/suggestions
     * Admin-facing: returns all unresolved suggestions.
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<LocationSuggestion>> getAllSuggestions() {
        return ResponseEntity.ok(suggestionRepo.findByResolvedFalse());
    }

    /**
     * PATCH /api/campus-map/suggestions/{id}/resolve
     */
    @PatchMapping("/suggestions/{id}/resolve")
    public ResponseEntity<?> resolveSuggestion(@PathVariable Long id) {
        Optional<LocationSuggestion> opt = suggestionRepo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        LocationSuggestion suggestion = opt.get();
        if (suggestion.isResolved()) {
            return ResponseEntity.badRequest().body("Suggestion already resolved");
        }
        suggestion.setResolved(true);
        LocationSuggestion saved = suggestionRepo.save(suggestion);

        activityLogRepo.save(new ActivityLog(
                "Location suggestion #" + id + " resolved",
                "SUGGESTION_RESOLVED"));

        return ResponseEntity.ok(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADMIN ENDPOINTS — mirror RideController pattern
    // ═══════════════════════════════════════════════════════════════════════════

    /** GET /api/campus-map/locations/pending */
    @GetMapping("/locations/pending")
    public ResponseEntity<List<CampusLocation>> getPendingLocations() {
        return ResponseEntity.ok(locationRepo.findByApprovedFalse());
    }

    /** GET /api/campus-map/locations/flagged */
    @GetMapping("/locations/flagged")
    public ResponseEntity<List<CampusLocation>> getFlaggedLocations() {
        return ResponseEntity.ok(locationRepo.findByFlaggedTrue());
    }

    /** GET /api/campus-map/locations/flagged/count */
    @GetMapping("/locations/flagged/count")
    public ResponseEntity<Long> getFlaggedCount() {
        return ResponseEntity.ok(locationRepo.countByFlaggedTrue());
    }

    /** GET /api/campus-map/locations/count/active */
    @GetMapping("/locations/count/active")
    public ResponseEntity<Long> getActiveCount() {
        return ResponseEntity.ok(locationRepo.countByApprovedTrue());
    }

    /** PUT /api/campus-map/locations/{id}/approve?reason= */
    @PutMapping("/locations/{id}/approve")
    public ResponseEntity<?> approveLocation(@PathVariable Long id,
                                             @RequestParam(required = false) String reason) {
        Optional<CampusLocation> opt = locationRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        CampusLocation loc = opt.get();
        if (loc.isApproved()) {
            return ResponseEntity.badRequest().body("Location is already approved");
        }
        loc.setApproved(true);
        loc.setModerationReason(reason);
        CampusLocation saved = locationRepo.save(loc);

        activityLogRepo.save(new ActivityLog("Location #" + id + " approved", "LOCATION_APPROVED"));
        return ResponseEntity.ok(saved);
    }

    /** PUT /api/campus-map/locations/{id}/flag?reason= */
    @PutMapping("/locations/{id}/flag")
    public ResponseEntity<?> flagLocation(@PathVariable Long id,
                                          @RequestParam(required = false) String reason) {
        Optional<CampusLocation> opt = locationRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        CampusLocation loc = opt.get();
        loc.setFlagged(true);
        loc.setModerationReason(reason);
        CampusLocation saved = locationRepo.save(loc);

        activityLogRepo.save(new ActivityLog(
                "Location #" + id + " flagged: " + reason, "LOCATION_FLAGGED"));
        return ResponseEntity.ok(saved);
    }

    /** PUT /api/campus-map/locations/{id}/resolve — clears flag */
    @PutMapping("/locations/{id}/resolve")
    public ResponseEntity<?> resolveLocationFlag(@PathVariable Long id) {
        Optional<CampusLocation> opt = locationRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        CampusLocation loc = opt.get();
        loc.setFlagged(false);
        loc.setModerationReason(null);
        CampusLocation saved = locationRepo.save(loc);

        activityLogRepo.save(new ActivityLog(
                "Flag on Location #" + id + " resolved", "LOCATION_FLAG_RESOLVED"));
        return ResponseEntity.ok(saved);
    }

    /**
     * DELETE /api/campus-map/locations/{id}?reason=
     * Cascades: deletes all route steps involving this location first.
     */
    @DeleteMapping("/locations/{id}")
    public ResponseEntity<?> deleteLocation(@PathVariable Long id,
                                            @RequestParam(required = false) String reason) {
        Optional<CampusLocation> opt = locationRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        CampusLocation loc = opt.get();
        String locName = loc.getLocationName();

        // Cascade: remove route steps that reference this location
        List<CampusMapRoute> fromRoutes = routeRepo.findAll().stream()
                .filter(r -> r.getFromLocation().equalsIgnoreCase(locName)
                          || r.getToLocation().equalsIgnoreCase(locName))
                .collect(Collectors.toList());
        routeRepo.deleteAll(fromRoutes);

        locationRepo.deleteById(id);

        activityLogRepo.save(new ActivityLog(
                "Location #" + id + " deleted. Reason: " + (reason != null ? reason : "None"),
                "LOCATION_DELETED"));

        return ResponseEntity.ok("Location and its routes deleted");
    }

    /**
     * POST /api/campus-map/locations
     * Admin/student adds a new location (starts as pending for students).
     */
    @PostMapping("/locations")
    public ResponseEntity<?> addLocation(@RequestBody CampusLocation location) {
        if (location.getLocationName() == null || location.getLocationName().isBlank()) {
            return ResponseEntity.badRequest().body("Location name is required");
        }
        if (location.getLocationType() == null || location.getLocationType().isBlank()) {
            return ResponseEntity.badRequest().body("Location type is required");
        }
        if (location.getCategory() == null || location.getCategory().isBlank()) {
            return ResponseEntity.badRequest().body("Category is required");
        }
        if (location.getOwnerEmail() == null || location.getOwnerEmail().isBlank()) {
            return ResponseEntity.badRequest().body("Owner email is required");
        }

        List<String> validTypes = Arrays.asList("BLOCK", "FACULTY_OFFICE", "ROOM");
        if (!validTypes.contains(location.getLocationType())) {
            return ResponseEntity.badRequest()
                    .body("Location type must be one of: BLOCK, FACULTY_OFFICE, ROOM");
        }

        List<String> validCats = Arrays.asList(
                "Academic Buildings", "Administrative Offices",
                "Facilities", "Parking Areas", "Sports Areas", "Faculty Offices");
        if (!validCats.contains(location.getCategory())) {
            return ResponseEntity.badRequest()
                    .body("Category must be one of the 6 valid values");
        }

        location.setApproved(false);
        location.setFlagged(false);
        CampusLocation saved = locationRepo.save(location);

        activityLogRepo.save(new ActivityLog(
                location.getOwnerName() + " added location: " + location.getLocationName(),
                "LOCATION_ADDED"));

        return ResponseEntity.ok(saved);
    }

    /**
     * PUT /api/campus-map/locations/{id}
     * Admin/owner updates an existing location.
     */
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
            activityLogRepo.save(new ActivityLog("Location #" + id + " updated", "LOCATION_UPDATED"));
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // IMAGE UPLOAD (ADMIN ONLY)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * POST /api/campus-map/admin/upload-image
     * Uploads a direction image to the static resources folder.
     */
    @PostMapping("/admin/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            // Target directory: src/main/resources/static/campus-map-images/
            // Note: During local development, this writes to the source folder so you can commit to Git.
            String uploadDir = "src/main/resources/static/campus-map-images/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = file.getOriginalFilename();
            // Sanitize filename: replace spaces with underscores, remove weird characters
            if (fileName != null) {
                fileName = fileName.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9._-]", "");
            } else {
                fileName = "upload_" + System.currentTimeMillis() + ".jpg";
            }

            Path path = Paths.get(uploadDir + fileName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            activityLogRepo.save(new ActivityLog(
                    "Image uploaded to campus map: " + fileName,
                    "MAP_IMAGE_UPLOADED"));

            return ResponseEntity.ok(Map.of("fileName", fileName));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not save image: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // ROUTE ADMIN ENDPOINTS
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/campus-map/admin/routes/step
     * Registers a single direction step (optionally with an image filename).
     */
    @PostMapping("/admin/routes/step")
    public ResponseEntity<?> addRouteStep(@RequestBody CampusMapRoute route) {
        // ── Duplicate Check ──
        List<CampusMapRoute> existing = routeRepo.findByFromLocationAndToLocation(
                route.getFromLocation(), route.getToLocation());
        boolean isDuplicate = existing.stream()
                .anyMatch(r -> r.getStepOrder() == route.getStepOrder());
        if (isDuplicate) {
            return ResponseEntity.badRequest()
                    .body("Step #" + route.getStepOrder() + " already exists for this route.");
        }

        if (route.getFromLocation() == null || route.getFromLocation().isBlank()) {
            return ResponseEntity.badRequest().body("From location is required");
        }
        if (route.getToLocation() == null || route.getToLocation().isBlank()) {
            return ResponseEntity.badRequest().body("To location is required");
        }
        if (route.getStepDescription() == null || route.getStepDescription().isBlank()) {
            return ResponseEntity.badRequest().body("Step description is required");
        }
        if (route.getOwnerEmail() == null || route.getOwnerEmail().isBlank()) {
            return ResponseEntity.badRequest().body("Owner email is required");
        }
        if (route.getStepOrder() < 1) {
            return ResponseEntity.badRequest().body("Step order must be 1 or greater");
        }
        if (route.getFromLocation().equalsIgnoreCase(route.getToLocation())) {
            return ResponseEntity.badRequest()
                    .body("From and To cannot be the same location");
        }

        route.setApproved(true); // admin-submitted steps are pre-approved
        CampusMapRoute saved = routeRepo.save(route);

        activityLogRepo.save(new ActivityLog(
                "Route step added: " + route.getFromLocation() + " → " + route.getToLocation()
                        + " step " + route.getStepOrder(),
                "ROUTE_STEP_ADDED"));

        return ResponseEntity.ok(saved);
    }

    /**
     * DELETE /api/campus-map/admin/routes?from={from}&to={to}
     * Removes all steps for the given route pair.
     */
    @DeleteMapping("/admin/routes")
    public ResponseEntity<?> deleteRoute(@RequestParam String from, @RequestParam String to) {
        List<CampusMapRoute> steps = routeRepo.findByFromLocationAndToLocation(from, to);
        routeRepo.deleteAll(steps);

        activityLogRepo.save(new ActivityLog(
                "Route deleted: " + from + " → " + to,
                "ROUTE_DELETED"));

        return ResponseEntity.ok("Route and all its steps deleted");
    }
    /**
     * GET /api/campus-map/admin/routes/all
     * Returns all route steps for the admin panel table.
     */
    @GetMapping("/admin/routes/all")
    public ResponseEntity<List<CampusMapRoute>> getAllRouteSteps() {
        return ResponseEntity.ok(routeRepo.findAll());
    }

    /**
     * DELETE /api/campus-map/admin/routes/step/{id}
     * Removes a single route step by its ID.
     */
    @DeleteMapping("/admin/routes/step/{id}")
    public ResponseEntity<?> deleteRouteStepById(@PathVariable Long id) {
        if (!routeRepo.existsById(id)) return ResponseEntity.notFound().build();
        routeRepo.deleteById(id);
        
        activityLogRepo.save(new ActivityLog(
                "Route step #" + id + " deleted via admin panel",
                "ROUTE_STEP_DELETED"));
                
        return ResponseEntity.ok("Step deleted");
    }
}
