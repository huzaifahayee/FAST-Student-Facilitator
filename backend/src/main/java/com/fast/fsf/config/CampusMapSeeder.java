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
 * This follows the same "guard with count()" pattern used elsewhere in the
 * project.
 *
 * TO ADD MORE LOCATIONS IN THE FUTURE:
 * → Call POST /api/campus-map/locations with the new location details
 * → No code change needed
 *
 * TO ADD MORE ROUTES IN THE FUTURE:
 * → Place your image file in:
 * backend/src/main/resources/static/campus-map-images/
 * → Name it: {from}_{to}_step{N}.jpg
 * e.g. block_c_block_f_step1.jpg
 * → Call POST /api/campus-map/admin/routes/step with:
 * fromLocation, toLocation, stepOrder, imageFileName, stepDescription
 * → The frontend picks it up automatically — zero code changes
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
        if (locationRepo.count() > 0) {
            System.out.println("DEBUG [CampusMapSeeder]: campus_locations already seeded — skipping.");
            return;
        }

        System.out.println("DEBUG [CampusMapSeeder]: Seeding campus locations...");

        List<CampusLocation> locations = Arrays.asList(

                // ── BLOCKS (Academic Buildings) ─────────────────────────────────
                makeLocation("Block A", "BLOCK", "Academic Buildings", "BLOCK_A",
                        "Main academic block with CS classrooms",
                        "Dr. Ali (CS)",
                        "CR-A1, CR-A2, CR-A3, CR-A4, CR-A5"),

                makeLocation("Block B", "BLOCK", "Academic Buildings", "BLOCK_B",
                        "Mathematics and sciences block",
                        "Dr. Kamran (Math), Dr. Hira (Math)",
                        "CR-B1, CR-B2, CR-B3, CR-B4"),

                makeLocation("Block C", "BLOCK", "Academic Buildings", "BLOCK_C",
                        "Computer Science labs and offices. First floor: C-1 to C-7. Second floor: C-8 to C-14.",
                        "Dr. Naveed (CS), Dr. Irfan (CS)",
                        "C-1, C-2, C-3, C-4, C-5, C-6, C-7, C-8, C-9, C-10, C-11, C-12, C-13, C-14"),

                makeLocation("Block D", "BLOCK", "Academic Buildings", "BLOCK_D",
                        "CS classrooms and faculty offices",
                        "Dr. Asghar (CS), Dr. Syed (CS)",
                        "CR-D1, CR-D2, CR-D3, Lab-D1"),

                makeLocation("Block E", "BLOCK", "Academic Buildings", "BLOCK_E",
                        "Physics, Humanities and seminar hall",
                        "Dr. Usman (PHY), Dr. Bilal (HUM)",
                        "CR-E1, CR-E2, Seminar Hall"),

                makeLocation("Block F", "BLOCK", "Academic Buildings", "BLOCK_F",
                        "Advanced CS labs and mathematics classrooms. Second floor: F201-F212. Third floor: F301-F312 and labs 13-18.",
                        "Dr. Sadaf (Math)",
                        "F201, F202, F203, F204, F205, F206, F207, F208, F209, F210, F211, F212, F301, F302, F303, F304, F305, F306, F307, F308, F309, F310, F311, F312, Lab 13, Lab 14, Lab 15, Lab 16, Lab 17, Lab 18"),

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
                        null, null));

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
        loc.setApproved(true); // admin-seeded → pre-approved
        loc.setFlagged(false);
        return loc;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ROUTES SEEDER (text-only: imageFileName = null for all)
    // ─────────────────────────────────────────────────────────────────────────
    private void seedRoutes() {
        if (routeRepo.count() > 0) {
            System.out.println("DEBUG [CampusMapSeeder]: campus_map_routes already seeded — skipping.");
            return;
        }

        System.out.println("DEBUG [CampusMapSeeder]: Seeding campus map routes...");

        // IMPORTANT: fromLocation / toLocation MUST match the locationName field
        // in campus_locations exactly, because the frontend dropdown sends
        // location.locationName as the query parameter value.
        List<CampusMapRoute> steps = Arrays.asList(

                // Block A → Block C (with images)
                makeStep("Block A", "Block C", 1, "block_a_block_c_step1.jpg",
                        "Exit Block A from the main door and turn left"),
                makeStep("Block A", "Block C", 2, "block_a_block_c_step2.jpg",
                        "Walk straight along the main lawn area"),
                makeStep("Block A", "Block C", 3, "block_a_block_c_step3.jpg",
                        "Continue past the parking zone entrance"),
                makeStep("Block A", "Block C", 4, "block_a_block_c_step4.jpg", "Walk towards the student lounge area"),
                makeStep("Block A", "Block C", 5, "block_a_block_c_step5.jpg",
                        "Follow the paved path towards the science labs"),
                makeStep("Block A", "Block C", 6, "block_a_block_c_step6.jpg", "Block C is now visible on your right"),
                makeStep("Block A", "Block C", 7, "block_a_block_c_step7.jpg",
                        "Arrive at the main entrance of Block C"),

                // Block A → Block E (with images)
                makeStep("Block A", "Block E", 1, "block_a_block_e_step1.jpg",
                        "Exit Block A and head towards the right corridor"),
                makeStep("Block A", "Block E", 2, "block_a_block_e_step2.jpg",
                        "Walk through the connecting bridge between blocks"),
                makeStep("Block A", "Block E", 3, "block_a_block_e_step3.jpg", "Arrive at the entrance of Block E"),

                // Block A → Block B
                makeStep("Block A", "Block B", 1, "block_a_to_block_b_step1.jpg.jpeg",
                        "Exit Block A from the main door and walk towards the central lawn"),
                makeStep("Block A", "Block B", 2, null, "Turn right at the junction and walk 20 meters"),
                makeStep("Block A", "Block B", 3, null, "Block B entrance is on your left"),

                // Block A → Block F
                makeStep("Block A", "Block F", 1, "block_a_to_block_f_step1.jpg.jpeg",
                        "Exit Block A and walk towards the back parking area"),
                makeStep("Block A", "Block F", 2, "block_a_to_block_f_step2.jpg.jpeg",
                        "Follow the road past the cafeteria and continue straight to Block F"),

                // Block A → Cafeteria
                makeStep("Block A", "Cafeteria", 1, null, "Exit Block A and walk towards the open ground"),
                makeStep("Block A", "Cafeteria", 2, null, "The cafeteria is visible at the far end of the ground"),

                // Block C → Block F
                makeStep("Block C", "Block F", 1, null, "Exit Block C from the main door and turn right"),
                makeStep("Block C", "Block F", 2, null, "Walk straight along the main corridor"),
                makeStep("Block C", "Block F", 3, null, "Turn left at the end of the corridor"),
                makeStep("Block C", "Block F", 4, null, "Block F entrance is directly ahead"),

                // Block C → Library
                makeStep("Block C", "Library", 1, null, "Exit Block C and turn left"),
                makeStep("Block C", "Library", 2, null, "Walk past Block B"),
                makeStep("Block C", "Library", 3, null, "The Library building is on your right"),

                // Main Gate → Block A
                makeStep("Main Gate", "Block A", 1, null, "Enter from the Main Gate and walk straight"),
                makeStep("Main Gate", "Block A", 2, null, "Block A is the first building on your left"),

                // Main Gate → Cafeteria
                makeStep("Main Gate", "Cafeteria", 1, null, "Enter from the Main Gate"),
                makeStep("Main Gate", "Cafeteria", 2, null, "Turn right and walk past the parking area"),
                makeStep("Main Gate", "Cafeteria", 3, null, "Cafeteria is visible at the end"),

                // Parking Area → Block A
                makeStep("Parking Area", "Block A", 1, null,
                        "From the Parking Area, walk towards the main building row"),
                makeStep("Parking Area", "Block A", 2, null, "Block A is the first block you reach"),

                // Block F → Block D
                makeStep("Block F", "Block D", 1, null, "Exit Block F and turn left towards the main walkway"),
                makeStep("Block F", "Block D", 2, null, "Walk past the science labs to reach Block D"),

                // Main Gate → Block F
                makeStep("Main Gate", "Block F", 1, null, "Enter the campus and take the first right turn"),
                makeStep("Main Gate", "Block F", 2, null,
                        "Follow the road until you reach the far end of the campus where Block F is located"));

        routeRepo.saveAll(steps);
        System.out.println("DEBUG [CampusMapSeeder]: Seeded " + steps.size() + " route steps.");
    }

    /** Convenience factory for a route step. */
    private CampusMapRoute makeStep(String from, String to, int order, String image, String description) {
        CampusMapRoute step = new CampusMapRoute();
        step.setFromLocation(from);
        step.setToLocation(to);
        step.setStepOrder(order);
        step.setImageFileName(image); // null = text-only, filename = with photo
        step.setStepDescription(description);
        step.setOwnerEmail("admin@nu.edu.pk");
        step.setOwnerName("FSF Admin");
        step.setApproved(true);
        step.setFlagged(false);
        return step;
    }
}
