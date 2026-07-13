package com.gempukku.swccgo.ai.models.common.trace.state;

import com.gempukku.swccgo.common.Side;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TRACE STAGE 4B2 (Handoffs/CODEX_TRACE_STAGE4_4B2_STRATEGY_CONTROLLER_PREFLIGHT_2026-07-13.md
 * "Required Tests" item 1): constructor, closed-identity, defensive-immutability,
 * canonical-equality, outcome-derivation, and impossible-input tests for
 * StrategyControllerSnapshot and the six StrategyController event families. Every
 * operation-specific frozen-remainder and transition invariant gets a negative
 * constructor fixture. No server, no bots, no game state.
 */
public class StrategyControllerEventTest {

    private static final int HALF = Float.floatToRawIntBits(0.5f);

    /** Mutable builder for readable snapshot fixtures; defaults model a fresh controller
     *  just after setSide (post-reset values, side assigned). */
    private static final class Ctl {
        Side side = Side.DARK;
        boolean underBattleOrderRules = false;
        boolean hasShieldsToPlay = true;
        boolean offeredConcedeThisGame = false;
        String phase = "early";
        int turnNumber = 0;
        int forceGeneration = 0;
        int forceGenerationTarget = 8;
        int forceDeficit = 8;
        String focus = "balanced";
        int focusConfidenceBits = HALF;
        int turnsWithFocus = 0;
        int focusDeployments = 0;
        List<Integer> contestedLocations = List.of();
        List<Integer> dangerousLocations = List.of();
        int reserveChecksThisTurn = 0;
        List<String> cardsSeenInReserve = List.of();
        int lastReserveCheckTurn = 0;
        int battlesWon = 0;
        int battlesLost = 0;
        String lastDecisionReason = "No decisions made yet.";

        StrategyControllerSnapshot build() {
            return new StrategyControllerSnapshot(side, underBattleOrderRules, hasShieldsToPlay,
                offeredConcedeThisGame, phase, turnNumber, forceGeneration, forceGenerationTarget,
                forceDeficit, focus, focusConfidenceBits, turnsWithFocus, focusDeployments,
                contestedLocations, dangerousLocations, reserveChecksThisTurn, cardsSeenInReserve,
                lastReserveCheckTurn, battlesWon, battlesLost, lastDecisionReason);
        }
    }

    private static int bits(float f) {
        return Float.floatToRawIntBits(f);
    }

    private static void expectIllegalArgument(Runnable construction, String what) {
        try {
            construction.run();
            fail(what + " must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
    }

    private static void expectNullPointer(Runnable construction, String what) {
        try {
            construction.run();
            fail(what + " must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
    }

    // =========================================================================
    // StrategyControllerSnapshot: nulls, nullable side, canonicalization, immutability
    // =========================================================================

    @Test
    public void snapshotRejectsNullFieldsButAcceptsNullSide() {
        expectNullPointer(() -> {
            Ctl c = new Ctl();
            c.phase = null;
            c.build();
        }, "null phase");
        expectNullPointer(() -> {
            Ctl c = new Ctl();
            c.focus = null;
            c.build();
        }, "null focus");
        expectNullPointer(() -> {
            Ctl c = new Ctl();
            c.lastDecisionReason = null;
            c.build();
        }, "null lastDecisionReason");
        expectNullPointer(() -> {
            Ctl c = new Ctl();
            c.cardsSeenInReserve = null;
            c.build();
        }, "null cardsSeenInReserve");
        expectNullPointer(() -> {
            Ctl c = new Ctl();
            List<String> withNull = new ArrayList<>();
            withNull.add(null);
            c.cardsSeenInReserve = withNull;
            c.build();
        }, "null cardsSeenInReserve element");
        expectNullPointer(() -> {
            Ctl c = new Ctl();
            List<Integer> withNull = new ArrayList<>();
            withNull.add(null);
            c.contestedLocations = withNull;
            c.build();
        }, "null contestedLocations element");

        // side is intentionally nullable: a controller before setSide has none
        Ctl noSide = new Ctl();
        noSide.side = null;
        assertEquals(null, noSide.build().side());
    }

    @Test
    public void snapshotSortsReserveCardsAndPreservesLocationOrder() {
        Ctl shuffled = new Ctl();
        shuffled.cardsSeenInReserve = new ArrayList<>(Arrays.asList("8_118", "1_2", "13_54", "1_2"));
        shuffled.contestedLocations = new ArrayList<>(Arrays.asList(3, 1, 2));
        shuffled.dangerousLocations = new ArrayList<>(Arrays.asList(9, 7));
        StrategyControllerSnapshot s = shuffled.build();
        // String.compareTo is lexicographic: '3' (0x33) < '_' (0x5F), so "13_54" sorts first
        assertEquals(Arrays.asList("13_54", "1_2", "1_2", "8_118"), s.cardsSeenInReserve());
        assertEquals(Arrays.asList(3, 1, 2), s.contestedLocations());
        assertEquals(Arrays.asList(9, 7), s.dangerousLocations());

        // two snapshots from differently ordered same-content reserve input compare equal
        Ctl other = new Ctl();
        other.cardsSeenInReserve = new ArrayList<>(Arrays.asList("1_2", "8_118", "1_2", "13_54"));
        other.contestedLocations = new ArrayList<>(Arrays.asList(3, 1, 2));
        other.dangerousLocations = new ArrayList<>(Arrays.asList(9, 7));
        assertEquals(s, other.build());
    }

    @Test
    public void snapshotIsDefensivelyImmutable() {
        List<String> mutableCards = new ArrayList<>(Arrays.asList("1_2", "3_4"));
        List<Integer> mutableLoc = new ArrayList<>(Arrays.asList(1, 2));
        Ctl c = new Ctl();
        c.cardsSeenInReserve = mutableCards;
        c.contestedLocations = mutableLoc;
        StrategyControllerSnapshot s = c.build();
        mutableCards.add("9_9");
        mutableLoc.add(99);
        assertEquals(2, s.cardsSeenInReserve().size());
        assertEquals(2, s.contestedLocations().size());
        try {
            s.cardsSeenInReserve().add("x");
            fail("snapshot reserve list must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // required
        }
        try {
            s.contestedLocations().add(0);
            fail("snapshot location list must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // required
        }
    }

    @Test
    public void snapshotEqualityIsValueBasedAndBitExact() {
        assertEquals(new Ctl().build(), new Ctl().build());
        Ctl a = new Ctl();
        a.focusConfidenceBits = bits(0.5f);
        Ctl b = new Ctl();
        b.focusConfidenceBits = bits(0.5000001f);
        assertFalse("raw float bits are preserved exactly", a.build().equals(b.build()));
    }

    // =========================================================================
    // StrategyControllerOwner: closed to exactly RANDO and CHOSENONE
    // =========================================================================

    @Test
    public void ownerIsClosedToTwoScopes() {
        assertEquals(2, StrategyControllerOwner.values().length);
        assertEquals(StrategyControllerOwner.RANDO, StrategyControllerOwner.valueOf("RANDO"));
        assertEquals(StrategyControllerOwner.CHOSENONE, StrategyControllerOwner.valueOf("CHOSENONE"));
    }

    // =========================================================================
    // SIDE_SET
    // =========================================================================

    @Test
    public void sideSetOutcomeAndNullableArgument() {
        // same value: setSide ran but nothing moved
        StrategyControllerSnapshot dark = new Ctl().build();
        StrategySideSetEvent noOp = StrategySideSetEvent.of(StrategyControllerOwner.RANDO,
            Side.DARK, dark, dark);
        assertEquals(MutationOutcome.NO_OP, noOp.outcome());

        // null -> DARK is a real CHANGED with only the side differing
        Ctl none = new Ctl();
        none.side = null;
        StrategySideSetEvent changed = StrategySideSetEvent.of(StrategyControllerOwner.RANDO,
            Side.DARK, none.build(), dark);
        assertEquals(MutationOutcome.CHANGED, changed.outcome());

        // the intentionally nullable argument: setSide(null) is accepted
        StrategySideSetEvent toNull = StrategySideSetEvent.of(StrategyControllerOwner.RANDO,
            null, dark, none.build());
        assertEquals(MutationOutcome.CHANGED, toNull.outcome());
    }

    @Test
    public void sideSetRejectsUnrelatedDeltaAndArgMismatch() {
        Ctl none = new Ctl();
        none.side = null;
        StrategyControllerSnapshot before = none.build();
        StrategyControllerSnapshot dark = new Ctl().build();

        // after moved a field other than side
        Ctl movedOther = new Ctl();
        movedOther.turnNumber = 3;
        expectIllegalArgument(() -> StrategySideSetEvent.of(StrategyControllerOwner.RANDO,
            Side.DARK, before, movedOther.build()), "SIDE_SET with an unrelated field delta");

        // after.side does not match the exact argument
        expectIllegalArgument(() -> StrategySideSetEvent.of(StrategyControllerOwner.RANDO,
            Side.LIGHT, before, dark), "SIDE_SET whose after side differs from the argument");

        expectNullPointer(() -> StrategySideSetEvent.of(null, Side.DARK, dark, dark),
            "null owner");

        // outcome inconsistency on the direct constructor
        expectIllegalArgument(() -> new StrategySideSetEvent(StrategyControllerOwner.RANDO,
            Side.DARK, dark, dark, MutationOutcome.CHANGED), "NO_OP snapshot with CHANGED outcome");
    }

    // =========================================================================
    // RESET
    // =========================================================================

    /** A "dirty" mid-game controller whose reset() must land on the exact projection. */
    private static StrategyControllerSnapshot dirty() {
        Ctl c = new Ctl();
        c.underBattleOrderRules = true;
        c.hasShieldsToPlay = false;
        c.offeredConcedeThisGame = true;
        c.phase = "late";
        c.turnNumber = 11;
        c.forceGeneration = 6;
        c.forceGenerationTarget = 5;
        c.forceDeficit = -1;
        c.focus = "ground";
        c.focusConfidenceBits = bits(0.9f);
        c.turnsWithFocus = 4;
        c.focusDeployments = 3;
        c.contestedLocations = new ArrayList<>(Arrays.asList(1, 2));
        c.dangerousLocations = new ArrayList<>(Arrays.asList(3));
        c.reserveChecksThisTurn = 2;
        c.cardsSeenInReserve = new ArrayList<>(Arrays.asList("8_118"));
        c.lastReserveCheckTurn = 10;
        c.battlesWon = 2;
        c.battlesLost = 1;
        c.lastDecisionReason = "Held Scarif.";
        return c.build();
    }

    private static StrategyControllerSnapshot resetOf(StrategyControllerSnapshot before) {
        // preserved side + decision reason, every other field at its reset constant
        return new StrategyControllerSnapshot(before.side(), false, true, false, "early", 0, 0, 8, 8,
            "balanced", HALF, 0, 0, List.of(), List.of(), 0, List.of(), 0, 0, 0,
            before.lastDecisionReason());
    }

    @Test
    public void resetLandsOnExactProjectionPreservingSideAndReason() {
        StrategyControllerSnapshot before = dirty();
        StrategyResetEvent event = StrategyResetEvent.of(StrategyControllerOwner.CHOSENONE,
            before, resetOf(before));
        assertEquals(MutationOutcome.CHANGED, event.outcome());
        assertEquals(before.side(), event.after().side());
        assertEquals(before.lastDecisionReason(), event.after().lastDecisionReason());

        // a fresh controller after side assignment is a real NO_OP reset
        StrategyControllerSnapshot fresh = new Ctl().build();
        assertEquals(MutationOutcome.NO_OP,
            StrategyResetEvent.of(StrategyControllerOwner.RANDO, fresh, fresh).outcome());
    }

    @Test
    public void resetRejectsNonResetAfter() {
        StrategyControllerSnapshot before = dirty();
        // turn not reset to zero
        StrategyControllerSnapshot badTurn = new StrategyControllerSnapshot(before.side(), false, true,
            false, "early", 1, 0, 8, 8, "balanced", HALF, 0, 0, List.of(), List.of(), 0, List.of(),
            0, 0, 0, before.lastDecisionReason());
        expectIllegalArgument(() -> StrategyResetEvent.of(StrategyControllerOwner.RANDO, before, badTurn),
            "RESET whose after did not zero the turn");
        // side not preserved
        StrategyControllerSnapshot badSide = new StrategyControllerSnapshot(Side.LIGHT, false, true,
            false, "early", 0, 0, 8, 8, "balanced", HALF, 0, 0, List.of(), List.of(), 0, List.of(),
            0, 0, 0, before.lastDecisionReason());
        expectIllegalArgument(() -> StrategyResetEvent.of(StrategyControllerOwner.RANDO, before, badSide),
            "RESET that changed the preserved side");
        // decision reason not preserved
        StrategyControllerSnapshot badReason = new StrategyControllerSnapshot(before.side(), false, true,
            false, "early", 0, 0, 8, 8, "balanced", HALF, 0, 0, List.of(), List.of(), 0, List.of(),
            0, 0, 0, "wiped");
        expectIllegalArgument(() -> StrategyResetEvent.of(StrategyControllerOwner.RANDO, before, badReason),
            "RESET that changed the preserved decision reason");
    }

    // =========================================================================
    // START_TURN
    // =========================================================================

    /** The exact after a startNewTurn(turn) leaves, given a before with no reserve
     *  cooldown elapsed and the writable fields set by the turn. */
    private static StrategyControllerSnapshot startTurnAfter(StrategyControllerSnapshot before,
                                                             int turn, String phase, int target,
                                                             List<String> cardsAfter) {
        return new StrategyControllerSnapshot(before.side(), before.underBattleOrderRules(),
            before.hasShieldsToPlay(), before.offeredConcedeThisGame(), phase, turn,
            before.forceGeneration(), target, before.forceDeficit(), before.focus(),
            before.focusConfidenceBits(), before.turnsWithFocus(), before.focusDeployments(),
            before.contestedLocations(), before.dangerousLocations(), 0, cardsAfter,
            before.lastReserveCheckTurn(), before.battlesWon(), before.battlesLost(),
            before.lastDecisionReason());
    }

    @Test
    public void startTurnPhaseTargetBoundaries() {
        Ctl c = new Ctl();
        c.reserveChecksThisTurn = 2;
        StrategyControllerSnapshot before = c.build();
        // early through turn 3
        assertEquals(MutationOutcome.CHANGED, StrategyStartTurnEvent.of(StrategyControllerOwner.RANDO,
            3, before, startTurnAfter(before, 3, "early", 8, List.of())).outcome());
        // mid from turn 4 through turn 8
        StrategyStartTurnEvent.of(StrategyControllerOwner.RANDO, 4, before,
            startTurnAfter(before, 4, "mid", 6, List.of()));
        StrategyStartTurnEvent.of(StrategyControllerOwner.RANDO, 8, before,
            startTurnAfter(before, 8, "mid", 6, List.of()));
        // late from turn 9
        StrategyStartTurnEvent.of(StrategyControllerOwner.RANDO, 9, before,
            startTurnAfter(before, 9, "late", 5, List.of()));

        // wrong phase/target for the turn is rejected
        expectIllegalArgument(() -> StrategyStartTurnEvent.of(StrategyControllerOwner.RANDO, 9, before,
            startTurnAfter(before, 9, "mid", 6, List.of())), "START_TURN with the wrong phase/target");
        // turn argument mismatch
        expectIllegalArgument(() -> StrategyStartTurnEvent.of(StrategyControllerOwner.RANDO, 4, before,
            startTurnAfter(before, 5, "mid", 6, List.of())), "START_TURN whose after turn != argument");
        // reserve checks not reset
        StrategyControllerSnapshot badReserve = new StrategyControllerSnapshot(before.side(),
            before.underBattleOrderRules(), before.hasShieldsToPlay(), before.offeredConcedeThisGame(),
            "early", 3, before.forceGeneration(), 8, before.forceDeficit(), before.focus(),
            before.focusConfidenceBits(), before.turnsWithFocus(), before.focusDeployments(),
            before.contestedLocations(), before.dangerousLocations(), 1, List.of(),
            before.lastReserveCheckTurn(), before.battlesWon(), before.battlesLost(),
            before.lastDecisionReason());
        expectIllegalArgument(() -> StrategyStartTurnEvent.of(StrategyControllerOwner.RANDO, 3, before,
            badReserve), "START_TURN that left reserveChecksThisTurn non-zero");
    }

    @Test
    public void startTurnSeenReserveCooldownAndFrozenFields() {
        // cooldown elapsed (turn 4 - lastReserveCheckTurn 0 > 2): seen-reserve must clear
        Ctl seeded = new Ctl();
        seeded.cardsSeenInReserve = new ArrayList<>(Arrays.asList("8_118"));
        seeded.lastReserveCheckTurn = 0;
        StrategyControllerSnapshot before = seeded.build();
        StrategyStartTurnEvent.of(StrategyControllerOwner.RANDO, 4, before,
            startTurnAfter(before, 4, "mid", 6, List.of()));
        expectIllegalArgument(() -> StrategyStartTurnEvent.of(StrategyControllerOwner.RANDO, 4, before,
            startTurnAfter(before, 4, "mid", 6, List.of("8_118"))),
            "START_TURN must clear seen-reserve after the cooldown");

        // cooldown NOT elapsed (turn 4 - lastReserveCheckTurn 3 == 1): seen-reserve retained
        Ctl recent = new Ctl();
        recent.cardsSeenInReserve = new ArrayList<>(Arrays.asList("8_118"));
        recent.lastReserveCheckTurn = 3;
        StrategyControllerSnapshot before2 = recent.build();
        StrategyStartTurnEvent.of(StrategyControllerOwner.RANDO, 4, before2,
            startTurnAfter(before2, 4, "mid", 6, List.of("8_118")));
        expectIllegalArgument(() -> StrategyStartTurnEvent.of(StrategyControllerOwner.RANDO, 4, before2,
            startTurnAfter(before2, 4, "mid", 6, List.of())),
            "START_TURN must retain seen-reserve within the cooldown");

        // a frozen field (battle counter) moved: rejected
        StrategyControllerSnapshot movedFrozen = new StrategyControllerSnapshot(before2.side(),
            before2.underBattleOrderRules(), before2.hasShieldsToPlay(), before2.offeredConcedeThisGame(),
            "mid", 4, before2.forceGeneration(), 6, before2.forceDeficit(), before2.focus(),
            before2.focusConfidenceBits(), before2.turnsWithFocus(), before2.focusDeployments(),
            before2.contestedLocations(), before2.dangerousLocations(), 0,
            List.of("8_118"), before2.lastReserveCheckTurn(), before2.battlesWon() + 1,
            before2.battlesLost(), before2.lastDecisionReason());
        expectIllegalArgument(() -> StrategyStartTurnEvent.of(StrategyControllerOwner.RANDO, 4, before2,
            movedFrozen), "START_TURN that changed a frozen field");
    }

    // =========================================================================
    // FOCUS_DEPLOY_RECORD
    // =========================================================================

    @Test
    public void focusDeployBalancedAndNonmatchingRequireIdentical() {
        StrategyControllerSnapshot balanced = new Ctl().build();
        assertEquals(MutationOutcome.NO_OP, StrategyFocusDeployRecordEvent.of(
            StrategyControllerOwner.RANDO, "character", balanced, balanced).outcome());

        // balanced focus may never register a deployment
        Ctl balancedBumped = new Ctl();
        balancedBumped.focusDeployments = 1;
        expectIllegalArgument(() -> StrategyFocusDeployRecordEvent.of(StrategyControllerOwner.RANDO,
            "character", balanced, balancedBumped.build()),
            "FOCUS_DEPLOY_RECORD that changed anything under a balanced focus");

        // ground focus, nonmatching card: identical snapshots required
        Ctl ground = new Ctl();
        ground.focus = "ground";
        StrategyControllerSnapshot g = ground.build();
        assertEquals(MutationOutcome.NO_OP, StrategyFocusDeployRecordEvent.of(
            StrategyControllerOwner.RANDO, "starship", g, g).outcome());
        Ctl groundMovedOther = new Ctl();
        groundMovedOther.focus = "ground";
        groundMovedOther.battlesWon = 1;
        expectIllegalArgument(() -> StrategyFocusDeployRecordEvent.of(StrategyControllerOwner.RANDO,
            "starship", g, groundMovedOther.build()),
            "FOCUS_DEPLOY_RECORD nonmatching card with a field delta");

        // impossible transition: a nonmatching card must never register a deployment increment.
        // ground focus + starship (nonmatching) with deployments 0 -> 1 is rejected because
        // matching is decided by cardType vs focus, not by the increment itself.
        Ctl groundStarshipBumped = new Ctl();
        groundStarshipBumped.focus = "ground";
        groundStarshipBumped.focusDeployments = 1;
        expectIllegalArgument(() -> StrategyFocusDeployRecordEvent.of(StrategyControllerOwner.RANDO,
            "starship", g, groundStarshipBumped.build()),
            "FOCUS_DEPLOY_RECORD ground focus + nonmatching starship that incremented deployments");

        // and the mirror: space focus + character (nonmatching) with deployments 0 -> 1 is rejected
        Ctl space = new Ctl();
        space.focus = "space";
        StrategyControllerSnapshot sp = space.build();
        Ctl spaceCharacterBumped = new Ctl();
        spaceCharacterBumped.focus = "space";
        spaceCharacterBumped.focusDeployments = 1;
        expectIllegalArgument(() -> StrategyFocusDeployRecordEvent.of(StrategyControllerOwner.RANDO,
            "character", sp, spaceCharacterBumped.build()),
            "FOCUS_DEPLOY_RECORD space focus + nonmatching character that incremented deployments");
    }

    @Test
    public void focusDeployMatchingIncrementsAndRaisesConfidenceOnSecond() {
        // first matching deploy: 0 -> 1, confidence unchanged
        Ctl before1 = new Ctl();
        before1.focus = "ground";
        Ctl after1 = new Ctl();
        after1.focus = "ground";
        after1.focusDeployments = 1;
        assertEquals(MutationOutcome.CHANGED, StrategyFocusDeployRecordEvent.of(
            StrategyControllerOwner.RANDO, "character", before1.build(), after1.build()).outcome());

        // first deploy must NOT change confidence
        Ctl after1Conf = new Ctl();
        after1Conf.focus = "ground";
        after1Conf.focusDeployments = 1;
        after1Conf.focusConfidenceBits = bits(0.7f);
        expectIllegalArgument(() -> StrategyFocusDeployRecordEvent.of(StrategyControllerOwner.RANDO,
            "character", before1.build(), after1Conf.build()),
            "FOCUS_DEPLOY_RECORD raising confidence on the first matching deploy");

        // second matching deploy: 1 -> 2, confidence +0.2
        Ctl before2 = new Ctl();
        before2.focus = "ground";
        before2.focusDeployments = 1;
        Ctl after2 = new Ctl();
        after2.focus = "ground";
        after2.focusDeployments = 2;
        after2.focusConfidenceBits = bits(0.7f);
        StrategyFocusDeployRecordEvent.of(StrategyControllerOwner.RANDO, "vehicle",
            before2.build(), after2.build());
        // wrong confidence on the second deploy is rejected
        Ctl after2Wrong = new Ctl();
        after2Wrong.focus = "ground";
        after2Wrong.focusDeployments = 2;
        after2Wrong.focusConfidenceBits = HALF;
        expectIllegalArgument(() -> StrategyFocusDeployRecordEvent.of(StrategyControllerOwner.RANDO,
            "vehicle", before2.build(), after2Wrong.build()),
            "FOCUS_DEPLOY_RECORD second deploy without the +0.2 confidence");

        // confidence caps at 1.0
        Ctl beforeCap = new Ctl();
        beforeCap.focus = "space";
        beforeCap.focusDeployments = 5;
        beforeCap.focusConfidenceBits = bits(0.9f);
        Ctl afterCap = new Ctl();
        afterCap.focus = "space";
        afterCap.focusDeployments = 6;
        afterCap.focusConfidenceBits = bits(1.0f);
        StrategyFocusDeployRecordEvent.of(StrategyControllerOwner.RANDO, "starship",
            beforeCap.build(), afterCap.build());

        // deployment jump by 2 is rejected
        Ctl afterJump = new Ctl();
        afterJump.focus = "ground";
        afterJump.focusDeployments = 2;
        expectIllegalArgument(() -> StrategyFocusDeployRecordEvent.of(StrategyControllerOwner.RANDO,
            "character", before1.build(), afterJump.build()),
            "FOCUS_DEPLOY_RECORD deployment jump");

        // matching but focus itself changed is rejected
        Ctl afterFocusMoved = new Ctl();
        afterFocusMoved.focus = "space";
        afterFocusMoved.focusDeployments = 1;
        expectIllegalArgument(() -> StrategyFocusDeployRecordEvent.of(StrategyControllerOwner.RANDO,
            "character", before1.build(), afterFocusMoved.build()),
            "FOCUS_DEPLOY_RECORD that changed the focus");

        expectNullPointer(() -> StrategyFocusDeployRecordEvent.of(StrategyControllerOwner.RANDO,
            null, before1.build(), after1.build()), "null cardType");
    }

    // =========================================================================
    // BATTLE_ORDER_REFRESH
    // =========================================================================

    @Test
    public void battleOrderRefreshOnlyUnderBattleOrderMayDiffer() {
        StrategyControllerSnapshot off = new Ctl().build();
        assertEquals(MutationOutcome.NO_OP, StrategyBattleOrderRefreshEvent.of(
            StrategyControllerOwner.RANDO, off, off).outcome());

        Ctl onCtl = new Ctl();
        onCtl.underBattleOrderRules = true;
        StrategyControllerSnapshot on = onCtl.build();
        assertEquals(MutationOutcome.CHANGED, StrategyBattleOrderRefreshEvent.of(
            StrategyControllerOwner.RANDO, off, on).outcome());

        // any other field delta is rejected
        Ctl onPlus = new Ctl();
        onPlus.underBattleOrderRules = true;
        onPlus.turnNumber = 5;
        expectIllegalArgument(() -> StrategyBattleOrderRefreshEvent.of(StrategyControllerOwner.RANDO,
            off, onPlus.build()), "BATTLE_ORDER_REFRESH with a non-battle-order delta");
    }

    // =========================================================================
    // BATTLE_RESULT_RECORD
    // =========================================================================

    @Test
    public void battleResultWinIncrementsOnlyWins() {
        StrategyControllerSnapshot before = new Ctl().build();
        Ctl afterCtl = new Ctl();
        afterCtl.battlesWon = 1;
        StrategyBattleResultRecordEvent win = StrategyBattleResultRecordEvent.of(
            StrategyControllerOwner.RANDO, true, before, afterCtl.build());
        assertEquals(MutationOutcome.CHANGED, win.outcome());
        assertTrue(win.won());

        // a win may not also touch confidence
        Ctl winPlusConf = new Ctl();
        winPlusConf.battlesWon = 1;
        winPlusConf.focusConfidenceBits = bits(0.7f);
        expectIllegalArgument(() -> StrategyBattleResultRecordEvent.of(StrategyControllerOwner.RANDO,
            true, before, winPlusConf.build()), "BATTLE_RESULT_RECORD win with a confidence delta");

        // a win may not increment losses
        Ctl winPlusLoss = new Ctl();
        winPlusLoss.battlesWon = 1;
        winPlusLoss.battlesLost = 1;
        expectIllegalArgument(() -> StrategyBattleResultRecordEvent.of(StrategyControllerOwner.RANDO,
            true, before, winPlusLoss.build()), "BATTLE_RESULT_RECORD win that also incremented losses");
    }

    @Test
    public void battleResultLossDropsConfidenceResetsFocusBelowThresholdAndKeepsCounters() {
        // confidence 0.5 -> 0.2 (< 0.3): focus resets to balanced, counters preserved
        Ctl before = new Ctl();
        before.focus = "ground";
        before.turnsWithFocus = 3;
        before.focusDeployments = 4;
        StrategyControllerSnapshot b = before.build();
        Ctl after = new Ctl();
        after.focus = "balanced";
        after.turnsWithFocus = 3;
        after.focusDeployments = 4;
        after.focusConfidenceBits = bits(0.5f - 0.3f);  // exact float op, not a decimal literal
        after.battlesLost = 1;
        StrategyBattleResultRecordEvent loss = StrategyBattleResultRecordEvent.of(
            StrategyControllerOwner.RANDO, false, b, after.build());
        assertEquals(MutationOutcome.CHANGED, loss.outcome());
        assertFalse(loss.won());

        // the focus counters must NOT be reset by a battle loss (unlike setFocus)
        Ctl afterCountersReset = new Ctl();
        afterCountersReset.focus = "balanced";
        afterCountersReset.turnsWithFocus = 0;
        afterCountersReset.focusDeployments = 0;
        afterCountersReset.focusConfidenceBits = bits(0.5f - 0.3f);
        afterCountersReset.battlesLost = 1;
        expectIllegalArgument(() -> StrategyBattleResultRecordEvent.of(StrategyControllerOwner.RANDO,
            false, b, afterCountersReset.build()),
            "BATTLE_RESULT_RECORD loss that reset the focus counters");

        // confidence 0.9 -> 0.6 (>= 0.3): focus stays ground
        Ctl highBefore = new Ctl();
        highBefore.focus = "ground";
        highBefore.focusConfidenceBits = bits(0.9f);
        StrategyControllerSnapshot hb = highBefore.build();
        Ctl highAfter = new Ctl();
        highAfter.focus = "ground";
        highAfter.focusConfidenceBits = bits(0.9f - 0.3f);
        highAfter.battlesLost = 1;
        StrategyBattleResultRecordEvent.of(StrategyControllerOwner.RANDO, false, hb, highAfter.build());
        // resetting the focus when confidence stayed high is rejected
        Ctl highAfterReset = new Ctl();
        highAfterReset.focus = "balanced";
        highAfterReset.focusConfidenceBits = bits(0.9f - 0.3f);
        highAfterReset.battlesLost = 1;
        expectIllegalArgument(() -> StrategyBattleResultRecordEvent.of(StrategyControllerOwner.RANDO,
            false, hb, highAfterReset.build()),
            "BATTLE_RESULT_RECORD loss that reset the focus while confidence stayed high");

        // zero floor: 0.1 -> 0.0 (not negative)
        Ctl lowBefore = new Ctl();
        lowBefore.focus = "space";
        lowBefore.focusConfidenceBits = bits(0.1f);
        StrategyControllerSnapshot lb = lowBefore.build();
        Ctl lowAfter = new Ctl();
        lowAfter.focus = "balanced";
        lowAfter.focusConfidenceBits = bits(0.0f);
        lowAfter.battlesLost = 1;
        StrategyBattleResultRecordEvent.of(StrategyControllerOwner.RANDO, false, lb, lowAfter.build());
        // wrong confidence (no floor, went negative) is rejected
        Ctl lowAfterNeg = new Ctl();
        lowAfterNeg.focus = "balanced";
        lowAfterNeg.focusConfidenceBits = bits(-0.2f);
        lowAfterNeg.battlesLost = 1;
        expectIllegalArgument(() -> StrategyBattleResultRecordEvent.of(StrategyControllerOwner.RANDO,
            false, lb, lowAfterNeg.build()), "BATTLE_RESULT_RECORD loss without the zero floor");
    }
}
