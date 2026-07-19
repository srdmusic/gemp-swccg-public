package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SetupFactsReaderTest {
    private static final String PLAYER = "rando";

    @Test
    public void locationTextKeepsBaseLightDarkOrderAndLocalSideFailures() {
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(blueprint.getGameText()).thenReturn("base");
        when(blueprint.getLocationLightSideGameText())
                .thenThrow(new IllegalStateException("light unavailable"));
        when(blueprint.getLocationDarkSideGameText()).thenReturn("dark");

        assertEquals("base dark ", SetupFactsReader.allLocationText(blueprint));
    }

    @Test(expected = IllegalStateException.class)
    public void baseLocationTextFailureStillPropagatesToCandidateBoundary() {
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(blueprint.getGameText())
                .thenThrow(new IllegalStateException("base unavailable"));

        SetupFactsReader.allLocationText(blueprint);
    }

    @Test
    public void sithStartingEffectScanIgnoresOpponentPermanents() {
        GameState gameState = mock(GameState.class);
        PhysicalCard opponent = card(
                "Revenge Of The Sith", "opponent", null, false);
        when(gameState.getAllPermanentCards()).thenReturn(List.of(opponent));

        assertFalse(SetupFactsReader.hasOwnedSithStartingEffect(
                gameState, PLAYER));

        PhysicalCard ownHand = card("Rise Of The Sith", PLAYER, null, false);
        when(gameState.getHand(PLAYER)).thenReturn(List.of(ownHand));
        assertTrue(SetupFactsReader.hasOwnedSithStartingEffect(
                gameState, PLAYER));
    }

    @Test
    public void revengeOfTheSithRequiresOwnedInPlayPermanent() {
        GameState gameState = mock(GameState.class);
        PhysicalCard ownInPlay = card(
                "Revenge Of The Sith", PLAYER, Zone.SIDE_OF_TABLE, true);
        when(gameState.getAllPermanentCards()).thenReturn(List.of(ownInPlay));

        assertTrue(SetupFactsReader.hasOwnedInPlayRevengeOfTheSith(
                gameState, PLAYER));

        PhysicalCard ownNotInPlay = card(
                "Revenge Of The Sith", PLAYER, Zone.RESERVE_DECK, true);
        PhysicalCard opponentInPlay = card(
                "Revenge Of The Sith", "opponent", Zone.SIDE_OF_TABLE, true);
        when(gameState.getAllPermanentCards())
                .thenReturn(List.of(ownNotInPlay, opponentInPlay));
        assertFalse(SetupFactsReader.hasOwnedInPlayRevengeOfTheSith(
                gameState, PLAYER));
    }

    private static PhysicalCard card(
            String title, String owner, Zone zone, boolean blueprintPresent) {
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        if (blueprintPresent) {
            when(card.getBlueprint()).thenReturn(mock(SwccgCardBlueprint.class));
        }
        return card;
    }
}
