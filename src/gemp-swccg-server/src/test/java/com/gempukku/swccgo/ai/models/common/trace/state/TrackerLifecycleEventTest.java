package com.gempukku.swccgo.ai.models.common.trace.state;

import org.junit.Test;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * TRACE STAGE 4A2a (Handoffs/CODEX_TRACE_STAGE4_4A2A_OUTER_TRACKER_LIFECYCLE_2026-07-13.md
 * "Acceptance fixtures", pure common-state tests): constructor, invariant, immutability,
 * and outcome-derivation tests for the outer tracker lifecycle records. No server, no
 * bots, no game state.
 */
public class TrackerLifecycleEventTest {

    private static DecisionTrackerSnapshot emptyDecisionState() {
        return new DecisionTrackerSnapshot(List.of(), 0, 0, "", "", "", 0, List.of());
    }

    private static DecisionTrackerSnapshot oneRowDecisionState() {
        return new DecisionTrackerSnapshot(
            List.of(new DecisionTrackerSnapshot.TrackerSequenceRow(
                "CARD_ACTION_CHOICE:Choose action", "5", "3:4:20:1:7")),
            0, 0, "CARD_ACTION_CHOICE:Choose action", "5", "", 0, List.of());
    }

    private static DecisionTrackerLifecycleSnapshot pristine() {
        return new DecisionTrackerLifecycleSnapshot(emptyDecisionState(), 0, "");
    }

    private static DecisionTrackerLifecycleSnapshot turnOne() {
        return new DecisionTrackerLifecycleSnapshot(emptyDecisionState(), 1, "3:4:20:1:7");
    }

    // =========================================================================
    // DecisionTrackerLifecycleSnapshot: null rejection, immutability, exact shape
    // =========================================================================

    @Test
    public void lifecycleSnapshotRejectsNullsAndKeepsNestedStateImmutable() {
        try {
            new DecisionTrackerLifecycleSnapshot(null, 0, "");
            fail("null decisionState must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
        try {
            new DecisionTrackerLifecycleSnapshot(emptyDecisionState(), 0, null);
            fail("null lastStateHash must be rejected");
        } catch (NullPointerException expected) {
            // required
        }

        // the nested decision-affecting state stays the immutable 4A1 snapshot
        List<DecisionTrackerSnapshot.TrackerSequenceRow> rows = new ArrayList<>();
        rows.add(new DecisionTrackerSnapshot.TrackerSequenceRow("k1", "r1", "h1"));
        DecisionTrackerLifecycleSnapshot lifecycle = new DecisionTrackerLifecycleSnapshot(
            new DecisionTrackerSnapshot(rows, 0, 0, "", "", "", 0, List.of()), 1, "h1");
        rows.clear();  // caller-owned list mutated after construction; snapshot must not move
        assertEquals(1, lifecycle.decisionState().sequenceRows().size());
        try {
            lifecycle.decisionState().sequenceRows().clear();
            fail("nested sequenceRows must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // required
        }

        // value equality over all components
        assertEquals(pristine(), pristine());
        assertEquals(turnOne(), turnOne());
    }

    @Test
    public void lifecycleSnapshotExcludesLastPhaseByAcceptedCorrection() {
        // The accepted preflight correction: the outer lifecycle snapshot carries the
        // nested decision state plus lastTurn and lastStateHash ONLY. lastPhase is
        // onPhaseChange-owned (inherited HeuristicAiBase path, 4A2b) and must not be
        // claimed here.
        RecordComponent[] components = DecisionTrackerLifecycleSnapshot.class.getRecordComponents();
        assertEquals(3, components.length);
        assertEquals("decisionState", components[0].getName());
        assertEquals("lastTurn", components[1].getName());
        assertEquals("lastStateHash", components[2].getName());
    }

    // =========================================================================
    // TrackerUpdateStateEvent: outcome from exact lifecycle-snapshot equality
    // =========================================================================

    @Test
    public void updateStateEventDerivesOutcomeFromLifecycleEquality() {
        // identical lifecycle snapshots: the legacy call ran and nothing moved
        TrackerUpdateStateEvent noOp = TrackerUpdateStateEvent.of(
            TrackerOwner.OUTER_RANDO, 3, 4, 20, 1, 7, turnOne(), turnOne());
        assertEquals(MutationOutcome.NO_OP, noOp.outcome());

        // only the lifecycle-owned fields differ (same nested decision state)
        TrackerUpdateStateEvent hashChanged = TrackerUpdateStateEvent.of(
            TrackerOwner.OUTER_CHOSENONE, 4, 4, 20, 1, 7, turnOne(),
            new DecisionTrackerLifecycleSnapshot(emptyDecisionState(), 1, "4:4:20:1:7"));
        assertEquals(MutationOutcome.CHANGED, hashChanged.outcome());
        TrackerUpdateStateEvent turnChanged = TrackerUpdateStateEvent.of(
            TrackerOwner.OUTER_RANDO, 3, 4, 20, 2, 7, turnOne(),
            new DecisionTrackerLifecycleSnapshot(emptyDecisionState(), 2, "3:4:20:2:7"));
        assertEquals(MutationOutcome.CHANGED, turnChanged.outcome());

        // nested decision-state change alone also flips the outcome
        TrackerUpdateStateEvent nestedChanged = TrackerUpdateStateEvent.of(
            TrackerOwner.OUTER_RANDO, 3, 4, 20, 1, 7,
            new DecisionTrackerLifecycleSnapshot(emptyDecisionState(), 1, "3:4:20:1:7"),
            new DecisionTrackerLifecycleSnapshot(oneRowDecisionState(), 1, "3:4:20:1:7"));
        assertEquals(MutationOutcome.CHANGED, nestedChanged.outcome());
    }

    // =========================================================================
    // TrackerClearEvent: outcome derivation + closed NEW_GAME_RESET cause
    // =========================================================================

    @Test
    public void clearEventDerivesOutcomeFromLifecycleEquality() {
        // populated tracker cleared back to pristine
        TrackerClearEvent changed = TrackerClearEvent.of(TrackerOwner.OUTER_RANDO,
            TrackerClearEvent.ClearCause.NEW_GAME_RESET,
            new DecisionTrackerLifecycleSnapshot(oneRowDecisionState(), 1, "3:4:20:1:7"),
            pristine());
        assertEquals(MutationOutcome.CHANGED, changed.outcome());

        // fresh tracker: the unconditional new-game clear had nothing to erase
        TrackerClearEvent noOp = TrackerClearEvent.of(TrackerOwner.OUTER_CHOSENONE,
            TrackerClearEvent.ClearCause.NEW_GAME_RESET, pristine(), pristine());
        assertEquals(MutationOutcome.NO_OP, noOp.outcome());
    }

    @Test
    public void clearCauseIsClosedToNewGameReset() {
        // the only reachable outer clear() call site is the new-game reset
        assertEquals(1, TrackerClearEvent.ClearCause.values().length);
        assertEquals(TrackerClearEvent.ClearCause.NEW_GAME_RESET,
            TrackerClearEvent.ClearCause.values()[0]);
        try {
            TrackerClearEvent.of(TrackerOwner.OUTER_RANDO, null, pristine(), pristine());
            fail("null ClearCause must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
    }

    // =========================================================================
    // Both events: inconsistent outcomes rejected; UPDATE_STATE accepts the 4A2b
    // expanded shared owner while CLEAR keeps rejecting it (no shared clear call)
    // =========================================================================

    @Test
    public void eventsRejectInconsistentOutcomeAndHeuristicOwnerAndNulls() {
        try {
            new TrackerUpdateStateEvent(TrackerOwner.OUTER_RANDO, 3, 4, 20, 1, 7,
                turnOne(), turnOne(), MutationOutcome.CHANGED);
            fail("CHANGED with equal lifecycle snapshots must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            new TrackerUpdateStateEvent(TrackerOwner.OUTER_RANDO, 3, 4, 20, 1, 7,
                pristine(), turnOne(), MutationOutcome.NO_OP);
            fail("NO_OP with differing lifecycle snapshots must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            new TrackerClearEvent(TrackerOwner.OUTER_RANDO,
                TrackerClearEvent.ClearCause.NEW_GAME_RESET,
                pristine(), pristine(), MutationOutcome.CHANGED);
            fail("CHANGED with equal lifecycle snapshots must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            new TrackerClearEvent(TrackerOwner.OUTER_RANDO,
                TrackerClearEvent.ClearCause.NEW_GAME_RESET,
                turnOne(), pristine(), MutationOutcome.NO_OP);
            fail("NO_OP with differing lifecycle snapshots must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        // TRACE STAGE 4A2b intentionally expands the 4A2a owner invariant: the shared
        // updateState call reuses this record with HEURISTIC_SHARED
        TrackerUpdateStateEvent sharedUpdate = TrackerUpdateStateEvent.of(
            TrackerOwner.HEURISTIC_SHARED, 3, 4, 20, 1, 7, turnOne(), turnOne());
        assertEquals(TrackerOwner.HEURISTIC_SHARED, sharedUpdate.owner());
        try {
            TrackerClearEvent.of(TrackerOwner.HEURISTIC_SHARED,
                TrackerClearEvent.ClearCause.NEW_GAME_RESET, pristine(), pristine());
            fail("HEURISTIC_SHARED owner must stay rejected on CLEAR (no shared clear call)");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            TrackerUpdateStateEvent.of(TrackerOwner.OUTER_RANDO, 3, 4, 20, 1, 7,
                null, turnOne());
            fail("null before snapshot must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
        try {
            TrackerClearEvent.of(TrackerOwner.OUTER_RANDO,
                TrackerClearEvent.ClearCause.NEW_GAME_RESET, pristine(), null);
            fail("null after snapshot must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
    }
}
