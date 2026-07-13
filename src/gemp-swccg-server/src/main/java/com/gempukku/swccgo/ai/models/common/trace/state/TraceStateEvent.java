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
 * PLAYER_LOST attempt, and pending-deploy SET/CLEAR. Later families (inherited
 * heuristic tracker, heuristic memory, strategy, objective, deck, shield, opponent
 * intel, deploy plan, retry budget, barrier memory) land beside their first real owner
 * hook per the matrix's corrected landing order (4A2, 4B, 4C); no complete type algebra
 * lands ahead of reachable hooks.
 *
 * Records contain immutable data only: no callback, no apply, no game/service reference,
 * no timestamp, no RNG value. List position in the envelope is the authoritative event
 * order. A hook records already-computed values after the legacy write or call; it never
 * invokes the mutator again.
 */
public sealed interface TraceStateEvent
    permits TrackerRecordResponseEvent, PendingConcedeEvent, EnginePlayerLostEvent,
            PendingDeployEvent {
}
