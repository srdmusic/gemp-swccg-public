package com.gempukku.swccgo.ai.models.rando.strategy;

import com.gempukku.swccgo.ai.models.common.trace.state.StrategyControllerSnapshot;

/**
 * TRACE STAGE 4B2 (Handoffs/CODEX_TRACE_STAGE4_4B2_STRATEGY_CONTROLLER_PREFLIGHT_2026-07-13.md
 * "Snapshot Boundary"): the one public, READ-ONLY trace-access bridge for this bot's
 * StrategyController. The bot entry point (RandoCalAi) lives in the parent package while
 * the controller's pure traceSnapshot() seam is package-local here, so the bot cannot
 * call the seam directly; this bridge is the smallest non-reflective crossing. It
 * DELEGATES to the existing pure package-local seam: no state field is exposed, nothing
 * mutates, nothing is reconstructed, and no reflection is used.
 *
 * DISABLED capture must never reach this class: the bot hooks call it only under their
 * active-session guard.
 */
public final class StrategyControllerTraceAccess {

    private StrategyControllerTraceAccess() {
        // static delegation only
    }

    /** The complete retained-state snapshot (pure traceSnapshot() seam). */
    public static StrategyControllerSnapshot snapshot(StrategyController controller) {
        return controller.traceSnapshot();
    }
}
