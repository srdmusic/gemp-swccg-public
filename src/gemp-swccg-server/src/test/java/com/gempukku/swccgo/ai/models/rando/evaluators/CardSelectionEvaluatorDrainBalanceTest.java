package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CardSelectionEvaluatorDrainBalanceTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";

    @Test
    public void imperialEntanglementsFrontDoesNotCreateFalseDrainPressure() {
        Fixture fixture = fixture(2.0f, 0.0f, 0.0f, Float.MAX_VALUE);

        assertEquals(0, CardSelectionEvaluator.computeNetDrainBalance(
                fixture.game(), fixture.gameState(), PLAYER));
    }

    @Test
    public void battleOrderThresholdUsesActualDamageForBothPlayers() {
        Fixture fixture = fixture(3.0f, 2.0f, 1.0f, Float.MAX_VALUE);

        assertEquals("Projected gap is 2 - 1, not nominal 3 - 1",
                1, CardSelectionEvaluator.computeNetDrainBalance(
                        fixture.game(), fixture.gameState(), PLAYER));

        when(fixture.modifiers().getForceToLoseFromForceDrainLimit(
                fixture.gameState(), PLAYER, fixture.opponentLocation()))
                .thenReturn(Float.MAX_VALUE);
        assertEquals("Uncapped postflip pressure remains nominal",
                2, CardSelectionEvaluator.computeNetDrainBalance(
                        fixture.game(), fixture.gameState(), PLAYER));
    }

    private static Fixture fixture(
            float opponentDrain, float opponentDrainLimit,
            float ourDrain, float ourDrainLimit) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard opponentLocation = mock(PhysicalCard.class);
        PhysicalCard ourLocation = mock(PhysicalCard.class);
        PhysicalCard opponentCard = mock(PhysicalCard.class);
        PhysicalCard ourCard = mock(PhysicalCard.class);

        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getOpponent(OPPONENT)).thenReturn(PLAYER);
        when(gameState.getLocationsInOrder()).thenReturn(
                List.of(opponentLocation, ourLocation));
        when(gameState.getCardsAtLocation(opponentLocation))
                .thenReturn(List.of(opponentCard));
        when(gameState.getCardsAtLocation(ourLocation))
                .thenReturn(List.of(ourCard));
        when(opponentCard.getOwner()).thenReturn(OPPONENT);
        when(ourCard.getOwner()).thenReturn(PLAYER);
        when(modifiers.getForceDrainAmount(
                gameState, opponentLocation, OPPONENT))
                .thenReturn(opponentDrain);
        when(modifiers.getForceDrainAmount(
                gameState, ourLocation, PLAYER)).thenReturn(ourDrain);
        when(modifiers.getForceToLoseFromForceDrainLimit(
                gameState, PLAYER, opponentLocation))
                .thenReturn(opponentDrainLimit);
        when(modifiers.getForceToLoseFromForceDrainLimit(
                gameState, OPPONENT, ourLocation)).thenReturn(ourDrainLimit);

        return new Fixture(gameState, game, modifiers, opponentLocation);
    }

    private record Fixture(
            GameState gameState,
            SwccgGame game,
            ModifiersQuerying modifiers,
            PhysicalCard opponentLocation) {
    }
}
