package com.fast.fsf.lostfound.event;

import com.fast.fsf.lostfound.domain.LostFoundListing;
import org.springframework.context.ApplicationEvent;

/**
 * Observer pattern (GoF): domain event published by {@link com.fast.fsf.lostfound.service.LostFoundService}
 * after a new {@link LostFoundListing} is persisted (UC "Report Lost/Found Item").
 * <p>
 * Observer subscribers ({@link com.fast.fsf.lostfound.observer.LostFoundActivityLogObserver}) react
 * without the service touching {@code ActivityLogRepository} directly.
 */
public class ListingCreatedEvent extends ApplicationEvent {

    private final LostFoundListing savedListing;

    public ListingCreatedEvent(Object source, LostFoundListing savedListing) {
        super(source);
        this.savedListing = savedListing;
    }

    public LostFoundListing getSavedListing() {
        return savedListing;
    }
}
