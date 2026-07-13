package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.Objects;

/**
 * TRACE STAGE 4A2b (Handoffs/CODEX_TRACE_STAGE4_4A2B_SHARED_TRACKER_PREFLIGHT_2026-07-13.md
 * "Authorized implementation shape"): the phase-owner view of the inherited SHARED
 * DecisionTracker: the complete decision-affecting DecisionTrackerSnapshot plus the
 * exact lastPhase field the legacy onPhaseChange(...) call reads and writes. Captured
 * by the tracker's pure package-local tracePhaseSnapshot() seam (through the public
 * read-only DecisionTrackerTraceAccess bridge) before and after each legacy
 * onPhaseChange(...) call; exact before/after equality, INCLUDING lastPhase, defines
 * CHANGED vs NO_OP per the packet's PHASE_CHANGE payload ownership.
 *
 * lastPhase is an OWNER field (what the legacy call left in the tracker), not asserted
 * board truth. lastTurn and lastStateHash are EXCLUDED here by the same ownership rule
 * that excluded lastPhase from DecisionTrackerLifecycleSnapshot: they are
 * updateState-owned and carry no onPhaseChange claim.
 */
public record DecisionTrackerPhaseSnapshot(
    DecisionTrackerSnapshot decisionState,
    String lastPhase) {

    public DecisionTrackerPhaseSnapshot {
        Objects.requireNonNull(decisionState, "decisionState");
        Objects.requireNonNull(lastPhase, "lastPhase");
    }
}
