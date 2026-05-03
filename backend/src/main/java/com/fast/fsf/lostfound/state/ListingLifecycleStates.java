package com.fast.fsf.lostfound.state;

import com.fast.fsf.lostfound.domain.LostFoundListing;

/**
 * State pattern (GoF): factory helper + concrete state implementations for
 * {@link ListingLifecycleState}.
 * <p>
 * Two legal states mirror the {@code status} strings persisted in the database:
 * <ul>
 *   <li><strong>Active</strong>  — listing is open; {@code resolve()} applies the auth check and
 *       flips status to "Resolved", preserving the exact same business rules previously inline in
 *       {@link com.fast.fsf.lostfound.service.LostFoundService#markAsResolved}.</li>
 *   <li><strong>Resolved</strong> — listing is closed; re-resolving is a no-op (legacy controller
 *       did not forbid it, so we silently absorb the call for regression safety).</li>
 * </ul>
 * Enum constants are JVM singletons, which additionally satisfies the Singleton intent for
 * these flyweight state objects (no Spring involvement needed).
 * <p>
 * Mirrors {@code RideModerationStates} from the carpool feature.
 */
public final class ListingLifecycleStates {

    private ListingLifecycleStates() {}

    /** Selects the concrete state from the persisted status string. */
    public static ListingLifecycleState fromListing(LostFoundListing listing) {
        if ("Resolved".equalsIgnoreCase(listing.getStatus())) {
            return ResolvedState.INSTANCE;
        }
        return ActiveState.INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Concrete state: Active
    // -------------------------------------------------------------------------

    /**
     * Open listing — the only legal action is {@code resolve()}.
     * Applies the same owner/admin auth check previously inline in LostFoundService.
     */
    private enum ActiveState implements ListingLifecycleState {
        INSTANCE;

        @Override
        public void resolve(ListingLifecycleContext ctx, String studentEmail) {
            LostFoundListing listing = ctx.getListing();

            // Preserve exact auth logic from original LostFoundService.markAsResolved
            if (!listing.getStudentEmail().equals(studentEmail)
                    && !studentEmail.contains("admin")) {
                throw new RuntimeException("Unauthorized");
            }

            listing.setStatus("Resolved");
        }
    }

    // -------------------------------------------------------------------------
    // Concrete state: Resolved
    // -------------------------------------------------------------------------

    /**
     * Already-resolved listing — re-resolving is a harmless no-op (legacy HTTP contract
     * did not return 409, so this mirrors that behaviour for regression safety).
     */
    private enum ResolvedState implements ListingLifecycleState {
        INSTANCE;

        @Override
        public void resolve(ListingLifecycleContext ctx, String studentEmail) {
            /* Already resolved — no-op, mirrors legacy behaviour. */
        }
    }
}
