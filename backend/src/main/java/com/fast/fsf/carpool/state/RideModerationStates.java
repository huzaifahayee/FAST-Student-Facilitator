package com.fast.fsf.carpool.state;

import com.fast.fsf.carpool.domain.Ride;

/**
 * Factory helper for {@link RideModerationState}: selects the proper state object from the persisted booleans.
 * <p>
 * The four tuples mirror every legacy combination reachable via {@code RideController}.
 */
public final class RideModerationStates {

    private RideModerationStates() {}

    public static RideModerationState fromRide(Ride ride) {
        if (!ride.isApproved() && !ride.isFlagged()) {
            return PendingOpen.INSTANCE;
        }
        if (!ride.isApproved() && ride.isFlagged()) {
            return PendingFlagged.INSTANCE;
        }
        if (ride.isApproved() && !ride.isFlagged()) {
            return ApprovedOpen.INSTANCE;
        }
        return ApprovedFlagged.INSTANCE;
    }

    /**
     * Pending submission — neither approved nor flagged yet (typical freshly-offered UC-06 listing).
     */
    private enum PendingOpen implements RideModerationState {
        INSTANCE;

        @Override
        public void approve(RideModerationContext ctx, String moderationReasonOrNull) {
            Ride r = ctx.getRide();
            r.setApproved(true);
            r.setModerationReason(moderationReasonOrNull);
        }

        @Override
        public void flag(RideModerationContext ctx, String flagReasonOrNull) {
            Ride r = ctx.getRide();
            r.setFlagged(true);
            r.setModerationReason(flagReasonOrNull);
        }

        @Override
        public void resolve(RideModerationContext ctx) {
            /* Legacy controller allowed resolve even when nothing flagged — harmless no-op here. */
        }
    }

    /** Pending approval but already flagged by moderation review. */
    private enum PendingFlagged implements RideModerationState {
        INSTANCE;

        @Override
        public void approve(RideModerationContext ctx, String moderationReasonOrNull) {
            Ride r = ctx.getRide();
            r.setApproved(true);
            r.setModerationReason(moderationReasonOrNull);
        }

        @Override
        public void flag(RideModerationContext ctx, String flagReasonOrNull) {
            ctx.getRide().setModerationReason(flagReasonOrNull);
        }

        @Override
        public void resolve(RideModerationContext ctx) {
            Ride r = ctx.getRide();
            r.setFlagged(false);
            r.setModerationReason(null);
        }
    }

    /** Live listing approved and currently clean (visible search candidate when combined with SRS rules). */
    private enum ApprovedOpen implements RideModerationState {
        INSTANCE;

        @Override
        public void approve(RideModerationContext ctx, String moderationReasonOrNull) {
            ctx.getRide().setModerationReason(moderationReasonOrNull);
        }

        @Override
        public void flag(RideModerationContext ctx, String flagReasonOrNull) {
            Ride r = ctx.getRide();
            r.setFlagged(true);
            r.setModerationReason(flagReasonOrNull);
        }

        @Override
        public void resolve(RideModerationContext ctx) {
            /* Nothing to resolve — mirrors legacy behaviour (still saves identical entity snapshot). */
        }
    }

    /** Approved listing currently carrying an active moderation flag. */
    private enum ApprovedFlagged implements RideModerationState {
        INSTANCE;

        @Override
        public void approve(RideModerationContext ctx, String moderationReasonOrNull) {
            Ride r = ctx.getRide();
            r.setApproved(true);
            r.setModerationReason(moderationReasonOrNull);
        }

        @Override
        public void flag(RideModerationContext ctx, String flagReasonOrNull) {
            ctx.getRide().setModerationReason(flagReasonOrNull);
        }

        @Override
        public void resolve(RideModerationContext ctx) {
            Ride r = ctx.getRide();
            r.setFlagged(false);
            r.setModerationReason(null);
        }
    }
}
