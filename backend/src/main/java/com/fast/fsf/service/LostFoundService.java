package com.fast.fsf.service;

import com.fast.fsf.model.LostFoundListing;
import com.fast.fsf.repository.LostFoundRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class LostFoundService {

    @Autowired
    private LostFoundRepository repository;

    public List<LostFoundListing> getListings(String type, String category, String keyword) {
        if (keyword != null && !keyword.isEmpty() && category != null && !category.isEmpty()) {
            return repository.searchByTypeAndCategoryAndKeyword(type, category, keyword);
        } else if (keyword != null && !keyword.isEmpty()) {
            return repository.searchByTypeAndKeyword(type, keyword);
        } else if (category != null && !category.isEmpty()) {
            return repository.findByTypeAndCategoryOrderByDateDesc(type, category);
        } else {
            return repository.findByTypeOrderByDateDesc(type);
        }
    }

    public LostFoundListing createListing(LostFoundListing listing) {
        if (listing.getDate() == null) {
            listing.setDate(LocalDate.now());
        }
        listing.setStatus("Active");
        return repository.save(listing);
    }

    public LostFoundListing markAsResolved(Long id, String studentEmail) {
        LostFoundListing listing = repository.findById(id).orElseThrow(() -> new RuntimeException("Listing not found"));
        // Simulating Auth check
        if (!listing.getStudentEmail().equals(studentEmail) && !studentEmail.contains("admin")) {
            throw new RuntimeException("Unauthorized");
        }
        listing.setStatus("Resolved");
        return repository.save(listing);
    }

    public void deleteListing(Long id) {
        repository.deleteById(id);
    }
}
