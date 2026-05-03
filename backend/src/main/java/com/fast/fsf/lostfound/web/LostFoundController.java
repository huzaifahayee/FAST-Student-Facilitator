package com.fast.fsf.lostfound.web;

import com.fast.fsf.lostfound.domain.LostFoundListing;
import com.fast.fsf.lostfound.service.LostFoundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Singleton pattern (GoF): Spring's default {@code @RestController} scope is singleton — one shared
 * instance per JVM context, satisfying the Singleton intent without manual locking.
 * <p>
 * This controller delegates all business logic to {@link com.fast.fsf.lostfound.service.LostFoundService},
 * keeping HTTP concerns (request mapping, response codes, CORS) strictly separated from domain logic.
 */
@RestController
@RequestMapping("/api/lost-found")
@CrossOrigin(origins = "*")
public class LostFoundController {


    @Autowired
    private LostFoundService service;

    @GetMapping
    public List<LostFoundListing> getListings(
            @RequestParam String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return service.getListings(type, category, keyword);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LostFoundListing> getListing(@PathVariable Long id) {
        Optional<LostFoundListing> listing = service.findById(id);
        return listing.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<LostFoundListing> createListing(@RequestBody LostFoundListing listing) {
        return ResponseEntity.ok(service.createListing(listing));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<LostFoundListing> resolveListing(
            @PathVariable Long id,
            @RequestParam String studentEmail) {
        return ResponseEntity.ok(service.markAsResolved(id, studentEmail));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id) {
        service.deleteListing(id);
        return ResponseEntity.noContent().build();
    }
}
