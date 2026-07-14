package com.gempukku.swccgo.ai.models.common.trace;

import com.gempukku.swccgo.ai.models.common.decision.DecisionFacts;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
 * "Route record"): the typed runtime route ids for one legacy decision execution.
 *
 * Constants mirror the ACTUAL runtime route map
 * (Handoffs/CODEX_RANDO_RUNTIME_ROUTE_MAP_2026-07-13.md stages 3-14): the five direct
 * interceptors, the chaos bypass, the normal CombinedEvaluator lane, the heuristic
 * fallback, and the raw-noPass emergency. OBSERVATION ONLY: recording a route never
 * redirects control flow (route-map "Cutover order" step 1 binds absolutely).
 * Phase/window subroutes are ADDED here as they are introduced, never reused with
 * changed semantics.
 */
public enum TraceRoute {
    /** V45: optional forfeit ("forfeit" + "if desired") direct interceptor. */
    V45_OPTIONAL_FORFEIT,
    /** V44/V67j: revert-approval direct interceptor. */
    V44_V67J_REVERT_APPROVAL,
    /** V170: undercover-spy choice direct interceptor. */
    V170_UNDERCOVER_CHOICE,
    /** V61: "The Force Is Strong In My Family" saga-choice direct interceptor. */
    V61_SAGA_CHOICE,
    /** V79b: Verge of Greatness parsec-choice direct interceptor. RANDO-ONLY:
     *  TheChosenOneAi lacks this branch (declared personality route per the
     *  V2 contract's minimum gate corpus). */
    V79B_PARSEC_CHOICE,
    /** Chaos gate passed outside deploy/battle: heuristic base bypasses evaluators. */
    CHAOS_FALLBACK,
    /** Normal lane: CombinedEvaluator handled the decision. */
    COMBINED_EVALUATOR,
    /** Canonical top-level DRAW decision owned by the typed DRAW finalizer lane. */
    DRAW_TOP_LEVEL,
    /** Typed top-level ACTIVATE action chooser. */
    ACTIVATE_TOP_LEVEL,
    /** Typed Force-activation amount. */
    ACTIVATE_AMOUNT,
    /** Typed opponent activation allowance amount. */
    ACTIVATE_ALLOWANCE,
    /** Typed zero-activation Yes/No confirmation. */
    ACTIVATE_ZERO_CONFIRM,
    /** Typed activation-interruption acknowledgement. */
    ACTIVATE_ACK,
    /** Typed top-level CONTROL action chooser. */
    CONTROL_TOP_LEVEL,
    /** Typed PULL parent action choice. */
    PULL_PARENT,
    /** Typed deploy-from-pile child card choice. */
    PULL_DEPLOY_CHILD,
    /** Typed take-into-hand child card choice. */
    PULL_TAKE_CHILD,
    /** Typed pulled-card deployment destination choice. */
    PULL_DESTINATION,
    /** Typed empty verification response after a failed standard search. */
    PULL_FAILED_VERIFY,
    /** No evaluator handled the decision: HeuristicAiBase.decide ran. */
    HEURISTIC_FALLBACK,
    /** Outer emergency: result null (or empty with raw noPass=true), emergency response used. */
    RAW_NOPASS_EMERGENCY;

    /**
     * TRACE-V2 GATE P1-5 (Handoffs/CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md "route
     * authority is duplicated and not validated"): frozen-fact cross-validation between
     * the runtime-selected route and the snapshot's frozen decision shape
     * (DecisionFacts.selectedRoute). TraceCollector.finish() records a typed ROUTE
     * failure when they disagree, so the envelope preserves the disagreement as
     * evidence instead of throwing it away.
     *
     * POLICY-FREE EVIDENCE ONLY (route-map amendment, CODEX_RANDO_RUNTIME_ROUTE_MAP
     * 2026-07-13 §3 "Semantic subroute"): the constraint here is the WIRE DECISION
     * SHAPE, nothing else. Phase is only an allowed-route window, never a route key —
     * no phase appears in this validation, and no score/assessment ever could. The
     * four MULTIPLE_CHOICE-guarded direct interceptors are shape-bound because their
     * own legacy guards test the wire shape; every other route (including V45, whose
     * legacy guard is text-only, and the cross-phase-capable lanes) is unconstrained.
     */
    public boolean isCompatibleWithFrozenShape(DecisionFacts.DecisionRoute frozenShape) {
        switch (this) {
            case V44_V67J_REVERT_APPROVAL:
            case V170_UNDERCOVER_CHOICE:
            case V61_SAGA_CHOICE:
            case V79B_PARSEC_CHOICE:
                return frozenShape == DecisionFacts.DecisionRoute.MULTIPLE_CHOICE;
            case DRAW_TOP_LEVEL:
            case PULL_PARENT:
            case ACTIVATE_TOP_LEVEL:
            case CONTROL_TOP_LEVEL:
                return frozenShape == DecisionFacts.DecisionRoute.CARD_ACTION_CHOICE;
            case ACTIVATE_AMOUNT:
            case ACTIVATE_ALLOWANCE:
                return frozenShape == DecisionFacts.DecisionRoute.INTEGER;
            case ACTIVATE_ZERO_CONFIRM:
            case ACTIVATE_ACK:
                return frozenShape == DecisionFacts.DecisionRoute.MULTIPLE_CHOICE;
            case PULL_DEPLOY_CHILD:
            case PULL_TAKE_CHILD:
            case PULL_FAILED_VERIFY:
                return frozenShape == DecisionFacts.DecisionRoute.ARBITRARY_CARDS;
            case PULL_DESTINATION:
                return frozenShape == DecisionFacts.DecisionRoute.CARD_SELECTION;
            default:
                return true;
        }
    }
}
