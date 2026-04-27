package com.fast.fsf.config;

import com.fast.fsf.model.CampusLocation;
import com.fast.fsf.model.CampusMapRoute;
import com.fast.fsf.repository.CampusLocationRepository;
import com.fast.fsf.repository.CampusMapRouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * CampusMapSeeder
 *
 * Runs on every application start-up.
 * Seeds campus_locations and campus_map_routes tables ONLY when they are empty.
 * This follows the same "guard with count()" pattern used elsewhere in the project.
 *
 * TO ADD MORE LOCATIONS IN THE FUTURE:
 *   → Call POST /api/campus-map/locations with the new location details
 *   → No code change needed
 *
 * TO ADD MORE ROUTES IN THE FUTURE:
 *   → Place your image file in:
 *        backend/src/main/resources/static/campus-map-images/
 *   → Name it: {from}_{to}_step{N}.jpg
 *        e.g. block_c_block_f_step1.jpg
 *   → Call POST /api/campus-map/admin/routes/step with:
 *        fromLocation, toLocation, stepOrder, imageFileName, stepDescription
 *   → The frontend picks it up automatically — zero code changes
 */
@Component
public class CampusMapSeeder implements CommandLineRunner {

    private final CampusLocationRepository locationRepo;
    private final CampusMapRouteRepository routeRepo;

    @Autowired
    public CampusMapSeeder(CampusLocationRepository locationRepo,
                           CampusMapRouteRepository routeRepo) {
        this.locationRepo = locationRepo;
        this.routeRepo = routeRepo;
    }

    @Override
    public void run(String... args) throws Exception {
        seedLocations();
        seedRoutes();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOCATIONS SEEDER
    // ─────────────────────────────────────────────────────────────────────────
    private void seedLocations() {
        long locCount = locationRepo.count();
        if (locCount > 0) {
            System.out.println("DEBUG [CampusMapSeeder]: Wiping and reloading campus_locations to apply new classroom structures.");
            return;
        }

        System.out.println("DEBUG [CampusMapSeeder]: Seeding campus locations...");

        List<CampusLocation> locations = Arrays.asList(

            // ── BLOCKS  (Academic Buildings) ─────────────────────────────────
            makeLocation("Block A",  "BLOCK", "Academic Buildings", "BLOCK_A",
                "Main academic block with CS classrooms",
                "Dr. Ali (CS)",
                "CR-A1, CR-A2, CR-A3, CR-A4, CR-A5"),

            makeLocation("Block B",  "BLOCK", "Academic Buildings", "BLOCK_B",
                "Mathematics and sciences block",
                "Dr. Kamran (Math), Dr. Hira (Math)",
                "CR-B1, CR-B2, CR-B3, CR-B4"),

            makeLocation("Block C",  "BLOCK", "Academic Buildings", "BLOCK_C",
                "Computer Science labs and offices. First floor: C-1 to C-7. Second floor: C-8 to C-14.",
                "Dr. Naveed (CS), Dr. Irfan (CS)",
                "C-1, C-2, C-3, C-4, C-5, C-6, C-7, C-8, C-9, C-10, C-11, C-12, C-13, C-14"),

            makeLocation("Block D",  "BLOCK", "Academic Buildings", "BLOCK_D",
                "CS classrooms and faculty offices",
                "Dr. Asghar (CS), Dr. Syed (CS)",
                "CR-D1, CR-D2, CR-D3, Lab-D1"),

            makeLocation("Block E",  "BLOCK", "Academic Buildings", "BLOCK_E",
                "Physics, Humanities and seminar hall",
                "Dr. Usman (PHY), Dr. Bilal (HUM)",
                "CR-E1, CR-E2, Seminar Hall"),

            makeLocation("Block F",  "BLOCK", "Academic Buildings", "BLOCK_F",
                "Advanced CS labs and mathematics classrooms",
                "Dr. Sadaf (Math)",
                "CR-F1, CR-F2, CR-F3, Lab-F1, Lab-F2"),

            // ── FACILITIES ───────────────────────────────────────────────────
            makeLocation("Library", "BLOCK", "Facilities", "LIBRARY",
                "Main campus library with study areas and books",
                null, null),

            makeLocation("Cafeteria", "BLOCK", "Facilities", "CAFETERIA",
                "Outdoor cafeteria near Block C",
                null, null),

            makeLocation("Indoor Cafeteria", "BLOCK", "Facilities", "INDOOR_CAFETERIA",
                "Indoor cafeteria on ground floor",
                null, null),

            makeLocation("Mosque", "BLOCK", "Facilities", "MOSQUE",
                "Campus mosque open for all prayers",
                null, null),

            // ── ADMINISTRATIVE OFFICES ───────────────────────────────────────
            makeLocation("Admin Block", "BLOCK", "Administrative Offices", "ADMIN_BLOCK",
                "Registrar, Finance, and Student Affairs offices",
                "Registrar Office, Finance Office, Student Affairs",
                null),

            // ── SPORTS ───────────────────────────────────────────────────────
            makeLocation("Sports Area", "BLOCK", "Sports Areas", "SPORTS_AREA",
                "Cricket ground, basketball court, and outdoor track",
                null, null),

            // ── PARKING ──────────────────────────────────────────────────────
            makeLocation("Parking Area", "BLOCK", "Parking Areas", "PARKING",
                "Main student and faculty parking",
                null, null),

            // ── ENTRANCES ────────────────────────────────────────────────────
            makeLocation("Main Gate", "BLOCK", "Facilities", "MAIN_GATE",
                "Main entrance of FAST-NUCES Lahore campus",
                null, null)
        );

        locationRepo.saveAll(locations);
        System.out.println("DEBUG [CampusMapSeeder]: Seeded " + locations.size() + " campus locations.");
    }

    /** Convenience factory for a pre-approved admin location. */
    private CampusLocation makeLocation(String name, String type, String category,
                                        String blockId, String description,
                                        String facultyOffices, String classroomNumbers) {
        CampusLocation loc = new CampusLocation();
        loc.setLocationName(name);
        loc.setLocationType(type);
        loc.setCategory(category);
        loc.setBlockId(blockId);
        loc.setDescription(description);
        loc.setFacultyOffices(facultyOffices);
        loc.setClassroomNumbers(classroomNumbers);
        loc.setOwnerEmail("admin@nu.edu.pk");
        loc.setOwnerName("FSF Admin");
        loc.setApproved(true);   // admin-seeded → pre-approved
        loc.setFlagged(false);
        return loc;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ROUTES SEEDER  (text-only: imageFileName = null for all)
    // ─────────────────────────────────────────────────────────────────────────
    private void seedRoutes() {
        if (routeRepo.count() > 0) {
            // Wipe old blockId-keyed rows (BLOCK_A, BLOCK_C etc.) so they are
            // replaced by location-name-keyed rows (Block A, Block C etc.)
            // that match what the frontend dropdowns send after first boot.
            boolean hasOldStyle = routeRepo.findAll().stream()
                    .anyMatch(r -> r.getFromLocation().startsWith("BLOCK_")
                               || r.getFromLocation().equals("MAIN_GATE")
                               || r.getFromLocation().equals("PARKING")
                               || r.getFromLocation().equals("CAFETERIA")
                               || r.getFromLocation().equals("LIBRARY"));
            if (hasOldStyle) {
                System.out.println("DEBUG [CampusMapSeeder]: Replacing old blockId-keyed routes with location-name-keyed routes...");
                routeRepo.deleteAll();
            } else {
                System.out.println("DEBUG [CampusMapSeeder]: campus_map_routes already seeded — skipping.");
                return;
            }
        }

        System.out.println("DEBUG [CampusMapSeeder]: Seeding campus map routes...");

        // IMPORTANT: fromLocation / toLocation MUST match the locationName field
        // in campus_locations exactly, because the frontend dropdown sends
        // location.locationName as the query parameter value.
        List<CampusMapRoute> steps = Arrays.asList(

            // Block A → Block B
            makeStep("Block A", "Block B", 1, "Exit Block A from the main door and walk straight ahead"),
            makeStep("Block A", "Block B", 2, "Turn right at the junction and walk 20 meters"),
            makeStep("Block A", "Block B", 3, "Block B entrance is on your left"),

            // Block A → Block C
            makeStep("Block A", "Block C", 1, "Exit Block A from the main door and turn left"),
            makeStep("Block A", "Block C", 2, "Walk straight past the parking area"),
            makeStep("Block A", "Block C", 3, "Block C is directly ahead"),

            // Block A → Cafeteria
            makeStep("Block A", "Cafeteria", 1, "Exit Block A and walk towards the open ground"),
            makeStep("Block A", "Cafeteria", 2, "The cafeteria is visible at the far end of the ground"),

            // Block C → Block F
            makeStep("Block C", "Block F", 1, "Exit Block C from the main door and turn right"),
            makeStep("Block C", "Block F", 2, "Walk straight along the main corridor"),
            makeStep("Block C", "Block F", 3, "Turn left at the end of the corridor"),
            makeStep("Block C", "Block F", 4, "Block F entrance is directly ahead"),

            // Block C → Library
            makeStep("Block C", "Library", 1, "Exit Block C and turn left"),
            makeStep("Block C", "Library", 2, "Walk past Block B"),
            makeStep("Block C", "Library", 3, "The Library building is on your right"),

            // Main Gate → Block A
            makeStep("Main Gate", "Block A", 1, "Enter from the Main Gate and walk straight"),
            makeStep("Main Gate", "Block A", 2, "Block A is the first building on your left"),

            // Main Gate → Cafeteria
            makeStep("Main Gate", "Cafeteria", 1, "Enter from the Main Gate"),
            makeStep("Main Gate", "Cafeteria", 2, "Turn right and walk past the parking area"),
            makeStep("Main Gate", "Cafeteria", 3, "Cafeteria is visible at the end"),

            // Parking Area → Block A
            makeStep("Parking Area", "Block A", 1, "From the Parking Area, walk towards the main building row"),
            makeStep("Parking Area", "Block A", 2, "Block A is the first block you reach")
        );

        routeRepo.saveAll(steps);
        System.out.println("DEBUG [CampusMapSeeder]: Seeded " + steps.size() + " route steps.");
    }

    /** Convenience factory for a text-only (no image) route step. */
    private CampusMapRoute makeStep(String from, String to, int order, String description) {
        CampusMapRoute step = new CampusMapRoute();
        step.setFromLocation(from);
        step.setToLocation(to);
        step.setStepOrder(order);
        step.setImageFileName(null);   // text-only — no image yet
        step.setStepDescription(description);
        step.setOwnerEmail("admin@nu.edu.pk");
        step.setOwnerName("FSF Admin");
        step.setApproved(true);
        step.setFlagged(false);
        return step;
    }
}
