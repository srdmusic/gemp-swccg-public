package com.gempukku.swccgo.ai.models.common.trace.state;

import com.gempukku.swccgo.common.Side;

import java.util.List;

/**
 * TRACE STAGE 4B2 (Handoffs/CODEX_TRACE_STAGE4_4B2_STRATEGY_CONTROLLER_PREFLIGHT_2026-07-13.md
 * "Operation-Specific Invariants"): package-private projection builders shared by the six
 * StrategyController event records. Each returns an immutable StrategyControllerSnapshot
 * with the fields a given operation MAY legally write neutralized to a fixed constant, so
 * two projected snapshots compare equal exactly when the operation left every other field
 * frozen. Pure and side-effect free; construct nothing but new snapshots.
 *
 * Snapshot inequality alone is insufficient to accept an event: the frozen-remainder
 * projections here are what reject an unrelated snapshot delta.
 */
final class StrategySnapshotProjections {

    /** The neutral confidence used only inside frozen-remainder comparisons. */
    private static final int NEUTRAL_BITS = 0;

    private StrategySnapshotProjections() {
    }

    /** before with ONLY the nullable side replaced by the exact argument (SIDE_SET). */
    static StrategyControllerSnapshot withSide(StrategyControllerSnapshot s, Side side) {
        return new StrategyControllerSnapshot(side, s.underBattleOrderRules(), s.hasShieldsToPlay(),
            s.offeredConcedeThisGame(), s.phase(), s.turnNumber(), s.forceGeneration(),
            s.forceGenerationTarget(), s.forceDeficit(), s.focus(), s.focusConfidenceBits(),
            s.turnsWithFocus(), s.focusDeployments(), s.contestedLocations(), s.dangerousLocations(),
            s.reserveChecksThisTurn(), s.cardsSeenInReserve(), s.lastReserveCheckTurn(),
            s.battlesWon(), s.battlesLost(), s.lastDecisionReason());
    }

    /** The exact reset() projection: preserved side and decision reason, everything else
     *  the reset constants (flags false/true/false; phase early; turn/generation zero;
     *  target and deficit 8; focus balanced; confidence exactly 0.5f; focus counters zero;
     *  location/reserve collections empty; reserve counters/turn zero; battle counters zero). */
    static StrategyControllerSnapshot resetProjection(StrategyControllerSnapshot before) {
        return new StrategyControllerSnapshot(before.side(), false, true, false,
            "early", 0, 0, 8, 8, "balanced", Float.floatToRawIntBits(0.5f), 0, 0,
            List.of(), List.of(), 0, List.of(), 0, 0, 0, before.lastDecisionReason());
    }

    /** startNewTurn writable fields neutralized (turn, phase, target, reserve-check count,
     *  seen-reserve cards); lastReserveCheckTurn stays frozen because startNewTurn never
     *  writes it. */
    static StrategyControllerSnapshot startTurnFrozen(StrategyControllerSnapshot s) {
        return new StrategyControllerSnapshot(s.side(), s.underBattleOrderRules(), s.hasShieldsToPlay(),
            s.offeredConcedeThisGame(), "", 0, s.forceGeneration(), 0, s.forceDeficit(),
            s.focus(), s.focusConfidenceBits(), s.turnsWithFocus(), s.focusDeployments(),
            s.contestedLocations(), s.dangerousLocations(), 0, List.of(), s.lastReserveCheckTurn(),
            s.battlesWon(), s.battlesLost(), s.lastDecisionReason());
    }

    /** onSuccessfulDeploy writable fields neutralized (deployment count and confidence);
     *  focus itself stays frozen because the call never changes it. */
    static StrategyControllerSnapshot focusFrozen(StrategyControllerSnapshot s) {
        return new StrategyControllerSnapshot(s.side(), s.underBattleOrderRules(), s.hasShieldsToPlay(),
            s.offeredConcedeThisGame(), s.phase(), s.turnNumber(), s.forceGeneration(),
            s.forceGenerationTarget(), s.forceDeficit(), s.focus(), NEUTRAL_BITS, s.turnsWithFocus(), 0,
            s.contestedLocations(), s.dangerousLocations(), s.reserveChecksThisTurn(),
            s.cardsSeenInReserve(), s.lastReserveCheckTurn(), s.battlesWon(), s.battlesLost(),
            s.lastDecisionReason());
    }

    /** updateBattleOrderFromGameState writable field neutralized (underBattleOrderRules). */
    static StrategyControllerSnapshot battleOrderFrozen(StrategyControllerSnapshot s) {
        return new StrategyControllerSnapshot(s.side(), false, s.hasShieldsToPlay(),
            s.offeredConcedeThisGame(), s.phase(), s.turnNumber(), s.forceGeneration(),
            s.forceGenerationTarget(), s.forceDeficit(), s.focus(), s.focusConfidenceBits(),
            s.turnsWithFocus(), s.focusDeployments(), s.contestedLocations(), s.dangerousLocations(),
            s.reserveChecksThisTurn(), s.cardsSeenInReserve(), s.lastReserveCheckTurn(),
            s.battlesWon(), s.battlesLost(), s.lastDecisionReason());
    }

    /** before with ONLY battlesWon incremented by one (onBattleResult(true)). */
    static StrategyControllerSnapshot withWin(StrategyControllerSnapshot before) {
        return new StrategyControllerSnapshot(before.side(), before.underBattleOrderRules(),
            before.hasShieldsToPlay(), before.offeredConcedeThisGame(), before.phase(),
            before.turnNumber(), before.forceGeneration(), before.forceGenerationTarget(),
            before.forceDeficit(), before.focus(), before.focusConfidenceBits(),
            before.turnsWithFocus(), before.focusDeployments(), before.contestedLocations(),
            before.dangerousLocations(), before.reserveChecksThisTurn(), before.cardsSeenInReserve(),
            before.lastReserveCheckTurn(), before.battlesWon() + 1, before.battlesLost(),
            before.lastDecisionReason());
    }

    /** onBattleResult(false) writable fields neutralized (losses, confidence, focus);
     *  turnsWithFocus and focusDeployments stay frozen because a battle loss never resets
     *  the focus counters, only the direct focus assignment. */
    static StrategyControllerSnapshot lossFrozen(StrategyControllerSnapshot s) {
        return new StrategyControllerSnapshot(s.side(), s.underBattleOrderRules(), s.hasShieldsToPlay(),
            s.offeredConcedeThisGame(), s.phase(), s.turnNumber(), s.forceGeneration(),
            s.forceGenerationTarget(), s.forceDeficit(), "", NEUTRAL_BITS, s.turnsWithFocus(),
            s.focusDeployments(), s.contestedLocations(), s.dangerousLocations(),
            s.reserveChecksThisTurn(), s.cardsSeenInReserve(), s.lastReserveCheckTurn(),
            s.battlesWon(), 0, s.lastDecisionReason());
    }
}
