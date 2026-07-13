package com.gempukku.swccgo.ai.models.common.trace.state;

import com.gempukku.swccgo.common.GameEndReason;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TRACE STAGE 4A1 (Handoffs/CODEX_TRACE_STAGE4_4A0_MUTATOR_EVENT_MATRIX_2026-07-13.md
 * + the accepted m00372 Option A tracker contract): pure constructor, invariant,
 * deep-copy, and outcome-derivation tests for the sealed state-event slice. No server,
 * no bots, no game state.
 */
public class TraceStateEventTest {

    private static DecisionTrackerSnapshot emptySnapshot() {
        return new DecisionTrackerSnapshot(List.of(), 0, 0, "", "", "", 0, List.of());
    }

    private static DecisionTrackerSnapshot oneRowSnapshot() {
        return new DecisionTrackerSnapshot(
            List.of(new DecisionTrackerSnapshot.TrackerSequenceRow(
                "CARD_ACTION_CHOICE:Choose action", "5", "3:4:20:1:7")),
            0, 0, "CARD_ACTION_CHOICE:Choose action", "5", "", 0, List.of());
    }

    // =========================================================================
    // Sealed hierarchy: exactly the 4A1 + 4A2a + 4A2b + 4B1 families, nothing else
    // =========================================================================

    @Test
    public void hierarchyIsSealedToThePermittedFamilies() {
        assertTrue(TraceStateEvent.class.isSealed());
        Class<?>[] permitted = TraceStateEvent.class.getPermittedSubclasses();
        assertEquals(20, permitted.length);
        List<String> names = new ArrayList<>();
        for (Class<?> c : permitted) {
            names.add(c.getSimpleName());
        }
        assertTrue(names.contains("TrackerRecordResponseEvent"));
        assertTrue(names.contains("PendingConcedeEvent"));
        assertTrue(names.contains("EnginePlayerLostEvent"));
        assertTrue(names.contains("PendingDeployEvent"));
        // TRACE STAGE 4A2a: the two outer tracker lifecycle families
        assertTrue(names.contains("TrackerUpdateStateEvent"));
        assertTrue(names.contains("TrackerClearEvent"));
        // TRACE STAGE 4A2b: the two inherited shared-tracker families
        assertTrue(names.contains("TrackerPhaseChangeEvent"));
        assertTrue(names.contains("TrackerBlockResponseEvent"));
        // TRACE STAGE 4B1: the six closed heuristic-memory families; the local block
        // and the reassignment count stay FOLDED, so no seventh 4B1 family exists
        assertTrue(names.contains("HeuristicStateUpdateEvent"));
        assertTrue(names.contains("HeuristicActionChoiceRememberEvent"));
        assertTrue(names.contains("HeuristicFailedSearchAddEvent"));
        assertTrue(names.contains("HeuristicSingleResponseRecordEvent"));
        assertTrue(names.contains("HeuristicRecentResponseAppendEvent"));
        assertTrue(names.contains("HeuristicReassignmentRecordEvent"));
        // TRACE STAGE 4B2: the six closed StrategyController families; the internal
        // setUnderBattleOrderRules write stays FOLDED into BATTLE_ORDER_REFRESH and the
        // two win/loss hooks share BATTLE_RESULT_RECORD, so no seventh 4B2 family exists
        assertTrue(names.contains("StrategySideSetEvent"));
        assertTrue(names.contains("StrategyResetEvent"));
        assertTrue(names.contains("StrategyStartTurnEvent"));
        assertTrue(names.contains("StrategyFocusDeployRecordEvent"));
        assertTrue(names.contains("StrategyBattleOrderRefreshEvent"));
        assertTrue(names.contains("StrategyBattleResultRecordEvent"));
    }

    // =========================================================================
    // DecisionTrackerSnapshot: canonical form + defensive copies
    // =========================================================================

    @Test
    public void trackerSnapshotDefensivelyCopiesAndCanonicalizes() {
        List<DecisionTrackerSnapshot.TrackerSequenceRow> rows = new ArrayList<>();
        rows.add(new DecisionTrackerSnapshot.TrackerSequenceRow("k1", "r1", "h1"));
        // turn blocks supplied UNSORTED on both axes; construction canonicalizes
        List<DecisionTrackerSnapshot.TrackerTurnBlockRow> blocks = new ArrayList<>();
        blocks.add(new DecisionTrackerSnapshot.TrackerTurnBlockRow("zeta",
            new ArrayList<>(List.of("b", "a"))));
        blocks.add(new DecisionTrackerSnapshot.TrackerTurnBlockRow("alpha",
            new ArrayList<>(List.of("z", "x"))));

        DecisionTrackerSnapshot snapshot =
            new DecisionTrackerSnapshot(rows, 2, 2, "k", "r", "ck", 1, blocks);

        // mutate the caller-owned lists AFTER construction; the snapshot must not move
        rows.clear();
        blocks.clear();
        assertEquals(1, snapshot.sequenceRows().size());
        assertEquals(2, snapshot.turnBlockRows().size());

        // canonical: rows sorted by decision key, responses sorted within each row
        assertEquals("alpha", snapshot.turnBlockRows().get(0).decisionKey());
        assertEquals(List.of("x", "z"), snapshot.turnBlockRows().get(0).sortedResponses());
        assertEquals("zeta", snapshot.turnBlockRows().get(1).decisionKey());
        assertEquals(List.of("a", "b"), snapshot.turnBlockRows().get(1).sortedResponses());

        // exposed lists are unmodifiable
        try {
            snapshot.sequenceRows().clear();
            fail("sequenceRows must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // required
        }
        try {
            snapshot.turnBlockRows().get(0).sortedResponses().add("tamper");
            fail("sortedResponses must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // required
        }
    }

    @Test
    public void trackerSnapshotRejectsNegativeCountsAndNulls() {
        try {
            new DecisionTrackerSnapshot(List.of(), -1, 0, "", "", "", 0, List.of());
            fail("negative sequenceRepeatCount must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            new DecisionTrackerSnapshot(List.of(), 0, 0, null, "", "", 0, List.of());
            fail("null lastActionChoiceKey must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
        try {
            new DecisionTrackerSnapshot.TrackerSequenceRow("k", null, "h");
            fail("null sequence-row response must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
    }

    // =========================================================================
    // TrackerRecordResponseEvent: outer owners only, outcome from snapshot equality
    // =========================================================================

    @Test
    public void trackerEventDerivesOutcomeFromSnapshotEquality() {
        TrackerRecordResponseEvent changed = TrackerRecordResponseEvent.of(
            TrackerOwner.OUTER_RANDO, "CARD_ACTION_CHOICE", "42",
            "CARD_ACTION_CHOICE:Choose action", "5", emptySnapshot(), oneRowSnapshot());
        assertEquals(MutationOutcome.CHANGED, changed.outcome());

        // equal before/after snapshots (a history-only append is excluded by ruling)
        TrackerRecordResponseEvent noOp = TrackerRecordResponseEvent.of(
            TrackerOwner.OUTER_CHOSENONE, "CARD_SELECTION", "43",
            "CARD_SELECTION:Choose cards", "", emptySnapshot(), emptySnapshot());
        assertEquals(MutationOutcome.NO_OP, noOp.outcome());
    }

    @Test
    public void trackerEventRejectsInconsistentOutcomeAndAcceptsTheExpandedOwner() {
        try {
            new TrackerRecordResponseEvent(TrackerOwner.OUTER_RANDO, "T", "1", "k", "r",
                emptySnapshot(), emptySnapshot(), MutationOutcome.CHANGED);
            fail("CHANGED with equal snapshots must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            new TrackerRecordResponseEvent(TrackerOwner.OUTER_RANDO, "T", "1", "k", "r",
                emptySnapshot(), oneRowSnapshot(), MutationOutcome.NO_OP);
            fail("NO_OP with differing snapshots must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        // TRACE STAGE 4A2b intentionally expands the 4A1 owner invariant: the shared
        // recordDecision call reuses this record with HEURISTIC_SHARED
        TrackerRecordResponseEvent shared = TrackerRecordResponseEvent.of(
            TrackerOwner.HEURISTIC_SHARED, "T", "1", "k", "r",
            emptySnapshot(), emptySnapshot());
        assertEquals(TrackerOwner.HEURISTIC_SHARED, shared.owner());
        try {
            TrackerRecordResponseEvent.of(TrackerOwner.OUTER_RANDO, "T", "1", "k", "r",
                null, emptySnapshot());
            fail("null before snapshot must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
    }

    // =========================================================================
    // PendingConcedeEvent: closed op/cause matrix + SET/CLEAR laws + outcome
    // =========================================================================

    @Test
    public void pendingConcedeSetRequiresInputsAndAfterValues() {
        PendingConcedeEvent set = PendingConcedeEvent.of(
            PendingConcedeEvent.Operation.SET_PENDING,
            PendingConcedeEvent.Cause.LOST_PILE_DEFICIT, "tester", 33, 2, 31,
            false, null, true, "Lost Pile deficit 31 (mine=33, opponent=2)");
        assertEquals(MutationOutcome.CHANGED, set.outcome());

        try {
            PendingConcedeEvent.of(PendingConcedeEvent.Operation.SET_PENDING,
                PendingConcedeEvent.Cause.NEW_GAME_RESET, "tester", 33, 2, 31,
                false, null, true, "reason");
            fail("SET_PENDING with a clear cause must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            PendingConcedeEvent.of(PendingConcedeEvent.Operation.SET_PENDING,
                PendingConcedeEvent.Cause.LOST_PILE_DEFICIT, "tester", null, null, null,
                false, null, true, "reason");
            fail("SET_PENDING without lost-pile inputs must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            PendingConcedeEvent.of(PendingConcedeEvent.Operation.SET_PENDING,
                PendingConcedeEvent.Cause.LOST_PILE_DEFICIT, "tester", 33, 2, 31,
                false, null, false, "reason");
            fail("SET_PENDING with pendingAfter=false must be rejected (SET requires an after value)");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            PendingConcedeEvent.of(PendingConcedeEvent.Operation.SET_PENDING,
                PendingConcedeEvent.Cause.LOST_PILE_DEFICIT, "tester", 33, 2, 31,
                false, null, true, null);
            fail("SET_PENDING with null reasonAfter must be rejected (SET requires an after value)");
        } catch (IllegalArgumentException expected) {
            // required
        }
    }

    @Test
    public void pendingConcedeClearRequiresAbsentAfterAndClearCause() {
        // post-player-lost clear: pending true -> false = CHANGED
        PendingConcedeEvent postCall = PendingConcedeEvent.of(
            PendingConcedeEvent.Operation.CLEAR_PENDING,
            PendingConcedeEvent.Cause.POST_PLAYER_LOST, "tester", null, null, null,
            true, "Lost Pile deficit 31 (mine=33, opponent=2)", false, null);
        assertEquals(MutationOutcome.CHANGED, postCall.outcome());

        // new-game clear with nothing pending: the write ran, nothing moved = NO_OP
        PendingConcedeEvent newGameNoOp = PendingConcedeEvent.of(
            PendingConcedeEvent.Operation.CLEAR_PENDING,
            PendingConcedeEvent.Cause.NEW_GAME_RESET, "tester", null, null, null,
            false, null, false, null);
        assertEquals(MutationOutcome.NO_OP, newGameNoOp.outcome());

        try {
            PendingConcedeEvent.of(PendingConcedeEvent.Operation.CLEAR_PENDING,
                PendingConcedeEvent.Cause.LOST_PILE_DEFICIT, "tester", null, null, null,
                true, "r", false, null);
            fail("CLEAR_PENDING with the SET cause must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            PendingConcedeEvent.of(PendingConcedeEvent.Operation.CLEAR_PENDING,
                PendingConcedeEvent.Cause.NEW_GAME_RESET, "tester", 33, 2, 31,
                true, "r", false, null);
            fail("CLEAR_PENDING with lost-pile inputs must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            PendingConcedeEvent.of(PendingConcedeEvent.Operation.CLEAR_PENDING,
                PendingConcedeEvent.Cause.NEW_GAME_RESET, "tester", null, null, null,
                true, "r", true, null);
            fail("CLEAR_PENDING with pendingAfter=true must be rejected (CLEAR requires an absent after value)");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            PendingConcedeEvent.of(PendingConcedeEvent.Operation.CLEAR_PENDING,
                PendingConcedeEvent.Cause.NEW_GAME_RESET, "tester", null, null, null,
                true, "r", false, "still here");
            fail("CLEAR_PENDING with a reasonAfter must be rejected (CLEAR requires an absent after value)");
        } catch (IllegalArgumentException expected) {
            // required
        }
    }

    @Test
    public void pendingConcedeRejectsInconsistentOutcome() {
        try {
            new PendingConcedeEvent(PendingConcedeEvent.Operation.CLEAR_PENDING,
                PendingConcedeEvent.Cause.NEW_GAME_RESET, "tester", null, null, null,
                false, null, false, null, MutationOutcome.CHANGED);
            fail("CHANGED with equal before/after must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            new PendingConcedeEvent(PendingConcedeEvent.Operation.CLEAR_PENDING,
                PendingConcedeEvent.Cause.POST_PLAYER_LOST, "tester", null, null, null,
                true, "r", false, null, MutationOutcome.NO_OP);
            fail("NO_OP with differing before/after must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
    }

    // =========================================================================
    // EnginePlayerLostEvent: distinct engine-call outcome, mandatory identity
    // =========================================================================

    @Test
    public void enginePlayerLostCarriesDistinctCallOutcome() {
        EnginePlayerLostEvent success = new EnginePlayerLostEvent(
            "tester", GameEndReason.LOSS__CONCEDED, EngineCallOutcome.SUCCESS);
        EnginePlayerLostEvent threw = new EnginePlayerLostEvent(
            "tester", GameEndReason.LOSS__CONCEDED, EngineCallOutcome.THREW);
        assertNotEquals(success, threw);

        try {
            new EnginePlayerLostEvent(null, GameEndReason.LOSS__CONCEDED, EngineCallOutcome.SUCCESS);
            fail("null playerId must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
        try {
            new EnginePlayerLostEvent("tester", null, EngineCallOutcome.SUCCESS);
            fail("null GameEndReason must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
        try {
            new EnginePlayerLostEvent("tester", GameEndReason.LOSS__CONCEDED, null);
            fail("null EngineCallOutcome must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
    }

    // =========================================================================
    // PendingDeployEvent: SET/CLEAR laws + outcome derivation
    // =========================================================================

    @Test
    public void pendingDeploySetAndClearLaws() {
        PendingDeployEvent set = PendingDeployEvent.of(
            PendingDeployEvent.Operation.SET, null, "character");
        assertEquals(MutationOutcome.CHANGED, set.outcome());

        // same-value rewrite: the write ran, the value did not move = real NO_OP SET
        PendingDeployEvent rewrite = PendingDeployEvent.of(
            PendingDeployEvent.Operation.SET, "starship", "starship");
        assertEquals(MutationOutcome.NO_OP, rewrite.outcome());

        PendingDeployEvent clear = PendingDeployEvent.of(
            PendingDeployEvent.Operation.CLEAR, "location", null);
        assertEquals(MutationOutcome.CHANGED, clear.outcome());

        try {
            PendingDeployEvent.of(PendingDeployEvent.Operation.SET, "vehicle", null);
            fail("SET without an after value must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            PendingDeployEvent.of(PendingDeployEvent.Operation.CLEAR, "vehicle", "vehicle");
            fail("CLEAR with an after value must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            new PendingDeployEvent(PendingDeployEvent.Operation.SET, null, "character",
                MutationOutcome.NO_OP);
            fail("NO_OP with differing before/after must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            new PendingDeployEvent(PendingDeployEvent.Operation.SET, "starship", "starship",
                MutationOutcome.CHANGED);
            fail("CHANGED with equal before/after must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
    }
}
