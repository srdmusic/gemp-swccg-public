package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class BattleEvaluatorAdapterParityTest {

    @Test
    public void bothBotsReplaySharedContributionsExactly() {
        com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoContext =
            new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                null, "bot", "CARD_ACTION_CHOICE", "Choose battle action", "1", Phase.BATTLE);
        randoContext.setActionIds(List.of("fire"));
        randoContext.setActionTexts(List.of("Fire weapon at unique character"));
        randoContext.setCardIds(List.of(""));

        com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenContext =
            new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                null, "bot", "CARD_ACTION_CHOICE", "Choose battle action", "1", Phase.BATTLE);
        chosenContext.setActionIds(List.of("fire"));
        chosenContext.setActionTexts(List.of("Fire weapon at unique character"));
        chosenContext.setCardIds(List.of(""));

        com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction rando =
            new com.gempukku.swccgo.ai.models.rando.evaluators.BattleEvaluator()
                .evaluate(randoContext).get(0);
        com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction chosen =
            new com.gempukku.swccgo.ai.models.chosenone.evaluators.BattleEvaluator()
                .evaluate(chosenContext).get(0);

        assertEquals(Float.floatToRawIntBits(220.0f), Float.floatToRawIntBits(rando.getScore()));
        assertEquals(Float.floatToRawIntBits(rando.getScore()), Float.floatToRawIntBits(chosen.getScore()));
        assertEquals(rando.getReasoningString(), chosen.getReasoningString());
        assertEquals(rando.isHardVetoed(), chosen.isHardVetoed());
    }
}
