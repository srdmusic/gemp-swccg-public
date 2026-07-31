package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    public void exactStillNeededShieldCannonIgnoresOnlyItsStalePositionalId() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard cannon = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint =
                mock(SwccgCardBlueprint.class);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getCurrentPlayerId()).thenReturn("bot");
        when(gameState.findCardById(77)).thenReturn(cannon);
        when(cannon.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory()).thenReturn(
                com.gempukku.swccgo.common.CardCategory.WEAPON);

        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer.class);
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer.class);
        when(randoAnalyzer
                .isShieldMainGeneratorPriorityCannonDeployAction(
                    game, "bot", cannon, "Deploy"))
                .thenReturn(true);
        when(chosenAnalyzer
                .isShieldMainGeneratorPriorityCannonDeployAction(
                    game, "bot", cannon, "Deploy"))
                .thenReturn(true);

        var randoContext =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext(
                        gameState, "bot",
                        "CARD_ACTION_CHOICE",
                        "Choose deploy action",
                        "shield-cannon-stale-id",
                        Phase.DEPLOY);
        randoContext.setGame(game);
        randoContext.setObjectiveAnalyzer(randoAnalyzer);
        randoContext.setActionIds(List.of("4", "pass"));
        randoContext.setActionTexts(List.of("Deploy", "Pass"));
        randoContext.setCardIds(List.of("77", "77"));
        randoContext.setBlockedResponses(Set.of("4"));

        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext(
                        gameState, "bot",
                        "CARD_ACTION_CHOICE",
                        "Choose deploy action",
                        "shield-cannon-stale-id",
                        Phase.DEPLOY);
        chosenContext.setGame(game);
        chosenContext.setObjectiveAnalyzer(chosenAnalyzer);
        chosenContext.setActionIds(List.of("4", "pass"));
        chosenContext.setActionTexts(List.of("Deploy", "Pass"));
        chosenContext.setCardIds(List.of("77", "77"));
        chosenContext.setBlockedResponses(Set.of("4"));

        var randoDeploy =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .DeployEvaluator()
                    .evaluate(randoContext).get(0);
        var chosenDeploy =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DeployEvaluator()
                    .evaluate(chosenContext).get(0);
        assertFalse(randoDeploy.getReasoningString().contains(
                "CANCEL-LOOP BLOCK"));
        assertFalse(chosenDeploy.getReasoningString().contains(
                "CANCEL-LOOP BLOCK"));
        assertRaw(randoDeploy.getScore(),
                chosenDeploy.getScore());

        var randoExactWinner =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CombinedEvaluator()
                    .evaluateDecision(randoContext);
        var chosenExactWinner =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .CombinedEvaluator()
                    .evaluateDecision(chosenContext);
        assertEquals("4", randoExactWinner.getActionId());
        assertEquals("4", chosenExactWinner.getActionId());

        randoContext.setActionTexts(
                List.of("Deploy to a different target", "Pass"));
        chosenContext.setActionTexts(
                List.of("Deploy to a different target", "Pass"));
        var randoNearMatchWinner =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CombinedEvaluator()
                    .evaluateDecision(randoContext);
        var chosenNearMatchWinner =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .CombinedEvaluator()
                    .evaluateDecision(chosenContext);
        assertEquals("pass", randoNearMatchWinner.getActionId());
        assertEquals("pass", chosenNearMatchWinner.getActionId());
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
