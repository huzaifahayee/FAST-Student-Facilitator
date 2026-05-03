package com.fast.fsf.lostfound.event;

import org.springframework.context.ApplicationEvent;

/**
 * Observer pattern (GoF): domain event published by {@link com.fast.fsf.lostfound.service.LostFoundService}
 * after a {@link com.fast.fsf.lostfound.domain.LostFoundListing} is transitioned to "Resolved"
 * (UC "Mark Item as Resolved").
 * <p>
 * Keeps moderation audit-logging fully decoupled from the service's core State-pattern transition logic.
 */
public class ListingResolvedEvent extends ApplicationEvent {

    private final Long listingId;
    private final String resolvedBy;

    public ListingResolvedEvent(Object source, Long listingId, String resolvedBy) {
        super(source);
        this.listingId = listingId;
        this.resolvedBy = resolvedBy;
    }

    public Long getListingId() {
        return listingId;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }
}
