package com.gempukku.swccgo.ai.models.chosenone;

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
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import com.gempukku.swccgo.logic.decisions.IntegerAwaitingDecision;
import org.junit.After;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * FINALIZER RUNTIME (2026-07-13, packet §4/§9): TheChosenOneAi lifecycle — the exact mirror of
 * RandoCalAiLifecycleTest (typed owner label OUTER_CHOSENONE aside). Direct decide() closes
 * inline; mediator-facing NONE and OUTER_COMMON both defer the close to one disposition callback.
 */
public class TheChosenOneAiLifecycleTest {

    @After
    public void clearAnyLeakedSession() {
        if (TraceSession.isActive()) {
            TraceSession.abandon();
        }
    }

    private static AwaitingDecision forfeitDecision() {
        return new AwaitingDecision() {
            @Override public int getAwaitingDecisionId() { return 3; }
            @Override public String getText() { return "Forfeit a card if desired"; }
            @Override public AwaitingDecisionType getDecisionType() { return AwaitingDecisionType.MULTIPLE_CHOICE; }
            @Override public Map<String, String[]> getDecisionParameters() { return new HashMap<>(); }
            @Override public void decisionMade(String result) { }
        };
    }

    private static IntegerAwaitingDecision integerDecision() {
        return new IntegerAwaitingDecision("Choose a number", 0, 10, 5) {
            @Override public void decisionMade(int result) { }
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

    private static boolean hasOuterTrackerRecord(DecisionTrace trace) {
        for (TraceStateEvent event : trace.getStateEvents()) {
            if (event instanceof TrackerRecordResponseEvent) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void directDecideClosesInlineNoDisposition() {
        TheChosenOneAi ai = new TheChosenOneAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        ai.setDecisionTraceSinkForTesting(sink);

        String result = ai.decide("tester", forfeitDecision(), null);

        assertEquals("", result);
        assertFalse(TraceSession.isActive());
        assertEquals(1, sink.getTraces().size());
        assertNull("direct call records NO disposition", sink.getTraces().get(0).getFinalization().disposition());
    }

    @Test
    public void mediatorNoneInterceptorDefersCloseUntilAccepted() {
        TheChosenOneAi ai = new TheChosenOneAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        ai.setDecisionTraceSinkForTesting(sink);
        AwaitingDecision decision = forfeitDecision();

        AiDecisionResult result = ai.decideForEngine("tester", decision, null, RejectionHistory.empty());
        assertEquals(AiDecisionResult.MutationMode.NONE, result.mutationMode());
        assertTrue("mediator-facing NONE keeps the trace open", TraceSession.isActive());
        assertEquals(0, sink.getTraces().size());

        ai.onDecisionAccepted("tester", decision, null, result);
        assertFalse(TraceSession.isActive());
        TraceFinalization fin = sink.getTraces().get(0).getFinalization();
        assertEquals(TraceFinalization.Disposition.ENGINE_ACCEPTED, fin.disposition());
        assertEquals(TraceFinalization.MutationMode.NONE, fin.acceptedMutationMode());
        assertFalse(hasOuterTrackerRecord(sink.getTraces().get(0)));
    }

    @Test
    public void acceptedOuterMutationFailureRetainsFinalWireAndClosesIncompleteTrace() {
        TheChosenOneAi ai = new TheChosenOneAi();
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

        assertFalse(TraceSession.isActive());
        assertEquals("one emitted trace", 1, sink.getTraces().size());
        DecisionTrace trace = sink.getTraces().get(0);
        TraceFinalization fin = trace.getFinalization();
        assertEquals("9", fin.proposedWireResponse());
        assertEquals("9", fin.finalResponse());
        assertEquals(TraceFinalization.Disposition.ENGINE_ACCEPTED, fin.disposition());
        assertEquals(TraceFinalization.MutationMode.OUTER_COMMON, fin.acceptedMutationMode());
        assertFalse(fin.acceptedMutationCompleted());
        assertEquals(TraceStatus.INCOMPLETE, trace.getStatus());
        assertTrue("expected mutation STATE_EVENT failure: " + trace.getCaptureFailures(),
                trace.getCaptureFailures().stream().anyMatch(failure ->
                        failure.stage() == TraceCaptureFailure.Stage.STATE_EVENT
                                && RuntimeException.class.getName().equals(failure.errorClass())));
    }

    @Test
    public void outerCommonAppliesTrackerOnAcceptSkipsOnReject() {
        TheChosenOneAi accepted = new TheChosenOneAi();
        TraceTestSupport.CaptureSink acceptedSink = new TraceTestSupport.CaptureSink();
        accepted.setDecisionTraceSinkForTesting(acceptedSink);
        IntegerAwaitingDecision acceptDecision = integerDecision();

        AiDecisionResult acceptResult = accepted.decideForEngine("tester", acceptDecision, null,
                RejectionHistory.empty());
        assertEquals(AiDecisionResult.MutationMode.OUTER_COMMON, acceptResult.mutationMode());
        assertTrue(TraceSession.isActive());
        accepted.onDecisionAccepted("tester", acceptDecision, null, acceptResult);
        assertFalse(TraceSession.isActive());
        assertTrue("acceptance applies the outer tracker record once",
                hasOuterTrackerRecord(acceptedSink.getTraces().get(0)));

        TheChosenOneAi rejected = new TheChosenOneAi();
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
}
