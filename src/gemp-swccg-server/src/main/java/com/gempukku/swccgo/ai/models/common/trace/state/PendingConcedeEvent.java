package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.Objects;

/**
 * TRACE STAGE 4A1 (Handoffs/CODEX_TRACE_STAGE4_4A0_MUTATOR_EVENT_MATRIX_2026-07-13.md
 * "Outer bot owner", SET_PENDING/CLEAR_PENDING rows): one completed mutation of the
 * outer bot's retained pendingConcede/pendingConcedeReason pair.
 *
 * SET_PENDING (lost-pile threshold write): closed cause LOST_PILE_DEFICIT with the
 * lost-pile inputs, and an after value (SET requires an after value).
 * CLEAR_PENDING (new-game reset or the post-fire clear after playerLost returns or
 * throws a caught Exception): closed clear cause, absent after value (CLEAR requires an
 * absent after value). The new-game clear runs unconditionally, so a NO_OP CLEAR is a
 * real observation: the owner write ran and nothing was pending.
 *
 * There is no FIRE_PENDING state mutation; the engine-call attempt is its own
 * EnginePlayerLostEvent and the required source order is PLAYER_LOST then CLEAR_PENDING.
 */
public record PendingConcedeEvent(
    Operation operation,
    Cause cause,
    String playerId,
    Integer myLostPileSize,
    Integer opponentLostPileSize,
    Integer lostPileDeficit,
    boolean pendingBefore,
    String reasonBefore,
    boolean pendingAfter,
    String reasonAfter,
    MutationOutcome outcome) implements TraceStateEvent {

    /** The two reachable owner operations. */
    public enum Operation {
        SET_PENDING,
        CLEAR_PENDING
    }

    /** Closed causes; each is legal for exactly one operation side. There is no
     *  FIRE_PENDING operation (matrix ruling), so the post-call clear cause is named
     *  after the playerLost CALL it follows, never after a fire operation. */
    public enum Cause {
        LOST_PILE_DEFICIT,   // SET_PENDING only (the V25/V67aw threshold write)
        NEW_GAME_RESET,      // CLEAR_PENDING only (trackGameState new-game reset)
        POST_PLAYER_LOST     // CLEAR_PENDING only (after the playerLost call returns or throws)
    }

    public PendingConcedeEvent {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(cause, "cause");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(outcome, "outcome");
        if (operation == Operation.SET_PENDING) {
            if (cause != Cause.LOST_PILE_DEFICIT) {
                throw new IllegalArgumentException("SET_PENDING requires cause LOST_PILE_DEFICIT, was " + cause);
            }
            if (myLostPileSize == null || opponentLostPileSize == null || lostPileDeficit == null) {
                throw new IllegalArgumentException("SET_PENDING requires the lost-pile inputs");
            }
            if (!pendingAfter) {
                throw new IllegalArgumentException("SET_PENDING requires pendingAfter=true (SET requires an after value)");
            }
            if (reasonAfter == null) {
                throw new IllegalArgumentException("SET_PENDING requires a non-null reasonAfter (SET requires an after value)");
            }
        } else {
            if (cause == Cause.LOST_PILE_DEFICIT) {
                throw new IllegalArgumentException("CLEAR_PENDING requires a clear cause, was " + cause);
            }
            if (myLostPileSize != null || opponentLostPileSize != null || lostPileDeficit != null) {
                throw new IllegalArgumentException("CLEAR_PENDING carries no lost-pile inputs");
            }
            if (pendingAfter) {
                throw new IllegalArgumentException("CLEAR_PENDING requires pendingAfter=false (CLEAR requires an absent after value)");
            }
            if (reasonAfter != null) {
                throw new IllegalArgumentException("CLEAR_PENDING requires a null reasonAfter (CLEAR requires an absent after value)");
            }
        }
        boolean changed = (pendingBefore != pendingAfter) || !Objects.equals(reasonBefore, reasonAfter);
        if (changed != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("outcome " + outcome
                + " inconsistent with before/after (changed=" + changed + ")");
        }
    }

    /** Factory used by the recording choke point: derives the outcome from before/after. */
    public static PendingConcedeEvent of(Operation operation, Cause cause, String playerId,
                                         Integer myLostPileSize, Integer opponentLostPileSize,
                                         Integer lostPileDeficit,
                                         boolean pendingBefore, String reasonBefore,
                                         boolean pendingAfter, String reasonAfter) {
        boolean changed = (pendingBefore != pendingAfter) || !Objects.equals(reasonBefore, reasonAfter);
        return new PendingConcedeEvent(operation, cause, playerId,
            myLostPileSize, opponentLostPileSize, lostPileDeficit,
            pendingBefore, reasonBefore, pendingAfter, reasonAfter,
            changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }
}
