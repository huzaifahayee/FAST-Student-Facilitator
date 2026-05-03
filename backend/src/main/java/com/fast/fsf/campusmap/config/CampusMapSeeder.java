package com.fast.fsf.campusmap.config;

import com.fast.fsf.campusmap.domain.CampusLocation;
import com.fast.fsf.campusmap.domain.CampusMapRoute;
import com.fast.fsf.campusmap.persistence.CampusLocationRepository;
import com.fast.fsf.campusmap.persistence.CampusMapRouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * CampusMapSeeder
 *
 * Runs on every application start-up.
 * Seeds campus_locations and campus_map_routes tables.
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

    private void seedLocations() {
        if (locationRepo.count() > 0) {
            System.out.println("DEBUG [CampusMapSeeder]: campus_locations already seeded — skipping.");
            return;
        }

        System.out.println("DEBUG [CampusMapSeeder]: Seeding campus locations...");

        List<CampusLocation> locations = Arrays.asList(
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
                        "Advanced CS labs and mathematics classrooms.",
                        "Dr. Sadaf (Math)",
                        "F201, Lab 13"),
                makeLocation("Library", "BLOCK", "Facilities", "LIBRARY",
                        "Main campus library", null, null),
                makeLocation("Cafeteria", "BLOCK", "Facilities", "CAFETERIA",
                        "Outdoor cafeteria near Block C", null, null),
                makeLocation("Main Gate", "BLOCK", "Facilities", "MAIN_GATE",
                        "Main entrance", null, null)
        );

        locationRepo.saveAll(locations);
    }

    private void seedRoutes() {
        System.out.println("DEBUG [CampusMapSeeder]: Checking/Seeding campus map routes...");

        List<CampusMapRoute> canonicalSteps = Arrays.asList(
                makeStep("Block A", "Block C", 1, "block_a_block_c_step1.jpg", "Exit Block A from the main door and turn left"),
                makeStep("Block A", "Block C", 2, "block_a_block_c_step2.jpg", "Walk straight along the main lawn area"),
                makeStep("Block A", "Block C", 3, "block_a_block_c_step3.jpg", "Continue past the parking zone entrance"),
                makeStep("Block A", "Block C", 4, "block_a_block_c_step4.jpg", "Walk towards the student lounge area"),
                makeStep("Block A", "Block C", 5, "block_a_block_c_step5.jpg", "Follow the paved path towards the science labs"),
                makeStep("Block A", "Block C", 6, "block_a_block_c_step6.jpg", "Block C is now visible on your right"),
                makeStep("Block A", "Block C", 7, "block_a_block_c_step7.jpg", "Arrive at the main entrance of Block C"),

                makeStep("Block A", "Block E", 1, "block_a_block_e_step1.jpg", "Exit Block A and head towards the right corridor"),
                makeStep("Block A", "Block E", 2, "block_a_block_e_step2.jpg", "Walk through the connecting bridge between blocks"),
                makeStep("Block A", "Block E", 3, "block_a_block_e_step3.jpg", "Arrive at the entrance of Block E"),

                makeStep("Block A", "Block B", 1, "block_a_to_block_b_step1.jpg.jpeg", "Exit Block A from the main door"),
                makeStep("Block A", "Block F", 1, "block_a_to_block_f_step1.jpg.jpeg", "Exit Block A and walk towards the back parking area"),
                makeStep("Block A", "Block F", 2, "block_a_to_block_f_step2.jpg.jpeg", "Follow the road past the cafeteria")
        );

        for (CampusMapRoute canonical : canonicalSteps) {
            List<CampusMapRoute> existing = routeRepo.findByFromLocationAndToLocation(
                    canonical.getFromLocation(), canonical.getToLocation());
            
            Optional<CampusMapRoute> matchingStep = existing.stream()
                    .filter(r -> r.getStepOrder() == canonical.getStepOrder())
                    .findFirst();

            if (matchingStep.isEmpty()) {
                routeRepo.save(canonical);
            } else {
                CampusMapRoute step = matchingStep.get();
                if (step.getImageFileName() == null && canonical.getImageFileName() != null) {
                    step.setImageFileName(canonical.getImageFileName());
                    routeRepo.save(step);
                    System.out.println("DEBUG [CampusMapSeeder]: Updated image for " + step.getFromLocation() + " -> " + step.getToLocation() + " step " + step.getStepOrder());
                }
            }
        }
    }

    private CampusLocation makeLocation(String name, String type, String category,
            String blockId, String description, String facultyOffices, String classroomNumbers) {
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
        loc.setApproved(true);
        loc.setFlagged(false);
        return loc;
    }

    private CampusMapRoute makeStep(String from, String to, int order, String image, String description) {
        CampusMapRoute step = new CampusMapRoute();
        step.setFromLocation(from);
        step.setToLocation(to);
        step.setStepOrder(order);
        step.setImageFileName(image);
        step.setStepDescription(description);
        step.setOwnerEmail("admin@nu.edu.pk");
        step.setOwnerName("FSF Admin");
        step.setApproved(true);
        step.setFlagged(false);
        return step;
    }
}
