package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

    @Test
    public void invasionMakesNabooSystemNonBattlegroundForAiFactsOnEitherFace() {
        for (boolean flipped : new boolean[]{false, true}) {
            GameState gameState = mock(GameState.class);
            SwccgGame game = mock(SwccgGame.class);
            ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
            PhysicalCard invasion = mock(PhysicalCard.class);
            PhysicalCard naboo = location("Naboo", CardSubtype.SYSTEM);

            when(game.getGameState()).thenReturn(gameState);
            when(game.getModifiersQuerying()).thenReturn(modifiers);
            when(game.getOpponent("tester")).thenReturn("opponent");
            when(gameState.getAllPermanentCards()).thenReturn(List.of(invasion));
            when(gameState.getTopLocations()).thenReturn(List.of(naboo));
            when(invasion.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
            when(invasion.getBlueprintId(true)).thenReturn("14_113");
            when(invasion.isFlipped()).thenReturn(flipped);
            when(modifiers.isBattleground(gameState, naboo, null)).thenReturn(true);
            when(modifiers.getTotalPowerAtLocation(
                    gameState, naboo, "tester", false, false)).thenReturn(4.0f);
            when(modifiers.getTotalPowerAtLocation(
                    gameState, naboo, "opponent", false, false)).thenReturn(4.0f);
            when(modifiers.getForceDrainAmount(
                    gameState, naboo, "opponent")).thenReturn(1.0f);

            ShieldFacts.FourthSlotFacts facts =
                    ShieldFacts.fourthSlotFacts(gameState, game, "tester");

            assertTrue(ShieldFacts.isInvasionNabooSystem(game, naboo));
            assertEquals(0, facts.ownBattlegroundCount());
            assertEquals(0, facts.opponentBattlegroundCount());
            assertTrue(facts.opponentDrainsNonBattleground());
        }
    }

    @Test
    public void nabooRetainsOrdinaryBattlegroundClassificationWithoutInvasion() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard naboo = location("Naboo", CardSubtype.SYSTEM);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent("tester")).thenReturn("opponent");
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        when(gameState.getTopLocations()).thenReturn(List.of(naboo));
        when(modifiers.isBattleground(gameState, naboo, null)).thenReturn(true);
        when(modifiers.getTotalPowerAtLocation(
                gameState, naboo, "tester", false, false)).thenReturn(4.0f);
        when(modifiers.getTotalPowerAtLocation(
                gameState, naboo, "opponent", false, false)).thenReturn(4.0f);
        when(modifiers.getForceDrainAmount(
                gameState, naboo, "opponent")).thenReturn(1.0f);

        ShieldFacts.FourthSlotFacts facts =
                ShieldFacts.fourthSlotFacts(gameState, game, "tester");

        assertFalse(ShieldFacts.isInvasionNabooSystem(game, naboo));
        assertEquals(1, facts.ownBattlegroundCount());
        assertEquals(1, facts.opponentBattlegroundCount());
        assertFalse(facts.opponentDrainsNonBattleground());
    }

    @Test
    public void invasionDetectionRequiresTheActiveFrontBlueprint() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard objective = mock(PhysicalCard.class);
        PhysicalCard naboo = location("Naboo", CardSubtype.SYSTEM);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getAllPermanentCards()).thenReturn(List.of(objective));
        when(objective.getBlueprintId(true)).thenReturn("14_113");

        when(objective.getZone()).thenReturn(Zone.LOST_PILE);
        assertFalse(ShieldFacts.isInvasionNabooSystem(game, naboo));

        when(objective.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(objective.getBlueprintId(true)).thenReturn("1_1");
        assertFalse(ShieldFacts.isInvasionNabooSystem(game, naboo));

        when(objective.getBlueprintId(true)).thenReturn("14_113");
        when(objective.isFlipped()).thenReturn(true);
        assertTrue(ShieldFacts.isInvasionNabooSystem(game, naboo));
    }

    @Test
    public void invasionExcludesOnlyTheNabooSystemFromBattlegroundCounts() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard invasion = mock(PhysicalCard.class);
        PhysicalCard naboo = location("Naboo", CardSubtype.SYSTEM);
        PhysicalCard throneRoom = location(
                "Naboo: Theed Palace Throne Room", CardSubtype.SITE);
        PhysicalCard coruscant = location("Coruscant", CardSubtype.SYSTEM);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent("tester")).thenReturn("opponent");
        when(gameState.getAllPermanentCards()).thenReturn(List.of(invasion));
        when(gameState.getTopLocations()).thenReturn(
                List.of(naboo, throneRoom, coruscant));
        when(invasion.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(invasion.getBlueprintId(true)).thenReturn("14_113");
        for (PhysicalCard location : List.of(naboo, throneRoom, coruscant)) {
            when(modifiers.isBattleground(gameState, location, null)).thenReturn(true);
            when(modifiers.getTotalPowerAtLocation(
                    gameState, location, "tester", false, false)).thenReturn(4.0f);
            when(modifiers.getTotalPowerAtLocation(
                    gameState, location, "opponent", false, false)).thenReturn(4.0f);
        }
        when(modifiers.getForceDrainAmount(
                gameState, naboo, "opponent")).thenReturn(1.0f);

        ShieldFacts.FourthSlotFacts facts =
                ShieldFacts.fourthSlotFacts(gameState, game, "tester");

        assertTrue(ShieldFacts.isInvasionNabooSystem(game, naboo));
        assertFalse(ShieldFacts.isInvasionNabooSystem(game, throneRoom));
        assertFalse(ShieldFacts.isInvasionNabooSystem(game, coruscant));
        assertEquals(2, facts.ownBattlegroundCount());
        assertEquals(2, facts.opponentBattlegroundCount());
        assertTrue(facts.opponentDrainsNonBattleground());
    }

    private static PhysicalCard location(String title, CardSubtype subtype) {
        PhysicalCard location = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(location.getBlueprint()).thenReturn(blueprint);
        when(location.getTitles()).thenReturn(List.of(title));
        when(location.isBlownAway()).thenReturn(false);
        when(blueprint.getCardSubtype()).thenReturn(subtype);
        return location;
    }
}
