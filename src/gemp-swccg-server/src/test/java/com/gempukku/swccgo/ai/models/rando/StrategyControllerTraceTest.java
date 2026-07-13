package com.gempukku.swccgo.ai.models.rando;

import com.gempukku.swccgo.ai.models.common.trace.state.MutationOutcome;
import com.gempukku.swccgo.ai.models.common.trace.state.StrategyBattleOrderRefreshEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.StrategyBattleResultRecordEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.StrategyControllerOwner;
import com.gempukku.swccgo.ai.models.common.trace.state.StrategyControllerSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.StrategyFocusDeployRecordEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.StrategyResetEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.StrategySideSetEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.StrategyStartTurnEvent;
import com.gempukku.swccgo.ai.models.rando.strategy.StrategyController;
import com.gempukku.swccgo.ai.models.rando.strategy.StrategyControllerTraceAccess;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TRACE STAGE 4B2 (Handoffs/CODEX_TRACE_STAGE4_4B2_STRATEGY_CONTROLLER_PREFLIGHT_2026-07-13.md
 * "Required Fixtures", direct controller): the REAL StrategyController driven through its
 * legacy mutators, observed through the public read-only StrategyControllerTraceAccess
 * bridge (which delegates to the pure package-local traceSnapshot() seam). Proves the six
 * events describe exactly what the existing owner does, including reset preservation, the
 * stale force deficit on turn start, reserve cooldown clearing, focus transitions,
 * repeated battle mutation, the folded Battle Order scan, and the direct null/unreachable
 * mutator behavior. No server, no bot.
 *
 * The canonical drive and its shared literal are IDENTICAL in the other bot's mirror of
 * this test (common snapshot type, owner constant normalized), so passing both proves the
 * Rando and ChosenOne snapshots match after package, class, and owner normalization.
 */
public class StrategyControllerTraceTest {

    private static StrategyControllerSnapshot snap(StrategyController c) {
        return StrategyControllerTraceAccess.snapshot(c);
    }

    /** Minimal GameState stand-in exposing only the permanent-card list the controller
     *  scans; a plain subclass is the minimum stand-in (GameState has a public no-arg
     *  constructor). */
    private static GameState permanentsState(List<PhysicalCard> permanents) {
        return new GameState() {
            @Override
            public List<PhysicalCard> getAllPermanentCards() {
                return permanents;
            }
        };
    }

    private static GameState throwingState() {
        return new GameState() {
            @Override
            public List<PhysicalCard> getAllPermanentCards() {
                throw new RuntimeException("injected scan failure");
            }
        };
    }

    /** A permanent card that answers only the four getters the scan reads (reflective
     *  Proxy needs an interface, and PhysicalCard is one). */
    private static PhysicalCard card(Zone zone, String blueprintId, String title, String owner) {
        return (PhysicalCard) Proxy.newProxyInstance(
            StrategyControllerTraceTest.class.getClassLoader(),
            new Class<?>[]{PhysicalCard.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getZone" -> zone;
                case "getBlueprintId" -> blueprintId;
                case "getTitle" -> title;
                case "getOwner" -> owner;
                case "toString" -> "card[" + blueprintId + "]";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> null;
            });
    }

    // =========================================================================
    // SIDE_SET + RESET: preservation of side and decision reason
    // =========================================================================

    @Test
    public void sideSetThenResetPreservesSideAndReason() {
        StrategyController c = new StrategyController();
        StrategyControllerSnapshot beforeSide = snap(c);
        assertEquals(null, beforeSide.side());
        c.setSide(Side.DARK);
        StrategySideSetEvent sideEvent = StrategySideSetEvent.of(StrategyControllerOwner.RANDO,
            Side.DARK, beforeSide, snap(c));
        assertEquals(MutationOutcome.CHANGED, sideEvent.outcome());
        assertEquals(Side.DARK, sideEvent.after().side());

        // fresh controller after side assignment: reset is a real NO_OP
        StrategyControllerSnapshot beforeReset = snap(c);
        c.reset();
        StrategyResetEvent freshReset = StrategyResetEvent.of(StrategyControllerOwner.RANDO,
            beforeReset, snap(c));
        assertEquals(MutationOutcome.NO_OP, freshReset.outcome());

        // dirty it, then reset: CHANGED, side + reason preserved
        c.setFocus(StrategyController.StrategyFocus.GROUND);
        c.onBattleResult(true);
        c.updateForceGeneration(3);
        c.startNewTurn(5);
        StrategyControllerSnapshot dirty = snap(c);
        c.reset();
        StrategyResetEvent dirtyReset = StrategyResetEvent.of(StrategyControllerOwner.RANDO,
            dirty, snap(c));
        assertEquals(MutationOutcome.CHANGED, dirtyReset.outcome());
        assertEquals(Side.DARK, dirtyReset.after().side());
        assertEquals(dirty.lastDecisionReason(), dirtyReset.after().lastDecisionReason());
        assertEquals("early", dirtyReset.after().phase());
        assertEquals(0, dirtyReset.after().turnNumber());
        assertEquals(8, dirtyReset.after().forceDeficit());
    }

    // =========================================================================
    // START_TURN: stale force deficit + reserve cooldown clearing
    // =========================================================================

    @Test
    public void startTurnLeavesForceDeficitStale() {
        StrategyController c = new StrategyController();
        c.setSide(Side.DARK);
        c.reset();
        c.updateForceGeneration(2);  // deficit = target(8) - 2 = 6
        StrategyControllerSnapshot before = snap(c);
        assertEquals(6, before.forceDeficit());
        c.startNewTurn(5);
        StrategyStartTurnEvent event = StrategyStartTurnEvent.of(StrategyControllerOwner.RANDO,
            5, before, snap(c));
        assertEquals(MutationOutcome.CHANGED, event.outcome());
        assertEquals(5, event.after().turnNumber());
        assertEquals("mid", event.after().phase());
        assertEquals(6, event.after().forceGenerationTarget());
        // startNewTurn does NOT recompute the deficit: it stays stale at 6
        assertEquals(6, event.after().forceDeficit());
        assertEquals(2, event.after().forceGeneration());
    }

    @Test
    public void startTurnReserveCooldownClearsAndRetains() {
        StrategyController c = new StrategyController();
        c.setSide(Side.DARK);
        c.reset();
        c.startNewTurn(3);
        c.recordReserveCheck(List.of("8_118"));  // lastReserveCheckTurn = 3, cardsSeen = {8_118}
        StrategyControllerSnapshot before = snap(c);
        assertEquals(List.of("8_118"), before.cardsSeenInReserve());

        // within cooldown (4 - 3 = 1): retained
        c.startNewTurn(4);
        StrategyStartTurnEvent retain = StrategyStartTurnEvent.of(StrategyControllerOwner.RANDO,
            4, before, snap(c));
        assertEquals(MutationOutcome.CHANGED, retain.outcome());
        assertEquals(List.of("8_118"), retain.after().cardsSeenInReserve());

        // cooldown elapsed (7 - 3 = 4 > 2): cleared
        StrategyControllerSnapshot before2 = snap(c);
        c.startNewTurn(7);
        StrategyStartTurnEvent clear = StrategyStartTurnEvent.of(StrategyControllerOwner.RANDO,
            7, before2, snap(c));
        assertTrue(clear.after().cardsSeenInReserve().isEmpty());
        assertEquals(0, clear.after().reserveChecksThisTurn());
    }

    // =========================================================================
    // FOCUS_DEPLOY_RECORD: transitions and NO_OPs
    // =========================================================================

    @Test
    public void focusDeployTransitions() {
        StrategyController c = new StrategyController();
        c.setSide(Side.DARK);
        c.reset();

        // balanced focus: onSuccessfulDeploy is a real NO_OP
        StrategyControllerSnapshot balancedBefore = snap(c);
        c.onSuccessfulDeploy("character");
        assertEquals(MutationOutcome.NO_OP, StrategyFocusDeployRecordEvent.of(
            StrategyControllerOwner.RANDO, "character", balancedBefore, snap(c)).outcome());

        c.setFocus(StrategyController.StrategyFocus.GROUND);

        // nonmatching card under ground focus: NO_OP
        StrategyControllerSnapshot nonMatchBefore = snap(c);
        c.onSuccessfulDeploy("starship");
        assertEquals(MutationOutcome.NO_OP, StrategyFocusDeployRecordEvent.of(
            StrategyControllerOwner.RANDO, "starship", nonMatchBefore, snap(c)).outcome());

        // first matching deploy: 0 -> 1, confidence unchanged
        StrategyControllerSnapshot first = snap(c);
        c.onSuccessfulDeploy("character");
        StrategyFocusDeployRecordEvent firstEvent = StrategyFocusDeployRecordEvent.of(
            StrategyControllerOwner.RANDO, "character", first, snap(c));
        assertEquals(MutationOutcome.CHANGED, firstEvent.outcome());
        assertEquals(1, firstEvent.after().focusDeployments());
        assertEquals(first.focusConfidenceBits(), firstEvent.after().focusConfidenceBits());

        // second matching deploy: 1 -> 2, confidence + 0.2
        StrategyControllerSnapshot second = snap(c);
        c.onSuccessfulDeploy("vehicle");
        StrategyFocusDeployRecordEvent secondEvent = StrategyFocusDeployRecordEvent.of(
            StrategyControllerOwner.RANDO, "vehicle", second, snap(c));
        assertEquals(2, secondEvent.after().focusDeployments());
        int expectedBits = Float.floatToRawIntBits(
            Math.min(1.0f, Float.intBitsToFloat(second.focusConfidenceBits()) + 0.2f));
        assertEquals(expectedBits, secondEvent.after().focusConfidenceBits());
    }

    // =========================================================================
    // BATTLE_RESULT_RECORD: repeats, and a loss preserves the focus counters
    // =========================================================================

    @Test
    public void battleResultRepeatsAndLossKeepsFocusCounters() {
        StrategyController c = new StrategyController();
        c.setSide(Side.DARK);
        c.reset();
        c.setFocus(StrategyController.StrategyFocus.GROUND);
        c.onSuccessfulDeploy("character");  // focusDeployments = 1

        // two wins in a row: no dedupe, wins 0 -> 1 -> 2
        StrategyControllerSnapshot w0 = snap(c);
        c.onBattleResult(true);
        StrategyBattleResultRecordEvent win1 = StrategyBattleResultRecordEvent.of(
            StrategyControllerOwner.RANDO, true, w0, snap(c));
        assertEquals(1, win1.after().battlesWon());
        StrategyControllerSnapshot w1 = snap(c);
        c.onBattleResult(true);
        StrategyBattleResultRecordEvent win2 = StrategyBattleResultRecordEvent.of(
            StrategyControllerOwner.RANDO, true, w1, snap(c));
        assertEquals(2, win2.after().battlesWon());

        // a loss drops confidence below 0.3 (0.5 -> 0.2), resets focus to balanced, but
        // must NOT reset the focus counters
        StrategyControllerSnapshot lBefore = snap(c);
        assertEquals(1, lBefore.focusDeployments());
        assertEquals("ground", lBefore.focus());
        c.onBattleResult(false);
        StrategyBattleResultRecordEvent loss = StrategyBattleResultRecordEvent.of(
            StrategyControllerOwner.RANDO, false, lBefore, snap(c));
        assertEquals(1, loss.after().battlesLost());
        assertEquals("balanced", loss.after().focus());
        assertEquals(1, loss.after().focusDeployments());  // counter preserved through the loss
    }

    // =========================================================================
    // BATTLE_ORDER_REFRESH: folded scan, detected/no-op, wrong zone, caught exception
    // =========================================================================

    @Test
    public void battleOrderRefreshFoldsTheScan() {
        StrategyController c = new StrategyController();
        c.setSide(Side.DARK);
        c.reset();

        // empty board: no Battle Order, NO_OP
        StrategyControllerSnapshot empty0 = snap(c);
        c.updateBattleOrderFromGameState(permanentsState(List.of()));
        assertEquals(MutationOutcome.NO_OP, StrategyBattleOrderRefreshEvent.of(
            StrategyControllerOwner.RANDO, empty0, snap(c)).outcome());

        // one of the exact six ids in SIDE_OF_TABLE: detected, only underBattleOrderRules moves
        StrategyControllerSnapshot before = snap(c);
        assertFalse(before.underBattleOrderRules());
        c.updateBattleOrderFromGameState(permanentsState(List.of(
            card(Zone.SIDE_OF_TABLE, "8_118", "Battle Order", "opponent"))));
        StrategyBattleOrderRefreshEvent detected = StrategyBattleOrderRefreshEvent.of(
            StrategyControllerOwner.RANDO, before, snap(c));
        assertEquals(MutationOutcome.CHANGED, detected.outcome());
        assertTrue(detected.after().underBattleOrderRules());

        // wrong zone: the same id in HAND is not counted, flag returns to false
        StrategyControllerSnapshot on = snap(c);
        c.updateBattleOrderFromGameState(permanentsState(List.of(
            card(Zone.HAND, "8_118", "Battle Order", "opponent"))));
        StrategyBattleOrderRefreshEvent wrongZone = StrategyBattleOrderRefreshEvent.of(
            StrategyControllerOwner.RANDO, on, snap(c));
        assertEquals(MutationOutcome.CHANGED, wrongZone.outcome());
        assertFalse(wrongZone.after().underBattleOrderRules());

        // a caught scan exception leaves the flag false: NO_OP
        StrategyControllerSnapshot before2 = snap(c);
        c.updateBattleOrderFromGameState(throwingState());
        assertEquals(MutationOutcome.NO_OP, StrategyBattleOrderRefreshEvent.of(
            StrategyControllerOwner.RANDO, before2, snap(c)).outcome());
    }

    // =========================================================================
    // Direct null / production-unreachable mutators (no owner event minted)
    // =========================================================================

    @Test
    public void directNullAndUnreachableMutatorsPreserveCurrentBehavior() {
        StrategyController c = new StrategyController();
        c.setSide(Side.DARK);
        c.reset();

        // setFocus(null) throws before mutation: the focus field is unchanged
        StrategyControllerSnapshot before = snap(c);
        try {
            c.setFocus(null);
            fail("setFocus(null) must throw before mutation");
        } catch (NullPointerException expected) {
            // required
        }
        assertEquals(before, snap(c));

        // recordReserveCheck does not enforce shouldCheckReserve(): it increments anyway
        c.recordReserveCheck(List.of("a"));
        c.recordReserveCheck(List.of("b"));
        c.recordReserveCheck(List.of("c"));  // past MAX_RESERVE_CHECKS_PER_TURN
        assertFalse(c.shouldCheckReserve());
        assertEquals(3, snap(c).reserveChecksThisTurn());
    }

    // =========================================================================
    // Cross-bot parity: the canonical drive matches the shared literal exactly
    // =========================================================================

    /** The exact snapshot the canonical drive must leave behind. The SAME literal appears
     *  in the other bot's mirror of this test (cross-bot snapshot parity). Confidence is
     *  computed with the identical float ops the controller uses, never a decimal constant. */
    private static StrategyControllerSnapshot canonicalDriveLiteral() {
        int confidence = Float.floatToRawIntBits(
            Math.max(0.0f, Math.min(1.0f, 0.5f + 0.2f) - 0.3f));
        return new StrategyControllerSnapshot(Side.DARK, false, true, false, "early", 2, 0, 8, 8,
            "ground", confidence, 0, 2, List.of(), List.of(), 1,
            List.of("1_2", "8_118"), 2, 1, 1, "No decisions made yet.");
    }

    @Test
    public void canonicalDriveMatchesTheCrossBotLiteral() {
        StrategyController c = new StrategyController();
        c.setSide(Side.DARK);
        c.reset();
        c.startNewTurn(2);
        c.setFocus(StrategyController.StrategyFocus.GROUND);
        c.onSuccessfulDeploy("character");
        c.onSuccessfulDeploy("vehicle");
        c.onBattleResult(true);
        c.onBattleResult(false);
        c.recordReserveCheck(List.of("8_118", "1_2"));
        assertEquals(canonicalDriveLiteral(), snap(c));
    }
}
