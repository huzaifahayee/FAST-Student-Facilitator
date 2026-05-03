package com.fast.fsf.lostfound.factory;

import com.fast.fsf.lostfound.domain.LostFoundListing;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Factory Method pattern (GoF): centralises construction and validation rules for
 * UC "Report Lost/Found Item".
 * <p>
 * Keeping creation rules here means {@link com.fast.fsf.lostfound.web.LostFoundController}
 * handles only HTTP concerns, while immutable business invariants (default date, forced
 * {@code Active} initial status, non-blank field guards) live in a single place that is easy
 * to extend without touching the controller or service.
 * <p>
 * Mirrors the approach of {@code RideOfferFactory} in the carpool feature.
 */
@Component
public class LostFoundListingFactory {

    private static final String DEFAULT_STATUS = "Active";

    /**
     * Factory method: validates and normalises an incoming listing before persistence.
     *
     * @param incoming the partially-filled listing from the HTTP request body.
     * @return the same instance, mutated to satisfy invariants.
     * @throws IllegalArgumentException when required fields are blank or {@code type} is invalid.
     */
    public LostFoundListing createActiveListing(LostFoundListing incoming) {
        if (incoming.getItemName() == null || incoming.getItemName().isBlank()) {
            throw new IllegalArgumentException("Item name must not be blank.");
        }
        if (incoming.getType() == null
                || (!incoming.getType().equalsIgnoreCase("Lost")
                    && !incoming.getType().equalsIgnoreCase("Found"))) {
            throw new IllegalArgumentException("Type must be 'Lost' or 'Found'.");
        }
        if (incoming.getStudentEmail() == null || incoming.getStudentEmail().isBlank()) {
            throw new IllegalArgumentException("Student email must not be blank.");
        }

        // Default date to today if caller omitted it (preserves legacy LostFoundService behaviour)
        if (incoming.getDate() == null) {
            incoming.setDate(LocalDate.now());
        }

        // Always start in Active state regardless of what the caller sends
        incoming.setStatus(DEFAULT_STATUS);

        return incoming;
    }
}
