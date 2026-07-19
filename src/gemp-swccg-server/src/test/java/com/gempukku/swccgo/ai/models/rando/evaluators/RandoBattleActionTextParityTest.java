package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.common.phase.AbstractBattleActionTextParityTest;
import com.gempukku.swccgo.ai.models.common.trace.NoOpTraceSink;
import com.gempukku.swccgo.common.Phase;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Rando adapter for the shared BATTLE tactic fixture matrix. */
public class RandoBattleActionTextParityTest
        extends AbstractBattleActionTextParityTest {

    @Override
    protected Score evaluate(String actionText) {
        DecisionContext context = new DecisionContext(
                null, "bot", "CARD_ACTION_CHOICE",
                "Choose battle action", "1", Phase.BATTLE);
        context.setActionIds(List.of("0"));
        context.setActionTexts(List.of(actionText));
        context.setCardIds(List.of(""));
        EvaluatedAction action = new ActionTextEvaluator().evaluate(context).get(0);
        return new Score(action.getScore(), action.isHardVetoed(),
                action.getReasoningString());
    }

    @Test
    public void battleOneAndV25MergeAdditivelyThroughCombinedEvaluator() {
        DecisionContext context = initiationContext();
        BattleEvaluator battleEvaluator = new BattleEvaluator();
        ActionTextEvaluator actionTextEvaluator = new ActionTextEvaluator();

        EvaluatedAction battleOne = battleEvaluator.evaluate(context).get(0);
        EvaluatedAction v25 = actionTextEvaluator.evaluate(context).get(0);
        EvaluatedAction combined = new CombinedEvaluator(
                List.of(battleEvaluator, actionTextEvaluator),
                NoOpTraceSink.INSTANCE).evaluateDecision(context);

        assertEquals(
                Float.floatToRawIntBits(battleOne.getScore() + v25.getScore()),
                Float.floatToRawIntBits(combined.getScore()));
        assertTrue(combined.getReasoningString().contains(
                "V25 BATTLE: Initiate battle (no location data)"));
        assertTrue(combined.getReasoningString().contains(
                "V25 BATTLE: Low reserve (0)"));
    }

    private static DecisionContext initiationContext() {
        DecisionContext context = new DecisionContext(
                null, "bot", "CARD_ACTION_CHOICE",
                "Choose battle action", "1", Phase.BATTLE);
        context.setActionIds(List.of("0"));
        context.setActionTexts(List.of("Initiate battle"));
        context.setCardIds(List.of(""));
        context.setNoPass(true);
        context.setMin(1);
        return context;
    }
}
