package com.gempukku.swccgo.ai.models.rando;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.DecisionRejectionKind;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceCaptureFailure;
import com.gempukku.swccgo.ai.models.common.trace.TraceFinalization;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceStatus;
import com.gempukku.swccgo.ai.models.common.trace.TraceTestSupport;
import com.gempukku.swccgo.ai.models.common.trace.state.TraceStateEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerRecordResponseEvent;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import com.gempukku.swccgo.logic.decisions.IntegerAwaitingDecision;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * FINALIZER RUNTIME (2026-07-13, Handoffs/CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md
 * §4/§7 "Rando And ChosenOne" + "Trace"): the Rando decide()/decideForEngine split and the
 * disposition callbacks. Direct decide() closes the trace inline; EVERY mediator-facing result
 * (mode NONE interceptor included) DEFERS the close to one synchronous disposition callback.
 * ENGINE_ACCEPTED for an OUTER_COMMON route records the outer tracker; a rejection records
 * neither and closes without a final response. No server, no game (null GameState exercises the
 * null-safe interceptor and heuristic-fallback routes).
 */
public class RandoCalAiLifecycleTest {

    @After
    public void clearAnyLeakedSession() {
        if (TraceSession.isActive()) {
            TraceSession.abandon();
        }
    }

    private static AwaitingDecision forfeitDecision() {
        // V45 direct interceptor (mode NONE): text contains "forfeit" + "if desired".
        return decision(3, AwaitingDecisionType.MULTIPLE_CHOICE, "Forfeit a card if desired",
                new HashMap<>());
    }

    private static AwaitingDecision revertDecision() {
        Map<String, String[]> params = new HashMap<>();
        params.put("results", new String[]{"No", "Allow revert"});
        return decision(44, AwaitingDecisionType.MULTIPLE_CHOICE,
                "Opponent requests a revert. Allow revert?", params);
    }

    private static IntegerAwaitingDecision integerDecision() {
        // No interceptor matches; the evaluator lane declines on a null GameState, so this reaches
        // the heuristic-fallback OUTER_COMMON boundary (pickInteger uses no GameState).
        return new IntegerAwaitingDecision("Choose a number", 0, 10, 5) {
            @Override
            public void decisionMade(int result) {
                // never submitted in these unit tests
            }
        };
    }

    private static AwaitingDecision outerMutationFailureDecision() {
        return new AwaitingDecision() {
            @Override public int getAwaitingDecisionId() { throw new RuntimeException("outer-mutation-fault"); }
            @Override public String getText() { return "Choose a number"; }
            @Override public AwaitingDecisionType getDecisionType() { return AwaitingDecisionType.INTEGER; }
            @Override public Map<String, String[]> getDecisionParameters() { return new HashMap<>(); }
            @Override public void decisionMade(String result) { }
        };
    }

    private static AwaitingDecision decision(int id, AwaitingDecisionType type, String text,
                                             Map<String, String[]> params) {
        return new AwaitingDecision() {
            @Override public int getAwaitingDecisionId() { return id; }
            @Override public String getText() { return text; }
            @Override public AwaitingDecisionType getDecisionType() { return type; }
            @Override public Map<String, String[]> getDecisionParameters() { return params; }
            @Override public void decisionMade(String result) { }
        };
    }

    private static boolean hasOuterTrackerRecord(DecisionTrace trace) {
        for (TraceStateEvent event : trace.getStateEvents()) {
            if (event instanceof TrackerRecordResponseEvent) {
                return true;
            }
        }
        return false;
    }

    // ── direct decide() closes inline, records finalResponse, no disposition ─────
    @Test
    public void directDecideClosesTraceInline() {
        RandoCalAi ai = new RandoCalAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        ai.setDecisionTraceSinkForTesting(sink);

        String result = ai.decide("tester", forfeitDecision(), null);

        assertEquals("V45 forfeit passes", "", result);
        assertFalse("trace closed inline after a direct call", TraceSession.isActive());
        assertEquals("direct call emits exactly one trace", 1, sink.getTraces().size());
        TraceFinalization fin = sink.getTraces().get(0).getFinalization();
        assertTrue("direct call records a final response", fin.finalResponseRecorded());
        assertNull("direct call records NO engine disposition", fin.disposition());
    }

    // ── mediator-facing NONE interceptor DEFERS close to the accepted callback ───
    @Test
    public void mediatorNoneInterceptorDefersCloseUntilAccepted() {
        RandoCalAi ai = new RandoCalAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        ai.setDecisionTraceSinkForTesting(sink);
        AwaitingDecision decision = forfeitDecision();

        AiDecisionResult result = ai.decideForEngine("tester", decision, null, RejectionHistory.empty());

        assertEquals("V45 forfeit is mode NONE", AiDecisionResult.MutationMode.NONE, result.mutationMode());
        assertEquals("wire preserved", "", result.wireResponse());
        assertTrue("mediator-facing NONE keeps the trace OPEN (deferred)", TraceSession.isActive());
        assertEquals("no trace emitted before disposition", 0, sink.getTraces().size());

        ai.onDecisionAccepted("tester", decision, null, result);

        assertFalse("accepted callback closed the trace", TraceSession.isActive());
        assertEquals("exactly one trace after disposition", 1, sink.getTraces().size());
        TraceFinalization fin = sink.getTraces().get(0).getFinalization();
        assertEquals("ENGINE_ACCEPTED disposition", TraceFinalization.Disposition.ENGINE_ACCEPTED,
                fin.disposition());
        assertEquals("NONE mutation mode", TraceFinalization.MutationMode.NONE,
                fin.acceptedMutationMode());
        assertFalse("NONE mutation did not execute", fin.acceptedMutationCompleted());
        assertFalse("NONE interceptor records NO outer tracker mutation",
                hasOuterTrackerRecord(sink.getTraces().get(0)));
    }

    @Test
    public void revertFinalizerNoneClosesOnAcceptedExactWire() throws Exception {
        RandoCalAi ai = new RandoCalAi();
        Field side = RandoCalAi.class.getDeclaredField("mySide");
        side.setAccessible(true);
        side.set(ai, Side.DARK);
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        ai.setDecisionTraceSinkForTesting(sink);
        AwaitingDecision decision = revertDecision();

        AiDecisionResult result = ai.decideForEngine(
                "tester", decision, null, RejectionHistory.empty());

        assertEquals(AiDecisionResult.Status.WIRE_RESPONSE, result.status());
        assertEquals("1", result.wireResponse());
        assertEquals(AiDecisionResult.MutationMode.NONE, result.mutationMode());
        assertTrue("revert wire came through the typed finalizer", result.fromTypedFinalizer());
        assertNull("NONE carries no outer mutation descriptor", result.trackerMutation());
        assertTrue("trace remains open until disposition", TraceSession.isActive());

        ai.onDecisionAccepted("tester", decision, null, result);

        assertFalse(TraceSession.isActive());
        assertEquals("one disposition closes one trace", 1, sink.getTraces().size());
        DecisionTrace trace = sink.getTraces().get(0);
        TraceFinalization fin = trace.getFinalization();
        assertEquals("1", fin.proposedWireResponse());
        assertEquals("1", fin.finalResponse());
        assertEquals(TraceFinalization.Disposition.ENGINE_ACCEPTED, fin.disposition());
        assertEquals(TraceFinalization.MutationMode.NONE, fin.acceptedMutationMode());
        assertFalse("NONE executes no accepted mutation", fin.acceptedMutationCompleted());
        assertTrue("common finalizer tail remains skipped", fin.skippedCommonFinalizer());
        assertFalse("revert acceptance records no outer tracker mutation",
                hasOuterTrackerRecord(trace));
    }

    // ── accepted NONE callback records the SUBMITTED (post-override) wire as final ──
    @Test
    public void acceptedCallbackRecordsSubmittedWireAsFinalResponse() {
        RandoCalAi ai = new RandoCalAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        ai.setDecisionTraceSinkForTesting(sink);
        AwaitingDecision decision = forfeitDecision();

        AiDecisionResult computed = ai.decideForEngine("tester", decision, null, RejectionHistory.empty());
        // Simulate a Curator override: the submitted wire differs from the computed "".
        AiDecisionResult submitted = computed.withWireResponse("9");
        ai.onDecisionAccepted("tester", decision, null, submitted);

        TraceFinalization fin = sink.getTraces().get(0).getFinalization();
        assertEquals("records the actual submitted wire as the accepted final response", "9",
                fin.finalResponse());
        assertEquals("proposed wire is the submitted wire", "9", fin.proposedWireResponse());
        assertEquals(TraceFinalization.Disposition.ENGINE_ACCEPTED, fin.disposition());
    }

    @Test
    public void acceptedOuterMutationFailureRetainsFinalWireAndClosesIncompleteTrace() {
        RandoCalAi ai = new RandoCalAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        ai.setDecisionTraceSinkForTesting(sink);
        AiDecisionResult result = ai.decideForEngine("tester", integerDecision(), null,
                RejectionHistory.empty()).withWireResponse("9");

        try {
            ai.onDecisionAccepted("tester", outerMutationFailureDecision(), null, result);
            fail("the outer mutation fault must propagate after acceptance");
        } catch (RuntimeException expected) {
            assertEquals("outer-mutation-fault", expected.getMessage());
        }

        assertFalse("accepted callback closes the trace", TraceSession.isActive());
        assertEquals("one emitted trace", 1, sink.getTraces().size());
        DecisionTrace trace = sink.getTraces().get(0);
        TraceFinalization fin = trace.getFinalization();
        assertEquals("9", fin.proposedWireResponse());
        assertEquals("9", fin.finalResponse());
        assertEquals(TraceFinalization.Disposition.ENGINE_ACCEPTED, fin.disposition());
        assertEquals(TraceFinalization.MutationMode.OUTER_COMMON, fin.acceptedMutationMode());
        assertFalse("failed outer mutation is not completed", fin.acceptedMutationCompleted());
        assertEquals(TraceStatus.INCOMPLETE, trace.getStatus());
        assertTrue("expected mutation STATE_EVENT failure: " + trace.getCaptureFailures(),
                trace.getCaptureFailures().stream().anyMatch(failure ->
                        failure.stage() == TraceCaptureFailure.Stage.STATE_EVENT
                                && RuntimeException.class.getName().equals(failure.errorClass())));
    }

    // ── mediator rejection closes without mutation or final response ─────────────
    @Test
    public void mediatorRejectionClosesWithoutTrackerOrFinalResponse() {
        RandoCalAi ai = new RandoCalAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        ai.setDecisionTraceSinkForTesting(sink);
        AwaitingDecision decision = forfeitDecision();

        AiDecisionResult result = ai.decideForEngine("tester", decision, null, RejectionHistory.empty());
        ai.onDecisionRejected("tester", decision, null, result,
                DecisionRejectionKind.ENGINE_REJECTED, "engine rejected");

        assertFalse("rejection callback cleared the trace session", TraceSession.isActive());
        assertEquals("exactly one trace", 1, sink.getTraces().size());
        TraceFinalization fin = sink.getTraces().get(0).getFinalization();
        assertEquals("ENGINE_REJECTED disposition", TraceFinalization.Disposition.ENGINE_REJECTED,
                fin.disposition());
        assertFalse("rejection records NO final response", fin.finalResponseRecorded());
        assertFalse("rejection records NO outer tracker mutation",
                hasOuterTrackerRecord(sink.getTraces().get(0)));
    }

    // ── attempt-failed disposition closes the session ───────────────────────────
    @Test
    public void attemptFailedDispositionClosesSession() {
        RandoCalAi ai = new RandoCalAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        ai.setDecisionTraceSinkForTesting(sink);
        AwaitingDecision decision = forfeitDecision();

        AiDecisionResult result = ai.decideForEngine("tester", decision, null, RejectionHistory.empty());
        ai.onDecisionAttemptFailed("tester", decision, null, "attempt aborted");

        assertFalse("attempt-failed callback cleared the trace session", TraceSession.isActive());
        assertEquals(TraceFinalization.Disposition.ATTEMPT_FAILED,
                sink.getTraces().get(0).getFinalization().disposition());
    }

    // ── OUTER_COMMON route defers the outer tracker record to acceptance ─────────
    @Test
    public void outerCommonAppliesTrackerOnAcceptOnly() {
        RandoCalAi accepted = new RandoCalAi();
        TraceTestSupport.CaptureSink acceptedSink = new TraceTestSupport.CaptureSink();
        accepted.setDecisionTraceSinkForTesting(acceptedSink);
        IntegerAwaitingDecision acceptDecision = integerDecision();

        AiDecisionResult acceptResult = accepted.decideForEngine("tester", acceptDecision, null,
                RejectionHistory.empty());
        assertEquals("heuristic fallback is OUTER_COMMON",
                AiDecisionResult.MutationMode.OUTER_COMMON, acceptResult.mutationMode());
        assertTrue("outer-common defers the trace close", TraceSession.isActive());

        accepted.onDecisionAccepted("tester", acceptDecision, null, acceptResult);
        assertFalse(TraceSession.isActive());
        DecisionTrace acceptedTrace = acceptedSink.getTraces().get(0);
        assertEquals(TraceFinalization.Disposition.ENGINE_ACCEPTED,
                acceptedTrace.getFinalization().disposition());
        assertEquals(TraceFinalization.MutationMode.OUTER_COMMON,
                acceptedTrace.getFinalization().acceptedMutationMode());
        assertTrue("acceptance applies the outer tracker record once",
                hasOuterTrackerRecord(acceptedTrace));

        RandoCalAi rejected = new RandoCalAi();
        TraceTestSupport.CaptureSink rejectedSink = new TraceTestSupport.CaptureSink();
        rejected.setDecisionTraceSinkForTesting(rejectedSink);
        IntegerAwaitingDecision rejectDecision = integerDecision();
        AiDecisionResult rejectResult = rejected.decideForEngine("tester", rejectDecision, null,
                RejectionHistory.empty());
        rejected.onDecisionRejected("tester", rejectDecision, null, rejectResult,
                DecisionRejectionKind.ENGINE_REJECTED, "engine rejected");
        assertFalse("rejection applies NO outer tracker record",
                hasOuterTrackerRecord(rejectedSink.getTraces().get(0)));
    }

    // ── trace-disabled and trace-enabled wire results are identical ──────────────
    @Test
    public void traceDisabledWireEqualsTraceEnabledWire() {
        RandoCalAi disabled = new RandoCalAi(); // default NoOpTraceSink
        AiDecisionResult disabledResult = disabled.decideForEngine("tester", forfeitDecision(), null,
                RejectionHistory.empty());
        assertFalse("no session opens under the disabled default sink", TraceSession.isActive());

        RandoCalAi enabled = new RandoCalAi();
        enabled.setDecisionTraceSinkForTesting(new TraceTestSupport.CaptureSink());
        AiDecisionResult enabledResult = enabled.decideForEngine("tester", forfeitDecision(), null,
                RejectionHistory.empty());

        assertEquals("identical wire regardless of trace", disabledResult.wireResponse(),
                enabledResult.wireResponse());
        assertEquals("identical mutation mode regardless of trace", disabledResult.mutationMode(),
                enabledResult.mutationMode());
    }
}
