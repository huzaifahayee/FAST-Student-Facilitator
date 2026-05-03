package com.fast.fsf.carpool.state;

/**
 * State pattern (GoF): <strong>State</strong> interface for moderation transitions on a {@link com.fast.fsf.carpool.domain.Ride}.
 * <p>
 * Concrete states encapsulate how approve / flag / resolve mutate flags while preserving legacy semantics encoded as
 * {@code approved} and {@code flagged} booleans on the entity (Phase‑1 SRS checkpoint & moderation wording).
 * <p>
 * Each concrete state is exposed as an {@code enum} constant — Java guarantees enum literals are JVM singletons,
 * which also satisfies the Singleton intent without Spring involvement.
 */
public interface RideModerationState {

    /** UC moderation: marks listing approved (still respects concurrent flagged combinations). */
    void approve(RideModerationContext ctx, String moderationReasonOrNull);

    /** Marks listing flagged for admin attention (reason may be {@code null}, mirroring legacy behaviour). */
    void flag(RideModerationContext ctx, String flagReasonOrNull);

    /** Clears moderation flag state exactly like the legacy controller (best-effort safe on every tuple). */
    void resolve(RideModerationContext ctx);
}
