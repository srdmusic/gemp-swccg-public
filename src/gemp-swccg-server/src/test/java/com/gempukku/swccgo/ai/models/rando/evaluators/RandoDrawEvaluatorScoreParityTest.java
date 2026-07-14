package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.common.evaluators.AbstractDrawEvaluatorScoreParityTest;
import com.gempukku.swccgo.ai.models.common.trace.TraceTestSupport;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;

import java.util.ArrayList;
import java.util.List;

/** Rando package adapter for the shared DRAW evaluator score contract. */
public class RandoDrawEvaluatorScoreParityTest extends AbstractDrawEvaluatorScoreParityTest {

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
        return context;
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
}
