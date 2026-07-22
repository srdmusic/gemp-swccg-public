package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ActivateActionTextPolicyParityTest {
    @Test
    public void fourCardFloorKeepsTheV61cPlusV383SumForBothBots() {
        var randoContext = randoContext("ACTION_CHOICE", "Choose action",
                List.of("activate"), List.of("Activate Force"));
        var chosenContext = chosenContext("ACTION_CHOICE", "Choose action",
                List.of("activate"), List.of("Activate Force"));

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenContext);

        assertEquals(1, rando.size());
        assertEquals(1, chosen.size());
        assertEquals(Float.floatToRawIntBits(-5500.0f),
                Float.floatToRawIntBits(rando.get(0).getScore()));
        assertEquals(Float.floatToRawIntBits(rando.get(0).getScore()),
                Float.floatToRawIntBits(chosen.get(0).getScore()));
        assertEquals(rando.get(0).getReasoning(), chosen.get(0).getReasoning());
    }

    @Test
    public void zeroActivationConfirmationKeepsExactYesNoRankingForBothBots() {
        List<String> ids = List.of("0", "1");
        List<String> labels = List.of("Yes", "No");
        var randoContext = randoContext("MULTIPLE_CHOICE",
                ActivateDecisionRouting.ZERO_CONFIRMATION_PROMPT, ids, labels);
        var chosenContext = chosenContext("MULTIPLE_CHOICE",
                ActivateDecisionRouting.ZERO_CONFIRMATION_PROMPT, ids, labels);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenContext);

        assertEquals(2, rando.size());
        assertEquals(2, chosen.size());
        assertEquals(Float.floatToRawIntBits(9999.0f),
                Float.floatToRawIntBits(rando.get(0).getScore()));
        assertEquals(Float.floatToRawIntBits(-9999.0f),
                Float.floatToRawIntBits(rando.get(1).getScore()));
        for (int i = 0; i < rando.size(); i++) {
            assertEquals(Float.floatToRawIntBits(rando.get(i).getScore()),
                    Float.floatToRawIntBits(chosen.get(i).getScore()));
            assertEquals(rando.get(i).getReasoning(), chosen.get(i).getReasoning());
        }
    }

    @Test
    public void malformedCombinedShapeDoesNotReplayTheTopLevelContribution() {
        var randoContext = randoContext("MULTIPLE_CHOICE",
                ActivateDecisionRouting.ZERO_CONFIRMATION_PROMPT,
                List.of("activate"), List.of("Activate Force"));
        var chosenContext = chosenContext("MULTIPLE_CHOICE",
                ActivateDecisionRouting.ZERO_CONFIRMATION_PROMPT,
                List.of("activate"), List.of("Activate Force"));

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenContext);

        assertEquals(1, rando.size());
        assertEquals(1, chosen.size());
        assertEquals(Float.floatToRawIntBits(-5500.0f),
                Float.floatToRawIntBits(rando.get(0).getScore()));
        assertEquals(Float.floatToRawIntBits(rando.get(0).getScore()),
                Float.floatToRawIntBits(chosen.get(0).getScore()));
        assertEquals(rando.get(0).getReasoning(), chosen.get(0).getReasoning());
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoContext(
            String type, String text, List<String> ids, List<String> labels) {
        var context = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                null, "tester", type, text, "activate-policy", Phase.ACTIVATE);
        context.setActionIds(ids);
        context.setActionTexts(labels);
        context.setCardIds(ids.stream().map(ignored -> "").toList());
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenContext(
            String type, String text, List<String> ids, List<String> labels) {
        var context = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                null, "tester", type, text, "activate-policy", Phase.ACTIVATE);
        context.setActionIds(ids);
        context.setActionTexts(labels);
        context.setCardIds(ids.stream().map(ignored -> "").toList());
        return context;
    }
}
