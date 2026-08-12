package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardVisitor;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.apache.logging.log4j.LogManager;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DrawPhaseFactsReaderTest {

    @Test
    public void missingGamePreservesBaseOneForceGeneration() {
        assertEquals(1, DrawPhaseFactsReader.calculateForceGeneration(
                null, null, null, LogManager.getLogger(getClass())));
    }

    @Test
    public void expensiveCardReaderPreservesInterruptSkipAndAffordability() {
        List<PhysicalCard> hand = List.of(
                card(CardCategory.CHARACTER, 4, 8, true),
                card(CardCategory.CHARACTER, 2, 3, true),
                card(CardCategory.INTERRUPT, 0, 12, false));

        DrawPhaseFactsReader.ExpensiveCards facts =
                DrawPhaseFactsReader.inspectExpensiveCards(hand, 4);

        assertEquals(3, facts.handCardCount());
        assertEquals(8, facts.maxDeployableCost());
        assertEquals(1, facts.affordableCardsCount());
        assertTrue(facts.expensiveCardInHand());
    }

    @Test
    public void forceStarvedReaderKeepsEfficiencySortAndGreedyCost() {
        List<PhysicalCard> hand = List.of(
                card(CardCategory.CHARACTER, 4, 4, true),
                card(CardCategory.CHARACTER, 2, 1, true),
                card(CardCategory.EFFECT, 0, 1, false));

        DrawPhaseFactsReader.ForceStarved facts =
                DrawPhaseFactsReader.inspectForceStarved(hand);

        assertEquals(6, facts.deployablePower());
        assertEquals(5, facts.minCostForThresholdPower());
    }

    @Test
    public void emptyHandProducesNeutralFactRecords() {
        DrawPhaseFactsReader.ExpensiveCards expensive =
                DrawPhaseFactsReader.inspectExpensiveCards(List.of(), 4);
        DrawPhaseFactsReader.ForceStarved starved =
                DrawPhaseFactsReader.inspectForceStarved(List.of());

        assertEquals(0, expensive.handCardCount());
        assertFalse(expensive.expensiveCardInHand());
        assertEquals(0, starved.deployablePower());
        assertEquals(999, starved.minCostForThresholdPower());
    }

    @Test
    public void onlyExactOrdinaryStockDrawMatchesResponseBankRoute() {
        assertTrue(DrawPhaseFactsReader.isOrdinaryStockForcePileDraw(
                "Draw card into hand from Force Pile"));
        for (String other : List.of(
                "Draw top card of Reserve Deck",
                "Draw destiny",
                "Draw weapon destiny",
                "Search Reserve Deck and take a card into hand",
                "Draw card from Force Pile using an Effect")) {
            assertFalse(other, DrawPhaseFactsReader
                    .isOrdinaryStockForcePileDraw(other));
        }
    }

    @Test
    public void boardDeficitCountsOnlyOwnedInPlayUnits() {
        GameState gameState = mock(GameState.class);
        when(gameState.getOpponent("us")).thenReturn("them");
        PhysicalCard oursOne = boardCard(
                "us", Zone.AT_LOCATION, CardCategory.CHARACTER);
        PhysicalCard oursTwo = boardCard(
                "us", Zone.AT_LOCATION, CardCategory.STARSHIP);
        PhysicalCard oursThree = boardCard(
                "us", Zone.AT_LOCATION, CardCategory.VEHICLE);
        PhysicalCard theirsOne = boardCard(
                "them", Zone.AT_LOCATION, CardCategory.CHARACTER);
        PhysicalCard theirsTwo = boardCard(
                "them", Zone.AT_LOCATION, CardCategory.STARSHIP);
        PhysicalCard theirsThree = boardCard(
                "them", Zone.AT_LOCATION, CardCategory.VEHICLE);
        PhysicalCard theirsFour = boardCard(
                "them", Zone.AT_LOCATION, CardCategory.CHARACTER);
        PhysicalCard ignoredEffect = boardCard(
                "them", Zone.AT_LOCATION, CardCategory.EFFECT);
        PhysicalCard ignoredHandUnit = boardCard(
                "them", Zone.HAND, CardCategory.CHARACTER);

        when(gameState.getAllPermanentCards()).thenReturn(List.of(
                oursOne, oursTwo, theirsOne, theirsTwo, theirsThree,
                theirsFour, ignoredEffect, ignoredHandUnit));
        DrawPhaseFactsReader.BoardUnits twoVersusFour =
                DrawPhaseFactsReader.inspectBoardUnits(gameState, "us");
        assertEquals(2, twoVersusFour.ourUnits());
        assertEquals(4, twoVersusFour.opponentUnits());
        assertTrue(twoVersusFour.behindOnBoard());

        when(gameState.getAllPermanentCards()).thenReturn(List.of(
                oursOne, oursTwo, oursThree, theirsOne, theirsTwo,
                theirsThree, theirsFour, ignoredEffect,
                ignoredHandUnit));
        assertFalse(DrawPhaseFactsReader.inspectBoardUnits(
                gameState, "us").behindOnBoard());
    }

    @Test
    public void offensiveBankIgnoresACharacterWhosePersonaIsAlreadyDeployed() {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard location = mock(PhysicalCard.class);
        PhysicalCard handVader = mock(PhysicalCard.class);
        PhysicalCard tableVader = mock(PhysicalCard.class);
        SwccgCardBlueprint handBlueprint = mock(SwccgCardBlueprint.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent("dark")).thenReturn("light");
        when(gameState.getLocationsInOrder()).thenReturn(List.of(location));
        when(handVader.getBlueprint()).thenReturn(handBlueprint);
        when(handVader.getCardId()).thenReturn(1);
        when(handBlueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        when(handBlueprint.hasPowerAttribute()).thenReturn(true);
        when(handBlueprint.getPower()).thenReturn(6.0f);
        when(handBlueprint.getDeployCost()).thenReturn(13.0f);
        when(handBlueprint.getPersonas()).thenReturn(Set.of(Persona.VADER));
        when(tableVader.getOwner()).thenReturn("dark");
        when(tableVader.getCardId()).thenReturn(2);
        when(modifiers.hasPersona(gameState, tableVader, Persona.VADER))
                .thenReturn(true);
        doAnswer(invocation -> {
            PhysicalCardVisitor visitor = invocation.getArgument(0);
            visitor.visitPhysicalCard(tableVader);
            return false;
        }).when(gameState).iterateAllCardsOnTable(
                any(PhysicalCardVisitor.class), eq(false), eq(false), eq(false));
        when(modifiers.getTotalPowerAtLocation(
                gameState, location, "light", false, false)).thenReturn(6.0f);
        when(modifiers.getTotalPowerAtLocation(
                gameState, location, "dark", false, false)).thenReturn(0.0f);
        when(modifiers.isBattleground(gameState, location, null)).thenReturn(true);

        assertEquals(0, DrawPhaseFactsReader.computeOffensiveBank(
                game, gameState, "dark", List.of(handVader),
                11, 14, LogManager.getLogger(getClass())));
    }

    @Test
    public void offensiveBankStillCountsADeployableCharacter() {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard location = mock(PhysicalCard.class);
        PhysicalCard character = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);

        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent("dark")).thenReturn("light");
        when(gameState.getLocationsInOrder()).thenReturn(List.of(location));
        when(character.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        when(blueprint.hasPowerAttribute()).thenReturn(true);
        when(blueprint.getPower()).thenReturn(6.0f);
        when(blueprint.getDeployCost()).thenReturn(13.0f);
        when(blueprint.getPersonas()).thenReturn(Set.of());
        when(modifiers.getTotalPowerAtLocation(
                gameState, location, "light", false, false)).thenReturn(6.0f);
        when(modifiers.getTotalPowerAtLocation(
                gameState, location, "dark", false, false)).thenReturn(0.0f);
        when(modifiers.isBattleground(gameState, location, null)).thenReturn(true);

        assertEquals(13, DrawPhaseFactsReader.computeOffensiveBank(
                game, gameState, "dark", List.of(character),
                11, 14, LogManager.getLogger(getClass())));
    }

    private static PhysicalCard card(CardCategory category, float power,
                                     float deployCost, boolean hasPower) {
        SwccgCardBlueprint blueprint = (SwccgCardBlueprint) Proxy.newProxyInstance(
                SwccgCardBlueprint.class.getClassLoader(),
                new Class<?>[]{SwccgCardBlueprint.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getCardCategory" -> category;
                    case "hasPowerAttribute" -> hasPower;
                    case "getPower" -> power;
                    case "getDeployCost" -> deployCost;
                    default -> throw new UnsupportedOperationException(
                            "Unexpected blueprint call: " + method.getName());
                });
        return (PhysicalCard) Proxy.newProxyInstance(
                PhysicalCard.class.getClassLoader(),
                new Class<?>[]{PhysicalCard.class},
                (proxy, method, args) -> {
                    if ("getBlueprint".equals(method.getName())) {
                        return blueprint;
                    }
                    throw new UnsupportedOperationException(
                            "Unexpected PhysicalCard call: " + method.getName());
                });
    }

    private static PhysicalCard boardCard(
            String owner, Zone zone, CardCategory category) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory()).thenReturn(category);
        return card;
    }
}
