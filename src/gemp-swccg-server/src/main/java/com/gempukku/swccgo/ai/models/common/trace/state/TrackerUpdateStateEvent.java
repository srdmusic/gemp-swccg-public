package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.Objects;

/**
 * TRACE STAGE 4A2a (Handoffs/CODEX_TRACE_STAGE4_4A2A_OUTER_TRACKER_LIFECYCLE_2026-07-13.md
 * "Typed model"): the one outer decision-tracker updateState(...) lifecycle call in each
 * bot's updateDecisionTrackerState(...) helper, observed AFTER the legacy call ran.
 * Exactly one event per legacy call when a session is active.
 *
 * Payload: the EXACT legacy call arguments (handSize, forcePile, reserveDeck, turn,
 * cardsInPlay — the helper's zero defaults after a caught game-state getter exception
 * are recorded verbatim as legacy call arguments, never as authoritative board
 * observations) plus the complete lifecycle before/after snapshots. The outcome is
 * derived from and constructor-validated against exact snapshot equality: CHANGED iff
 * before != after.
 *
 * Owner is one of the two OUTER trackers only; the inherited HeuristicAiBase tracker's
 * updateState call is a distinct owner reserved for 4A2b.
 */
public record TrackerUpdateStateEvent(
    TrackerOwner owner,
    int handSize,
    int forcePile,
    int reserveDeck,
    int turn,
    int cardsInPlay,
    DecisionTrackerLifecycleSnapshot before,
    DecisionTrackerLifecycleSnapshot after,
    MutationOutcome outcome) implements TraceStateEvent {

    public TrackerUpdateStateEvent {
        Objects.requireNonNull(owner, "owner");
        if (owner == TrackerOwner.HEURISTIC_SHARED) {
            throw new IllegalArgumentException(
                "UPDATE_STATE observes the OUTER trackers only; the inherited heuristic"
                    + " tracker is a distinct owner (4A2b)");
        }
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(outcome, "outcome");
        boolean changed = !before.equals(after);
        if (changed != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("outcome " + outcome
                + " inconsistent with snapshot equality (changed=" + changed + ")");
        }
    }

    /** Factory used by the recording choke point: derives the outcome from the snapshots. */
    public static TrackerUpdateStateEvent of(TrackerOwner owner, int handSize, int forcePile,
                                             int reserveDeck, int turn, int cardsInPlay,
                                             DecisionTrackerLifecycleSnapshot before,
                                             DecisionTrackerLifecycleSnapshot after) {
        boolean changed = !Objects.requireNonNull(before, "before")
            .equals(Objects.requireNonNull(after, "after"));
        return new TrackerUpdateStateEvent(owner, handSize, forcePile, reserveDeck, turn,
            cardsInPlay, before, after,
            changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }
}
