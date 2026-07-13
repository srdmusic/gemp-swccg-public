package com.gempukku.swccgo.ai.models.common.trace;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md):
 * pure construction, deep-copy, typed-id, and session-lifecycle tests for the V2
 * envelope. No production consumer, no server, no bot classes — common package only.
 */
public class DecisionTraceEnvelopeTest {

    private static final TraceFinalization EMPTY_FINALIZATION = new TraceFinalization(
        null, null, false, null, null, null, null, null, null, List.of(), null, false, false);

    private static TraceCaptureFailure failure(String detail) {
        return new TraceCaptureFailure(TraceCaptureFailure.Stage.SNAPSHOT, "test-failure", detail);
    }

    // =========================================================================
    // Deep immutability: defensive copies, not unmodifiable wrappers
    // =========================================================================

    @Test
    public void envelopeDefensivelyCopiesEveryCallerOwnedList() {
        List<TraceCaptureFailure> failures = new ArrayList<>(List.of(failure("f1")));
        List<String> raw = new ArrayList<>(Arrays.asList("A", "B"));
        List<String> merge = new ArrayList<>(Arrays.asList("B", "A"));
        List<TraceOperation> ops = new ArrayList<>();
        ops.add(new TraceOperation(0, TraceOp.INITIAL, 0, null, "A", "E1",
            TraceRuleId.LEGACY_UNTAGGED, null, null, null, null,
            Float.floatToRawIntBits(1.0f), false, null, "init"));
        List<TraceIntendedStateEvent> events = new ArrayList<>(List.of(
            new TraceIntendedStateEvent(TraceIntendedStateEvent.Kind.PENDING_CONCEDE, "test")));

        DecisionTrace trace = new DecisionTrace(DecisionTrace.SCHEMA_VERSION, "test-bot",
            "d1", "CARD_ACTION_CHOICE", "Choose action", null,
            TraceStatus.INCOMPLETE, failures, null, raw, merge, ops, EMPTY_FINALIZATION, events);

        // mutate every source list AFTER construction — the trace must not move
        failures.add(failure("f2"));
        raw.add("C");
        merge.clear();
        ops.clear();
        events.clear();

        assertEquals(1, trace.getCaptureFailures().size());
        assertEquals(Arrays.asList("A", "B"), trace.getRawCandidateOrder());
        assertEquals(Arrays.asList("B", "A"), trace.getMergeOrder());
        assertEquals(1, trace.getOperations().size());
        assertEquals(1, trace.getIntendedStateEvents().size());

        // and the exposed lists are unmodifiable
        try {
            trace.getRawCandidateOrder().add("Z");
            fail("rawCandidateOrder must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // required
        }
        try {
            trace.getOperations().clear();
            fail("operations must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // required
        }
    }

    @Test
    public void routeRecordAndFinalizationDefensivelyCopy() {
        List<String> evidence = new ArrayList<>(List.of("COMBINED_EVALUATOR: seam"));
        TraceRouteRecord route = new TraceRouteRecord(TraceRoute.COMBINED_EVALUATOR, evidence, null);
        evidence.add("tampered");
        assertEquals(1, route.orderedEvidence().size());

        List<TraceCorrection> corrections = new ArrayList<>(List.of(new TraceCorrection(
            TraceCorrection.Kind.SELECTABLE_CLAMP, "x", "y", "clamped")));
        TraceFinalization finalization = new TraceFinalization(null, null, false, null,
            null, null, null, null, null, corrections, "y", true, false);
        corrections.clear();
        assertEquals(1, finalization.corrections().size());
    }

    // =========================================================================
    // Status/failure consistency: no silent truncation, no phantom failures
    // =========================================================================

    @Test
    public void statusAndFailuresMustAgree() {
        try {
            new DecisionTrace(DecisionTrace.SCHEMA_VERSION, "test-bot", "d1", "T", "text", null,
                TraceStatus.COMPLETE, List.of(failure("f")), null,
                List.of(), List.of(), List.of(), EMPTY_FINALIZATION, List.of());
            fail("COMPLETE with capture failures must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            new DecisionTrace(DecisionTrace.SCHEMA_VERSION, "test-bot", "d1", "T", "text", null,
                TraceStatus.INCOMPLETE, List.of(), null,
                List.of(), List.of(), List.of(), EMPTY_FINALIZATION, List.of());
            fail("INCOMPLETE without capture failures must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
    }

    // =========================================================================
    // Typed identity: stable-id validation, prose rejected, LEGACY_UNTAGGED remains
    // =========================================================================

    @Test
    public void ruleIdValidatesStableRegistryForm() {
        assertEquals("V67bc", TraceRuleId.of("V67bc").id());
        assertEquals("V24.15-drain", TraceRuleId.of("V24.15-drain").id());
        assertEquals("FS-L1-abandon", TraceRuleId.of("FS-L1-abandon").id());
        assertEquals("vehicle-pilot+docking-bay", TraceRuleId.of("vehicle-pilot+docking-bay").id());
        assertEquals(TraceRuleId.LEGACY_UNTAGGED, TraceRuleId.of("LEGACY_UNTAGGED"));

        for (String prose : new String[]{
                "", "  ", "V148 (Steve, 2026-05-28): always offer Done",
                "reasoning text with spaces", "-leading-dash"}) {
            try {
                TraceRuleId.of(prose);
                fail("prose/blank must never validate as rule identity: \"" + prose + "\"");
            } catch (IllegalArgumentException expected) {
                // required
            }
        }
    }

    // =========================================================================
    // Session lifecycle: nested opens refused, no leaks, thread-local always cleared
    // =========================================================================

    private static TraceSnapshots.Result testSnapshot(String decisionId, List<String> actionIds) {
        TraceSnapshots.Input in = new TraceSnapshots.Input();
        in.decisionId = decisionId;
        in.decisionTypeName = "CARD_ACTION_CHOICE";
        in.decisionText = "Choose action";
        in.phase = Phase.DEPLOY;
        in.turn = 1;
        in.currentPlayer = "tester";
        in.side = Side.DARK;
        in.noPassParam = false;
        in.minParam = 0;
        in.maxParam = 1;
        in.actionIds = actionIds;
        return TraceSnapshots.build(in);
    }

    @Test
    public void nestedOpenIsRefusedAndOuterSessionSurvives() {
        TraceSnapshots.Result snap = testSnapshot("outer", List.of("A"));
        assertTrue(TraceSession.open("outer-bot", "outer", "CARD_ACTION_CHOICE", "Choose action",
            List.of("A"), snap.snapshot(), snap.issues(), false));
        try {
            assertFalse("nested open must be refused",
                TraceSession.open("inner-bot", "inner", "CARD_ACTION_CHOICE", "text",
                    List.of("B"), null, List.of("inner"), false));
            assertTrue(TraceSession.isActive());
            TraceSession.recordRoute(TraceRoute.COMBINED_EVALUATOR, "outer evidence", null);
        } finally {
            DecisionTrace trace = TraceSession.close();
            assertNotNull(trace);
            assertEquals("outer-bot", trace.getBotModel());
            assertEquals("outer", trace.getDecisionId());
            assertEquals(List.of("A"), trace.getRawCandidateOrder());
        }
        assertFalse("close must always clear the thread-local", TraceSession.isActive());
    }

    @Test
    public void closeWithoutOpenReturnsNullAndRecordCallsNoOp() {
        assertFalse(TraceSession.isActive());
        // every record call must be a safe no-op with no session open
        TraceSession.recordRoute(TraceRoute.HEURISTIC_FALLBACK, "no session", null);
        TraceSession.recordFinalResponse("x", false);
        TraceSession.recordInitial(new Object(), "A", 1.0f, TraceRuleId.LEGACY_UNTAGGED, null, null, "d");
        assertNull(TraceSession.close());
        assertFalse(TraceSession.isActive());
    }

    // =========================================================================
    // Scripted bot-boundary flow: route fallback chain + finalization capture
    // =========================================================================

    @Test
    public void scriptedRouteAndFinalResponseFlowIsCaptured() {
        TraceSnapshots.Result snap = testSnapshot("d9", List.of("5", "7"));
        assertTrue(TraceSession.open("bot", "d9", "CARD_ACTION_CHOICE", "Choose action",
            List.of("5", "7"), snap.snapshot(), snap.issues(), true));

        TraceSession.recordRoute(TraceRoute.HEURISTIC_FALLBACK, "no evaluator handled", null);
        TraceSession.recordRoute(TraceRoute.RAW_NOPASS_EMERGENCY, "empty result with noPass", null);
        TraceSession.recordEmergencyResponse("5", "Emergency: Choosing random action (5)");
        TraceSession.recordCorrection(TraceCorrection.Kind.SAFETY_FORCED_CHOICE, "", "5", "forced");
        TraceSession.recordIntendedStateEvent(
            TraceIntendedStateEvent.Kind.DECISION_TRACKER_RECORD, "recordDecision response='5'");
        TraceSession.recordFinalResponse("5", false);

        DecisionTrace trace = TraceSession.close();
        assertNotNull(trace);
        assertEquals(TraceStatus.COMPLETE, trace.getStatus());
        // selected route = the lane that produced the response; the earlier lane stays
        // in the ordered evidence and names the fall-through
        assertEquals(TraceRoute.RAW_NOPASS_EMERGENCY, trace.getRoute().selected());
        assertEquals(2, trace.getRoute().orderedEvidence().size());
        assertTrue(trace.getRoute().orderedEvidence().get(0).startsWith("HEURISTIC_FALLBACK"));
        assertEquals("fell through from HEURISTIC_FALLBACK", trace.getRoute().bypassOrFallbackReason());
        // finalization record complete
        assertEquals("5", trace.getFinalization().emergencyResponse());
        assertEquals(1, trace.getFinalization().corrections().size());
        assertEquals(TraceCorrection.Kind.SAFETY_FORCED_CHOICE,
            trace.getFinalization().corrections().get(0).kind());
        assertEquals("5", trace.getFinalization().finalResponse());
        assertTrue(trace.getFinalization().finalResponseRecorded());
        assertFalse(trace.getFinalization().skippedCommonFinalizer());
        // intended state events observed, never applied
        assertEquals(1, trace.getIntendedStateEvents().size());
        assertEquals(TraceIntendedStateEvent.Kind.DECISION_TRACKER_RECORD,
            trace.getIntendedStateEvents().get(0).kind());
    }

    @Test
    public void botBoundarySessionWithoutFinalResponseIsIncomplete() {
        TraceSnapshots.Result snap = testSnapshot("d10", List.of("A"));
        assertTrue(TraceSession.open("bot", "d10", "CARD_ACTION_CHOICE", "Choose action",
            List.of("A"), snap.snapshot(), snap.issues(), true));
        TraceSession.recordRoute(TraceRoute.COMBINED_EVALUATOR, "evidence", null);
        DecisionTrace trace = TraceSession.close();
        assertNotNull(trace);
        assertEquals(TraceStatus.INCOMPLETE, trace.getStatus());
        boolean finalizationFailure = false;
        for (TraceCaptureFailure f : trace.getCaptureFailures()) {
            if (f.stage() == TraceCaptureFailure.Stage.FINALIZATION) {
                finalizationFailure = true;
            }
        }
        assertTrue("missing final response must be a FINALIZATION capture failure",
            finalizationFailure);
    }

    @Test
    public void missingRouteObservationIsIncomplete() {
        TraceSnapshots.Result snap = testSnapshot("d11", List.of("A"));
        assertTrue(TraceSession.open("bot", "d11", "CARD_ACTION_CHOICE", "Choose action",
            List.of("A"), snap.snapshot(), snap.issues(), false));
        DecisionTrace trace = TraceSession.close();
        assertNotNull(trace);
        assertEquals(TraceStatus.INCOMPLETE, trace.getStatus());
        assertNull(trace.getRoute());
        boolean routeFailure = false;
        for (TraceCaptureFailure f : trace.getCaptureFailures()) {
            if (f.stage() == TraceCaptureFailure.Stage.ROUTE) {
                routeFailure = true;
            }
        }
        assertTrue("missing route must be a ROUTE capture failure", routeFailure);
    }

    // =========================================================================
    // Frozen input: parallel raw arrays of different lengths mark INCOMPLETE
    // =========================================================================

    @Test
    public void mismatchedParallelArraysMarkTraceIncomplete() {
        TraceSnapshots.Input in = new TraceSnapshots.Input();
        in.decisionId = "d12";
        in.decisionTypeName = "CARD_ACTION_CHOICE";
        in.decisionText = "Choose action";
        in.phase = Phase.DEPLOY;
        in.turn = 1;
        in.currentPlayer = "tester";
        in.side = Side.DARK;
        in.noPassParam = false;
        in.minParam = 0;
        in.maxParam = 1;
        in.actionIds = List.of("A", "B");
        in.actionTexts = List.of("Deploy A", "Deploy B", "Deploy GHOST");  // longer!
        TraceSnapshots.Result result = TraceSnapshots.build(in);

        assertNotNull("mismatch is retained in the snapshot, not rejected", result.snapshot());
        assertFalse("length mismatch must be reported", result.issues().isEmpty());
        // the mismatch is RETAINED: 3 rows, the ghost row has text but no id (no padding)
        assertEquals(3, result.snapshot().actionFacts().size());
        assertNull(result.snapshot().actionFacts().get(2).actionId());
        assertEquals("Deploy GHOST", result.snapshot().actionFacts().get(2).actionText());

        assertTrue(TraceSession.open("bot", "d12", "CARD_ACTION_CHOICE", "Choose action",
            List.of("A", "B"), result.snapshot(), result.issues(), false));
        TraceSession.recordRoute(TraceRoute.COMBINED_EVALUATOR, "evidence", null);
        DecisionTrace trace = TraceSession.close();
        assertEquals(TraceStatus.INCOMPLETE, trace.getStatus());
        assertEquals(TraceCaptureFailure.Stage.SNAPSHOT,
            trace.getCaptureFailures().get(0).stage());
    }
}
