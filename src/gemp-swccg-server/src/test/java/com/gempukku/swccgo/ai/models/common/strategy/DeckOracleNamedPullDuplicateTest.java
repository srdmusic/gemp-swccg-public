package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.cards.set205.dark.Card205_017;
import com.gempukku.swccgo.cards.set208.dark.Card208_059;
import com.gempukku.swccgo.cards.set209.dark.Card209_050;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeckOracleNamedPullDuplicateTest {
    private static final String PLAYER = "dark";

    @Test
    public void iayf20859PullIgnoresVadersCastleOneTokenCollisionAndMirrors() {
        PhysicalCard saber = card(1, "Darth Vader's Lightsaber", "208_59",
                new Card208_059(), Zone.RESERVE_DECK);
        PhysicalCard iayf = card(2, "I Am Your Father", "205_17",
                new Card205_017(), Zone.SIDE_OF_TABLE);
        PhysicalCard castle = card(3, "Mustafar: Vader's Castle", "209_50",
                new Card209_050(), Zone.LOCATIONS);
        Fixture fixture = fixture(List.of(saber), List.of(iayf, castle));

        var rando = randoOracle(fixture);
        var chosen = chosenOracle(fixture);

        // Source-zone matching remains the broad V66 OR lookup.
        assertTrue(rando.hasTargetInZone(
                Zone.RESERVE_DECK, "missing", "Lightsaber"));
        assertTrue(chosen.hasTargetInZone(
                Zone.RESERVE_DECK, "missing", "Lightsaber"));
        assertTrue(rando.hasTargetInPlay("Vader's", "Lightsaber"));
        assertTrue(chosen.hasTargetInPlay("Vader's", "Lightsaber"));

        assertEquals(
                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.PullOutcome.WILL_SUCCEED,
                rando.validatePull(
                        Zone.RESERVE_DECK, "Vader's", "Lightsaber").outcome);
        assertEquals(
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.PullOutcome.WILL_SUCCEED,
                chosen.validatePull(
                        Zone.RESERVE_DECK, "Vader's", "Lightsaber").outcome);
    }

    @Test
    public void actual20859InPlayStillBlocksExactAndAllTokenNamedDuplicates() {
        PhysicalCard reserveSaber = card(1, "Darth Vader's Lightsaber", "208_59",
                new Card208_059(), Zone.RESERVE_DECK);
        PhysicalCard deployedSaber = card(2, "Darth Vader's Lightsaber", "208_59",
                new Card208_059(), Zone.ATTACHED);
        Fixture fixture = fixture(List.of(reserveSaber), List.of(deployedSaber));

        var rando = randoOracle(fixture);
        var chosen = chosenOracle(fixture);

        assertEquals(
                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.PullOutcome.WASTEFUL,
                rando.validatePull(
                        Zone.RESERVE_DECK, "Vader's Lightsaber").outcome);
        assertEquals(
                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.PullOutcome.WASTEFUL,
                rando.validatePull(
                        Zone.RESERVE_DECK, "Vader's", "Lightsaber").outcome);
        assertEquals(
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.PullOutcome.WASTEFUL,
                chosen.validatePull(
                        Zone.RESERVE_DECK, "Vader's Lightsaber").outcome);
        assertEquals(
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.PullOutcome.WASTEFUL,
                chosen.validatePull(
                        Zone.RESERVE_DECK, "Vader's", "Lightsaber").outcome);
    }

    private static com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle
    randoOracle(Fixture fixture) {
        var oracle = new com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle();
        oracle.analyze(fixture.game(), PLAYER, Side.DARK);
        return oracle;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
    chosenOracle(Fixture fixture) {
        var oracle = new com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle();
        oracle.analyze(fixture.game(), PLAYER, Side.DARK);
        return oracle;
    }

    private static Fixture fixture(
            List<PhysicalCard> reserve, List<PhysicalCard> inPlay) {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        when(game.getGameState()).thenReturn(gameState);
        when(gameState.getHand(PLAYER)).thenReturn(List.of());
        when(gameState.getReserveDeck(PLAYER)).thenReturn(reserve);
        when(gameState.getUsedPile(PLAYER)).thenReturn(List.of());
        when(gameState.getLostPile(PLAYER)).thenReturn(List.of());
        when(gameState.getForcePile(PLAYER)).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(inPlay);
        return new Fixture(game);
    }

    private static PhysicalCard card(
            int cardId, String title, String blueprintId,
            SwccgCardBlueprint blueprint, Zone zone) {
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getCardId()).thenReturn(cardId);
        when(card.getPermanentCardId()).thenReturn(cardId);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(zone);
        return card;
    }

    private record Fixture(SwccgGame game) {
    }
}
