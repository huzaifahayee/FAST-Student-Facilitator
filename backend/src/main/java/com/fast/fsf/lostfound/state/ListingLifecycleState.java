package com.fast.fsf.lostfound.state;

/**
 * State pattern (GoF): <strong>State</strong> interface for lifecycle transitions on a
 * {@link com.fast.fsf.lostfound.domain.LostFoundListing}.
 * <p>
 * A listing moves through two states: <em>Active</em> (freshly reported, still open) and
 * <em>Resolved</em> (owner reunited / item returned). Concrete states encapsulate which
 * transitions are legal from each state, eliminating the raw {@code if/else} auth-and-status
 * block that previously lived inside
 * {@link com.fast.fsf.lostfound.service.LostFoundService#markAsResolved}.
 * <p>
 * Mirrors the design of {@code RideModerationState} in the carpool feature.
 */
public interface ListingLifecycleState {

    /**
     * Attempt to mark the listing resolved.
     *
     * @param ctx          the mutable context wrapping the listing.
     * @param studentEmail the actor requesting the transition (used for auth check).
     * @throws RuntimeException if the transition is illegal from the current state.
     */
    void resolve(ListingLifecycleContext ctx, String studentEmail);
}
