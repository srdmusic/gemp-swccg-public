package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceFinalization;
import com.gempukku.swccgo.ai.models.common.trace.TraceOp;
import com.gempukku.swccgo.ai.models.common.trace.TraceRoute;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceStatus;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerRecordResponseEvent;
import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PullDecisionWire;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Shared bot-boundary contract for typed PULL cutover and exact legacy parity. */
public abstract class AbstractPullCutoverIntegrationTest {

    protected static final String DECIDING_PLAYER = "tester";

    private static final String PARENT_WIRE = "pull-parent-wire";
    private static final String ARBITRARY_WIRE = "temp7";
    private static final String PHYSICAL_CARD_ID = "701";
    private static final String PHYSICAL_PERMANENT_CARD_ID = "7001";
    private static final String SOURCE_CARD_ID = "901";
    private static final String SOURCE_PERMANENT_CARD_ID = "9001";
    private static final String DESTINATION_WIRE = "801";
    private static final String SECOND_DESTINATION_WIRE = "802";
    private static final String DESTINATION_PERMANENT_CARD_ID = "8001";
    private static final String SECOND_DESTINATION_PERMANENT_CARD_ID = "8002";
    private static final long TRANSACTION_ID = 90001L;
    private static final int PARENT_DECISION_ID = 300;
    private static final int PARENT_ACTION_ORDINAL = 0;
    private static final String EVALUATOR_NOT_APPLICABLE =
            "failed PULL verification finalizes an empty selection without scoring";

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
    public void typedParentUsesTypedFinalizerAndExactCombinedEvaluatorWire() {
        AwaitingDecision decision = parentDecision(301, true);

        CapturedAccepted captured = runAccepted(
                decision, pullState(), RejectionHistory.empty());

        assertTypedEvaluatorAccepted(captured, PARENT_WIRE, TraceRoute.PULL_PARENT,
                List.of(PARENT_WIRE), List.of(PARENT_WIRE));
        assertEquals(List.of(DecisionActionSemantic.PULL_TAKE_INTO_HAND_FROM_PILE.name()),
                captured.trace().getSnapshot().rawDecision()
                        .values(DecisionActionSemantic.WIRE_PARAMETER));
    }

    @Test
    public void deployAndTakeChildrenKeepDistinctTraceRoutesWithExactWire() {
        CapturedAccepted deploy = runAccepted(
                childDecision(302, DecisionOrigin.PULL_DEPLOY_CHILD),
                pullState(), RejectionHistory.empty());
        CapturedAccepted take = runAccepted(
                childDecision(303, DecisionOrigin.PULL_TAKE_CHILD),
                pullState(), RejectionHistory.empty());

        assertTypedEvaluatorAccepted(deploy, ARBITRARY_WIRE,
                TraceRoute.PULL_DEPLOY_CHILD,
                List.of(ARBITRARY_WIRE), List.of(ARBITRARY_WIRE));
        assertTypedEvaluatorAccepted(take, ARBITRARY_WIRE,
                TraceRoute.PULL_TAKE_CHILD,
                List.of(ARBITRARY_WIRE), List.of(ARBITRARY_WIRE));
        assertNotEquals(deploy.trace().getRoute().selected(), take.trace().getRoute().selected());
        assertArbitraryIdentityIsSeparate(deploy.trace());
        assertArbitraryIdentityIsSeparate(take.trace());
    }

    @Test
    public void typedDestinationUsesOrderedCandidatesWithoutForcedMetadata() {
        AwaitingDecision decision = destinationDecision(304);

        CapturedAccepted captured = runAccepted(
                decision, pullState(), RejectionHistory.empty());

        assertTypedEvaluatorAccepted(captured, DESTINATION_WIRE,
                TraceRoute.PULL_DESTINATION,
                List.of(DESTINATION_WIRE, SECOND_DESTINATION_WIRE),
                List.of(DESTINATION_WIRE, SECOND_DESTINATION_WIRE));
        assertFalse(captured.trace().getSnapshot().rawDecision()
                .has(PullDecisionWire.FORCED_DESTINATION_CARD_ID));
        assertFalse(captured.trace().getSnapshot().rawDecision()
                .has(PullDecisionWire.FORCED_DESTINATION_PERMANENT_CARD_ID));
    }

    @Test
    public void failedVerifyReturnsEmptyBypassesEvaluatorAndCompletesTrace() {
        AwaitingDecision decision = failedVerifyDecision(305);

        CapturedAccepted captured = runAccepted(
                decision, pullState(), RejectionHistory.empty());

        assertTypedResult(captured.result(), "");
        assertAcceptedLifecycle(captured.trace(), TraceRoute.PULL_FAILED_VERIFY, "");
        assertEquals(List.of(ARBITRARY_WIRE), captured.trace().getRawCandidateOrder());
        assertTrue("failed verify must not register evaluator candidates",
                captured.trace().getMergeOrder().isEmpty());
        assertTrue("failed verify must not emit evaluator operations",
                captured.trace().getOperations().isEmpty());
        TraceFinalization finalization = captured.trace().getFinalization();
        assertFalse(finalization.preSafetyWinnerRecorded());
        assertNull(finalization.preSafetyWinnerActionId());
        assertEquals(EVALUATOR_NOT_APPLICABLE,
                finalization.preSafetyWinnerNotApplicableReason());
        assertNull(finalization.passEligible());
        assertEquals(EVALUATOR_NOT_APPLICABLE,
                finalization.passEligibilityNotApplicableReason());
    }

    @Test
    public void missingTransactionStaysLegacyWithExactResponseParity() {
        CapturedAccepted typed = runAccepted(
                childDecision(306, DecisionOrigin.PULL_DEPLOY_CHILD),
                pullState(), RejectionHistory.empty());
        Map<String, String[]> legacyParameters = childParameters(
                DecisionOrigin.PULL_DEPLOY_CHILD);
        legacyParameters.remove(PullDecisionWire.TRANSACTION_ID);
        AwaitingDecision legacyDecision = decision(
                307,
                AwaitingDecisionType.ARBITRARY_CARDS,
                "Choose card to deploy from Reserve Deck",
                legacyParameters);

        CapturedAccepted legacy = runAccepted(
                legacyDecision, pullState(), RejectionHistory.empty());

        assertTypedEvaluatorAccepted(typed, ARBITRARY_WIRE,
                TraceRoute.PULL_DEPLOY_CHILD,
                List.of(ARBITRARY_WIRE), List.of(ARBITRARY_WIRE));
        assertLegacyEvaluatorAccepted(legacy, ARBITRARY_WIRE,
                List.of(ARBITRARY_WIRE), List.of(ARBITRARY_WIRE));
        assertEquals(typed.result().wireResponse(), legacy.result().wireResponse());
    }

    @Test
    public void missingSemanticStaysLegacyWithExactResponseParity() {
        CapturedAccepted typed = runAccepted(
                parentDecision(308, true), pullState(), RejectionHistory.empty());
        CapturedAccepted legacy = runAccepted(
                parentDecision(309, false), pullState(), RejectionHistory.empty());

        assertTypedEvaluatorAccepted(typed, PARENT_WIRE, TraceRoute.PULL_PARENT,
                List.of(PARENT_WIRE), List.of(PARENT_WIRE));
        assertLegacyEvaluatorAccepted(legacy, PARENT_WIRE,
                List.of(PARENT_WIRE), List.of(PARENT_WIRE));
        assertEquals(typed.result().wireResponse(), legacy.result().wireResponse());
        assertFalse(legacy.trace().getSnapshot().rawDecision()
                .has(DecisionActionSemantic.WIRE_PARAMETER));
    }

    private static void assertTypedEvaluatorAccepted(CapturedAccepted captured,
                                                      String expectedWire,
                                                      TraceRoute expectedRoute,
                                                      List<String> expectedRawOrder,
                                                      List<String> expectedMergeOrder) {
        assertTypedResult(captured.result(), expectedWire);
        assertAcceptedLifecycle(captured.trace(), expectedRoute, expectedWire);
        assertEvaluatorWire(captured.trace(), expectedWire,
                expectedRawOrder, expectedMergeOrder);
    }

    private static void assertLegacyEvaluatorAccepted(CapturedAccepted captured,
                                                       String expectedWire,
                                                       List<String> expectedRawOrder,
                                                       List<String> expectedMergeOrder) {
        AiDecisionResult result = captured.result();
        assertEquals(AiDecisionResult.Status.WIRE_RESPONSE, result.status());
        assertEquals(expectedWire, result.wireResponse());
        assertFalse("legacy route must not claim typed-finalizer ownership",
                result.fromTypedFinalizer());
        assertEquals(AiDecisionResult.MutationMode.OUTER_COMMON, result.mutationMode());
        assertNull(result.trackerMutation());
        assertAcceptedLifecycle(captured.trace(), TraceRoute.COMBINED_EVALUATOR, expectedWire);
        assertEvaluatorWire(captured.trace(), expectedWire,
                expectedRawOrder, expectedMergeOrder);
    }

    private static void assertTypedResult(AiDecisionResult result, String expectedWire) {
        assertEquals(AiDecisionResult.Status.WIRE_RESPONSE, result.status());
        assertEquals(expectedWire, result.wireResponse());
        assertTrue("typed route must return the typed finalizer envelope",
                result.fromTypedFinalizer());
        assertEquals(AiDecisionResult.MutationMode.OUTER_COMMON, result.mutationMode());
        assertNotNull(result.trackerMutation());
        assertEquals(expectedWire, result.trackerMutation().wireResponse());
        assertEquals(result.decisionId(), result.trackerMutation().decisionId());
    }

    private static void assertAcceptedLifecycle(DecisionTrace trace,
                                                TraceRoute expectedRoute,
                                                String expectedWire) {
        assertNotNull(trace);
        assertEquals(TraceStatus.COMPLETE, trace.getStatus());
        assertTrue(trace.getCaptureFailures().isEmpty());
        assertEquals(expectedRoute, trace.getRoute().selected());
        TraceFinalization finalization = trace.getFinalization();
        assertTrue(finalization.finalResponseRecorded());
        assertEquals(expectedWire, finalization.finalResponse());
        assertTrue(finalization.proposedWireRecorded());
        assertEquals(expectedWire, finalization.proposedWireResponse());
        assertEquals(TraceFinalization.Disposition.ENGINE_ACCEPTED,
                finalization.disposition());
        assertEquals(TraceFinalization.MutationMode.OUTER_COMMON,
                finalization.acceptedMutationMode());
        assertTrue(finalization.acceptedMutationCompleted());
        assertFalse(finalization.skippedCommonFinalizer());
        assertTrue(finalization.corrections().isEmpty());
        long trackerMutations = trace.getStateEvents().stream()
                .filter(TrackerRecordResponseEvent.class::isInstance)
                .count();
        assertEquals("accepted path applies the outer tracker mutation once",
                1L, trackerMutations);
        assertFalse("accepted disposition must close the active trace", TraceSession.isActive());
    }

    private static void assertEvaluatorWire(DecisionTrace trace,
                                            String expectedWire,
                                            List<String> expectedRawOrder,
                                            List<String> expectedMergeOrder) {
        assertEquals(expectedRawOrder, trace.getRawCandidateOrder());
        assertEquals(expectedMergeOrder, trace.getMergeOrder());
        TraceFinalization finalization = trace.getFinalization();
        assertTrue(finalization.preSafetyWinnerRecorded());
        assertEquals(expectedWire, finalization.preSafetyWinnerActionId());
        assertNotNull(finalization.preSafetyWinnerScoreBits());
        assertNull(finalization.preSafetyWinnerNotApplicableReason());
        assertTrue("exact CombinedEvaluator selection must be recorded",
                trace.getOperations().stream().anyMatch(operation ->
                        operation.getOp() == TraceOp.SELECT
                                && "COMBINED_EVALUATOR".equals(operation.getEvaluatorId())
                                && expectedWire.equals(operation.getActionId())));
    }

    private static void assertArbitraryIdentityIsSeparate(DecisionTrace trace) {
        assertEquals(List.of(ARBITRARY_WIRE), trace.getRawCandidateOrder());
        assertEquals(List.of(PHYSICAL_CARD_ID), trace.getSnapshot().rawDecision()
                .values(PullDecisionWire.PHYSICAL_CARD_ID));
        assertEquals(List.of(PHYSICAL_PERMANENT_CARD_ID), trace.getSnapshot().rawDecision()
                .values(PullDecisionWire.PHYSICAL_PERMANENT_CARD_ID));
        assertNotEquals(ARBITRARY_WIRE, PHYSICAL_CARD_ID);
        assertNotEquals(ARBITRARY_WIRE, PHYSICAL_PERMANENT_CARD_ID);
    }

    private static GameState pullState() {
        return AbstractActivateControlDecisionHarnessTest.stubWithTurnPlayer(
                Phase.DEPLOY, DECIDING_PLAYER);
    }

    private static AwaitingDecision parentDecision(int id, boolean includeSemantic) {
        Map<String, String[]> parameters = params(
                DecisionOrigin.WIRE_PARAMETER, arr(DecisionOrigin.PHASE_ACTION.name()),
                "actionId", arr(PARENT_WIRE),
                "cardId", arr(SOURCE_CARD_ID),
                "blueprintId", arr("inPlay"),
                "actionText", arr("Take a card into hand from Force Pile"),
                "noPass", arr("true"),
                PullDecisionWire.SOURCE_CARD_ID, arr(SOURCE_CARD_ID),
                PullDecisionWire.SOURCE_PERMANENT_CARD_ID,
                        arr(SOURCE_PERMANENT_CARD_ID),
                PullDecisionWire.GAME_TEXT_ACTION_ID,
                        arr(GameTextActionId.ECHO_BASE_DESTROYED__TAKE_CARD_INTO_HAND_FROM_FORCE_PILE.name()));
        if (includeSemantic) {
            parameters.put(DecisionActionSemantic.WIRE_PARAMETER,
                    arr(DecisionActionSemantic.PULL_TAKE_INTO_HAND_FROM_PILE.name()));
        }
        return decision(id, AwaitingDecisionType.CARD_ACTION_CHOICE,
                "Choose one pull action", parameters);
    }

    private static AwaitingDecision childDecision(int id, DecisionOrigin origin) {
        String text = origin == DecisionOrigin.PULL_TAKE_CHILD
                ? "Choose card to take into hand"
                : "Choose card to deploy from Reserve Deck";
        return decision(id, AwaitingDecisionType.ARBITRARY_CARDS,
                text, childParameters(origin));
    }

    private static Map<String, String[]> childParameters(DecisionOrigin origin) {
        GameTextActionId actionId = origin == DecisionOrigin.PULL_TAKE_CHILD
                ? GameTextActionId.ECHO_BASE_DESTROYED__TAKE_CARD_INTO_HAND_FROM_FORCE_PILE
                : GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD;
        Zone sourceZone = origin == DecisionOrigin.PULL_TAKE_CHILD
                ? Zone.FORCE_PILE : Zone.RESERVE_DECK;
        Map<String, String[]> parameters = transaction(origin, actionId, sourceZone);
        parameters.put("cardId", arr(ARBITRARY_WIRE));
        parameters.put("blueprintId", arr("7_1"));
        parameters.put("min", arr("1"));
        parameters.put("max", arr("1"));
        parameters.put("selectable", arr("true"));
        parameters.put("preselected", arr("false"));
        parameters.put("returnAnyChange", arr("false"));
        parameters.put(PullDecisionWire.PHYSICAL_CARD_ID, arr(PHYSICAL_CARD_ID));
        parameters.put(PullDecisionWire.PHYSICAL_PERMANENT_CARD_ID,
                arr(PHYSICAL_PERMANENT_CARD_ID));
        return parameters;
    }

    private static AwaitingDecision destinationDecision(int id) {
        Map<String, String[]> parameters = transaction(
                DecisionOrigin.PULL_DESTINATION,
                GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD,
                Zone.RESERVE_DECK);
        parameters.put("cardId", arr(DESTINATION_WIRE, SECOND_DESTINATION_WIRE));
        parameters.put("min", arr("1"));
        parameters.put("max", arr("1"));
        parameters.put("selectable", arr("true", "true"));
        parameters.put(PullDecisionWire.SELECTED_CARD_ID, arr(PHYSICAL_CARD_ID));
        parameters.put(PullDecisionWire.SELECTED_PERMANENT_CARD_ID,
                arr(PHYSICAL_PERMANENT_CARD_ID));
        parameters.put(PullDecisionWire.DESTINATION_CARD_ID,
                arr(DESTINATION_WIRE, SECOND_DESTINATION_WIRE));
        parameters.put(PullDecisionWire.DESTINATION_PERMANENT_CARD_ID,
                arr(DESTINATION_PERMANENT_CARD_ID,
                        SECOND_DESTINATION_PERMANENT_CARD_ID));
        return decision(id, AwaitingDecisionType.CARD_SELECTION,
                "Select one offered card", parameters);
    }

    private static AwaitingDecision failedVerifyDecision(int id) {
        Map<String, String[]> parameters = childParameters(
                DecisionOrigin.PULL_FAILED_VERIFY);
        parameters.put("min", arr("0"));
        parameters.put("max", arr("0"));
        parameters.put("selectable", arr("false"));
        return decision(id, AwaitingDecisionType.ARBITRARY_CARDS,
                "Verify unsuccessful search", parameters);
    }

    private static Map<String, String[]> transaction(DecisionOrigin origin,
                                                     GameTextActionId actionId,
                                                     Zone sourceZone) {
        return params(
                DecisionOrigin.WIRE_PARAMETER, arr(origin.name()),
                PullDecisionWire.TRANSACTION_ID, arr(String.valueOf(TRANSACTION_ID)),
                PullDecisionWire.PARENT_DECISION_ID,
                        arr(String.valueOf(PARENT_DECISION_ID)),
                PullDecisionWire.PARENT_ACTION_ORDINAL,
                        arr(String.valueOf(PARENT_ACTION_ORDINAL)),
                PullDecisionWire.PLAYER_ID, arr(DECIDING_PLAYER),
                PullDecisionWire.SOURCE_CARD_ID, arr(SOURCE_CARD_ID),
                PullDecisionWire.SOURCE_PERMANENT_CARD_ID,
                        arr(SOURCE_PERMANENT_CARD_ID),
                PullDecisionWire.GAME_TEXT_ACTION_ID, arr(actionId.name()),
                PullDecisionWire.SOURCE_ZONE, arr(sourceZone.name()),
                PullDecisionWire.SOURCE_ZONE_OWNER, arr(DECIDING_PLAYER));
    }

    private static AwaitingDecision decision(int id,
                                             AwaitingDecisionType type,
                                             String text,
                                             Map<String, String[]> parameters) {
        return AbstractActivateControlDecisionHarnessTest.decision(
                id, type, text, parameters);
    }

    private static Map<String, String[]> params(Object... keyThenArray) {
        return AbstractActivateControlDecisionHarnessTest.params(keyThenArray);
    }

    private static String[] arr(String... values) {
        return AbstractActivateControlDecisionHarnessTest.arr(values);
    }
}
