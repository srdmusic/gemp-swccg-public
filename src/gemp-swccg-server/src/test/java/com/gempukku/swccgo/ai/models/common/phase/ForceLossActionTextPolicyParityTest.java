package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ForceLossActionTextPolicyParityTest {

    @Test
    public void genericRoutesStayExactAndMirrored() {
        assertRoutes("Choose an action",
                List.of("place", "upkeep", "use", "lose", "sacrifice"),
                List.of(
                        "Place in Lost Pile",
                        "Use 1 Force for upkeep",
                        "Use 1 Force now",
                        "Lose 1 Force now",
                        "Sacrifice this card"),
                List.of(-50.0f, 150.0f, -20.0f, -30.0f, -150.0f),
                List.of(
                        "Avoid losing cards (-50.0)",
                        "V22.3 MAINTENANCE: Pay upkeep cost! (+150.0)",
                        "'Use Force' action \u2014 prefer not to use force unnecessarily (-20.0)",
                        "'Lose Force' action \u2014 avoid losing force (-30.0)",
                        "V22.3: Avoid sacrificing cards \u2014 prefer alternatives (-150.0)"));
    }

    @Test
    public void maintenanceRoutesStayExactOrderedAndMirrored() {
        assertRoutes("Choose a maintenance option",
                List.of("pay", "out", "used", "sacrifice"),
                List.of(
                        "Use 1 Force now",
                        "Place this card out of play",
                        "Lose 1 Force and place in Used Pile",
                        "Sacrifice this card"),
                List.of(400.0f, -800.0f, -200.0f, -800.0f),
                List.of(
                        "V74 MAINTENANCE PAY: keep the card alive! (+400.0)",
                        "V74 MAINTENANCE SACRIFICE: place out of play is PERMANENT loss! (-800.0)",
                        "V74 MAINTENANCE USED-PILE: lose card to used pile, keep blueprint (-200.0)",
                        "V74 MAINTENANCE SACRIFICE: avoid (-800.0)"));
    }

    private static void assertRoutes(String decisionText, List<String> ids,
                                     List<String> texts, List<Float> scores,
                                     List<String> reasons) {
        var randoContext = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                null, "tester", "ACTION_CHOICE", decisionText,
                "force-loss-action-text", Phase.CONTROL);
        var chosenContext = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                null, "tester", "ACTION_CHOICE", decisionText,
                "force-loss-action-text", Phase.CONTROL);
        randoContext.setActionIds(ids);
        randoContext.setActionTexts(texts);
        randoContext.setCardIds(java.util.Collections.nCopies(ids.size(), ""));
        chosenContext.setActionIds(ids);
        chosenContext.setActionTexts(texts);
        chosenContext.setCardIds(java.util.Collections.nCopies(ids.size(), ""));

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenContext);

        assertEquals(ids.size(), rando.size());
        assertEquals(ids.size(), chosen.size());
        for (int i = 0; i < ids.size(); i++) {
            assertEquals(ids.get(i), rando.get(i).getActionId());
            assertEquals(rando.get(i).getActionId(), chosen.get(i).getActionId());
            assertBits(scores.get(i), rando.get(i).getScore());
            assertBits(rando.get(i).getScore(), chosen.get(i).getScore());
            assertEquals(List.of(reasons.get(i)), rando.get(i).getReasoning());
            assertEquals(rando.get(i).getReasoning(), chosen.get(i).getReasoning());
        }
    }

    private static void assertBits(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(actual));
    }
}
