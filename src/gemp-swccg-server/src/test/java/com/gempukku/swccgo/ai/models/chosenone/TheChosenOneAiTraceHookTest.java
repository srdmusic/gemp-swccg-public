package com.gempukku.swccgo.ai.models.chosenone;

import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceCaptureFailure;
import com.gempukku.swccgo.ai.models.common.trace.TraceCorrection;
import com.gempukku.swccgo.ai.models.common.trace.TraceRoute;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceStatus;
import com.gempukku.swccgo.ai.models.common.trace.TraceTestSupport;
import com.gempukku.swccgo.ai.models.common.trace.state.MutationOutcome;
import com.gempukku.swccgo.ai.models.common.trace.state.PendingDeployEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TraceStateEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerOwner;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerRecordResponseEvent;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
 * "Route record" + "Finalization record"): scripted-flow proof of the BOT-BOUNDARY
 * hooks — a real decide() call records a direct-interceptor route with its final
 * response and skipped-finalizer flag, and DecisionSafety records typed corrections.
 * No server, no game state (null GameState exercises the null-safe interceptor path).
 *
 * NOTE (increment scope): with no GameState the bot's Side is unknown, so the shadow
 * DecisionSnapshot cannot be constructed and the envelope is honestly INCOMPLETE with a
 * SNAPSHOT-stage failure. Full COMPLETE bot-entry fixtures over a real game state are
 * the contract's landing increment 5.
 */
public class TheChosenOneAiTraceHookTest {

    /** Minimal scripted AwaitingDecision; decisionMade is never called by the AI. */
    private static AwaitingDecision decision(int id, AwaitingDecisionType type, String text,
                                             Map<String, String[]> params) {
        return new AwaitingDecision() {
            @Override
            public int getAwaitingDecisionId() {
                return id;
            }

            @Override
            public String getText() {
                return text;
            }

            @Override
            public AwaitingDecisionType getDecisionType() {
                return type;
            }

            @Override
            public Map<String, String[]> getDecisionParameters() {
                return params;
            }

            @Override
            public void decisionMade(String result) {
                throw new AssertionError("the trace must never call decisionMade");
            }
        };
    }

    // =========================================================================
    // Direct interceptor (V45): distinct route + final response, skipped finalizer
    // =========================================================================

    @Test
    public void v45InterceptorRecordsRouteAndFinalResponse() {
        TheChosenOneAi ai = new TheChosenOneAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        ai.setDecisionTraceSinkForTesting(sink);

        Map<String, String[]> params = new HashMap<>();
        params.put("cardId", new String[]{"temp1", "temp2"});
        params.put("selectable", new String[]{"true", "true"});
        params.put("min", new String[]{"0"});
        params.put("max", new String[]{"1"});
        params.put("noPass", new String[]{"false"});

        String result = ai.decide("tester",
            decision(42, AwaitingDecisionType.CARD_SELECTION,
                "Choose cards to forfeit, if desired", params),
            null);

        // legacy behavior unchanged: V45 passes on the optional forfeit
        assertEquals("", result);
        assertFalse("decide() must not leak a trace session", TraceSession.isActive());

        DecisionTrace trace = sink.single();
        assertNotNull(trace.getRoute());
        assertEquals(TraceRoute.V45_OPTIONAL_FORFEIT, trace.getRoute().selected());
        assertTrue("the direct interceptor skips the common finalizer — recorded, not hidden",
            trace.getFinalization().skippedCommonFinalizer());
        assertTrue(trace.getFinalization().finalResponseRecorded());
        assertEquals("", trace.getFinalization().finalResponse());
        // GATE P0-3: the fields the direct route skips are EXPLICITLY not-applicable
        assertNotNull("pass eligibility must be explicit n/a on a direct route",
            trace.getFinalization().passEligibilityNotApplicableReason());
        assertNotNull("pre-safety winner must be explicit n/a on a direct route",
            trace.getFinalization().preSafetyWinnerNotApplicableReason());
        // full frozen raw input: CARD_SELECTION candidates come from the raw cardId array
        assertEquals(Arrays.asList("temp1", "temp2"), trace.getRawCandidateOrder());
        assertEquals("42", trace.getDecisionId());
        assertTrue("bot model is the bot source-package identifier",
            trace.getBotModel().contains("models"));

        // no GameState => Side unknown => snapshot honestly INCOMPLETE (never fabricated)
        assertEquals(TraceStatus.INCOMPLETE, trace.getStatus());
        boolean snapshotFailure = false;
        for (TraceCaptureFailure f : trace.getCaptureFailures()) {
            if (f.stage() == TraceCaptureFailure.Stage.SNAPSHOT) {
                snapshotFailure = true;
            }
        }
        assertTrue("snapshot construction failure must be typed, not silent", snapshotFailure);
    }

    // =========================================================================
    // DecisionSafety hook: typed correction with before/after (scripted flow)
    // =========================================================================

    @Test
    public void decisionSafetyRecordsTypedCorrection() {
        Map<String, String[]> params = new HashMap<>();
        params.put("noPass", new String[]{"true"});
        params.put("min", new String[]{"1"});
        AwaitingDecision mustChoose = decision(7, AwaitingDecisionType.CARD_ACTION_CHOICE,
            "Choose blast destiny target", params);

        assertTrue(TraceSession.open("test-bot", "7", "CARD_ACTION_CHOICE",
            "Choose blast destiny target", List.of("5", "7"), null,
            List.of("test: snapshot deliberately absent"), false));
        try {
            TraceSession.recordRoute(TraceRoute.COMBINED_EVALUATOR, "scripted", null);
            String[] corrected = DecisionSafety.ensureValidResponse(mustChoose, "",
                new String[]{"5", "7"});
            // legacy behavior unchanged: empty response force-corrected to a valid option
            assertTrue(corrected[0].equals("5") || corrected[0].equals("7"));
            assertFalse(corrected[1].isEmpty());

            TraceSession.recordFinalResponse(corrected[0], false);
            DecisionTrace trace = TraceSession.close();
            assertNotNull(trace);
            assertEquals(1, trace.getFinalization().corrections().size());
            TraceCorrection correction = trace.getFinalization().corrections().get(0);
            assertEquals(TraceCorrection.Kind.SAFETY_FORCED_CHOICE, correction.kind());
            assertEquals("", correction.beforeResponse());
            assertEquals(corrected[0], correction.afterResponse());
            assertEquals(corrected[0], trace.getFinalization().finalResponse());
        } finally {
            TraceSession.abandon();
        }
        assertFalse(TraceSession.isActive());
    }

    // =========================================================================
    // TRACE 4A1 (matrix "prove one legacy call with or without tracing"): the same
    // scripted decision through a NoOp sink and a capture sink returns the identical
    // decision result, and the capture sink sees the typed state events in order:
    // tracker RECORD_RESPONSE (captured AFTER the legacy call), then the
    // pending-deploy SET from the actual direct write in trackStrategicEvents.
    // =========================================================================

    @Test
    public void legacyDecisionIsIdenticalWithOrWithoutTracingAndTypedEventsAreOrdered() {
        Map<String, String[]> params = new HashMap<>();
        params.put("results", new String[]{"Option A", "Option B"});
        params.put("min", new String[]{"0"});
        params.put("max", new String[]{"1"});
        // noPass=true keeps this deterministic end-to-end: the evaluator lane cannot
        // synthesize a winning pass, so the decision falls to the keyword heuristic's
        // deterministic index pick (no emergency randomness: the pick is non-empty).
        params.put("noPass", new String[]{"true"});

        // run 1: production default NoOp sink — no session opens
        TheChosenOneAi untraced = new TheChosenOneAi();
        String untracedResult = untraced.decide("tester",
            decision(77, AwaitingDecisionType.MULTIPLE_CHOICE,
                "Deploy which character to your site?", params),
            null);
        assertFalse(TraceSession.isActive());

        // run 2: identical fresh bot + identical decision with a capture sink
        TheChosenOneAi traced = new TheChosenOneAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        traced.setDecisionTraceSinkForTesting(sink);
        String tracedResult = traced.decide("tester",
            decision(77, AwaitingDecisionType.MULTIPLE_CHOICE,
                "Deploy which character to your site?", params),
            null);
        assertFalse(TraceSession.isActive());

        // identical legacy decision result with or without tracing
        assertNotNull(untracedResult);
        assertEquals(untracedResult, tracedResult);
        assertFalse("scripted choice must pick an option, not pass", tracedResult.isEmpty());

        // the capture sink sees the typed events in list order
        DecisionTrace trace = sink.single();
        List<TraceStateEvent> events = trace.getStateEvents();
        assertEquals("expected tracker RECORD_RESPONSE then PENDING_DEPLOY SET: " + events,
            2, events.size());
        TrackerRecordResponseEvent tracker = (TrackerRecordResponseEvent) events.get(0);
        assertEquals(TrackerOwner.OUTER_CHOSENONE, tracker.owner());
        assertEquals("MULTIPLE_CHOICE", tracker.decisionType());
        assertEquals("77", tracker.decisionId());
        assertEquals(tracedResult, tracker.response());
        // captured AFTER the legacy call: the after snapshot gained the sequence row
        assertEquals(0, tracker.before().sequenceRows().size());
        assertEquals(1, tracker.after().sequenceRows().size());
        assertEquals(tracedResult, tracker.after().sequenceRows().get(0).response());
        assertEquals(MutationOutcome.CHANGED, tracker.outcome());
        PendingDeployEvent deploySet = (PendingDeployEvent) events.get(1);
        assertEquals(PendingDeployEvent.Operation.SET, deploySet.operation());
        assertNull(deploySet.typeBefore());
        assertEquals("character", deploySet.typeAfter());
        assertEquals(MutationOutcome.CHANGED, deploySet.outcome());
    }

    // =========================================================================
    // Production default: no sink, no session, no trace — decide() unaffected
    // =========================================================================

    @Test
    public void productionDefaultOpensNoSession() {
        TheChosenOneAi ai = new TheChosenOneAi();  // default NoOpTraceSink, nothing set
        Map<String, String[]> params = new HashMap<>();
        params.put("cardId", new String[]{"temp1"});
        params.put("min", new String[]{"0"});

        String result = ai.decide("tester",
            decision(43, AwaitingDecisionType.CARD_SELECTION,
                "Choose cards to forfeit, if desired", params),
            null);

        assertEquals("", result);
        assertFalse(TraceSession.isActive());
    }
}
