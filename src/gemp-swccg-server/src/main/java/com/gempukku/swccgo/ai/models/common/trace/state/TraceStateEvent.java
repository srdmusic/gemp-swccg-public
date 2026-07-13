package com.gempukku.swccgo.ai.models.common.trace.state;

/**
 * TRACE STAGE 4A1 (2026-07-13, Handoffs/CODEX_TRACE_STAGE4_4A0_MUTATOR_EVENT_MATRIX_2026-07-13.md
 * "First Java slice after review"): one typed, completed legacy state mutation OBSERVED
 * during the one legacy run. Replaces the retired prose TraceIntendedStateEvent(Kind, detail):
 * Stage 4 observes completed legacy state operations, not intentions, and prose cannot
 * define identity. No detail string supplies owner, operation, subject, cause, or value;
 * existing logs remain the human narrative.
 *
 * SEALED BY DESIGN: each family gets its own record and closed operation enum so
 * impossible combinations cannot construct. This slice permits ONLY the concrete records
 * the existing outer bot hooks need: the outer tracker RECORD_RESPONSE (full-payload
 * per the accepted m00372 Option A contract), pending-concede SET/CLEAR, the engine
 * PLAYER_LOST attempt, and pending-deploy SET/CLEAR. TRACE STAGE 4A2a
 * (Handoffs/CODEX_TRACE_STAGE4_4A2A_OUTER_TRACKER_LIFECYCLE_2026-07-13.md) adds the two
 * outer tracker lifecycle families beside their real hooks: UPDATE_STATE and CLEAR
 * (separate records, never one nullable Cartesian lifecycle record) — more of schema
 * 3's typed union, no envelope change, no schema bump. TRACE STAGE 4A2b
 * (Handoffs/CODEX_TRACE_STAGE4_4A2B_SHARED_TRACKER_PREFLIGHT_2026-07-13.md "Authorized
 * implementation shape") adds the two inherited shared-tracker families beside their
 * real HeuristicAiBase hooks: PHASE_CHANGE and BLOCK_RESPONSE (owner fixed to
 * HEURISTIC_SHARED); the shared UPDATE_STATE and RECORD_RESPONSE calls REUSE the
 * accepted records, whose owner invariant is intentionally expanded to accept
 * HEURISTIC_SHARED; still schema 3, no envelope change. TRACE STAGE 4B1
 * (Handoffs/CODEX_TRACE_STAGE4_4B1_HEURISTIC_MEMORY_PREFLIGHT_2026-07-13.md
 * "Source-Complete Owner Table") adds the six closed heuristic-memory families beside
 * their real HeuristicAiBase owner hooks: STATE_UPDATE, ACTION_CHOICE_REMEMBER,
 * FAILED_SEARCH_ADD, SINGLE_RESPONSE_RECORD (the internal local-block mutation stays
 * folded in), RECENT_RESPONSE_APPEND, and REASSIGNMENT_RECORD (the count increment
 * stays folded in); no seventh local-block or nested-count family exists, still
 * schema 3, no envelope change. TRACE STAGE 4B2
 * (Handoffs/CODEX_TRACE_STAGE4_4B2_STRATEGY_CONTROLLER_PREFLIGHT_2026-07-13.md
 * "Source-Complete Owner Table") adds the six closed StrategyController families beside
 * their real per-bot owner hooks: SIDE_SET, RESET, and START_TURN (lifecycle);
 * FOCUS_DEPLOY_RECORD, BATTLE_ORDER_REFRESH (the internal setUnderBattleOrderRules
 * write stays folded in), and BATTLE_RESULT_RECORD (its two win/loss lexical hooks
 * share one operation kind); StrategyControllerOwner is closed to RANDO and CHOSENONE,
 * still schema 3, no envelope change. Later families (objective, deck, shield,
 * opponent intel, deploy plan, retry budget, barrier memory) land beside their first
 * real owner hook per the matrix's corrected landing order (4B, 4C); no complete type
 * algebra lands ahead of reachable hooks.
 *
 * Records contain immutable data only: no callback, no apply, no game/service reference,
 * no timestamp, no RNG value. List position in the envelope is the authoritative event
 * order. A hook records already-computed values after the legacy write or call; it never
 * invokes the mutator again.
 */
public sealed interface TraceStateEvent
    permits TrackerRecordResponseEvent, PendingConcedeEvent, EnginePlayerLostEvent,
            PendingDeployEvent, TrackerUpdateStateEvent, TrackerClearEvent,
            TrackerPhaseChangeEvent, TrackerBlockResponseEvent,
            HeuristicStateUpdateEvent, HeuristicActionChoiceRememberEvent,
            HeuristicFailedSearchAddEvent, HeuristicSingleResponseRecordEvent,
            HeuristicRecentResponseAppendEvent, HeuristicReassignmentRecordEvent,
            StrategySideSetEvent, StrategyResetEvent, StrategyStartTurnEvent,
            StrategyFocusDeployRecordEvent, StrategyBattleOrderRefreshEvent,
            StrategyBattleResultRecordEvent {
}
