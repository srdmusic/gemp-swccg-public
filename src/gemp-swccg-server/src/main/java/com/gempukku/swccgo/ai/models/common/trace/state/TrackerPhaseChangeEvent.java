package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.Objects;

/**
 * TRACE STAGE 4A2b (Handoffs/CODEX_TRACE_STAGE4_4A2B_SHARED_TRACKER_PREFLIGHT_2026-07-13.md
 * "Payload ownership", PHASE_CHANGE): the one inherited shared-tracker
 * onPhaseChange(...) call at the HeuristicAiBase.decide(...) boundary, observed AFTER
 * the legacy call ran. Exactly one event per direct legacy call when a session is
 * active; no event when phase == null (the call did not run).
 *
 * Payload: the EXACT phase string passed to the call plus the complete phase-owner
 * before/after snapshots (decision-affecting state + exact lastPhase). The outcome is
 * derived from and constructor-validated against exact snapshot equality, INCLUDING
 * lastPhase: a repeated phase leaves the owner untouched and is a real NO_OP; a changed
 * phase writes lastPhase and resets sequence/repeat state, so it is CHANGED. In BOTH
 * legitimate cases the legacy call leaves lastPhase equal to its argument, so the
 * constructor REJECTS after.lastPhase() != phase: a typed event may never claim a
 * call argument its own owner snapshot disproves (reviewer m00441).
 *
 * OWNER IS FIXED to HEURISTIC_SHARED: the only reachable onPhaseChange(...) call in
 * production is the inherited HeuristicAiBase tracker path; neither outer bot calls it
 * on its outer tracker. A future outer caller must widen this deliberately, not
 * silently.
 */
public record TrackerPhaseChangeEvent(
    TrackerOwner owner,
    String phase,
    DecisionTrackerPhaseSnapshot before,
    DecisionTrackerPhaseSnapshot after,
    MutationOutcome outcome) implements TraceStateEvent {

    public TrackerPhaseChangeEvent {
        Objects.requireNonNull(owner, "owner");
        if (owner != TrackerOwner.HEURISTIC_SHARED) {
            throw new IllegalArgumentException(
                "PHASE_CHANGE observes the inherited HeuristicAiBase tracker only; no outer"
                    + " tracker has a reachable onPhaseChange(...) call (owner was " + owner + ")");
        }
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(outcome, "outcome");
        if (!phase.equals(after.lastPhase())) {
            throw new IllegalArgumentException("after.lastPhase '" + after.lastPhase()
                + "' contradicts the phase argument '" + phase
                + "': the legacy call always leaves lastPhase equal to its argument");
        }
        boolean changed = !before.equals(after);
        if (changed != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("outcome " + outcome
                + " inconsistent with snapshot equality (changed=" + changed + ")");
        }
    }

    /** Factory used by the recording choke point: derives the outcome from the snapshots. */
    public static TrackerPhaseChangeEvent of(TrackerOwner owner, String phase,
                                             DecisionTrackerPhaseSnapshot before,
                                             DecisionTrackerPhaseSnapshot after) {
        boolean changed = !Objects.requireNonNull(before, "before")
            .equals(Objects.requireNonNull(after, "after"));
        return new TrackerPhaseChangeEvent(owner, phase, before, after,
            changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }
}
