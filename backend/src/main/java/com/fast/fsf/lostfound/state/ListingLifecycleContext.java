package com.fast.fsf.lostfound.state;

import com.fast.fsf.lostfound.domain.LostFoundListing;

/**
 * State pattern (GoF): lightweight <strong>Context</strong> object holding the mutable
 * {@link LostFoundListing} aggregate while delegating lifecycle transitions to whichever
 * {@link ListingLifecycleState} matches the listing's current {@code status} string.
 * <p>
 * Mirrors {@code RideModerationContext} from the carpool feature.
 */
public final class ListingLifecycleContext {

    private final LostFoundListing listing;

    public ListingLifecycleContext(LostFoundListing listing) {
        this.listing = listing;
    }

    public LostFoundListing getListing() {
        return listing;
    }

    /**
     * Delegates the resolve transition to the concrete state determined by current status.
     *
     * @param studentEmail the actor requesting resolution (for auth enforcement inside the state).
     */
    public void resolve(String studentEmail) {
        ListingLifecycleStates.fromListing(listing).resolve(this, studentEmail);
    }
}
