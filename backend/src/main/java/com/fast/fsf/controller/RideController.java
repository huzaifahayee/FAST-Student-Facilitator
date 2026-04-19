package com.fast.fsf.controller;

import com.fast.fsf.model.ActivityLog;
import com.fast.fsf.model.Ride;
import com.fast.fsf.repository.ActivityLogRepository;
import com.fast.fsf.repository.RideRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RideController
 * 
 * What is this?
 * This is the 'Gatekeeper' for Carpool requests. 
 * When the React frontend wants to find or post a ride, it talks here.
 */
@RestController
@RequestMapping("/api/rides")
@CrossOrigin(origins = "http://localhost:5173") // Allow our React app to talk to this API
public class RideController {

    private final RideRepository rideRepository;
    private final ActivityLogRepository activityLogRepository;

    public RideController(RideRepository rideRepository, ActivityLogRepository activityLogRepository) {
        this.rideRepository = rideRepository;
        this.activityLogRepository = activityLogRepository;
    }

    /**
     * GET /api/rides
     * Returns all approved rides.
     */
    @GetMapping
    public List<Ride> getAllRides() {
        return rideRepository.findAll();
    }

    /**
     * POST /api/rides
     * Saves a new ride. It starts as PENDING (approved=false).
     */
    @PostMapping
    public ResponseEntity<Ride> offerRide(@RequestBody Ride ride) {
        if (ride.getCheckpoints().size() > 5) {
            return ResponseEntity.badRequest().build();
        }
        ride.setApproved(false); // Force pending status
        Ride saved = rideRepository.save(ride);
        
        System.out.println("DEBUG: New Ride Saved. ID: " + saved.getId() + " | Approved: " + saved.isApproved());
        
        // Log the activity
        activityLogRepository.save(new ActivityLog(
            ride.getDriverName() + " offered a new ride to " + ride.getDestination() + ".",
            "RIDE_POSTED"
        ));
        
        return ResponseEntity.ok(saved);
    }

    /**
     * GET /api/rides/pending
     * Returns rides waiting for Admin approval.
     */
    @GetMapping("/pending")
    public List<Ride> getPendingRides() {
        try {
            List<Ride> pending = rideRepository.findByApprovedFalse();
            System.out.println("DEBUG: Fetching Pending Rides. Found: " + pending.size());
            return pending;
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Failed to fetch pending rides: " + e.getMessage());
            throw e;
        }
    }

    /**
     * PUT /api/rides/{id}/approve
     * Admin action to approve a ride and make it public.
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<Ride> approveRide(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return rideRepository.findById(id)
            .map(ride -> {
                ride.setApproved(true);
                ride.setModerationReason(reason);
                Ride saved = rideRepository.save(ride);
                
                // Log the approval
                activityLogRepository.save(new ActivityLog(
                    "Ride #" + id + " was approved. Reason: " + (reason != null ? reason : "None"),
                    "RIDE_APPROVED"
                ));
                
                return ResponseEntity.ok(saved);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/rides/flagged/count
     */
    @GetMapping("/flagged/count")
    public long getFlaggedCount() {
        return rideRepository.countByFlaggedTrue();
    }

    /**
     * GET /api/rides/flagged
     */
    @GetMapping("/flagged")
    public List<Ride> getFlaggedRides() {
        return rideRepository.findByFlaggedTrue();
    }

    /**
     * PUT /api/rides/{id}/flag
     */
    @PutMapping("/{id}/flag")
    public ResponseEntity<Ride> flagRide(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return rideRepository.findById(id)
            .map(ride -> {
                ride.setFlagged(true);
                ride.setModerationReason(reason);
                Ride saved = rideRepository.save(ride);
                activityLogRepository.save(new ActivityLog("Ride #" + id + " flagged. Reason: " + reason, "RIDE_FLAGGED"));
                return ResponseEntity.ok(saved);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/rides/{id}/resolve
     */
    @PutMapping("/{id}/resolve")
    public ResponseEntity<Ride> resolveRide(@PathVariable Long id) {
        return rideRepository.findById(id)
            .map(ride -> {
                ride.setFlagged(false);
                ride.setModerationReason(null); // Clear reason on resolution
                Ride saved = rideRepository.save(ride);
                activityLogRepository.save(new ActivityLog("Moderation flag cleared for Ride #" + id + ".", "FLAG_RESOLVED"));
                return ResponseEntity.ok(saved);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/rides/count/active
     * Returns the count of rides that are public (approved).
     */
    @GetMapping("/count/active")
    public long getActiveCount() {
        return rideRepository.countByApprovedTrue();
    }

    /**
     * GET /api/rides/search?destination=...
     * Filters ONLY approved rides by destination.
     */
    @GetMapping("/search")
    public List<Ride> searchRides(@RequestParam String destination) {
        return rideRepository.findByDestinationContainingIgnoreCaseAndApprovedTrue(destination);
    }
    /**
     * DELETE /api/rides/{id}
     * Admin action to permanently remove a ride from the system.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRide(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return rideRepository.findById(id)
            .map(ride -> {
                // Log the deletion before removing
                activityLogRepository.save(new ActivityLog(
                    "Ride #" + id + " deleted by Admin. Reason: " + (reason != null ? reason : "Compliance violation"),
                    "CONTENT_DELETED"
                ));
                
                rideRepository.deleteById(id);
                return ResponseEntity.ok().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
