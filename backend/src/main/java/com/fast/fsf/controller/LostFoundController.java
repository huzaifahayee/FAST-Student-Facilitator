package com.fast.fsf.controller;

import com.fast.fsf.model.LostFoundListing;
import com.fast.fsf.service.LostFoundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
