package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.apache.logging.log4j.LogManager;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ObjectiveAnalyzerBattleParticipantFactTest {

    @Test
    public void opponentEffectAloneDoesNotCreateEvictionPresence() {
        ObjectiveAnalyzer analyzer = new ObjectiveAnalyzer(
                LogManager.getLogger(
                        ObjectiveAnalyzerBattleParticipantFactTest.class));
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard location = mock(PhysicalCard.class);
        PhysicalCard effect = mock(PhysicalCard.class);
        SwccgCardBlueprint effectBlueprint = mock(SwccgCardBlueprint.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent("bot")).thenReturn("opponent");
        when(gameState.getAllPermanentCards()).thenReturn(List.of(effect));
        when(effect.getOwner()).thenReturn("opponent");
        when(effect.getBlueprint()).thenReturn(effectBlueprint);
        when(effectBlueprint.getCardCategory()).thenReturn(
                CardCategory.EFFECT);

        assertFalse(analyzer.hasOpponentBattleParticipantAt(
                game, "bot", location));
    }
}
