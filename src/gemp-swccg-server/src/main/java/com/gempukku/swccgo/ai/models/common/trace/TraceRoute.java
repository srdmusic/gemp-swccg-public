package com.gempukku.swccgo.ai.models.common.trace;

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
    /** No evaluator handled the decision: HeuristicAiBase.decide ran. */
    HEURISTIC_FALLBACK,
    /** Outer emergency: result null (or empty with raw noPass=true), emergency response used. */
    RAW_NOPASS_EMERGENCY
}
