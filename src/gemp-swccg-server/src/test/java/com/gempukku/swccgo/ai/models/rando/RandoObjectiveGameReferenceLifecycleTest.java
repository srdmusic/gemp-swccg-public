package com.gempukku.swccgo.ai.models.rando;

import com.gempukku.swccgo.ai.models.common.objective.ObjectiveFactsSource;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RandoObjectiveGameReferenceLifecycleTest {

    @Test
    public void newGameReferenceResetsObjectiveStateButSameReferenceDoesNot() {
        RandoCalAi ai = new RandoCalAi();
        SwccgGame first = objectiveGame();
        SwccgGame rematch = objectiveGame();

        ai.setCurrentGame(first);
        ObjectiveFactsSource source = ai.objectiveFactsSourceForTesting();
        source.analyze(first, "dark", Side.DARK);
        assertTrue(source.isAnalyzed());

        ai.setCurrentGame(first);
        assertTrue("same-game snapshot/revert must retain objective state", source.isAnalyzed());

        ai.setCurrentGame(rematch);
        assertFalse("same-opponent rematch must reset objective state", source.isAnalyzed());
    }

    private static SwccgGame objectiveGame() {
        SwccgCardBlueprint front = mock(SwccgCardBlueprint.class);
        when(front.getCardCategory()).thenReturn(CardCategory.OBJECTIVE);
        when(front.getTitle()).thenReturn("The Hidden Path");
        when(front.getGameText()).thenReturn(
                "Deploy Test Site. Flip this card if you occupy Test Site.");
        SwccgCardBlueprint back = mock(SwccgCardBlueprint.class);
        when(back.getTitle()).thenReturn("Gather Allies And Train");
        when(back.getGameText()).thenReturn(
                "Flip this card if you do not occupy Test Site.");
        PhysicalCard objective = mock(PhysicalCard.class);
        when(objective.getOwner()).thenReturn("dark");
        when(objective.getBlueprint()).thenReturn(front);
        when(objective.getOtherSideBlueprint()).thenReturn(back);
        when(objective.getBlueprintId(true)).thenReturn("226_28");
        when(objective.getZone()).thenReturn(Zone.SIDE_OF_TABLE);

        GameState state = mock(GameState.class);
        when(state.getAllPermanentCards()).thenReturn(List.of(objective));
        SwccgGame game = mock(SwccgGame.class);
        when(game.getDarkPlayer()).thenReturn("dark");
        when(game.getLightPlayer()).thenReturn("light");
        when(game.getGameState()).thenReturn(state);
        return game;
    }
}
