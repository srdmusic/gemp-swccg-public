package com.gempukku.swccgo.ai.models.common.trace.state;

import com.gempukku.swccgo.common.GameEndReason;

import java.util.Objects;

/**
 * TRACE STAGE 4A1 (Handoffs/CODEX_TRACE_STAGE4_4A0_MUTATOR_EVENT_MATRIX_2026-07-13.md
 * "Outer bot owner", playerLost row): one observed currentGame.playerLost(...) call
 * attempt, recorded around the actual legacy call.
 *
 * The outcome is the DISTINCT EngineCallOutcome, never MutationOutcome: SUCCESS records
 * that the call returned and does not prove an engine-state transition (the engine's
 * playerLost is internally idempotent); THREW records the caught-Exception path. The
 * required event order is PLAYER_LOST then CLEAR_PENDING, matching the source (the
 * pending clear runs after the catch; an escaping Error skips both).
 */
public record EnginePlayerLostEvent(
    String playerId,
    GameEndReason reason,
    EngineCallOutcome outcome) implements TraceStateEvent {

    public EnginePlayerLostEvent {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(outcome, "outcome");
    }
}
