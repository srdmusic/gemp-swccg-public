package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployEvaluatorEnvelopeParityTest {

    @Test
    public void blockedResponseRetainsDoubleScoreAndTerminalRouting() {
        Pair pair = evaluate("deploy", "Deploy a character", Set.of("deploy"));
        assertMirrored(pair);
        assertRaw(-19998.0f, pair.rando.getScore());
        assertTrue(pair.rando.getReasoningString().contains(
                "CANCEL-LOOP BLOCK"));
        assertFalse(pair.rando.isHardVetoed());
    }

    @Test
    public void personaReplacementRetainsDoubleScoreAndTerminalRouting() {
        Pair pair = evaluate("persona", "Persona replace Darth Vader", Set.of());
        assertMirrored(pair);
        assertRaw(-1000.0f, pair.rando.getScore());
        assertTrue(pair.rando.getReasoningString().contains(
                "V38.4 PERSONA REPLACE"));
    }

    @Test
    public void turnOneEffectRetainsBasePlusTerminalPenalty() {
        Pair pair = evaluate("effect", "Deploy No Escape", Set.of());
        assertMirrored(pair);
        assertRaw(-9949.0f, pair.rando.getScore());
        assertTrue(pair.rando.getReasoningString().contains(
                "Do not deploy this Effect on turn 1"));
    }

    @Test
    public void locationRetainsBasePlusPriorityAndTerminalRouting() {
        Pair pair = evaluate("location", "Deploy Bespin system location", Set.of());
        assertMirrored(pair);
        assertRaw(250.0f, pair.rando.getScore());
        assertEquals(1, pair.rando.getReasoning().size());
        assertTrue(pair.rando.getReasoningString().contains(
                "LOCATION - deploy first!"));
    }

    @Test
    public void textOnlySiteDestinationFallsThroughToNormalScoring() {
        Pair pair = evaluate("character", "Deploy character to a site", Set.of());
        assertMirrored(pair);
        assertRaw(50.0f, pair.rando.getScore());
        assertEquals(1, pair.rando.getReasoning().size());
        assertTrue(pair.rando.getReasoningString().contains(
                "V40: Unknown card"));
        assertFalse(pair.rando.getReasoningString().contains(
                "LOCATION - deploy first!"));
    }

    private static Pair evaluate(
            String actionId, String actionText, Set<String> blocked) {
        var randoContext =
                new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                        null, "bot", "CARD_ACTION_CHOICE", "Choose deploy action",
                        "envelope-parity", Phase.DEPLOY);
        randoContext.setActionIds(List.of(actionId));
        randoContext.setActionTexts(List.of(actionText));
        randoContext.setTestingTexts(List.of(
                actionText.equals("Deploy No Escape") ? "No Escape" : ""));
        randoContext.setBlockedResponses(blocked);

        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                        null, "bot", "CARD_ACTION_CHOICE", "Choose deploy action",
                        "envelope-parity", Phase.DEPLOY);
        chosenContext.setActionIds(List.of(actionId));
        chosenContext.setActionTexts(List.of(actionText));
        chosenContext.setTestingTexts(List.of(
                actionText.equals("Deploy No Escape") ? "No Escape" : ""));
        chosenContext.setBlockedResponses(blocked);

        return new Pair(
                new com.gempukku.swccgo.ai.models.rando.evaluators.DeployEvaluator()
                        .evaluate(randoContext).get(0),
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.DeployEvaluator()
                        .evaluate(chosenContext).get(0));
    }

    private static void assertMirrored(Pair pair) {
        assertEquals(pair.rando.getActionId(), pair.chosen.getActionId());
        assertRaw(pair.rando.getScore(), pair.chosen.getScore());
        assertEquals(pair.rando.getReasoning(), pair.chosen.getReasoning());
    }

    private static void assertRaw(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(actual));
    }

    private static final class Pair {
        private final com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction rando;
        private final com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction chosen;

        private Pair(
                com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction rando,
                com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction chosen) {
            this.rando = rando;
            this.chosen = chosen;
        }
    }
}
