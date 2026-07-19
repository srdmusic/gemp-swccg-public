package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BattleForfeitAdapterParityTest {

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
}
