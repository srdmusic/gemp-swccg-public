package com.gempukku.swccgo.ai.models.chosenone.evaluators;

import com.gempukku.swccgo.ai.models.common.evaluators.AbstractDrawEvaluatorScoreParityTest;
import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceTestSupport;
import com.gempukku.swccgo.ai.models.common.trace.TraceOp;
import com.gempukku.swccgo.ai.models.common.trace.TraceOperation;
import com.gempukku.swccgo.ai.models.common.trace.TraceSnapshots;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Chosen One package adapter for the shared DRAW evaluator score contract. */
public class TheChosenOneDrawEvaluatorScoreParityTest extends AbstractDrawEvaluatorScoreParityTest {

    @Override
    protected Captured runScenario(Scenario scenario) {
        DecisionContext context = context(scenario);
        List<ActionEvaluator> evaluators = new ArrayList<>();
        evaluators.add(alwaysApplicable(new DrawEvaluator()));
        if (scenario.drawAdjustment() != null) {
            evaluators.add(adjustment(scenario));
        }
        if (scenario.includeActionText()) {
            evaluators.add(new ActionTextEvaluator());
        }
        if (scenario.includePass()) {
            evaluators.add(new PassEvaluator());
        }

        TraceTestSupport.StrictFixtureSink sink = new TraceTestSupport.StrictFixtureSink();
        EvaluatedAction winner = new CombinedEvaluator(evaluators, sink).evaluateDecision(context);
        return new Captured(winner.getActionId(), Float.floatToRawIntBits(winner.getScore()),
            winner.isHardVetoed(), winner.getReasoningString(), sink.single());
    }

    @Test
    public void fullEightEvaluatorDrawWindowKeepsBattleCrossTalkTieAndPassLedger() {
        DecisionContext context = fullStackContext();

        List<ActionEvaluator> evaluators = new ArrayList<>(new CombinedEvaluator().getEvaluators());
        assertEquals(List.of("ForceActivation", "Deploy", "Battle", "Move", "Draw",
            "CardSelection", "ActionText", "Pass"),
            evaluators.stream().map(ActionEvaluator::getName).toList());
        assertTrue(evaluators.get(2) instanceof BattleEvaluator);

        // The pure harness has no GameState, so preserve the real Draw scoring via its delegate.
        evaluators.set(4, alwaysApplicable(evaluators.get(4)));
        TraceTestSupport.StrictFixtureSink sink = new TraceTestSupport.StrictFixtureSink();
        EvaluatedAction winner = new CombinedEvaluator(evaluators, sink).evaluateDecision(context);
        DecisionTrace trace = sink.single();

        assertEquals("deploy-a", winner.getActionId());
        assertEquals(ActionType.BATTLE, winner.getActionType());
        assertEquals(bits(100.0f), bits(winner.getScore()));
        assertEquals(Boolean.TRUE, trace.getFinalization().passEligible());
        assertEquals(List.of("draw", "deploy-a", "deploy-b"), trace.getRawCandidateOrder());
        assertEquals(List.of("deploy-a", "deploy-b", "draw", ""), trace.getMergeOrder());
        record Op(TraceOp type, int ordinal, String actionId, String evaluatorId,
                  Integer beforeBits, Integer deltaBits, Integer afterBits, boolean vetoed) {
        }
        List<Op> operations = trace.getOperations().stream()
            .map(operation -> new Op(operation.getOp(), operation.getCandidateOrdinal(),
                operation.getActionId(), operation.getEvaluatorId(), operation.getBeforeBits(),
                operation.getDeltaBits(), operation.getAfterBits(), operation.isVetoed()))
            .toList();
        assertEquals(List.of(
            new Op(TraceOp.INITIAL, 1, "deploy-a", "Battle", null, null, bits(100.0f), false),
            new Op(TraceOp.INITIAL, 2, "deploy-b", "Battle", null, null, bits(100.0f), false),
            new Op(TraceOp.INITIAL, 0, "draw", "Draw", null, null, bits(0.0f), false),
            new Op(TraceOp.ADD, 0, "draw", "Draw", bits(0.0f), bits(0.0f), bits(0.0f), false),
            new Op(TraceOp.INITIAL, 0, "draw", "ActionText", null, null, bits(0.0f), false),
            new Op(TraceOp.ADD, 0, "draw", "ActionText", bits(0.0f), bits(0.0f), bits(0.0f), false),
            new Op(TraceOp.INITIAL, 1, "deploy-a", "ActionText", null, null, bits(0.0f), false),
            new Op(TraceOp.INITIAL, 2, "deploy-b", "ActionText", null, null, bits(0.0f), false),
            new Op(TraceOp.MERGE, 0, "draw", "ActionText", bits(0.0f), null, bits(0.0f), false),
            new Op(TraceOp.INITIAL, TraceOperation.ORDINAL_UNKNOWN, "", "Pass",
                null, null, bits(5.0f), false),
            new Op(TraceOp.ADD, TraceOperation.ORDINAL_UNKNOWN, "", "Pass",
                bits(5.0f), bits(0.0f), bits(5.0f), false),
            new Op(TraceOp.ADD, TraceOperation.ORDINAL_UNKNOWN, "", "Pass",
                bits(5.0f), bits(-3.0f), bits(PASS_SCORE), false),
            new Op(TraceOp.RANK, 1, "deploy-a", TraceOperation.PRODUCER_COMBINED_EVALUATOR,
                null, null, bits(100.0f), false),
            new Op(TraceOp.SELECT, 1, "deploy-a", TraceOperation.PRODUCER_COMBINED_EVALUATOR,
                null, null, bits(100.0f), false)
        ), operations);
    }

    private static DecisionContext fullStackContext() {
        DecisionContext context = new DecisionContext(null, "tester", "CARD_ACTION_CHOICE",
            "Choose draw action or Pass", "draw-parity-full-stack", Phase.DRAW);
        context.setNoPass(false);
        context.setMin(0);
        context.setSide(Side.DARK);
        context.setActionIds(List.of("draw", "deploy-a", "deploy-b"));
        context.setActionTexts(List.of(
            CANONICAL_DRAW_TEXT,
            "Deploy unique Alpha to battleground site",
            "Deploy unique Beta to battleground site"));
        attachSnapshot(context);
        return context;
    }

    private static DecisionContext context(Scenario scenario) {
        DecisionContext context = new DecisionContext(null, "tester", "CARD_ACTION_CHOICE",
            "Choose draw action or Pass", "draw-parity", Phase.DRAW);
        context.setNoPass(scenario.noPass());
        context.setMin(0);
        context.setSide(Side.DARK);
        context.setBlockedResponses(scenario.blockedResponses());
        List<String> actionIds = new ArrayList<>();
        List<String> actionTexts = new ArrayList<>();
        for (Candidate candidate : scenario.candidates()) {
            actionIds.add(candidate.actionId());
            actionTexts.add(candidate.actionText());
        }
        context.setActionIds(actionIds);
        context.setActionTexts(actionTexts);
        attachSnapshot(context);
        return context;
    }

    private static void attachSnapshot(DecisionContext context) {
        TraceSnapshots.Input in = new TraceSnapshots.Input();
        in.producerId = "draw-parity-test";
        in.decisionId = context.getDecisionId();
        in.decisionTypeName = context.getDecisionType();
        in.decisionText = context.getDecisionText();
        in.phase = context.getPhase();
        in.turn = context.getTurnNumber();
        in.currentPlayer = context.getPlayerId();
        in.side = context.getSide();
        in.noPassParam = context.isNoPass();
        in.minParam = context.getMin();
        in.maxParam = context.getMax();
        in.blockedResponses = context.getBlockedResponses();
        in.actionIds = TraceSnapshots.contextListOrAbsent(context.getActionIds());
        in.actionTexts = TraceSnapshots.contextListOrAbsent(context.getActionTexts());
        in.cardIds = TraceSnapshots.contextListOrAbsent(context.getCardIds());
        in.blueprintIds = TraceSnapshots.contextListOrAbsent(context.getBlueprints());
        in.testingTexts = TraceSnapshots.contextListOrAbsent(context.getTestingTexts());
        in.selectable = TraceSnapshots.contextListOrAbsent(context.getSelectable());
        TraceSnapshots.Result result = TraceSnapshots.build(in);
        if (result.snapshot() == null) {
            throw new AssertionError("test snapshot construction failed: " + result.issues());
        }
        context.setDecisionSnapshot(result.snapshot());
    }

    private static ActionEvaluator alwaysApplicable(ActionEvaluator delegate) {
        return new ActionEvaluator(delegate.getName()) {
            @Override
            public boolean canEvaluate(DecisionContext context) {
                return true;
            }

            @Override
            public List<EvaluatedAction> evaluate(DecisionContext context) {
                return delegate.evaluate(context);
            }
        };
    }

    private static ActionEvaluator adjustment(Scenario scenario) {
        return new ActionEvaluator("DrawMargin") {
            @Override
            public boolean canEvaluate(DecisionContext context) {
                return true;
            }

            @Override
            public List<EvaluatedAction> evaluate(DecisionContext context) {
                Candidate draw = scenario.candidates().get(0);
                return List.of(new EvaluatedAction(draw.actionId(), ActionType.DRAW,
                    scenario.drawAdjustment(), draw.actionText()));
            }
        };
    }

    private static int bits(float value) {
        return Float.floatToRawIntBits(value);
    }
}
