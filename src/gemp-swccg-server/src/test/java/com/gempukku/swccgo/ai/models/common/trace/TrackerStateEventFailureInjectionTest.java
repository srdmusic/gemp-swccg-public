package com.gempukku.swccgo.ai.models.common.trace;

import com.gempukku.swccgo.ai.models.common.trace.state.DecisionTrackerLifecycleSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.DecisionTrackerPhaseSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.DecisionTrackerSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerClearEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerOwner;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TRACE STAGE 4A2b (Handoffs/CODEX_TRACE_STAGE4_4A2B_SHARED_TRACKER_PREFLIGHT_2026-07-13.md
 * "Gate additions") + the 4A2a gate's pinned fault-injection debt (m00434): every typed
 * tracker recording method must swallow BOTH failure modes (event CONSTRUCTION failure
 * inside the choke point and event APPEND failure from the collector) into a typed
 * STATE_EVENT capture failure on an INCOMPLETE envelope, never a throw into the
 * decision path and never a fabricated event. Append failure is injected through
 * Codex's TraceStateEventFailureTestSupport prepared collector (same-package
 * TraceSession.openForTesting seam; a throwing TraceSink is NOT this fixture because
 * sinks receive only finalized envelopes). The companion bot-boundary fixtures in
 * RandoCalAiTraceHookTest / TheChosenOneAiTraceHookTest prove the legacy mutators still
 * run exactly once under the same injection.
 */
public class TrackerStateEventFailureInjectionTest {

    private static DecisionTrackerSnapshot emptyDecisionState() {
        return new DecisionTrackerSnapshot(List.of(), 0, 0, "", "", "", 0, List.of());
    }

    private static DecisionTrackerLifecycleSnapshot lifecycle() {
        return new DecisionTrackerLifecycleSnapshot(emptyDecisionState(), 1, "3:4:20:1:7");
    }

    private static DecisionTrackerPhaseSnapshot phase(String lastPhase) {
        return new DecisionTrackerPhaseSnapshot(emptyDecisionState(), lastPhase);
    }

    /** Every tracker family's recording method, called once with VALID payloads. */
    private static void recordOneValidEventPerTrackerFamily() {
        TraceSession.recordTrackerRecordResponse(TrackerOwner.HEURISTIC_SHARED,
            "CARD_SELECTION", "9", "CARD_SELECTION:Choose target", "",
            emptyDecisionState(), emptyDecisionState());
        TraceSession.recordTrackerUpdateState(TrackerOwner.HEURISTIC_SHARED,
            3, 4, 20, 1, 7, lifecycle(), lifecycle());
        TraceSession.recordTrackerClear(TrackerOwner.OUTER_RANDO,
            TrackerClearEvent.ClearCause.NEW_GAME_RESET, lifecycle(), lifecycle());
        TraceSession.recordTrackerPhaseChange(TrackerOwner.HEURISTIC_SHARED,
            "DEPLOY", phase(""), phase("DEPLOY"));
        TraceSession.recordTrackerBlockResponse(TrackerOwner.HEURISTIC_SHARED,
            "CARD_SELECTION", "Choose target, or click Done to cancel", false,
            emptyDecisionState(), emptyDecisionState());
    }

    /** Every tracker family's recording method, called once with payloads whose EVENT
     *  CONSTRUCTION throws inside the choke point (null snapshots / contradictions). */
    private static void recordOneConstructionFailurePerTrackerFamily() {
        TraceSession.recordTrackerRecordResponse(TrackerOwner.HEURISTIC_SHARED,
            "CARD_SELECTION", "9", "CARD_SELECTION:Choose target", "",
            null, emptyDecisionState());
        TraceSession.recordTrackerUpdateState(TrackerOwner.HEURISTIC_SHARED,
            3, 4, 20, 1, 7, null, lifecycle());
        TraceSession.recordTrackerClear(TrackerOwner.HEURISTIC_SHARED,  // rejected owner
            TrackerClearEvent.ClearCause.NEW_GAME_RESET, lifecycle(), lifecycle());
        TraceSession.recordTrackerPhaseChange(TrackerOwner.HEURISTIC_SHARED,
            "BATTLE", phase(""), phase("DEPLOY"));  // after.lastPhase contradicts phase
        TraceSession.recordTrackerBlockResponse(TrackerOwner.HEURISTIC_SHARED,
            "CARD_SELECTION", "Choose target, or click Done to cancel", true,  // true+NO_OP
            emptyDecisionState(), emptyDecisionState());
    }

    // =========================================================================
    // Append failure: injected throwing collector, one attempt per tracker family
    // =========================================================================

    @Test
    public void appendFailureIsTypedPerFamilyAndNeverThrowsIntoTheDecisionPath() {
        TraceStateEventFailureTestSupport.openThrowingStateEventSession();
        try {
            recordOneValidEventPerTrackerFamily();  // must not throw

            DecisionTrace trace = TraceStateEventFailureTestSupport.close();
            assertNotNull(trace);
            assertEquals(TraceStatus.INCOMPLETE, trace.getStatus());
            assertTrue("no event may survive an append failure", trace.getStateEvents().isEmpty());
            long injected = trace.getCaptureFailures().stream().filter(failure ->
                failure.stage() == TraceCaptureFailure.Stage.STATE_EVENT
                    && failure.detail().contains("injected state-event append failure")).count();
            assertEquals("one typed STATE_EVENT failure per attempted append", 5, injected);
            assertFalse(TraceSession.isActive());
        } finally {
            TraceSession.abandon();
        }
    }

    // =========================================================================
    // Construction failure: bad payloads at the choke point, real collector
    // =========================================================================

    @Test
    public void constructionFailureIsTypedPerFamilyAndNeverThrowsIntoTheDecisionPath() {
        assertTrue(TraceSession.open("test-bot", "1", "CARD_SELECTION",
            "Choose target, or click Done to cancel", List.of(), null,
            List.of("test: snapshot deliberately absent"), false));
        try {
            recordOneConstructionFailurePerTrackerFamily();  // must not throw
            recordOneValidEventPerTrackerFamily();           // the channel still works after failures

            DecisionTrace trace = TraceSession.close();
            assertNotNull(trace);
            assertEquals(TraceStatus.INCOMPLETE, trace.getStatus());
            assertEquals("the five valid events after the five failures must all land",
                5, trace.getStateEvents().size());
            long stateEventFailures = trace.getCaptureFailures().stream().filter(failure ->
                failure.stage() == TraceCaptureFailure.Stage.STATE_EVENT).count();
            assertEquals("one typed STATE_EVENT failure per failed construction",
                5, stateEventFailures);
            assertFalse(TraceSession.isActive());
        } finally {
            TraceSession.abandon();
        }
    }
}
