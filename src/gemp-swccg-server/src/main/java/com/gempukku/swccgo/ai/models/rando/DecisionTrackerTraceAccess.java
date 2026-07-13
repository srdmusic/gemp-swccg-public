package com.gempukku.swccgo.ai.models.rando;

import com.gempukku.swccgo.ai.models.common.trace.state.DecisionTrackerLifecycleSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.DecisionTrackerPhaseSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.DecisionTrackerSnapshot;

/**
 * TRACE STAGE 4A2b (Handoffs/CODEX_TRACE_STAGE4_4A2B_SHARED_TRACKER_PREFLIGHT_2026-07-13.md
 * "Package-boundary constraint"): the one public, READ-ONLY trace-access bridge for the
 * inherited shared DecisionTracker. HeuristicAiBase lives in
 * com.gempukku.swccgo.ai.models while the tracker and its pure trace seams are
 * package-local here, so the base class cannot call the seams directly; this bridge is
 * the smallest non-reflective crossing. Every method DELEGATES to an existing pure
 * package-local seam: no state field is exposed, nothing mutates, nothing is
 * reconstructed (decisionKey(...) in particular is the tracker's own seam value, never
 * a duplicate of its logic), and no reflection is used.
 *
 * The bridge is Rando-package-only BY DESIGN: HeuristicAiBase imports the Rando tracker
 * class, so the inherited owner is always this package's tracker for both bots. The
 * ChosenOne OUTER tracker remains separately mirrored and separately observed; it gets
 * no bridge because no cross-package caller observes it.
 *
 * DISABLED capture must never reach this class: the HeuristicAiBase hooks call it only
 * under their active-session guard.
 */
public final class DecisionTrackerTraceAccess {

    private DecisionTrackerTraceAccess() {
        // static delegation only
    }

    /** The complete decision-affecting snapshot (pure traceSnapshot() seam). */
    public static DecisionTrackerSnapshot decisionSnapshot(DecisionTracker tracker) {
        return tracker.traceSnapshot();
    }

    /** The phase-owner snapshot: decision state + exact lastPhase (pure tracePhaseSnapshot() seam). */
    public static DecisionTrackerPhaseSnapshot phaseSnapshot(DecisionTracker tracker) {
        return tracker.tracePhaseSnapshot();
    }

    /** The lifecycle snapshot: decision state + lastTurn + lastStateHash (pure traceLifecycleSnapshot() seam). */
    public static DecisionTrackerLifecycleSnapshot lifecycleSnapshot(DecisionTracker tracker) {
        return tracker.traceLifecycleSnapshot();
    }

    /** The exact decision key recordDecision(...) uses (pure traceDecisionKey() seam). */
    public static String decisionKey(DecisionTracker tracker, String decisionType, String decisionText) {
        return tracker.traceDecisionKey(decisionType, decisionText);
    }
}
