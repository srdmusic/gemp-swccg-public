package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.Objects;

/**
 * TRACE STAGE 4A2a (Handoffs/CODEX_TRACE_STAGE4_4A2A_OUTER_TRACKER_LIFECYCLE_2026-07-13.md
 * "Typed model"): the one outer decision-tracker clear() lifecycle call in each bot's
 * trackGameState(...) new-game block, observed AFTER the legacy call ran at its
 * unchanged source position (pending-concede clear, outer new-game writes, chat reset,
 * THIS tracker clear, seen-set clears, then strategy-component resets). Exactly one
 * event per legacy call when a session is active.
 *
 * ClearCause is CLOSED to NEW_GAME_RESET: the new-game reset is the only reachable
 * outer clear() call site. The clear runs unconditionally on the new-game branch, so a
 * NO_OP (fresh tracker, nothing to erase) is a real observation. The outcome is derived
 * from and constructor-validated against exact lifecycle-snapshot equality.
 *
 * Owner is one of the two OUTER trackers only; the inherited HeuristicAiBase tracker is
 * a distinct owner reserved for 4A2b.
 */
public record TrackerClearEvent(
    TrackerOwner owner,
    ClearCause cause,
    DecisionTrackerLifecycleSnapshot before,
    DecisionTrackerLifecycleSnapshot after,
    MutationOutcome outcome) implements TraceStateEvent {

    /** Closed cause set: the only reachable outer clear() is the new-game reset. */
    public enum ClearCause {
        NEW_GAME_RESET
    }

    public TrackerClearEvent {
        Objects.requireNonNull(owner, "owner");
        if (owner == TrackerOwner.HEURISTIC_SHARED) {
            throw new IllegalArgumentException(
                "CLEAR observes the OUTER trackers only; the inherited heuristic"
                    + " tracker is a distinct owner (4A2b)");
        }
        Objects.requireNonNull(cause, "cause");
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
    public static TrackerClearEvent of(TrackerOwner owner, ClearCause cause,
                                       DecisionTrackerLifecycleSnapshot before,
                                       DecisionTrackerLifecycleSnapshot after) {
        boolean changed = !Objects.requireNonNull(before, "before")
            .equals(Objects.requireNonNull(after, "after"));
        return new TrackerClearEvent(owner, cause, before, after,
            changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }
}
