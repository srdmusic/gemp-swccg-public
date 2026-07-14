package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceFinalization;
import com.gempukku.swccgo.ai.models.common.trace.TraceRoute;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceStatus;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerRecordResponseEvent;
import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Shared bot-boundary proof for the canonical DRAW cutover and its legacy fallback. */
public abstract class AbstractDrawCutoverIntegrationTest {

    protected static final String DECIDING_PLAYER = "tester";
    private static final String DRAW_ACTION_ID = "draw-1";

    public record CapturedAccepted(AiDecisionResult result, DecisionTrace trace) {
    }

    /** Package-local trace setters require one thin adapter in each bot package. */
    protected abstract CapturedAccepted runAccepted(AwaitingDecision decision,
                                                     GameState gameState,
                                                     RejectionHistory history);

    @After
    public void clearAnyLeakedTrace() {
        if (TraceSession.isActive()) {
            TraceSession.abandon();
        }
    }

    @Test
    public void canonicalTypedDrawReturnsCombinedEvaluatorWinnerThroughOwnerLifecycle() {
        AwaitingDecision decision = drawDecision(201, true);
        RejectionHistory history = RejectionHistory.empty().append(
                "previous-wire",
                FinalizedResponse.RejectReason.ENGINE_DECISION_INVALID,
                "fixture rejection carried through mediator entrypoint");

        CapturedAccepted captured = runAccepted(decision, drawState(), history);

        assertEquals("fixture history remains immutable", 1, history.size());
        assertTypedWinner(captured.result());
        assertAcceptedTrace(captured.trace(), TraceRoute.DRAW_TOP_LEVEL);
    }

    @Test
    public void missingSemanticRemainsLegacyCombinedEvaluatorRoute() {
        AwaitingDecision decision = drawDecision(202, false);

        CapturedAccepted captured = runAccepted(
                decision, drawState(), RejectionHistory.empty());

        AiDecisionResult result = captured.result();
        assertEquals(AiDecisionResult.Status.WIRE_RESPONSE, result.status());
        assertEquals("legacy and owned paths retain the evaluator winner", DRAW_ACTION_ID,
                result.wireResponse());
        assertFalse("unowned shape must not claim typed-finalizer ownership",
                result.fromTypedFinalizer());
        assertEquals(AiDecisionResult.MutationMode.OUTER_COMMON, result.mutationMode());
        assertAcceptedTrace(captured.trace(), TraceRoute.COMBINED_EVALUATOR);
    }

    private static void assertTypedWinner(AiDecisionResult result) {
        assertEquals(AiDecisionResult.Status.WIRE_RESPONSE, result.status());
        assertEquals(DRAW_ACTION_ID, result.wireResponse());
        assertTrue("canonical route must return the typed finalizer envelope",
                result.fromTypedFinalizer());
        assertEquals(AiDecisionResult.MutationMode.OUTER_COMMON, result.mutationMode());
        assertNotNull(result.trackerMutation());
        assertEquals(DRAW_ACTION_ID, result.trackerMutation().wireResponse());
        assertEquals(result.decisionId(), result.trackerMutation().decisionId());
    }

    private static void assertAcceptedTrace(DecisionTrace trace, TraceRoute expectedRoute) {
        assertNotNull(trace);
        assertEquals(TraceStatus.COMPLETE, trace.getStatus());
        assertEquals(expectedRoute, trace.getRoute().selected());
        assertEquals(DRAW_ACTION_ID, trace.getFinalization().preSafetyWinnerActionId());
        assertEquals(DRAW_ACTION_ID, trace.getFinalization().finalResponse());
        assertEquals(TraceFinalization.Disposition.ENGINE_ACCEPTED,
                trace.getFinalization().disposition());
        assertEquals(TraceFinalization.MutationMode.OUTER_COMMON,
                trace.getFinalization().acceptedMutationMode());
        assertTrue(trace.getFinalization().acceptedMutationCompleted());
        long trackerMutations = trace.getStateEvents().stream()
                .filter(TrackerRecordResponseEvent.class::isInstance)
                .count();
        assertEquals("accepted path applies the outer tracker mutation once",
                1L, trackerMutations);
        assertFalse("accepted disposition must close the active trace", TraceSession.isActive());
    }

    private static GameState drawState() {
        return AbstractActivateControlDecisionHarnessTest.stubWithTurnPlayer(
                Phase.DRAW, DECIDING_PLAYER);
    }

    private static AwaitingDecision drawDecision(int id, boolean includeSemantic) {
        Map<String, String[]> parameters = AbstractActivateControlDecisionHarnessTest.params(
                "actionId", arr(DRAW_ACTION_ID),
                "cardId", arr("draw-card"),
                "blueprintId", arr("draw-blueprint"),
                "actionText", arr("Draw card into hand from Force Pile"),
                "testingText", arr(""),
                "backSideTestingText", arr(""),
                "horizontal", arr("false"),
                "yourTurn", arr("true"),
                "noPass", arr("false"),
                DecisionOrigin.WIRE_PARAMETER, arr(DecisionOrigin.PHASE_ACTION.name()));
        if (includeSemantic) {
            parameters.put(DecisionActionSemantic.WIRE_PARAMETER,
                    arr(DecisionActionSemantic.DRAW_CARD_INTO_HAND_FROM_FORCE_PILE.name()));
        }
        return AbstractActivateControlDecisionHarnessTest.decision(
                id,
                AwaitingDecisionType.CARD_ACTION_CHOICE,
                "Choose draw action or Pass",
                parameters);
    }

    private static String[] arr(String... values) {
        return AbstractActivateControlDecisionHarnessTest.arr(values);
    }
}
