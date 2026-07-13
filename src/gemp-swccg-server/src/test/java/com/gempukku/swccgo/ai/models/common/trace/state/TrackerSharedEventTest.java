package com.gempukku.swccgo.ai.models.common.trace.state;

import org.junit.Test;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * TRACE STAGE 4A2b (Handoffs/CODEX_TRACE_STAGE4_4A2B_SHARED_TRACKER_PREFLIGHT_2026-07-13.md
 * "Authorized implementation shape", pure common-state tests): constructor, invariant,
 * immutability, and outcome-derivation tests for the inherited shared-tracker records
 * (DecisionTrackerPhaseSnapshot, TrackerPhaseChangeEvent, and TrackerBlockResponseEvent)
 * plus the intentionally expanded HEURISTIC_SHARED owner on the reused records. No
 * server, no bots, no game state.
 */
public class TrackerSharedEventTest {

    private static DecisionTrackerSnapshot emptyDecisionState() {
        return new DecisionTrackerSnapshot(List.of(), 0, 0, "", "", "", 0, List.of());
    }

    private static DecisionTrackerSnapshot oneRowDecisionState() {
        return new DecisionTrackerSnapshot(
            List.of(new DecisionTrackerSnapshot.TrackerSequenceRow(
                "CARD_ACTION_CHOICE:Choose action", "5", "3:4:20:1:7")),
            0, 0, "CARD_ACTION_CHOICE:Choose action", "5", "", 0, List.of());
    }

    private static DecisionTrackerSnapshot blockedDecisionState() {
        // what a successful block leaves behind: last-action pair cleared, turn-block row written
        return new DecisionTrackerSnapshot(
            List.of(new DecisionTrackerSnapshot.TrackerSequenceRow(
                "CARD_ACTION_CHOICE:Choose action", "5", "3:4:20:1:7")),
            0, 0, "", "", "", 0,
            List.of(new DecisionTrackerSnapshot.TrackerTurnBlockRow(
                "CARD_ACTION_CHOICE:Choose action", List.of("5"))));
    }

    private static DecisionTrackerPhaseSnapshot pristinePhase() {
        return new DecisionTrackerPhaseSnapshot(emptyDecisionState(), "");
    }

    private static DecisionTrackerPhaseSnapshot deployPhase() {
        return new DecisionTrackerPhaseSnapshot(emptyDecisionState(), "DEPLOY");
    }

    // =========================================================================
    // DecisionTrackerPhaseSnapshot: null rejection, immutability, exact shape
    // =========================================================================

    @Test
    public void phaseSnapshotRejectsNullsAndKeepsNestedStateImmutable() {
        try {
            new DecisionTrackerPhaseSnapshot(null, "");
            fail("null decisionState must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
        try {
            new DecisionTrackerPhaseSnapshot(emptyDecisionState(), null);
            fail("null lastPhase must be rejected");
        } catch (NullPointerException expected) {
            // required
        }

        // the nested decision-affecting state stays the immutable 4A1 snapshot
        List<DecisionTrackerSnapshot.TrackerSequenceRow> rows = new ArrayList<>();
        rows.add(new DecisionTrackerSnapshot.TrackerSequenceRow("k1", "r1", "h1"));
        DecisionTrackerPhaseSnapshot snapshot = new DecisionTrackerPhaseSnapshot(
            new DecisionTrackerSnapshot(rows, 0, 0, "", "", "", 0, List.of()), "DEPLOY");
        rows.clear();  // caller-owned list mutated after construction; snapshot must not move
        assertEquals(1, snapshot.decisionState().sequenceRows().size());
        try {
            snapshot.decisionState().sequenceRows().clear();
            fail("nested sequenceRows must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // required
        }

        // value equality over all components
        assertEquals(pristinePhase(), pristinePhase());
        assertEquals(deployPhase(), deployPhase());
    }

    @Test
    public void phaseSnapshotExcludesLifecycleFieldsByOwnership() {
        // The packet's PHASE_CHANGE payload: decision state + exact lastPhase ONLY.
        // lastTurn and lastStateHash are updateState-owned (lifecycle snapshot) and
        // must not be claimed here: the mirror of the 4A2a lastPhase exclusion.
        RecordComponent[] components = DecisionTrackerPhaseSnapshot.class.getRecordComponents();
        assertEquals(2, components.length);
        assertEquals("decisionState", components[0].getName());
        assertEquals("lastPhase", components[1].getName());
    }

    // =========================================================================
    // TrackerPhaseChangeEvent: outcome from equality INCLUDING lastPhase
    // =========================================================================

    @Test
    public void phaseChangeEventDerivesOutcomeFromPhaseSnapshotEquality() {
        // repeated phase: the legacy call ran and left the owner untouched
        TrackerPhaseChangeEvent noOp = TrackerPhaseChangeEvent.of(
            TrackerOwner.HEURISTIC_SHARED, "DEPLOY", deployPhase(), deployPhase());
        assertEquals(MutationOutcome.NO_OP, noOp.outcome());

        // changed phase: lastPhase written (sequence/repeat resets ride the nested state)
        TrackerPhaseChangeEvent changed = TrackerPhaseChangeEvent.of(
            TrackerOwner.HEURISTIC_SHARED, "DEPLOY",
            new DecisionTrackerPhaseSnapshot(oneRowDecisionState(), "ACTIVATE"),
            deployPhase());
        assertEquals(MutationOutcome.CHANGED, changed.outcome());

        // lastPhase alone flips the outcome even with identical nested decision state
        TrackerPhaseChangeEvent phaseOnly = TrackerPhaseChangeEvent.of(
            TrackerOwner.HEURISTIC_SHARED, "DEPLOY", pristinePhase(), deployPhase());
        assertEquals(MutationOutcome.CHANGED, phaseOnly.outcome());
    }

    @Test
    public void phaseChangeEventRejectsContradictionsAndForeignOwnersAndNulls() {
        // m00441: after.lastPhase must equal the phase argument: the legacy call
        // always leaves lastPhase equal to its argument, on BOTH outcomes
        try {
            TrackerPhaseChangeEvent.of(TrackerOwner.HEURISTIC_SHARED, "BATTLE",
                pristinePhase(), deployPhase());
            fail("after.lastPhase != phase argument must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            new TrackerPhaseChangeEvent(TrackerOwner.HEURISTIC_SHARED, "DEPLOY",
                deployPhase(), deployPhase(), MutationOutcome.CHANGED);
            fail("CHANGED with equal phase snapshots must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            new TrackerPhaseChangeEvent(TrackerOwner.HEURISTIC_SHARED, "DEPLOY",
                pristinePhase(), deployPhase(), MutationOutcome.NO_OP);
            fail("NO_OP with differing phase snapshots must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        // owner is FIXED to the inherited shared tracker: no outer tracker has a
        // reachable onPhaseChange(...) call
        try {
            TrackerPhaseChangeEvent.of(TrackerOwner.OUTER_RANDO, "DEPLOY",
                pristinePhase(), deployPhase());
            fail("OUTER_RANDO owner must be rejected (no reachable outer onPhaseChange)");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            TrackerPhaseChangeEvent.of(TrackerOwner.OUTER_CHOSENONE, "DEPLOY",
                pristinePhase(), deployPhase());
            fail("OUTER_CHOSENONE owner must be rejected (no reachable outer onPhaseChange)");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            TrackerPhaseChangeEvent.of(TrackerOwner.HEURISTIC_SHARED, null,
                pristinePhase(), deployPhase());
            fail("null phase must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
        try {
            TrackerPhaseChangeEvent.of(TrackerOwner.HEURISTIC_SHARED, "DEPLOY",
                null, deployPhase());
            fail("null before snapshot must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
    }

    // =========================================================================
    // TrackerBlockResponseEvent: return/outcome biconditional + fixed owner
    // =========================================================================

    @Test
    public void blockResponseEventDerivesOutcomeAndHoldsTheReturnBiconditional() {
        // successful block: true return with the mutated canonical snapshot
        TrackerBlockResponseEvent blocked = TrackerBlockResponseEvent.of(
            TrackerOwner.HEURISTIC_SHARED, "CARD_SELECTION",
            "Choose target, or click Done to cancel", true,
            oneRowDecisionState(), blockedDecisionState());
        assertEquals(MutationOutcome.CHANGED, blocked.outcome());

        // ineligible or absent-last-action call: false return, nothing moved
        TrackerBlockResponseEvent declined = TrackerBlockResponseEvent.of(
            TrackerOwner.HEURISTIC_SHARED, "CARD_SELECTION",
            "Choose target, or click Done to cancel", false,
            emptyDecisionState(), emptyDecisionState());
        assertEquals(MutationOutcome.NO_OP, declined.outcome());
    }

    @Test
    public void blockResponseEventRejectsContradictionsAndForeignOwnersAndNulls() {
        // m00441 biconditional, direction 1: a true return can never pair with NO_OP
        try {
            TrackerBlockResponseEvent.of(TrackerOwner.HEURISTIC_SHARED, "CARD_SELECTION",
                "Choose target, or click Done to cancel", true,
                emptyDecisionState(), emptyDecisionState());
            fail("true return with unchanged snapshots must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        // m00441 biconditional, direction 2: every false return exits before any
        // mutation, so false can never pair with CHANGED
        try {
            TrackerBlockResponseEvent.of(TrackerOwner.HEURISTIC_SHARED, "CARD_SELECTION",
                "Choose target, or click Done to cancel", false,
                oneRowDecisionState(), blockedDecisionState());
            fail("false return with changed snapshots must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            new TrackerBlockResponseEvent(TrackerOwner.HEURISTIC_SHARED, "CARD_SELECTION",
                "Choose target, or click Done to cancel", false,
                emptyDecisionState(), emptyDecisionState(), MutationOutcome.CHANGED);
            fail("CHANGED with equal snapshots must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            new TrackerBlockResponseEvent(TrackerOwner.HEURISTIC_SHARED, "CARD_SELECTION",
                "Choose target, or click Done to cancel", true,
                oneRowDecisionState(), blockedDecisionState(), MutationOutcome.NO_OP);
            fail("NO_OP with differing snapshots must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        // owner is FIXED to the inherited shared tracker: no outer tracker has a
        // reachable external blockLastActionOnCancel(...) call
        try {
            TrackerBlockResponseEvent.of(TrackerOwner.OUTER_RANDO, "CARD_SELECTION",
                "Choose target, or click Done to cancel", false,
                emptyDecisionState(), emptyDecisionState());
            fail("OUTER_RANDO owner must be rejected (no reachable outer cancel block)");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            TrackerBlockResponseEvent.of(TrackerOwner.HEURISTIC_SHARED, null,
                "Choose target, or click Done to cancel", false,
                emptyDecisionState(), emptyDecisionState());
            fail("null decisionType must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
        try {
            TrackerBlockResponseEvent.of(TrackerOwner.HEURISTIC_SHARED, "CARD_SELECTION",
                null, false, emptyDecisionState(), emptyDecisionState());
            fail("null decisionText must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
    }

    // =========================================================================
    // Intentionally expanded owner invariant on the REUSED records (this slice)
    // =========================================================================

    @Test
    public void reusedRecordsNowAcceptTheSharedOwnerWhileClearStillRejectsIt() {
        // 4A2b intentionally expands the 4A1/4A2a owner invariant: the shared
        // tracker's updateState and recordDecision calls reuse the accepted records
        DecisionTrackerLifecycleSnapshot lifecycle = new DecisionTrackerLifecycleSnapshot(
            emptyDecisionState(), 1, "3:4:20:1:7");
        TrackerUpdateStateEvent sharedUpdate = TrackerUpdateStateEvent.of(
            TrackerOwner.HEURISTIC_SHARED, 3, 4, 20, 1, 7, lifecycle, lifecycle);
        assertEquals(TrackerOwner.HEURISTIC_SHARED, sharedUpdate.owner());
        assertEquals(MutationOutcome.NO_OP, sharedUpdate.outcome());

        TrackerRecordResponseEvent sharedRecord = TrackerRecordResponseEvent.of(
            TrackerOwner.HEURISTIC_SHARED, "CARD_ACTION_CHOICE", "42",
            "CARD_ACTION_CHOICE:Choose action", "5",
            emptyDecisionState(), oneRowDecisionState());
        assertEquals(TrackerOwner.HEURISTIC_SHARED, sharedRecord.owner());
        assertEquals(MutationOutcome.CHANGED, sharedRecord.outcome());

        // CLEAR keeps rejecting the shared owner: there is no reachable shared clear()
        try {
            TrackerClearEvent.of(TrackerOwner.HEURISTIC_SHARED,
                TrackerClearEvent.ClearCause.NEW_GAME_RESET,
                lifecycle, lifecycle);
            fail("HEURISTIC_SHARED owner must stay rejected on CLEAR (no shared clear call)");
        } catch (IllegalArgumentException expected) {
            // required
        }
    }
}
