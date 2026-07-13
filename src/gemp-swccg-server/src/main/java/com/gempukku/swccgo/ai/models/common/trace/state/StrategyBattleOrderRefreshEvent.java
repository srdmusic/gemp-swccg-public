package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.Objects;

/**
 * TRACE STAGE 4B2 (Handoffs/CODEX_TRACE_STAGE4_4B2_STRATEGY_CONTROLLER_PREFLIGHT_2026-07-13.md
 * "Source-Complete Owner Table", BATTLE_ORDER_REFRESH): the one
 * updateBattleOrderFromGameState(GameState) call in each bot, observed AFTER the legacy
 * call ran on every non-null trackGameState path. It scans permanent cards in
 * Zone.SIDE_OF_TABLE for the exact six Battle Order/Plan blueprint ids, catches Exception,
 * then assigns underBattleOrderRules; the internal setUnderBattleOrderRules(...) write
 * stays folded into this single external event.
 *
 * No GameState or service reference is ever stored: the game read is the legacy call's
 * concern, and only its retained effect is observed. The operation-specific invariant:
 * only underBattleOrderRules may differ between before and after.
 */
public record StrategyBattleOrderRefreshEvent(
    StrategyControllerOwner owner,
    StrategyControllerSnapshot before,
    StrategyControllerSnapshot after,
    MutationOutcome outcome) implements TraceStateEvent {

    public StrategyBattleOrderRefreshEvent {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(outcome, "outcome");
        boolean changed = !before.equals(after);
        if (changed != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("outcome " + outcome
                + " inconsistent with snapshot equality (changed=" + changed + ")");
        }
        if (!StrategySnapshotProjections.battleOrderFrozen(before)
                .equals(StrategySnapshotProjections.battleOrderFrozen(after))) {
            throw new IllegalArgumentException(
                "BATTLE_ORDER_REFRESH may change only underBattleOrderRules; every other field"
                    + " is frozen");
        }
    }

    /** Factory used by the recording choke point: derives the outcome from the snapshots. */
    public static StrategyBattleOrderRefreshEvent of(StrategyControllerOwner owner,
                                                     StrategyControllerSnapshot before,
                                                     StrategyControllerSnapshot after) {
        boolean changed = !Objects.requireNonNull(before, "before")
            .equals(Objects.requireNonNull(after, "after"));
        return new StrategyBattleOrderRefreshEvent(owner, before, after,
            changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }
}
