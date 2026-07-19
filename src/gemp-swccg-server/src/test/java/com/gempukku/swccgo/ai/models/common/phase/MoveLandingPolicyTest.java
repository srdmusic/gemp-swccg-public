package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MoveLandingPolicyTest {
    @Test
    public void subtypeStarfighterPreservesHardVetoPrecedence() {
        PhysicalCard card = card("Red 5", CardSubtype.STARFIGHTER);

        MoveLandingPolicy.Evaluation result =
                MoveLandingPolicy.evaluate("land red 5", card, null);

        assertEquals(MoveLandingPolicy.Route.HARD_VETO, result.route());
        assertFalse(result.passengerScanRan());
        assertEquals(0, result.actualPassengers());
        assertFloat(0.0f, result.delta());
        assertEquals(
                "V49 BLOCKED: Landing Red 5 at a site with NO passengers = power 0 = instant death from overflow! NEVER land unprotected!",
                result.reason());
    }

    @Test
    public void capitalWithoutPassengerPreservesHardVeto() {
        PhysicalCard card = card("Executor", CardSubtype.CAPITAL);
        SwccgGame game = gameWithPassengers(card, List.of(), List.of());

        MoveLandingPolicy.Evaluation result =
                MoveLandingPolicy.evaluate("land executor", card, game);

        assertEquals(MoveLandingPolicy.Route.HARD_VETO, result.route());
        assertTrue(result.passengerScanRan());
        assertEquals(0, result.actualPassengers());
    }

    @Test
    public void transportWithPassengerPreservesAllowedScoreAndReason() {
        PhysicalCard card = card("Wild Karrde", CardSubtype.TRANSPORT);
        PhysicalCard passenger = passenger();
        SwccgGame game = gameWithPassengers(
                card, List.of(passenger), List.of(passenger));

        MoveLandingPolicy.Evaluation result =
                MoveLandingPolicy.evaluate("land wild karrde", card, game);

        assertEquals(MoveLandingPolicy.Route.PASSENGER_SHIP_ALLOWED,
                result.route());
        assertTrue(result.passengerScanRan());
        assertEquals(1, result.actualPassengers());
        assertFloat(10.0f, result.delta());
        assertEquals(
                "V49: Landing Wild Karrde with  passengers aboard — can disembark to protect",
                result.reason());
    }

    @Test
    public void passengerScanFailurePreservesNoPassengerFallback() {
        PhysicalCard card = card("Wild Karrde", CardSubtype.TRANSPORT);
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        when(game.getGameState()).thenReturn(gameState);
        when(gameState.getAllPermanentCards())
                .thenThrow(new IllegalStateException("scan failed"));

        MoveLandingPolicy.Evaluation result =
                MoveLandingPolicy.evaluate("land wild karrde", card, game);

        assertEquals(MoveLandingPolicy.Route.HARD_VETO, result.route());
        assertTrue(result.passengerScanRan());
        assertEquals(0, result.actualPassengers());
    }

    @Test
    public void nameDetectedShipPreservesPostScanFallbackOrdering() {
        PhysicalCard card = card("Millennium Falcon", null);
        PhysicalCard passenger = passenger();
        SwccgGame game = gameWithPassengers(
                card, List.of(passenger), List.of(passenger));

        MoveLandingPolicy.Evaluation result =
                MoveLandingPolicy.evaluate(
                        "land millennium falcon", card, game);

        assertEquals(MoveLandingPolicy.Route.HARD_VETO, result.route());
        assertFalse(result.passengerScanRan());
        assertEquals(0, result.actualPassengers());
    }

    @Test
    public void nameDetectedStarfighterPreservesHardVetoPrecedence() {
        MoveLandingPolicy.Evaluation result =
                MoveLandingPolicy.evaluate(
                        "land x-wing at docking bay", null, null);

        assertEquals(MoveLandingPolicy.Route.HARD_VETO, result.route());
        assertEquals("unknown", result.cardName());
        assertFalse(result.passengerScanRan());
    }

    @Test
    public void groundLandingPreservesPositiveScore() {
        PhysicalCard card = card("Luke Skywalker", null);

        MoveLandingPolicy.Evaluation result =
                MoveLandingPolicy.evaluate(
                        "land luke skywalker", card, null);

        assertEquals(MoveLandingPolicy.Route.GROUND_ALLOWED,
                result.route());
        assertFloat(10.0f, result.delta());
        assertEquals("Land (ground deployment)", result.reason());
    }

    @Test
    public void landspeedSubstringPreservesGenericLandingQuirk() {
        MoveLandingPolicy.Evaluation result =
                MoveLandingPolicy.evaluate(
                        "move using landspeed", null, null);

        assertEquals(MoveLandingPolicy.Route.GROUND_ALLOWED,
                result.route());
        assertFloat(10.0f, result.delta());
        assertEquals("Land (ground deployment)", result.reason());
    }

    private static PhysicalCard card(String title, CardSubtype subtype) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getPermanentCardId()).thenReturn(42);
        when(blueprint.getCardSubtype()).thenReturn(subtype);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.STARSHIP);
        return card;
    }

    private static PhysicalCard passenger() {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        return card;
    }

    private static SwccgGame gameWithPassengers(
            PhysicalCard ship, List<PhysicalCard> permanents,
            List<PhysicalCard> aboard) {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getAllPermanentCards()).thenReturn(permanents);
        when(gameState.findCardByPermanentId(42)).thenReturn(ship);
        for (PhysicalCard card : permanents) {
            when(modifiers.isAboard(
                    gameState, card, ship, false, true))
                    .thenReturn(aboard.contains(card));
        }
        return game;
    }

    private static void assertFloat(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(actual));
    }
}
