package com.gempukku.swccgo.ai.models.chosenone;

import com.gempukku.swccgo.ai.models.common.trace.state.DecisionTrackerLifecycleSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.DecisionTrackerSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.MutationOutcome;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerClearEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerOwner;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerUpdateStateEvent;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TRACE STAGE 4A2a (Handoffs/CODEX_TRACE_STAGE4_4A2A_OUTER_TRACKER_LIFECYCLE_2026-07-13.md
 * "Acceptance fixtures", real tracker fixtures): the REAL outer DecisionTracker driven
 * through its legacy lifecycle mutators, observed through the pure package-local
 * traceLifecycleSnapshot() seam. Proves the lifecycle events describe exactly what the
 * existing owner does — no behavior change, snapshots read-only.
 *
 * The scripted canonical drive and its expected literal are IDENTICAL in the other
 * bot's mirror of this test (common snapshot types, owner constant normalized), so
 * passing both proves the Rando and ChosenOne snapshots/events match after package,
 * class, and owner normalization.
 */
public class DecisionTrackerLifecycleTraceTest {

    private static final String KEY_A = "CARD_ACTION_CHOICE:Choose action";
    private static final String KEY_B = "CARD_SELECTION:Choose card";
    private static final String KEY_C = "CARD_SELECTION:Choose target, or click Done to cancel";
    private static final String HASH_1 = "3:4:20:1:7";

    private static DecisionTrackerLifecycleSnapshot pristine() {
        return new DecisionTrackerLifecycleSnapshot(
            new DecisionTrackerSnapshot(List.of(), 0, 0, "", "", "", 0, List.of()), 0, "");
    }

    /** The canonical scripted drive: one updateState, a 2-decision loop repeated twice
     *  (crosses LOOP_RANDOMIZE_THRESHOLD, so the owner writes turn-scoped blocks), then
     *  one cancelable Done that starts a cancel streak. Populates EVERY lifecycle field. */
    private static DecisionTracker drivenTracker() {
        DecisionTracker tracker = new DecisionTracker();
        tracker.updateState(3, 4, 20, 1, 7);
        tracker.recordDecision("CARD_ACTION_CHOICE", "Choose action", "1", "5");
        tracker.recordDecision("CARD_SELECTION", "Choose card", "2", "7");
        tracker.recordDecision("CARD_ACTION_CHOICE", "Choose action", "3", "5");
        tracker.recordDecision("CARD_SELECTION", "Choose card", "4", "7");
        tracker.recordDecision("CARD_SELECTION", "Choose target, or click Done to cancel", "5", "");
        return tracker;
    }

    /** The exact lifecycle literal the canonical drive must leave behind — the SAME
     *  literal appears in the other bot's mirror (cross-bot snapshot parity). */
    private static DecisionTrackerLifecycleSnapshot canonicalDriveLiteral() {
        return new DecisionTrackerLifecycleSnapshot(
            new DecisionTrackerSnapshot(
                List.of(
                    new DecisionTrackerSnapshot.TrackerSequenceRow(KEY_A, "5", HASH_1),
                    new DecisionTrackerSnapshot.TrackerSequenceRow(KEY_B, "7", HASH_1),
                    new DecisionTrackerSnapshot.TrackerSequenceRow(KEY_A, "5", HASH_1),
                    new DecisionTrackerSnapshot.TrackerSequenceRow(KEY_B, "7", HASH_1)),
                2, 2, KEY_A, "5", KEY_C, 1,
                List.of(
                    new DecisionTrackerSnapshot.TrackerTurnBlockRow(KEY_A, List.of("5")),
                    new DecisionTrackerSnapshot.TrackerTurnBlockRow(KEY_B, List.of("7")))),
            1, HASH_1);
    }

    // =========================================================================
    // Identical repeated update produces NO_OP
    // =========================================================================

    @Test
    public void identicalRepeatedUpdateProducesNoOp() {
        DecisionTracker tracker = new DecisionTracker();
        tracker.updateState(3, 4, 20, 1, 7);
        DecisionTrackerLifecycleSnapshot before = tracker.traceLifecycleSnapshot();
        tracker.updateState(3, 4, 20, 1, 7);  // identical legacy call, nothing moves
        DecisionTrackerLifecycleSnapshot after = tracker.traceLifecycleSnapshot();

        assertEquals(before, after);
        TrackerUpdateStateEvent event = TrackerUpdateStateEvent.of(
            TrackerOwner.OUTER_CHOSENONE, 3, 4, 20, 1, 7, before, after);
        assertEquals(MutationOutcome.NO_OP, event.outcome());
        assertEquals(1, after.lastTurn());
        assertEquals(HASH_1, after.lastStateHash());
    }

    // =========================================================================
    // Changed state hash resets repeat/loop counts according to the existing owner
    // =========================================================================

    @Test
    public void changedStateHashResetsRepeatAndLoopCountsExactlyAsOwnerDoes() {
        DecisionTracker tracker = drivenTracker();
        DecisionTrackerLifecycleSnapshot before = tracker.traceLifecycleSnapshot();
        assertEquals(2, before.decisionState().sequenceRepeatCount());
        assertEquals(2, before.decisionState().detectedLoopLength());

        tracker.updateState(4, 4, 20, 1, 7);  // hand size moved: state hash changes, same turn
        DecisionTrackerLifecycleSnapshot after = tracker.traceLifecycleSnapshot();

        // the owner resets ONLY the repeat/loop counts; same-turn blocks survive
        assertEquals(0, after.decisionState().sequenceRepeatCount());
        assertEquals(0, after.decisionState().detectedLoopLength());
        assertEquals(before.decisionState().turnBlockRows(), after.decisionState().turnBlockRows());
        assertEquals(before.decisionState().sequenceRows(), after.decisionState().sequenceRows());
        assertEquals(1, after.lastTurn());
        assertEquals("4:4:20:1:7", after.lastStateHash());

        TrackerUpdateStateEvent event = TrackerUpdateStateEvent.of(
            TrackerOwner.OUTER_CHOSENONE, 4, 4, 20, 1, 7, before, after);
        assertEquals(MutationOutcome.CHANGED, event.outcome());
    }

    // =========================================================================
    // Turn change clears canonical turn-block rows
    // =========================================================================

    @Test
    public void turnChangeClearsCanonicalTurnBlockRows() {
        DecisionTracker tracker = new DecisionTracker();
        tracker.updateState(3, 4, 20, 1, 7);
        tracker.recordDecision("CARD_ACTION_CHOICE", "Choose action", "1", "5");
        assertTrue(tracker.blockLastActionOnCancel(
            "CARD_SELECTION", "Choose target, or click Done to cancel"));
        DecisionTrackerLifecycleSnapshot before = tracker.traceLifecycleSnapshot();
        assertEquals(1, before.decisionState().turnBlockRows().size());
        assertEquals(KEY_A, before.decisionState().turnBlockRows().get(0).decisionKey());

        tracker.updateState(3, 4, 20, 2, 7);  // turn 1 -> 2
        DecisionTrackerLifecycleSnapshot after = tracker.traceLifecycleSnapshot();

        assertEquals(0, after.decisionState().turnBlockRows().size());
        assertEquals(2, after.lastTurn());
        assertEquals("3:4:20:2:7", after.lastStateHash());
        TrackerUpdateStateEvent event = TrackerUpdateStateEvent.of(
            TrackerOwner.OUTER_CHOSENONE, 3, 4, 20, 2, 7, before, after);
        assertEquals(MutationOutcome.CHANGED, event.outcome());
    }

    // =========================================================================
    // New-game clear resets every field included in the lifecycle snapshot
    // =========================================================================

    @Test
    public void newGameClearResetsEveryLifecycleField() {
        DecisionTracker tracker = drivenTracker();
        DecisionTrackerLifecycleSnapshot before = tracker.traceLifecycleSnapshot();
        // every lifecycle field is populated before the clear
        assertFalse(before.decisionState().sequenceRows().isEmpty());
        assertTrue(before.decisionState().sequenceRepeatCount() > 0);
        assertTrue(before.decisionState().detectedLoopLength() > 0);
        assertFalse(before.decisionState().lastActionChoiceKey().isEmpty());
        assertFalse(before.decisionState().lastActionChoiceResponse().isEmpty());
        assertFalse(before.decisionState().consecutiveCancelKey().isEmpty());
        assertTrue(before.decisionState().consecutiveCancelCount() > 0);
        assertFalse(before.decisionState().turnBlockRows().isEmpty());
        assertTrue(before.lastTurn() != 0);
        assertFalse(before.lastStateHash().isEmpty());

        tracker.clear();
        DecisionTrackerLifecycleSnapshot after = tracker.traceLifecycleSnapshot();

        assertEquals(pristine(), after);
        TrackerClearEvent event = TrackerClearEvent.of(TrackerOwner.OUTER_CHOSENONE,
            TrackerClearEvent.ClearCause.NEW_GAME_RESET, before, after);
        assertEquals(MutationOutcome.CHANGED, event.outcome());

        // clearing the already-pristine tracker is a real NO_OP observation
        tracker.clear();
        TrackerClearEvent repeat = TrackerClearEvent.of(TrackerOwner.OUTER_CHOSENONE,
            TrackerClearEvent.ClearCause.NEW_GAME_RESET, after, tracker.traceLifecycleSnapshot());
        assertEquals(MutationOutcome.NO_OP, repeat.outcome());
    }

    // =========================================================================
    // Cross-bot parity: the canonical drive matches the shared literal exactly
    // =========================================================================

    @Test
    public void canonicalDriveMatchesTheCrossBotLiteral() {
        DecisionTracker tracker = drivenTracker();
        assertEquals(canonicalDriveLiteral(), tracker.traceLifecycleSnapshot());
    }
}
