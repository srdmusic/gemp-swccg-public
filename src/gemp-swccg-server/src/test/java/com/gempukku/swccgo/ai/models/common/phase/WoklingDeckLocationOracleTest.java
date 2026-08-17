package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WoklingDeckLocationOracleTest {
    private static final String PLAYER = "tester";
    private static final SwccgCardBlueprintLibrary CARD_LIBRARY =
            new SwccgCardBlueprintLibrary();
    private static final AtomicInteger NEXT_PERMANENT_CARD_ID =
            new AtomicInteger(100);

    @Test
    public void originalLocationMustMoveFromLifeForceToTheTableForBothBots() {
        CardFixture kessel = card("1_126", Zone.RESERVE_DECK);
        OracleFixture fixture = fixture("1_126,200_47|", List.of(kessel));

        assertFalse(fixture.rando().areAllOriginalDeckLocationsInPlay());
        assertFalse(fixture.chosen().areAllOriginalDeckLocationsInPlay());

        kessel.zone().set(Zone.LOCATIONS);
        fixture.rando().refresh(fixture.gameState(), PLAYER);
        fixture.chosen().refresh(fixture.gameState(), PLAYER);

        assertTrue(fixture.rando().areAllOriginalDeckLocationsInPlay());
        assertTrue(fixture.chosen().areAllOriginalDeckLocationsInPlay());
    }

    @Test
    public void everyPhysicalOriginalLocationCopyMustBeDeployed() {
        CardFixture first = card("1_126", Zone.LOCATIONS);
        CardFixture second = card("1_126", Zone.LOST_PILE);
        OracleFixture fixture = fixture(
                "1_126,1_126,200_47|", List.of(first, second));

        assertFalse(fixture.rando().areAllOriginalDeckLocationsInPlay());
        assertFalse(fixture.chosen().areAllOriginalDeckLocationsInPlay());

        second.zone().set(Zone.LOCATIONS);
        fixture.rando().refresh(fixture.gameState(), PLAYER);
        fixture.chosen().refresh(fixture.gameState(), PLAYER);

        assertTrue(fixture.rando().areAllOriginalDeckLocationsInPlay());
        assertTrue(fixture.chosen().areAllOriginalDeckLocationsInPlay());
    }

    @Test
    public void outsideDeckLocationsDoNotExtendTheWoklingHold() {
        CardFixture original = card("1_126", Zone.LOCATIONS);
        CardFixture outside = card("1_128", Zone.OUTSIDE_OF_DECK);
        OracleFixture fixture = fixture(
                "1_126,200_47|1_128", List.of(original, outside));

        assertTrue(fixture.rando().areAllOriginalDeckLocationsInPlay());
        assertTrue(fixture.chosen().areAllOriginalDeckLocationsInPlay());
    }

    @Test
    public void sameBlueprintOutsideCopyCannotSubstituteForOriginalCopy() {
        CardFixture outside = card("1_126", Zone.LOCATIONS);
        CardFixture original = card("1_126", Zone.RESERVE_DECK);
        OracleFixture fixture = fixture(
                "1_126,200_47|1_126", List.of(original, outside));

        assertFalse(fixture.rando().areAllOriginalDeckLocationsInPlay());
        assertFalse(fixture.chosen().areAllOriginalDeckLocationsInPlay());

        original.zone().set(Zone.LOCATIONS);
        fixture.rando().refresh(fixture.gameState(), PLAYER);
        fixture.chosen().refresh(fixture.gameState(), PLAYER);

        assertTrue(fixture.rando().areAllOriginalDeckLocationsInPlay());
        assertTrue(fixture.chosen().areAllOriginalDeckLocationsInPlay());
    }

    @Test
    public void laterAcquiredSameBlueprintCannotSubstituteForOriginalCopy() {
        CardFixture original = card("1_126", Zone.RESERVE_DECK);
        CardFixture acquired = card("1_126", Zone.LOCATIONS);
        OracleFixture fixture = fixture(
                "1_126,200_47|", List.of(original, acquired));

        assertFalse(fixture.rando().areAllOriginalDeckLocationsInPlay());
        assertFalse(fixture.chosen().areAllOriginalDeckLocationsInPlay());
    }

    @Test
    public void onlyTheTopLocationZoneReleasesWokling() {
        CardFixture original = card("1_126", Zone.RESERVE_DECK);
        OracleFixture fixture = fixture(
                "1_126,200_47|", List.of(original));

        for (Zone zone : Zone.values()) {
            original.zone().set(zone);
            fixture.rando().refresh(fixture.gameState(), PLAYER);
            fixture.chosen().refresh(fixture.gameState(), PLAYER);
            assertEquals("Rando zone boundary for " + zone,
                    zone == Zone.LOCATIONS,
                    fixture.rando().areAllOriginalDeckLocationsInPlay());
            assertEquals("Chosen One zone boundary for " + zone,
                    zone == Zone.LOCATIONS,
                    fixture.chosen().areAllOriginalDeckLocationsInPlay());
        }
    }

    @Test
    public void unknownOrZeroLocationInventoryFailsClosed() {
        assertFalse(new com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle()
                .areAllOriginalDeckLocationsInPlay());
        assertFalse(new com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle()
                .areAllOriginalDeckLocationsInPlay());

        OracleFixture fixture = fixture("200_47|", List.of());
        assertFalse(fixture.rando().areAllOriginalDeckLocationsInPlay());
        assertFalse(fixture.chosen().areAllOriginalDeckLocationsInPlay());
    }

    private static OracleFixture fixture(String deckString,
                                         List<CardFixture> cards) {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getDeckString(Side.LIGHT)).thenReturn(deckString);

        when(gameState.getHand(PLAYER)).thenAnswer(ignored ->
                cardsInZone(cards, Zone.HAND));
        when(gameState.getReserveDeck(PLAYER)).thenAnswer(ignored ->
                cardsInZone(cards, Zone.RESERVE_DECK));
        when(gameState.getForcePile(PLAYER)).thenAnswer(ignored ->
                cardsInZone(cards, Zone.FORCE_PILE));
        when(gameState.getUsedPile(PLAYER)).thenAnswer(ignored ->
                cardsInZone(cards, Zone.USED_PILE));
        when(gameState.getLostPile(PLAYER)).thenAnswer(ignored ->
                cardsInZone(cards, Zone.LOST_PILE));
        when(gameState.getAllPermanentCards()).thenAnswer(ignored -> cards.stream()
                .map(CardFixture::card)
                .toList());

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle();
        rando.analyze(game, PLAYER, Side.LIGHT);
        chosen.analyze(game, PLAYER, Side.LIGHT);
        return new OracleFixture(gameState, rando, chosen);
    }

    private static List<PhysicalCard> cardsInZone(List<CardFixture> cards,
                                                   Zone zone) {
        List<PhysicalCard> matches = new ArrayList<>();
        for (CardFixture card : cards) {
            if (card.zone().get() == zone) {
                matches.add(card.card());
            }
        }
        return matches;
    }

    private static CardFixture card(String blueprintId, Zone initialZone) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint =
                CARD_LIBRARY.getSwccgoCardBlueprint(blueprintId);
        assertEquals(CardCategory.LOCATION, blueprint.getCardCategory());
        AtomicReference<Zone> zone = new AtomicReference<>(initialZone);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getTitle()).thenReturn(blueprintId);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getPermanentCardId()).thenReturn(
                NEXT_PERMANENT_CARD_ID.getAndIncrement());
        when(card.getZone()).thenAnswer(ignored -> zone.get());
        return new CardFixture(card, zone);
    }

    private record CardFixture(PhysicalCard card, AtomicReference<Zone> zone) {
    }

    private record OracleFixture(
            GameState gameState,
            com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle rando,
            com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle chosen) {
    }
}
