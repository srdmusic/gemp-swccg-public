package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BattleForfeitAdapterParityTest {

    @Test
    public void bothBotsProtectTheLastRequiredActorWhenSurplusCanBeLost() {
        List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction>
                actions = evaluateFormationRoles(
                    ObjectiveAnalyzer.FlipGateFormationRole.LAST_REQUIRED_ACTOR,
                    ObjectiveAnalyzer.FlipGateFormationRole.NONE);

        assertBits(-9949.0f, actions.get(0).getScore());
        assertBits(50.0f, actions.get(1).getScore());
        assertTrue(actions.get(0).getReasoningString().contains(
                "preserve the last required actor while another legal loss exists"));
    }

    @Test
    public void bothBotsProtectTheLastRequiredBuddyWhenSurplusCanBeLost() {
        List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction>
                actions = evaluateFormationRoles(
                    ObjectiveAnalyzer.FlipGateFormationRole.LAST_REQUIRED_BUDDY,
                    ObjectiveAnalyzer.FlipGateFormationRole.NONE);

        assertBits(-9949.0f, actions.get(0).getScore());
        assertBits(50.0f, actions.get(1).getScore());
        assertTrue(actions.get(0).getReasoningString().contains(
                "preserve the required actor's last buddy while another legal loss exists"));
    }

    @Test
    public void bothBotsProtectTheSolePostFlipBlockerWhenSurplusCanBeLost() {
        List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction>
                actions = evaluateFormationRoles(
                    ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_FLIP_BACK_BLOCKER,
                    ObjectiveAnalyzer.FlipGateFormationRole.NONE);

        assertBits(-9949.0f, actions.get(0).getScore());
        assertBits(50.0f, actions.get(1).getScore());
        assertTrue(actions.get(0).getReasoningString().contains(
                "preserve the sole flip-back blocker while another legal loss exists"));
    }

    @Test
    public void bothBotsLeaveTheSurplusCandidateUnprotected() {
        List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction>
                actions = evaluateFormationRoles(
                    ObjectiveAnalyzer.FlipGateFormationRole.LAST_REQUIRED_ACTOR,
                    ObjectiveAnalyzer.FlipGateFormationRole.LAST_REQUIRED_BUDDY,
                    ObjectiveAnalyzer.FlipGateFormationRole.NONE);

        assertBits(-9949.0f, actions.get(0).getScore());
        assertBits(-9949.0f, actions.get(1).getScore());
        assertBits(50.0f, actions.get(2).getScore());
        assertFalse(actions.get(2).getReasoningString().contains(
                "BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD"));
    }

    @Test
    public void bothBotsDoNotBlockAnUnavoidableMandatoryFormationLoss() {
        List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction>
                actions = evaluateFormationRoles(
                    ObjectiveAnalyzer.FlipGateFormationRole.LAST_REQUIRED_ACTOR,
                    ObjectiveAnalyzer.FlipGateFormationRole.LAST_REQUIRED_BUDDY);

        assertBits(50.0f, actions.get(0).getScore());
        assertBits(50.0f, actions.get(1).getScore());
        assertFalse(actions.get(0).getReasoningString().contains(
                "BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD"));
        assertFalse(actions.get(1).getReasoningString().contains(
                "BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD"));
    }

    @Test
    public void objectiveProtectionStillAppliesWhenBlueprintIsNull() {
        GameState gameState = mock(GameState.class);
        PhysicalCard card = mock(PhysicalCard.class);
        when(gameState.findCardById(7)).thenReturn(card);
        when(card.getBlueprint()).thenReturn(null);
        when(card.getTitle()).thenReturn("Objective Card");

        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer randoAnalyzer =
                mock(com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        when(randoAnalyzer.isAnalyzed()).thenReturn(true);
        when(randoAnalyzer.isRequiredCardForFlip("Objective Card")).thenReturn(true);
        com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoContext =
                new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                        gameState, "bot", "CARD_SELECTION",
                        "Choose a card from battle to forfeit", "decision", Phase.BATTLE);
        randoContext.setCardIds(List.of("7"));
        randoContext.setObjectiveAnalyzer(randoAnalyzer);

        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer chosenAnalyzer =
                mock(com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        when(chosenAnalyzer.isAnalyzed()).thenReturn(true);
        when(chosenAnalyzer.isRequiredCardForFlip("Objective Card")).thenReturn(true);
        com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                        gameState, "bot", "CARD_SELECTION",
                        "Choose a card from battle to forfeit", "decision", Phase.BATTLE);
        chosenContext.setCardIds(List.of("7"));
        chosenContext.setObjectiveAnalyzer(chosenAnalyzer);

        com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction rando =
                new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                        .evaluate(randoContext).get(0);
        com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction chosen =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                        .evaluate(chosenContext).get(0);

        assertEquals(Float.floatToRawIntBits(-9949.0f),
                Float.floatToRawIntBits(rando.getScore()));
        assertEquals(Float.floatToRawIntBits(rando.getScore()),
                Float.floatToRawIntBits(chosen.getScore()));
        assertEquals(rando.getReasoningString(), chosen.getReasoningString());
        assertTrue(rando.getReasoningString().contains(
                "OBJECTIVE CRITICAL - NEVER FORFEIT!"));
        assertFalse(rando.isHardVetoed());
        assertFalse(chosen.isHardVetoed());
    }

    private static List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction>
            evaluateFormationRoles(
                    ObjectiveAnalyzer.FlipGateFormationRole... roles) {
        GameState gameState = mock(GameState.class);
        List<String> cardIds = new ArrayList<>();

        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer
                randoAnalyzer = mock(
                    com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer.class);
        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer
                chosenAnalyzer = mock(
                    com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer.class);

        for (int index = 0; index < roles.length; index++) {
            String cardId = String.valueOf(70 + index);
            PhysicalCard card = mock(PhysicalCard.class);
            cardIds.add(cardId);
            when(gameState.findCardById(Integer.parseInt(cardId)))
                    .thenReturn(card);
            when(card.getTitle()).thenReturn("Candidate " + cardId);
            when(randoAnalyzer.classifyGateFormationPieceIfRemoved(
                    null, "bot", card)).thenReturn(roles[index]);
            when(chosenAnalyzer.classifyGateFormationPieceIfRemoved(
                    null, "bot", card)).thenReturn(roles[index]);
        }

        com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext
                randoContext =
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                        .DecisionContext(
                            gameState, "bot", "CARD_SELECTION",
                            "Choose a card from battle to forfeit",
                            "formation-forfeit", Phase.BATTLE);
        randoContext.setCardIds(cardIds);
        randoContext.setObjectiveAnalyzer(randoAnalyzer);

        com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext
                chosenContext =
                    new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .DecisionContext(
                            gameState, "bot", "CARD_SELECTION",
                            "Choose a card from battle to forfeit",
                            "formation-forfeit", Phase.BATTLE);
        chosenContext.setCardIds(cardIds);
        chosenContext.setObjectiveAnalyzer(chosenAnalyzer);

        List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction>
                rando =
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                        .CardSelectionEvaluator().evaluate(randoContext);
        List<com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction>
                chosen =
                    new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .CardSelectionEvaluator().evaluate(chosenContext);

        assertEquals(rando.size(), chosen.size());
        for (int index = 0; index < rando.size(); index++) {
            assertEquals(rando.get(index).getActionId(),
                    chosen.get(index).getActionId());
            assertBits(rando.get(index).getScore(),
                    chosen.get(index).getScore());
            assertEquals(rando.get(index).getReasoningString(),
                    chosen.get(index).getReasoningString());
        }
        return rando;
    }

    private static void assertBits(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(actual));
    }
}
