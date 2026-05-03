package com.fast.fsf.lostfound.observer;

import com.fast.fsf.lostfound.adapter.LostFoundActivityLogRecorder;
import com.fast.fsf.lostfound.event.ListingCreatedEvent;
import com.fast.fsf.lostfound.event.ListingResolvedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observer pattern (GoF): this component is an <strong>Observer</strong> / subscriber reacting
 * to Lost &amp; Found listing lifecycle
 * {@linkplain org.springframework.context.ApplicationEvent ApplicationEvents}.
 * <p>
 * Spring's {@link EventListener} dispatches synchronously on the publishing thread by default,
 * so the audit row is inserted immediately after the transactional save — matching legacy behaviour
 * (where the service itself would have done both in sequence).
 * <p>
 * Persistence is delegated through {@link LostFoundActivityLogRecorder} — the Adapter Target —
 * keeping this observer decoupled from JPA types.
 * <p>
 * Mirrors {@code RideActivityLogObserver} from the carpool feature.
 */
@Component
public class LostFoundActivityLogObserver {

    private final LostFoundActivityLogRecorder recorder;

    public LostFoundActivityLogObserver(LostFoundActivityLogRecorder recorder) {
        this.recorder = recorder;
    }

    /**
     * Reacts when a new listing is reported (UC "Report Lost/Found Item").
     */
    @EventListener
    public void onListingCreated(ListingCreatedEvent event) {
        var listing = event.getSavedListing();
        String msg = listing.getStudentEmail()
                + " reported a " + listing.getType()
                + " item: \"" + listing.getItemName() + "\".";
        recorder.record(msg, "ITEM_REPORTED");
    }

    /**
     * Reacts when a listing is marked resolved (UC "Mark Item as Resolved").
     */
    @EventListener
    public void onListingResolved(ListingResolvedEvent event) {
        String msg = "Listing #" + event.getListingId()
                + " marked as Resolved by " + event.getResolvedBy() + ".";
        recorder.record(msg, "ITEM_RESOLVED");
    }
}
