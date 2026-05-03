package com.fast.fsf.carpool.web;

import com.fast.fsf.carpool.event.RideOfferedEvent;
import com.fast.fsf.carpool.factory.RideOfferFactory;
import com.fast.fsf.search.factory.RideSearchCriterionFactory;
import com.fast.fsf.search.service.RideSearchService;
import com.fast.fsf.carpool.template.ApproveRideWorkflow;
import com.fast.fsf.carpool.template.DeleteRideWorkflow;
import com.fast.fsf.carpool.template.FlagRideWorkflow;
import com.fast.fsf.carpool.template.ResolveRideWorkflow;
import com.fast.fsf.carpool.domain.Ride;
import com.fast.fsf.carpool.persistence.RideRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * RideController — HTTP adapter for Feature #1 Carpool (UC‑05 … UC‑09 in Phase‑2 diagrams).
 * <p>
 * <strong>Singleton (GoF / Spring)</strong>: Spring registers {@code @RestController} beans in {@linkplain org.springframework.beans.factory.config.BeanDefinition#SCOPE_SINGLETON singleton}
 * scope by default — exactly one shared instance serves every concurrent HTTP request. This matches classic Singleton
 * intent (single coordinated resource) without unsafe global {@code static} fields.
 */
@RestController
@RequestMapping("/api/rides")
@CrossOrigin(originPatterns = {"http://localhost:*"})
public class RideController {

    private final RideRepository rideRepository;
    private final RideOfferFactory rideOfferFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final RideSearchService rideSearchService;
    private final RideSearchCriterionFactory rideSearchCriterionFactory;
    private final ApproveRideWorkflow approveRideWorkflow;
    private final FlagRideWorkflow flagRideWorkflow;
    private final ResolveRideWorkflow resolveRideWorkflow;
    private final DeleteRideWorkflow deleteRideWorkflow;

    public RideController(
            RideRepository rideRepository,
            RideOfferFactory rideOfferFactory,
            ApplicationEventPublisher eventPublisher,
            RideSearchService rideSearchService,
            RideSearchCriterionFactory rideSearchCriterionFactory,
            ApproveRideWorkflow approveRideWorkflow,
            FlagRideWorkflow flagRideWorkflow,
            ResolveRideWorkflow resolveRideWorkflow,
            DeleteRideWorkflow deleteRideWorkflow) {
        this.rideRepository = rideRepository;
        this.rideOfferFactory = rideOfferFactory;
        this.eventPublisher = eventPublisher;
        this.rideSearchService = rideSearchService;
        this.rideSearchCriterionFactory = rideSearchCriterionFactory;
        this.approveRideWorkflow = approveRideWorkflow;
        this.flagRideWorkflow = flagRideWorkflow;
        this.resolveRideWorkflow = resolveRideWorkflow;
        this.deleteRideWorkflow = deleteRideWorkflow;
    }

    /**
     * GET /api/rides
     */
    @GetMapping
    public List<Ride> getAllRides() {
        return rideRepository.findAll();
    }

    /**
     * POST /api/rides — UC‑06 Offer a Ride (factory validates checkpoints + Observer logs via {@link RideOfferedEvent}).
     */
    @PostMapping
    public ResponseEntity<Ride> offerRide(@RequestBody Ride ride) {
        try {
            Ride prepared = rideOfferFactory.createPendingListing(ride);
            Ride saved = rideRepository.save(prepared);
            System.out.println("DEBUG: New Ride Saved. ID: " + saved.getId() + " | Approved: " + saved.isApproved());
            eventPublisher.publishEvent(new RideOfferedEvent(this, saved));
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/rides/pending
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
     * PUT /api/rides/{id}/approve — Admin approval (Template Method + State + Observer).
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<Ride> approveRide(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return approveRideWorkflow.approve(id, Optional.ofNullable(reason));
    }

    @GetMapping("/flagged/count")
    public long getFlaggedCount() {
        return rideRepository.countByFlaggedTrue();
    }

    @GetMapping("/flagged")
    public List<Ride> getFlaggedRides() {
        return rideRepository.findByFlaggedTrue();
    }

    /**
     * PUT /api/rides/{id}/flag
     */
    @PutMapping("/{id}/flag")
    public ResponseEntity<Ride> flagRide(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return flagRideWorkflow.flag(id, Optional.ofNullable(reason));
    }

    /**
     * PUT /api/rides/{id}/resolve
     */
    @PutMapping("/{id}/resolve")
    public ResponseEntity<Ride> resolveRide(@PathVariable Long id) {
        return resolveRideWorkflow.resolve(id);
    }

    @GetMapping("/count/active")
    public long getActiveCount() {
        return rideRepository.countByApprovedTrue();
    }

    /**
     * GET /api/rides/search?destination=… — UC‑05 Find a Ride; matching delegated to {@link com.fast.fsf.search.service.RideSearchService}
     * (Search feature — Factory criterion + Template executor + Composite tree + Observer analytics hook).
     */
    @GetMapping("/search")
    public List<Ride> searchRides(@RequestParam String destination) {
        return rideSearchService.searchApproved(
                rideSearchCriterionFactory.approvedListingDestinationContains(destination));
    }

    /**
     * DELETE /api/rides/{id} — UC‑08 Admin Deletes Carpool Listing.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRide(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return deleteRideWorkflow.execute(id, Optional.ofNullable(reason));
    }
}
