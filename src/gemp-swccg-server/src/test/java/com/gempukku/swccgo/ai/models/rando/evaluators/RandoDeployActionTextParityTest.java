package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.common.phase.AbstractDeployActionTextParityTest;
import com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Rando adapter for the shared DEPLOY action-text fixture matrix. */
public class RandoDeployActionTextParityTest
        extends AbstractDeployActionTextParityTest {

    @Override
    protected Score evaluate(String actionText) {
        DecisionContext context = new DecisionContext(
                null, "bot", "CARD_ACTION_CHOICE",
                "Choose deploy action", "1", Phase.DEPLOY);
        context.setActionIds(List.of("0"));
        context.setActionTexts(List.of(actionText));
        context.setCardIds(List.of(""));
        EvaluatedAction action = new ActionTextEvaluator().evaluate(context).get(0);
        return new Score(action.getScore(), action.isHardVetoed(),
                action.getReasoningString());
    }

    @Test
    public void approvedAmsdStacksAndMissingPiettKeepsTerminalMutation() {
        GameState gameState = bespinGameState();

        DeckOracle approvedOracle = mock(DeckOracle.class);
        when(approvedOracle.isAnalyzed()).thenReturn(true);
        when(approvedOracle.isCardInHand("Admiral Piett")).thenReturn(true);
        when(approvedOracle.isCardInHand("Executor")).thenReturn(true);
        EvaluatedAction approved = evaluateAmsd(gameState, approvedOracle);
        assertEquals(Float.floatToRawIntBits(1800.0f),
                Float.floatToRawIntBits(approved.getScore()));
        assertTrue(approved.getReasoningString().contains(
                "V24.15 AMSD MEGA PRIORITY"));
        assertTrue(approved.getReasoningString().contains(
                "V22.5 CRITICAL: Deploy ship to Bespin"));

        DeckOracle missingPiettOracle = mock(DeckOracle.class);
        when(missingPiettOracle.isAnalyzed()).thenReturn(true);
        when(missingPiettOracle.isCardInHand("Executor")).thenReturn(true);
        EvaluatedAction blocked = evaluateAmsd(gameState, missingPiettOracle);
        assertEquals(Float.floatToRawIntBits(-9999.0f),
                Float.floatToRawIntBits(blocked.getScore()));
        assertTrue(blocked.getReasoningString().contains(
                "Piett NOT in hand"));
        verify(missingPiettOracle, times(1)).recordAmsdFailedOnTurn(1);
    }

    private static EvaluatedAction evaluateAmsd(
            GameState gameState, DeckOracle oracle) {
        DecisionContext context = new DecisionContext(
                gameState, "bot", "CARD_ACTION_CHOICE",
                "Choose deploy action", "1", Phase.DEPLOY);
        context.setDeckOracle(oracle);
        context.setActionIds(List.of("0"));
        context.setActionTexts(List.of(
                "Reveal pilot or Star Destroyer from hand"));
        context.setCardIds(List.of(""));
        return new ActionTextEvaluator().evaluate(context).get(0);
    }

    private static GameState bespinGameState() {
        GameState gameState = mock(GameState.class);
        PhysicalCard bespin = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(gameState.getPlayersLatestTurnNumber("bot")).thenReturn(1);
        when(gameState.getCurrentPlayerId()).thenReturn("bot");
        when(gameState.getForcePileSize("bot")).thenReturn(7);
        when(gameState.getLocationsInOrder()).thenReturn(List.of(bespin));
        when(bespin.getTitle()).thenReturn("Bespin");
        when(bespin.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardSubtype()).thenReturn(CardSubtype.SYSTEM);
        return gameState;
    }
}
