package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WmaopActionTextOracleGateTest {
    private static final String PLAYER = "tester";
    private static final String EXPECTED_BLOCK =
            "V142 WMAOP BLOCK: WMAOP.FODDER_HOLD: no Blockade Flagship site"
                    + " remains in Reserve Deck — search would fail; hold WMAOP as fodder"
                    + " — hold the interrupt (-2000.0)";

    @Test
    public void nullOracleLocationListIsUnknownAndDoesNotBlockEitherMirror() {
        assertOracleGate(null, false);
    }

    @Test
    public void emptyOracleLocationListIsUnknownAndDoesNotBlockEitherMirror() {
        assertOracleGate(List.of(), false);
    }

    @Test
    public void affirmativeNonBlockadeReserveInventoryBlocksBothMirrors() {
        assertOracleGate(List.of("Naboo: Theed Palace Throne Room"), true);
    }

    @Test
    public void blockadeSiteInReserveKeepsTheSanctionedModeOpenInBothMirrors() {
        assertOracleGate(List.of("Blockade Flagship: Bridge"), false);
    }

    private static void assertOracleGate(
            List<String> reserveLocationTitles, boolean expectBlock) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getPlayersLatestTurnNumber(PLAYER)).thenReturn(4);
        when(gameState.getCurrentPlayerId()).thenReturn(PLAYER);
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        when(gameState.getTopLocations()).thenReturn(List.of());
        when(gameState.getHand(PLAYER)).thenReturn(List.of());
        when(gameState.getLocationsInOrder()).thenReturn(List.of());

        PhysicalCard wmaop = mock(PhysicalCard.class);
        when(wmaop.getTitle()).thenReturn("We Must Accelerate Our Plans");
        when(wmaop.getOwner()).thenReturn(PLAYER);
        when(gameState.findCardById(100)).thenReturn(wmaop);

        var randoOracle = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.class);
        var randoReserveLocations = randoDeckCards(reserveLocationTitles);
        when(randoOracle.isAnalyzed()).thenReturn(true);
        when(randoOracle.getCardsByCategory(
                CardCategory.LOCATION, Zone.RESERVE_DECK))
                .thenReturn(randoReserveLocations);

        var chosenOracle = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.class);
        var chosenReserveLocations = chosenDeckCards(reserveLocationTitles);
        when(chosenOracle.isAnalyzed()).thenReturn(true);
        when(chosenOracle.getCardsByCategory(
                CardCategory.LOCATION, Zone.RESERVE_DECK))
                .thenReturn(chosenReserveLocations);

        var randoContext =
                new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                        gameState, PLAYER, "CARD_ACTION_CHOICE", "Choose action",
                        "wmaop-oracle-rando", Phase.DEPLOY);
        randoContext.setGame(game);
        randoContext.setSide(Side.DARK);
        randoContext.setDeckOracle(randoOracle);
        randoContext.setActionIds(List.of("wmaop"));
        randoContext.setActionTexts(List.of(
                "Deploy a Blockade Flagship site from Reserve Deck"));
        randoContext.setCardIds(List.of("100"));

        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                        gameState, PLAYER, "CARD_ACTION_CHOICE", "Choose action",
                        "wmaop-oracle-chosen", Phase.DEPLOY);
        chosenContext.setGame(game);
        chosenContext.setSide(Side.DARK);
        chosenContext.setDeckOracle(chosenOracle);
        chosenContext.setActionIds(List.of("wmaop"));
        chosenContext.setActionTexts(List.of(
                "Deploy a Blockade Flagship site from Reserve Deck"));
        chosenContext.setCardIds(List.of("100"));

        var rando =
                new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                        .evaluate(randoContext);
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                        .evaluate(chosenContext);

        assertEquals(1, rando.size());
        assertEquals(1, chosen.size());
        assertEquals(rando.get(0).getReasoning(), chosen.get(0).getReasoning());
        if (expectBlock) {
            assertTrue(rando.get(0).getReasoning().contains(EXPECTED_BLOCK));
        } else {
            assertFalse(rando.get(0).getReasoning().stream().anyMatch(
                    reason -> reason.startsWith("V142 WMAOP BLOCK")));
        }
    }

    private static List<com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.DeckCard>
            randoDeckCards(List<String> titles) {
        if (titles == null) {
            return null;
        }
        List<com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.DeckCard>
                cards = new ArrayList<>();
        for (String title : titles) {
            var card = mock(
                    com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.DeckCard.class);
            when(card.getTitle()).thenReturn(title);
            cards.add(card);
        }
        return cards;
    }

    private static List<com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.DeckCard>
            chosenDeckCards(List<String> titles) {
        if (titles == null) {
            return null;
        }
        List<com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.DeckCard>
                cards = new ArrayList<>();
        for (String title : titles) {
            var card = mock(
                    com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.DeckCard.class);
            when(card.getTitle()).thenReturn(title);
            cards.add(card);
        }
        return cards;
    }
}
