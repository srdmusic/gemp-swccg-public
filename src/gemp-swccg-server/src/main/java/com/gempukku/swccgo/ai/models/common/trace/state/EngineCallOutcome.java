package com.gempukku.swccgo.ai.models.common.trace.state;

/**
 * TRACE STAGE 4A1 (Handoffs/CODEX_TRACE_STAGE4_4A0_MUTATOR_EVENT_MATRIX_2026-07-13.md
 * "Incorporated review decisions"): the outcome of one observed ENGINE CALL attempt.
 * A distinct closed type by council constraint: it is not a value in MutationOutcome,
 * because call success does not prove that engine state changed (playerLost is
 * internally idempotent).
 *
 * SUCCESS: the call returned normally. THREW: the call threw a caught Exception (the
 * legacy catch is Exception, not Throwable; an escaping Error skips both the event and
 * the pending clear, exactly as source does).
 */
public enum EngineCallOutcome {
    SUCCESS,
    THREW
}
