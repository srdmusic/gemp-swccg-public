package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.actions.PlayCardAction;
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
    public void ordinaryBattlePlanClosesTheBattleOrderRoute() {
        GameState gameState = mock(GameState.class);
        PhysicalCard battlePlan = mock(PhysicalCard.class);
        when(battlePlan.getTitle()).thenReturn("Battle Plan");
        when(battlePlan.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(gameState.getAllPermanentCards()).thenReturn(
                List.of(battlePlan));

        assertTrue(ShieldFacts
                .battleOrderPlanEquivalentOnTable(gameState));
    }

    @Test
    public void invasionNabooExceptionDoesNotRewriteSimpleTricksCardTextFacts() {
        for (boolean flipped : new boolean[]{false, true}) {
            GameState gameState = mock(GameState.class);
            SwccgGame game = mock(SwccgGame.class);
            ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
            PhysicalCard invasion = mock(PhysicalCard.class);
            PhysicalCard naboo = location("Naboo", CardSubtype.SYSTEM);

            when(game.getGameState()).thenReturn(gameState);
            when(gameState.getGame()).thenReturn(game);
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
            when(modifiers.occupiesLocation(
                    gameState, naboo, "tester")).thenReturn(true);
            when(modifiers.occupiesLocation(
                    gameState, naboo, "opponent")).thenReturn(true);
            when(modifiers.controlsLocation(
                    gameState, naboo, "opponent")).thenReturn(true);
            when(modifiers.getForceDrainAmount(
                    gameState, naboo, "opponent")).thenReturn(1.0f);

            ShieldFacts.FourthSlotFacts facts =
                    ShieldFacts.fourthSlotFacts(gameState, game, "tester");

            assertTrue(ShieldFacts.isInvasionNabooSystem(game, naboo));
            assertEquals(0, facts.ownBattlegroundCount());
            assertEquals(0, facts.opponentBattlegroundCount());
            assertFalse(facts.opponentDrainsNonBattleground());
        }
    }

    @Test
    public void nabooRetainsOrdinaryBattlegroundClassificationWithoutInvasion() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard naboo = location("Naboo", CardSubtype.SYSTEM);

        when(game.getGameState()).thenReturn(gameState);
        when(gameState.getGame()).thenReturn(game);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent("tester")).thenReturn("opponent");
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        when(gameState.getTopLocations()).thenReturn(List.of(naboo));
        when(modifiers.isBattleground(gameState, naboo, null)).thenReturn(true);
        when(modifiers.getTotalPowerAtLocation(
                gameState, naboo, "tester", false, false)).thenReturn(4.0f);
        when(modifiers.getTotalPowerAtLocation(
                gameState, naboo, "opponent", false, false)).thenReturn(4.0f);
        when(modifiers.occupiesLocation(
                gameState, naboo, "tester")).thenReturn(true);
        when(modifiers.occupiesLocation(
                gameState, naboo, "opponent")).thenReturn(true);
        when(modifiers.controlsLocation(
                gameState, naboo, "opponent")).thenReturn(true);
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
        when(gameState.getGame()).thenReturn(game);
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
            when(modifiers.occupiesLocation(
                    gameState, location, "tester")).thenReturn(true);
            when(modifiers.occupiesLocation(
                    gameState, location, "opponent")).thenReturn(true);
        }
        when(modifiers.controlsLocation(
                gameState, naboo, "opponent")).thenReturn(true);
        when(modifiers.getForceDrainAmount(
                gameState, naboo, "opponent")).thenReturn(1.0f);

        ShieldFacts.FourthSlotFacts facts =
                ShieldFacts.fourthSlotFacts(gameState, game, "tester");

        assertTrue(ShieldFacts.isInvasionNabooSystem(game, naboo));
        assertFalse(ShieldFacts.isInvasionNabooSystem(game, throneRoom));
        assertFalse(ShieldFacts.isInvasionNabooSystem(game, coruscant));
        assertEquals(2, facts.ownBattlegroundCount());
        assertEquals(2, facts.opponentBattlegroundCount());
        assertFalse(facts.opponentDrainsNonBattleground());
    }

    @Test
    public void nonBattlegroundDrainAbilityRequiresControlForForceDrain() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard nonBattleground = location(
                "Coruscant: The Works", CardSubtype.SITE);

        when(game.getGameState()).thenReturn(gameState);
        when(gameState.getGame()).thenReturn(game);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent("tester")).thenReturn("opponent");
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        when(gameState.getTopLocations()).thenReturn(List.of(nonBattleground));
        when(modifiers.isBattleground(
                gameState, nonBattleground, null)).thenReturn(false);
        when(modifiers.getForceDrainAmount(
                gameState, nonBattleground, "opponent")).thenReturn(1.0f);

        ShieldFacts.FourthSlotFacts iconsOnly =
                ShieldFacts.fourthSlotFacts(gameState, game, "tester");
        assertFalse(iconsOnly.opponentDrainsNonBattleground());

        when(modifiers.controlsLocation(
                gameState, nonBattleground, "opponent")).thenReturn(true);
        ShieldFacts.FourthSlotFacts controlled =
                ShieldFacts.fourthSlotFacts(gameState, game, "tester");
        assertTrue(controlled.opponentDrainsNonBattleground());
    }

    @Test
    public void activeOpponentNonBattlegroundDrainIsObservedExactly() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard location = location(
                "Coruscant: Upper Plaza Corridor", CardSubtype.SITE);

        when(game.getOpponent("tester")).thenReturn("opponent");
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.isDuringForceDrainInitiatedBy("opponent"))
                .thenReturn(true);
        when(gameState.getForceDrainLocation()).thenReturn(location);
        when(modifiers.isBattleground(gameState, location, null))
                .thenReturn(false);

        assertTrue(ShieldFacts.opponentNonBattlegroundDrainObservedNow(
                gameState, game, "tester"));

        when(modifiers.isBattleground(gameState, location, null))
                .thenReturn(true);
        assertFalse(ShieldFacts.opponentNonBattlegroundDrainObservedNow(
                gameState, game, "tester"));
    }

    @Test
    public void stackedShieldMustBeEnginePlayableBeforeParentPursuesIt() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard source = mock(PhysicalCard.class);
        PhysicalCard shield = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);

        when(source.getPermanentCardId()).thenReturn(7);
        when(gameState.findCardByPermanentId(7)).thenReturn(source);
        when(gameState.getGame()).thenReturn(game);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getStackedCards(source)).thenReturn(List.of(shield));
        when(shield.getTitle()).thenReturn("Simple Tricks And Nonsense");
        when(shield.getOwner()).thenReturn("tester");
        when(shield.getBlueprint()).thenReturn(blueprint);

        assertFalse(ShieldFacts.stackedCardTitlePlayable(
                gameState, game, source, "Simple Tricks And Nonsense"));

        PlayCardAction playable = mock(PlayCardAction.class);
        when(blueprint.getPlayCardAction(
                "tester", game, shield, source, false, 0.0f,
                null, null, null, null, null, false, 0.0f,
                com.gempukku.swccgo.filters.Filters.any, null))
                .thenReturn(playable);

        assertTrue(ShieldFacts.stackedCardTitlePlayable(
                gameState, game, source, "Simple Tricks And Nonsense"));
    }

    private static PhysicalCard location(String title, CardSubtype subtype) {
        PhysicalCard location = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(location.getBlueprint()).thenReturn(blueprint);
        when(location.getTitles()).thenReturn(List.of(title));
        when(location.isBlownAway()).thenReturn(false);
        when(location.getZone()).thenReturn(Zone.LOCATIONS);
        when(blueprint.getCardSubtype()).thenReturn(subtype);
        return location;
    }
}
