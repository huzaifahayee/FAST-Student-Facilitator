package com.fast.fsf.lostfound.service;

import com.fast.fsf.lostfound.domain.LostFoundListing;
import com.fast.fsf.lostfound.event.ListingCreatedEvent;
import com.fast.fsf.lostfound.event.ListingResolvedEvent;
import com.fast.fsf.lostfound.factory.LostFoundListingFactory;
import com.fast.fsf.lostfound.persistence.LostFoundRepository;
import com.fast.fsf.lostfound.state.ListingLifecycleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Singleton pattern (GoF): Spring's default {@code @Service} scope is singleton — one shared
 * instance per JVM context. All collaborators ({@link LostFoundRepository},
 * {@link LostFoundListingFactory}, {@link ApplicationEventPublisher}) are injected once at
 * startup and never replaced, satisfying the Singleton intent without manual locking.
 * <p>
 * This service coordinates the following patterns for the Lost &amp; Found feature:
 * <ul>
 *   <li><strong>Factory Method</strong> — {@link LostFoundListingFactory} validates and
 *       normalises every incoming listing before persistence.</li>
 *   <li><strong>State</strong> — {@link ListingLifecycleContext} delegates the resolve
 *       transition to the concrete state (Active/Resolved) determined by current status.</li>
 *   <li><strong>Observer</strong> — {@link ApplicationEventPublisher} fires domain events after
 *       mutations so {@link com.fast.fsf.lostfound.observer.LostFoundActivityLogObserver}
 *       records audit lines without this class importing any logging repository.</li>
 * </ul>
 */
@Service
public class LostFoundService {

    @Autowired
    private LostFoundRepository repository;

    @Autowired
    private LostFoundListingFactory listingFactory;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // -------------------------------------------------------------------------
    // Queries — unchanged from original
    // -------------------------------------------------------------------------

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

    public Optional<LostFoundListing> findById(Long id) {
        return repository.findById(id);
    }

    // -------------------------------------------------------------------------
    // Commands — now use Factory Method + State + Observer
    // -------------------------------------------------------------------------

    /**
     * Creates a new Active listing.
     * <p>
     * Factory Method validates/normalises {@code listing} before save;
     * Observer records an audit line after save.
     */
    public LostFoundListing createListing(LostFoundListing listing) {
        // Factory Method: validate + normalise (sets date default + Active status)
        LostFoundListing validated = listingFactory.createActiveListing(listing);
        LostFoundListing saved = repository.save(validated);

        // Observer: publish domain event — audit logging decoupled from this service
        eventPublisher.publishEvent(new ListingCreatedEvent(this, saved));

        return saved;
    }

    /**
     * Marks a listing as Resolved.
     * <p>
     * State pattern handles the transition and the embedded auth check;
     * Observer records an audit line after save.
     */
    public LostFoundListing markAsResolved(Long id, String studentEmail) {
        LostFoundListing listing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        // State: delegate transition (and auth check) to the concrete state object
        ListingLifecycleContext ctx = new ListingLifecycleContext(listing);
        ctx.resolve(studentEmail); // throws RuntimeException("Unauthorized") if not allowed

        LostFoundListing saved = repository.save(ctx.getListing());

        // Observer: publish domain event
        eventPublisher.publishEvent(new ListingResolvedEvent(this, saved.getId(), studentEmail));

        return saved;
    }

    public void deleteListing(Long id) {
        repository.deleteById(id);
    }
}
