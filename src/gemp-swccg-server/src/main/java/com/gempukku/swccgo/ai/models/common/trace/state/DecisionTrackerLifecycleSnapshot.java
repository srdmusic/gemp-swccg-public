package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.Objects;

/**
 * TRACE STAGE 4A2a (Handoffs/CODEX_TRACE_STAGE4_4A2A_OUTER_TRACKER_LIFECYCLE_2026-07-13.md
 * "Typed model"): the complete lifecycle view of one OUTER DecisionTracker owner — the
 * existing decision-affecting DecisionTrackerSnapshot plus the exact lastTurn and
 * lastStateHash fields the outer updateState(...)/clear() lifecycle calls write.
 * Captured by the tracker's pure package-local traceLifecycleSnapshot() seam before and
 * after each legacy lifecycle call; exact before/after equality defines CHANGED vs
 * NO_OP.
 *
 * lastTurn and lastStateHash are OWNER fields (what the legacy call left in the
 * tracker), not asserted board truth. lastPhase is EXCLUDED by the accepted preflight
 * correction: only onPhaseChange(...) writes it, and that call is reachable through the
 * inherited HeuristicAiBase tracker path reserved for 4A2b, not either outer tracker
 * lifecycle owner.
 */
public record DecisionTrackerLifecycleSnapshot(
    DecisionTrackerSnapshot decisionState,
    int lastTurn,
    String lastStateHash) {

    public DecisionTrackerLifecycleSnapshot {
        Objects.requireNonNull(decisionState, "decisionState");
        Objects.requireNonNull(lastStateHash, "lastStateHash");
    }
}
