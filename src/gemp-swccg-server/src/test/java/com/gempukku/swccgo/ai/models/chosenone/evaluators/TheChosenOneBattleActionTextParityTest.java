package com.gempukku.swccgo.ai.models.chosenone.evaluators;

import com.gempukku.swccgo.ai.models.common.phase.AbstractBattleActionTextParityTest;
import com.gempukku.swccgo.common.Phase;

import java.util.List;

/** ChosenOne adapter for the shared BATTLE tactic fixture matrix. */
public class TheChosenOneBattleActionTextParityTest
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
}
