package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.Objects;

/**
 * TRACE STAGE 4B2 (Handoffs/CODEX_TRACE_STAGE4_4B2_STRATEGY_CONTROLLER_PREFLIGHT_2026-07-13.md
 * "Source-Complete Owner Table", BATTLE_RESULT_RECORD): the one onBattleResult(boolean)
 * operation, reached from the two separate win/loss lexical hooks in each bot's
 * trackStrategicEvents helper after exact battle-text recognition, observed AFTER the
 * legacy call ran. A win increments wins; a loss increments losses, reduces confidence by
 * 0.3f, and resets focus below 0.3f. There is no dedupe, so repeated matching text repeats
 * the mutation.
 *
 * Payload: the exact boolean won argument. The operation-specific invariant: a win
 * increments only wins; a loss increments only losses, subtracts exactly 0.3f with a zero
 * floor, and changes only the focus to balanced when the resulting confidence is below
 * 0.3f. The focus counters (turnsWithFocus, focusDeployments) are NOT reset by a battle
 * loss, unlike the direct focus mutator. Every other field is frozen.
 */
public record StrategyBattleResultRecordEvent(
    StrategyControllerOwner owner,
    boolean won,
    StrategyControllerSnapshot before,
    StrategyControllerSnapshot after,
    MutationOutcome outcome) implements TraceStateEvent {

    public StrategyBattleResultRecordEvent {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(outcome, "outcome");
        boolean changed = !before.equals(after);
        if (changed != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("outcome " + outcome
                + " inconsistent with snapshot equality (changed=" + changed + ")");
        }
        if (won) {
            if (!after.equals(StrategySnapshotProjections.withWin(before))) {
                throw new IllegalArgumentException(
                    "BATTLE_RESULT_RECORD win must increment only wins; every other field is frozen");
            }
        } else {
            if (after.battlesLost() != before.battlesLost() + 1) {
                throw new IllegalArgumentException(
                    "BATTLE_RESULT_RECORD loss must increment losses by exactly one");
            }
            float newConfidence = Math.max(0.0f,
                Float.intBitsToFloat(before.focusConfidenceBits()) - 0.3f);
            if (after.focusConfidenceBits() != Float.floatToRawIntBits(newConfidence)) {
                throw new IllegalArgumentException(
                    "BATTLE_RESULT_RECORD loss subtracts exactly 0.3f from confidence with a zero floor");
            }
            String expectedFocus = (newConfidence < 0.3f) ? "balanced" : before.focus();
            if (!expectedFocus.equals(after.focus())) {
                throw new IllegalArgumentException(
                    "BATTLE_RESULT_RECORD loss resets the focus to balanced only when the resulting"
                        + " confidence is below 0.3f, otherwise the focus is frozen");
            }
            if (!StrategySnapshotProjections.lossFrozen(before)
                    .equals(StrategySnapshotProjections.lossFrozen(after))) {
                throw new IllegalArgumentException(
                    "BATTLE_RESULT_RECORD loss may change only losses, confidence, and (conditionally)"
                        + " the focus; the focus counters and every other field are frozen");
            }
        }
    }

    /** Factory used by the recording choke point: derives the outcome from the snapshots. */
    public static StrategyBattleResultRecordEvent of(StrategyControllerOwner owner, boolean won,
                                                     StrategyControllerSnapshot before,
                                                     StrategyControllerSnapshot after) {
        boolean changed = !Objects.requireNonNull(before, "before")
            .equals(Objects.requireNonNull(after, "after"));
        return new StrategyBattleResultRecordEvent(owner, won, before, after,
            changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }
}
