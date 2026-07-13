package com.gempukku.swccgo.ai.models.rando;

import com.gempukku.swccgo.ai.models.common.trace.state.DecisionTrackerLifecycleSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.DecisionTrackerPhaseSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.DecisionTrackerSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.MutationOutcome;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerBlockResponseEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerOwner;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerPhaseChangeEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerRecordResponseEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerUpdateStateEvent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * TRACE STAGE 4A2b (Handoffs/CODEX_TRACE_STAGE4_4A2B_SHARED_TRACKER_PREFLIGHT_2026-07-13.md
 * "Acceptance fixtures", real tracker + bridge fixtures): the REAL Rando DecisionTracker
 * class (the class HeuristicAiBase inherits for BOTH bots) driven through its legacy
 * mutators and observed exclusively through the public read-only
 * DecisionTrackerTraceAccess bridge, exactly how the HeuristicAiBase hooks observe it.
 * Proves the shared-owner events describe exactly what the existing owner does and that
 * the bridge is pure delegation: no behavior change, snapshots read-only.
 */
public class DecisionTrackerSharedTraceTest {

    private static final String KEY_A = "CARD_ACTION_CHOICE:Choose action";
    private static final String KEY_C = "CARD_SELECTION:Choose target, or click Done to cancel";
    private static final String HASH_1 = "3:4:20:1:7";

    // =========================================================================
    // PHASE_CHANGE: changed phase resets sequence/repeat state; repeated is NO_OP
    // =========================================================================

    @Test
    public void phaseChangeResetsSequenceStateAndRepeatedPhaseIsNoOp() {
        DecisionTracker tracker = new DecisionTracker();
        tracker.updateState(3, 4, 20, 1, 7);
        // drive a 2-decision loop so sequence rows and repeat counts are populated
        tracker.recordDecision("CARD_ACTION_CHOICE", "Choose action", "1", "5");
        tracker.recordDecision("CARD_SELECTION", "Choose card", "2", "7");
        tracker.recordDecision("CARD_ACTION_CHOICE", "Choose action", "3", "5");
        tracker.recordDecision("CARD_SELECTION", "Choose card", "4", "7");

        DecisionTrackerPhaseSnapshot before = DecisionTrackerTraceAccess.phaseSnapshot(tracker);
        assertEquals("", before.lastPhase());
        assertEquals(4, before.decisionState().sequenceRows().size());
        assertEquals(2, before.decisionState().sequenceRepeatCount());
        assertEquals(2, before.decisionState().detectedLoopLength());

        tracker.onPhaseChange("DEPLOY");  // legacy call: phase moved, loop state resets
        DecisionTrackerPhaseSnapshot after = DecisionTrackerTraceAccess.phaseSnapshot(tracker);
        assertEquals("DEPLOY", after.lastPhase());
        assertEquals(0, after.decisionState().sequenceRows().size());
        assertEquals(0, after.decisionState().sequenceRepeatCount());
        assertEquals(0, after.decisionState().detectedLoopLength());
        // the last-action pair survives a phase change (only the loop state resets)
        assertEquals(KEY_A, after.decisionState().lastActionChoiceKey());
        assertEquals("5", after.decisionState().lastActionChoiceResponse());

        TrackerPhaseChangeEvent changed = TrackerPhaseChangeEvent.of(
            TrackerOwner.HEURISTIC_SHARED, "DEPLOY", before, after);
        assertEquals(MutationOutcome.CHANGED, changed.outcome());

        // repeated phase: the legacy call runs and leaves the owner untouched
        tracker.onPhaseChange("DEPLOY");
        TrackerPhaseChangeEvent repeated = TrackerPhaseChangeEvent.of(
            TrackerOwner.HEURISTIC_SHARED, "DEPLOY", after,
            DecisionTrackerTraceAccess.phaseSnapshot(tracker));
        assertEquals(MutationOutcome.NO_OP, repeated.outcome());
    }

    // =========================================================================
    // UPDATE_STATE (shared owner): identical args NO_OP; turn/hash change CHANGED
    // =========================================================================

    @Test
    public void sharedUpdateStateFollowsTheLifecycleSnapshotExactly() {
        DecisionTracker tracker = new DecisionTracker();
        tracker.updateState(3, 4, 20, 1, 7);
        DecisionTrackerLifecycleSnapshot before = DecisionTrackerTraceAccess.lifecycleSnapshot(tracker);

        tracker.updateState(3, 4, 20, 1, 7);  // identical legacy call, nothing moves
        TrackerUpdateStateEvent noOp = TrackerUpdateStateEvent.of(
            TrackerOwner.HEURISTIC_SHARED, 3, 4, 20, 1, 7, before,
            DecisionTrackerTraceAccess.lifecycleSnapshot(tracker));
        assertEquals(MutationOutcome.NO_OP, noOp.outcome());
        assertEquals(1, noOp.after().lastTurn());
        assertEquals(HASH_1, noOp.after().lastStateHash());

        tracker.updateState(3, 4, 20, 2, 7);  // turn moved: hash and lastTurn change
        TrackerUpdateStateEvent changed = TrackerUpdateStateEvent.of(
            TrackerOwner.HEURISTIC_SHARED, 3, 4, 20, 2, 7, noOp.after(),
            DecisionTrackerTraceAccess.lifecycleSnapshot(tracker));
        assertEquals(MutationOutcome.CHANGED, changed.outcome());
        assertEquals(2, changed.after().lastTurn());
        assertEquals("3:4:20:2:7", changed.after().lastStateHash());
    }

    // =========================================================================
    // BLOCK_RESPONSE: successful block true+CHANGED; ineligible/absent false+NO_OP
    // =========================================================================

    @Test
    public void cancelBlockReturnAndSnapshotDeltaMatchOnEveryPath() {
        DecisionTracker tracker = new DecisionTracker();
        tracker.updateState(3, 4, 20, 1, 7);

        // absent last action: eligible type/text but nothing to block: false, NO_OP
        DecisionTrackerSnapshot before = DecisionTrackerTraceAccess.decisionSnapshot(tracker);
        boolean absentReturn = tracker.blockLastActionOnCancel(
            "CARD_SELECTION", "Choose target, or click Done to cancel");
        assertFalse(absentReturn);
        TrackerBlockResponseEvent absent = TrackerBlockResponseEvent.of(
            TrackerOwner.HEURISTIC_SHARED, "CARD_SELECTION",
            "Choose target, or click Done to cancel", absentReturn, before,
            DecisionTrackerTraceAccess.decisionSnapshot(tracker));
        assertEquals(MutationOutcome.NO_OP, absent.outcome());

        // arm the last-action pair, then an INELIGIBLE decision type: false, NO_OP
        tracker.recordDecision("CARD_ACTION_CHOICE", "Choose action", "1", "5");
        DecisionTrackerSnapshot armed = DecisionTrackerTraceAccess.decisionSnapshot(tracker);
        assertEquals(KEY_A, armed.lastActionChoiceKey());
        boolean ineligibleReturn = tracker.blockLastActionOnCancel(
            "MULTIPLE_CHOICE", "Choose target, or click Done to cancel");
        assertFalse(ineligibleReturn);
        TrackerBlockResponseEvent ineligible = TrackerBlockResponseEvent.of(
            TrackerOwner.HEURISTIC_SHARED, "MULTIPLE_CHOICE",
            "Choose target, or click Done to cancel", ineligibleReturn, armed,
            DecisionTrackerTraceAccess.decisionSnapshot(tracker));
        assertEquals(MutationOutcome.NO_OP, ineligible.outcome());

        // successful block: true return and CHANGED; last-action pair cleared, turn-block row written
        boolean blockedReturn = tracker.blockLastActionOnCancel(
            "CARD_SELECTION", "Choose target, or click Done to cancel");
        assertTrue(blockedReturn);
        DecisionTrackerSnapshot afterBlock = DecisionTrackerTraceAccess.decisionSnapshot(tracker);
        TrackerBlockResponseEvent blocked = TrackerBlockResponseEvent.of(
            TrackerOwner.HEURISTIC_SHARED, "CARD_SELECTION",
            "Choose target, or click Done to cancel", blockedReturn, armed, afterBlock);
        assertEquals(MutationOutcome.CHANGED, blocked.outcome());
        assertEquals("", afterBlock.lastActionChoiceKey());
        assertEquals("", afterBlock.lastActionChoiceResponse());
        assertEquals(1, afterBlock.turnBlockRows().size());
        assertEquals(KEY_A, afterBlock.turnBlockRows().get(0).decisionKey());
        assertEquals("5", afterBlock.turnBlockRows().get(0).sortedResponses().get(0));
    }

    // =========================================================================
    // RECORD_RESPONSE (shared owner): exact seam key and tracking response
    // =========================================================================

    @Test
    public void sharedRecordResponseCarriesTheExactSeamKeyAndResponse() {
        DecisionTracker tracker = new DecisionTracker();
        tracker.updateState(3, 4, 20, 1, 7);
        DecisionTrackerSnapshot before = DecisionTrackerTraceAccess.decisionSnapshot(tracker);

        tracker.recordDecision("CARD_SELECTION",
            "Choose target, or click Done to cancel", "9", "");
        DecisionTrackerSnapshot after = DecisionTrackerTraceAccess.decisionSnapshot(tracker);

        String seamKey = DecisionTrackerTraceAccess.decisionKey(tracker,
            "CARD_SELECTION", "Choose target, or click Done to cancel");
        assertEquals(KEY_C, seamKey);
        TrackerRecordResponseEvent event = TrackerRecordResponseEvent.of(
            TrackerOwner.HEURISTIC_SHARED, "CARD_SELECTION", "9", seamKey, "", before, after);
        assertEquals(TrackerOwner.HEURISTIC_SHARED, event.owner());
        // the empty cancelable response started a cancel streak: a real CHANGED
        assertEquals(MutationOutcome.CHANGED, event.outcome());
        assertEquals(KEY_C, event.after().consecutiveCancelKey());
        assertEquals(1, event.after().consecutiveCancelCount());
    }

    // =========================================================================
    // The bridge is pure delegation: reading through it never moves the owner
    // =========================================================================

    @Test
    public void bridgeReadsArePureAndMatchThePackageLocalSeams() {
        DecisionTracker tracker = new DecisionTracker();
        tracker.updateState(3, 4, 20, 1, 7);
        tracker.recordDecision("CARD_ACTION_CHOICE", "Choose action", "1", "5");
        tracker.onPhaseChange("DEPLOY");

        DecisionTrackerSnapshot first = DecisionTrackerTraceAccess.decisionSnapshot(tracker);
        // every bridge read, repeatedly, mutates nothing
        DecisionTrackerTraceAccess.phaseSnapshot(tracker);
        DecisionTrackerTraceAccess.lifecycleSnapshot(tracker);
        DecisionTrackerTraceAccess.decisionKey(tracker, "CARD_ACTION_CHOICE", "Choose action");
        assertEquals(first, DecisionTrackerTraceAccess.decisionSnapshot(tracker));

        // the bridge returns exactly the package-local seam values
        assertEquals(tracker.traceSnapshot(), DecisionTrackerTraceAccess.decisionSnapshot(tracker));
        assertEquals(tracker.tracePhaseSnapshot(), DecisionTrackerTraceAccess.phaseSnapshot(tracker));
        assertEquals(tracker.traceLifecycleSnapshot(), DecisionTrackerTraceAccess.lifecycleSnapshot(tracker));
        assertEquals(tracker.traceDecisionKey("CARD_ACTION_CHOICE", "Choose action"),
            DecisionTrackerTraceAccess.decisionKey(tracker, "CARD_ACTION_CHOICE", "Choose action"));
        // the phase snapshot carries the phase the legacy call wrote
        assertEquals("DEPLOY", DecisionTrackerTraceAccess.phaseSnapshot(tracker).lastPhase());
    }
}
