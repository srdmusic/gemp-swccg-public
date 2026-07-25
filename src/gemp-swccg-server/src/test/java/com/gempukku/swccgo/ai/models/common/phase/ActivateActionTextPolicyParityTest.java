package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    public void objectiveActivateOneForceBeatsPassDuringMoveForBothBots() {
        GameState gameState = mock(GameState.class);
        when(gameState.getReserveDeckSize("tester")).thenReturn(10);
        when(gameState.getPlayersLatestTurnNumber("tester")).thenReturn(1);
        when(gameState.getCurrentPlayerId()).thenReturn("tester");

        List<String> ids = List.of("activate-one", "pass");
        List<String> labels = List.of("Activate 1 Force", "Pass");
        var randoContext = randoContext(
                gameState, Phase.MOVE, "ACTION_CHOICE",
                "Choose action", ids, labels);
        var chosenContext = chosenContext(
                gameState, Phase.MOVE, "ACTION_CHOICE",
                "Choose action", ids, labels);

        var rando =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                        .ActionTextEvaluator()
                        .evaluate(randoContext);
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .ActionTextEvaluator()
                        .evaluate(chosenContext);

        assertEquals(2, rando.size());
        assertEquals(2, chosen.size());
        assertEquals(Float.floatToRawIntBits(5500.0f),
                Float.floatToRawIntBits(rando.get(0).getScore()));
        assertTrue(rando.get(0).getScore() > rando.get(1).getScore());
        assertTrue(rando.get(0).getReasoning().stream().anyMatch(
                reason -> reason.contains("V168 ALWAYS ACTIVATE")));
        assertTrue(rando.get(0).getReasoning().stream().anyMatch(
                reason -> reason.contains("V38.3 ALWAYS ACTIVATE")));
        for (int i = 0; i < rando.size(); i++) {
            assertEquals(Float.floatToRawIntBits(rando.get(i).getScore()),
                    Float.floatToRawIntBits(chosen.get(i).getScore()));
            assertEquals(rando.get(i).getReasoning(),
                    chosen.get(i).getReasoning());
        }
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoContext(
            String type, String text, List<String> ids, List<String> labels) {
        return randoContext(
                null, Phase.ACTIVATE, type, text, ids, labels);
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoContext(
            GameState gameState, Phase phase, String type, String text,
            List<String> ids, List<String> labels) {
        var context =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                        .DecisionContext(
                        gameState, "tester", type, text,
                        "activate-policy", phase);
        context.setActionIds(ids);
        context.setActionTexts(labels);
        context.setCardIds(ids.stream().map(ignored -> "").toList());
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenContext(
            String type, String text, List<String> ids, List<String> labels) {
        return chosenContext(
                null, Phase.ACTIVATE, type, text, ids, labels);
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenContext(
            GameState gameState, Phase phase, String type, String text,
            List<String> ids, List<String> labels) {
        var context =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .DecisionContext(
                        gameState, "tester", type, text,
                        "activate-policy", phase);
        context.setActionIds(ids);
        context.setActionTexts(labels);
        context.setCardIds(ids.stream().map(ignored -> "").toList());
        return context;
    }
}
