package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.Objects;

/**
 * TRACE STAGE 4B2 (Handoffs/CODEX_TRACE_STAGE4_4B2_STRATEGY_CONTROLLER_PREFLIGHT_2026-07-13.md
 * "Source-Complete Owner Table", START_TURN): the one startNewTurn(int) call in each bot's
 * turn-changed branch, observed AFTER the legacy call ran. It writes turn, phase, target,
 * and the per-turn reserve-check count, and clears seen-reserve memory only when
 * turnNumber - lastReserveCheckTurn > 2. It does not recompute forceDeficit.
 *
 * Payload: the exact int turnNumber argument. The operation-specific invariant freezes
 * side, flags, generation, deficit, focus, locations, last reserve-check turn, battle
 * counters, and decision reason; turn equals the argument; reserve checks become zero;
 * phase/target are early/8 through turn 3, mid/6 through turn 8, otherwise late/5; and the
 * seen-reserve cards clear iff turnNumber - lastReserveCheckTurn > 2.
 */
public record StrategyStartTurnEvent(
    StrategyControllerOwner owner,
    int turnNumber,
    StrategyControllerSnapshot before,
    StrategyControllerSnapshot after,
    MutationOutcome outcome) implements TraceStateEvent {

    public StrategyStartTurnEvent {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(outcome, "outcome");
        boolean changed = !before.equals(after);
        if (changed != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("outcome " + outcome
                + " inconsistent with snapshot equality (changed=" + changed + ")");
        }
        if (after.turnNumber() != turnNumber) {
            throw new IllegalArgumentException("START_TURN after.turnNumber "
                + after.turnNumber() + " must equal the argument " + turnNumber);
        }
        String expectedPhase;
        int expectedTarget;
        if (turnNumber <= 3) {
            expectedPhase = "early";
            expectedTarget = 8;
        } else if (turnNumber <= 8) {
            expectedPhase = "mid";
            expectedTarget = 6;
        } else {
            expectedPhase = "late";
            expectedTarget = 5;
        }
        if (!expectedPhase.equals(after.phase()) || after.forceGenerationTarget() != expectedTarget) {
            throw new IllegalArgumentException("START_TURN phase/target for turn " + turnNumber
                + " must be " + expectedPhase + "/" + expectedTarget + ", got "
                + after.phase() + "/" + after.forceGenerationTarget());
        }
        if (after.reserveChecksThisTurn() != 0) {
            throw new IllegalArgumentException("START_TURN resets reserveChecksThisTurn to zero, got "
                + after.reserveChecksThisTurn());
        }
        boolean cleared = turnNumber - before.lastReserveCheckTurn() > 2;
        if (cleared) {
            if (!after.cardsSeenInReserve().isEmpty()) {
                throw new IllegalArgumentException(
                    "START_TURN clears seen-reserve cards when turnNumber - lastReserveCheckTurn > 2");
            }
        } else if (!after.cardsSeenInReserve().equals(before.cardsSeenInReserve())) {
            throw new IllegalArgumentException(
                "START_TURN retains seen-reserve cards when the cooldown has not elapsed");
        }
        if (!StrategySnapshotProjections.startTurnFrozen(before)
                .equals(StrategySnapshotProjections.startTurnFrozen(after))) {
            throw new IllegalArgumentException(
                "START_TURN may not claim changes to fields it never writes (side, flags,"
                    + " generation, deficit, focus, locations, last reserve-check turn, battle"
                    + " counters, decision reason)");
        }
    }

    /** Factory used by the recording choke point: derives the outcome from the snapshots. */
    public static StrategyStartTurnEvent of(StrategyControllerOwner owner, int turnNumber,
                                            StrategyControllerSnapshot before,
                                            StrategyControllerSnapshot after) {
        boolean changed = !Objects.requireNonNull(before, "before")
            .equals(Objects.requireNonNull(after, "after"));
        return new StrategyStartTurnEvent(owner, turnNumber, before, after,
            changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }
}
