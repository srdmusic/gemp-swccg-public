package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PullCardSelectionCharacterizationTest {
    private static final String PLAYER = "tester";

    @Test
    public void takeChildSortsDescendingKeepsStableTiesSkipsNonSelectableAndReturnsCandidateIds() {
        GameState gameState = mock(GameState.class);
        PhysicalCard alpha = card("Alpha", 6.0f);
        PhysicalCard beta = card("Beta", 5.0f);
        PhysicalCard skipped = card("Skipped", 6.0f);
        PhysicalCard gamma = card("Gamma", 5.0f);
        when(gameState.getPlayersLatestTurnNumber(PLAYER)).thenReturn(4);
        when(gameState.getCurrentPlayerId()).thenReturn(PLAYER);
        when(gameState.findCardById(101)).thenReturn(alpha);
        when(gameState.findCardById(102)).thenReturn(beta);
        when(gameState.findCardById(103)).thenReturn(skipped);
        when(gameState.findCardById(104)).thenReturn(gamma);

        var randoContext = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                gameState, PLAYER, "CARD_SELECTION", "Choose card to take into hand",
                "take-child-rando", Phase.DEPLOY);
        configure(randoContext);

        var chosenContext = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                gameState, PLAYER, "CARD_SELECTION", "Choose card to take into hand",
                "take-child-chosen", Phase.DEPLOY);
        configure(chosenContext);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext);

        assertEquals(List.of("101", "102", "104"),
                rando.stream().map(action -> action.getActionId()).toList());
        assertEquals(List.of("Alpha", "Beta", "Gamma"),
                rando.stream().map(action -> action.getCardName()).toList());
        assertEquals(List.of(
                        Float.floatToRawIntBits(110.0f),
                        Float.floatToRawIntBits(90.0f),
                        Float.floatToRawIntBits(90.0f)),
                rando.stream().map(action -> Float.floatToRawIntBits(action.getScore())).toList());

        assertEquals(rando.size(), chosen.size());
        for (int i = 0; i < rando.size(); i++) {
            assertEquals(rando.get(i).getActionId(), chosen.get(i).getActionId());
            assertEquals(Float.floatToRawIntBits(rando.get(i).getScore()),
                    Float.floatToRawIntBits(chosen.get(i).getScore()));
            assertEquals(rando.get(i).getReasoning(), chosen.get(i).getReasoning());
        }
    }

    @Test
    public void v70DeployCandidateBlockKeepsLegacyMagnitudeAndStopsCandidate() {
        PullDeployCandidatePolicy.Evaluation evaluation =
                PullDeployCandidatePolicy.evaluate(new PullDeployCandidateFacts(
                        "101", "Leia's Lightsaber", "all legal holders already armed"));

        assertEquals(PullDeployCandidatePolicy.AdapterStep.CONTINUE_CANDIDATE,
                evaluation.adapterStep());
        assertEquals(1, evaluation.result().operations().size());
        assertEquals(Float.floatToRawIntBits(-9999.0f),
                Float.floatToRawIntBits(evaluation.result().operations().get(0).delta()));
        assertEquals("V70-reserve-candidate",
                evaluation.result().operations().get(0).ruleArmId().id());
        assertTrue(evaluation.result().operations().get(0).reason()
                .contains("Leia's Lightsaber"));
    }

    @Test
    public void v70DeployCandidateWithoutBlockFallsThroughWithoutContribution() {
        PullDeployCandidatePolicy.Evaluation evaluation =
                PullDeployCandidatePolicy.evaluate(new PullDeployCandidateFacts(
                        "101", "Leia's Lightsaber", ""));

        assertEquals(PullDeployCandidatePolicy.AdapterStep.FALL_THROUGH,
                evaluation.adapterStep());
        assertTrue(evaluation.result().operations().isEmpty());
    }

    private static void configure(
            com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext context) {
        context.setCardIds(List.of("101", "102", "103", "104"));
        context.setBlueprints(List.of("inPlay", "inPlay", "inPlay", "inPlay"));
        context.setSelectable(List.of(true, true, false, true));
        context.setTestingTexts(List.of("Alpha", "Beta", "Skipped", "Gamma"));
    }

    private static void configure(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext context) {
        context.setCardIds(List.of("101", "102", "103", "104"));
        context.setBlueprints(List.of("inPlay", "inPlay", "inPlay", "inPlay"));
        context.setSelectable(List.of(true, true, false, true));
        context.setTestingTexts(List.of("Alpha", "Beta", "Skipped", "Gamma"));
    }

    private static PhysicalCard card(String title, float destiny) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getDestiny()).thenReturn(destiny);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.EFFECT);
        return card;
    }
}
