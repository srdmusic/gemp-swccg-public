package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ShieldFactsTest {

    @Test
    public void failedOpponentScanDoesNotSuppressTheIndependentDrainScan() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        when(game.getOpponent("tester")).thenReturn("opponent");
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        doThrow(new IllegalStateException("opponent scan failed"))
                .doReturn(List.of())
                .when(gameState).getTopLocations();

        ShieldFacts.FourthSlotFacts facts =
                ShieldFacts.fourthSlotFacts(gameState, game, "tester");

        verify(gameState, times(2)).getTopLocations();
        assertFalse(facts.opponentCanDrainThreePlus());
        assertFalse(facts.opponentDrainsNonBattleground());
    }
}
