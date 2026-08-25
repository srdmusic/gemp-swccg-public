package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardVisitor;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SurpriseAssaultFactsReaderTest {
    @Test
    public void siteProjectionUsesActualPresentCountPowerAndReserveDestinies() {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard surpriseAssault = card(
                113, "Surprise Assault", CardCategory.INTERRUPT, null, "light");
        PhysicalCard site = card(
                100, "Test Site", CardCategory.LOCATION, CardSubtype.SITE, null);
        List<PhysicalCard> opponentCharacters = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            opponentCharacters.add(card(
                    200 + i, "Opponent " + i,
                    CardCategory.CHARACTER, null, "dark"));
        }
        List<PhysicalCard> reserve = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            PhysicalCard destinyCard = card(
                    300 + i, "Destiny " + i,
                    CardCategory.INTERRUPT, null, "light");
            reserve.add(destinyCard);
            when(modifiers.getDestinyForDestinyDraw(
                    gameState, destinyCard, surpriseAssault))
                    .thenReturn(3.0f + i);
        }

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent("light")).thenReturn("dark");
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getPlayersLatestTurnNumber("light")).thenReturn(4);
        when(gameState.getCurrentPlayerId()).thenReturn("dark");
        when(gameState.getForceDrainLocation()).thenReturn(site);
        when(gameState.findCardByPermanentId(site.getPermanentCardId()))
                .thenReturn(site);
        when(gameState.getReserveDeck("light")).thenReturn(reserve);
        when(modifiers.getLocationHere(gameState, site)).thenReturn(site);
        for (PhysicalCard character : opponentCharacters) {
            when(modifiers.getLocationThatCardIsPresentAt(gameState, character))
                    .thenReturn(site);
        }
        when(modifiers.getTotalPowerAtLocation(
                gameState, site, "dark", false, false)).thenReturn(23.0f);
        doAnswer(invocation -> {
            PhysicalCardVisitor visitor = invocation.getArgument(0);
            for (PhysicalCard character : opponentCharacters) {
                if (visitor.visitPhysicalCard(character)) {
                    return true;
                }
            }
            return false;
        }).when(gameState).iterateActiveCards(
                any(PhysicalCardVisitor.class), eq(modifiers),
                nullable(PhysicalCard.class), isNull(), isNull());

        SurpriseAssaultFactsReader.Facts facts =
                SurpriseAssaultFactsReader.read(
                        "site", game, "light", surpriseAssault);

        assertTrue(facts.complete());
        assertEquals(SurpriseAssaultFactsReader.LocationKind.SITE,
                facts.locationKind());
        assertEquals(5, facts.opponentPresentCards());
        assertEquals(5, facts.reserveCards());
        assertEquals(5.0, facts.averageDestiny(), 0.0);
        assertEquals(23.0f, facts.opponentPower(), 0.0f);
        assertEquals(2.0, facts.projectedMargin(), 0.0);

        when(gameState.findCardById(113)).thenReturn(surpriseAssault);
        when(surpriseAssault.getBlueprintId(true)).thenReturn("1_113");
        when(modifiers.getTotalPowerAtLocation(
                gameState, site, "dark", false, false)).thenReturn(24.0f);
        var randoContext = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                gameState, "light", "ACTION_CHOICE", "Choose RESPONSE action",
                "surprise-site", Phase.CONTROL);
        randoContext.setGame(game);
        randoContext.setActionIds(List.of("site"));
        randoContext.setActionTexts(List.of("Cancel Force drain"));
        randoContext.setCardIds(List.of("113"));
        var chosenContext = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                gameState, "light", "ACTION_CHOICE", "Choose RESPONSE action",
                "surprise-site", Phase.CONTROL);
        chosenContext.setGame(game);
        chosenContext.setActionIds(List.of("site"));
        chosenContext.setActionTexts(List.of("Cancel Force drain"));
        chosenContext.setCardIds(List.of("113"));

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext).get(0);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenContext).get(0);

        assertTrue(rando.isDeferred());
        assertTrue(chosen.isDeferred());
        assertEquals(rando.getReasoning(), chosen.getReasoning());
        assertEquals(Float.floatToRawIntBits(35.0f),
                Float.floatToRawIntBits(rando.getScore()));
    }

    private static PhysicalCard card(
            int permanentId,
            String title,
            CardCategory category,
            CardSubtype subtype,
            String owner) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getPermanentCardId()).thenReturn(permanentId);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getOwner()).thenReturn(owner);
        when(blueprint.getCardCategory()).thenReturn(category);
        when(blueprint.getCardSubtype()).thenReturn(subtype);
        return card;
    }
}
