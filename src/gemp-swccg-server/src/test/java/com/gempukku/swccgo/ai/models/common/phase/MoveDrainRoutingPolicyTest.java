package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MoveDrainRoutingPolicyTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";

    @Test
    public void uncontestedDeparturePreservesBestAdjacentPenalty() {
        Harness harness = new Harness();
        PhysicalCard source = location("Tatooine: Cantina");
        PhysicalCard first = location("Tatooine: Lars' Moisture Farm");
        PhysicalCard best = location("Tatooine: Mos Eisley");
        harness.locationsInOrder(source, first, best);
        harness.drain(source, 3.0f);
        harness.drain(first, 1.0f);
        harness.drain(best, 2.0f);
        harness.adjacent(source, first, true);
        harness.adjacent(source, best, true);

        MoveDrainRoutingPolicy.UncontestedDeparture result =
                MoveDrainRoutingPolicy.uncontestedDeparture(
                        harness.gameState, harness.game, source, PLAYER);

        assertTrue(result.contribution().applies());
        assertSame(best, result.bestAdjacent());
        assertFloat(3.0f, result.currentDrain());
        assertFloat(2.0f, result.bestAdjacentDrain());
        assertFloat(-800.0f, result.contribution().delta());
        assertEquals(
                "V85 UNCONTESTED: at Tatooine: Cantina (drain 3) with no opponent — best adjacent Tatooine: Mos Eisley only drains 2. STAY for the better drain!",
                result.contribution().reason());
    }

    @Test
    public void uncontestedDepartureRequiresPositiveCurrentAndStrictLoss() {
        Harness zero = new Harness();
        PhysicalCard zeroSource = location("Zero");
        zero.locationsInOrder(zeroSource);
        zero.drain(zeroSource, 0.0f);
        assertFalse(MoveDrainRoutingPolicy.uncontestedDeparture(
                zero.gameState, zero.game, zeroSource, PLAYER)
                .contribution().applies());

        Harness equal = new Harness();
        PhysicalCard equalSource = location("Source");
        PhysicalCard equalDestination = location("Equal");
        equal.locationsInOrder(equalSource, equalDestination);
        equal.drain(equalSource, 2.0f);
        equal.drain(equalDestination, 2.0f);
        equal.adjacent(equalSource, equalDestination, true);
        assertFalse(MoveDrainRoutingPolicy.uncontestedDeparture(
                equal.gameState, equal.game, equalSource, PLAYER)
                .contribution().applies());
    }

    @Test
    public void uncontestedDepartureKeepsFirstOnTiedDrain() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard first = location("First");
        PhysicalCard second = location("Second");
        harness.locationsInOrder(source, first, second);
        harness.drain(source, 3.0f);
        harness.drain(first, 1.0f);
        harness.drain(second, 1.0f);
        harness.adjacent(source, first, true);
        harness.adjacent(source, second, true);

        MoveDrainRoutingPolicy.UncontestedDeparture result =
                MoveDrainRoutingPolicy.uncontestedDeparture(
                        harness.gameState, harness.game, source, PLAYER);

        assertSame(first, result.bestAdjacent());
    }

    @Test
    public void uncontestedDepartureSkipsOneAdjacencyFailure() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard broken = location("Broken");
        PhysicalCard usable = location("Usable");
        harness.locationsInOrder(source, broken, usable);
        harness.drain(source, 3.0f);
        harness.drain(usable, 1.0f);
        when(harness.modifiers.isAdjacentSites(
                harness.gameState, source, broken))
                .thenThrow(new RuntimeException("injected"));
        harness.adjacent(source, usable, true);

        MoveDrainRoutingPolicy.UncontestedDeparture result =
                MoveDrainRoutingPolicy.uncontestedDeparture(
                        harness.gameState, harness.game, source, PLAYER);

        assertTrue(result.contribution().applies());
        assertSame(usable, result.bestAdjacent());
    }

    @Test
    public void explicitDestinationDrainPreservesLossAndZeroSurcharge() {
        Harness harness = new Harness();
        PhysicalCard source = location("Tatooine: Cantina");
        PhysicalCard destination = location("Tatooine: Mos Eisley");
        harness.locationsInOrder(source, destination);
        harness.drain(source, 2.0f);
        harness.drain(destination, 0.0f);

        MoveDrainRoutingPolicy.ExplicitDestinationDrain result =
                MoveDrainRoutingPolicy.explicitDestinationDrain(
                        harness.gameState, harness.game, source, PLAYER,
                        "move to tatooine: mos eisley");

        assertEquals(MoveDrainRoutingPolicy.DrainDirection.LOSS,
                result.direction());
        assertSame(destination, result.destination());
        assertFloat(2.0f, result.drainDelta());
        assertFloat(-160.0f, result.contribution().delta());
        assertEquals(
                "V29.13 BAD DRAIN SITE: Tatooine: Mos Eisley has drain 0 (current location has 2) — stay for better drain!",
                result.contribution().reason());
    }

    @Test
    public void explicitDestinationDrainPreservesFractionalLossFormula() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard destination = location("Destination");
        harness.locationsInOrder(source, destination);
        harness.drain(source, 2.5f);
        harness.drain(destination, 1.0f);

        MoveDrainRoutingPolicy.ExplicitDestinationDrain result =
                MoveDrainRoutingPolicy.explicitDestinationDrain(
                        harness.gameState, harness.game, source, PLAYER,
                        "destination");

        assertFloat(1.5f, result.drainDelta());
        assertFloat(-60.0f, result.contribution().delta());
    }

    @Test
    public void explicitDestinationDrainPreservesGainFormula() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard destination = location("Destination");
        harness.locationsInOrder(source, destination);
        harness.drain(source, 1.0f);
        harness.drain(destination, 3.0f);

        MoveDrainRoutingPolicy.ExplicitDestinationDrain result =
                MoveDrainRoutingPolicy.explicitDestinationDrain(
                        harness.gameState, harness.game, source, PLAYER,
                        "move to destination");

        assertEquals(MoveDrainRoutingPolicy.DrainDirection.GAIN,
                result.direction());
        assertFloat(2.0f, result.drainDelta());
        assertFloat(80.0f, result.contribution().delta());
        assertEquals(
                "V29.13 GOOD DRAIN SITE: Destination has drain 3 — better than current 1!",
                result.contribution().reason());
    }

    @Test
    public void explicitDestinationDrainPreservesEqualAndMissingNoops() {
        Harness equal = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard destination = location("Destination");
        equal.locationsInOrder(source, destination);
        equal.drain(source, 2.0f);
        equal.drain(destination, 2.0f);
        MoveDrainRoutingPolicy.ExplicitDestinationDrain equalResult =
                MoveDrainRoutingPolicy.explicitDestinationDrain(
                        equal.gameState, equal.game, source, PLAYER,
                        "destination");
        assertFalse(equalResult.contribution().applies());
        assertSame(destination, equalResult.destination());

        Harness missing = new Harness();
        missing.locationsInOrder(source, destination);
        MoveDrainRoutingPolicy.ExplicitDestinationDrain missingResult =
                MoveDrainRoutingPolicy.explicitDestinationDrain(
                        missing.gameState, missing.game, source, PLAYER,
                        "somewhere else");
        assertFalse(missingResult.contribution().applies());
        assertNull(missingResult.destination());
    }

    @Test
    public void explicitDestinationDrainKeepsFirstTextualMatch() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard first = location("First");
        PhysicalCard second = location("Second");
        harness.locationsInOrder(source, first, second);
        harness.drain(source, 1.0f);
        harness.drain(first, 2.0f);
        harness.drain(second, 4.0f);

        MoveDrainRoutingPolicy.ExplicitDestinationDrain result =
                MoveDrainRoutingPolicy.explicitDestinationDrain(
                        harness.gameState, harness.game, source, PLAYER,
                        "move through first toward second");

        assertSame(first, result.destination());
        assertFloat(40.0f, result.contribution().delta());
    }

    @Test
    public void cantinaShuttlePreservesForwardAndReverseRoutes() {
        PhysicalCard cantina = location("Tatooine: Cantina");
        PhysicalCard mosEisley = location("Tatooine: Mos Eisley");
        PhysicalCard mover = card(PLAYER, CardCategory.CHARACTER);
        PhysicalCard remaining = card(PLAYER, CardCategory.CHARACTER);

        GameState forwardState = mock(GameState.class);
        when(forwardState.getTopLocations())
                .thenReturn(List.of(cantina, mosEisley));
        when(forwardState.getCardsAtLocation(cantina))
                .thenReturn(List.of(mover, remaining));
        MoveDrainRoutingPolicy.CantinaShuttle forward =
                MoveDrainRoutingPolicy.cantinaShuttle(
                        forwardState, cantina, mover, PLAYER,
                        "move to tatooine: mos eisley");
        assertShuttle(forward, mosEisley, 1, "Tatooine: Cantina");

        GameState reverseState = mock(GameState.class);
        when(reverseState.getTopLocations())
                .thenReturn(List.of(mosEisley, cantina));
        when(reverseState.getCardsAtLocation(mosEisley))
                .thenReturn(List.of(mover, remaining));
        MoveDrainRoutingPolicy.CantinaShuttle reverse =
                MoveDrainRoutingPolicy.cantinaShuttle(
                        reverseState, mosEisley, mover, PLAYER,
                        "move to tatooine: cantina");
        assertShuttle(reverse, cantina, 1, "Tatooine: Mos Eisley");
    }

    @Test
    public void cantinaShuttlePreservesIdentityAndCharacterFilters() {
        PhysicalCard source = location("Tatooine: Cantina");
        PhysicalCard destination = location("Tatooine: Mos Eisley");
        PhysicalCard mover = card(PLAYER, CardCategory.CHARACTER);
        PhysicalCard opponent = card(OPPONENT, CardCategory.CHARACTER);
        PhysicalCard vehicle = card(PLAYER, CardCategory.VEHICLE);
        PhysicalCard noBlueprint = mock(PhysicalCard.class);
        when(noBlueprint.getOwner()).thenReturn(PLAYER);
        GameState gameState = mock(GameState.class);
        when(gameState.getTopLocations())
                .thenReturn(List.of(source, destination));
        when(gameState.getCardsAtLocation(source))
                .thenReturn(List.of(mover, opponent, vehicle, noBlueprint));

        MoveDrainRoutingPolicy.CantinaShuttle result =
                MoveDrainRoutingPolicy.cantinaShuttle(
                        gameState, source, mover, PLAYER,
                        "move to tatooine: mos eisley");

        assertTrue(result.pairMatched());
        assertFalse(result.contribution().applies());
        assertEquals(0, result.sourceCharactersRemaining());
    }

    @Test
    public void cantinaShuttleUsesFirstTopLocationMatchOnly() {
        PhysicalCard source = location("Tatooine: Cantina");
        PhysicalCard first = location("Tatooine: Docking Bay");
        PhysicalCard validLater = location("Tatooine: Mos Eisley");
        GameState gameState = mock(GameState.class);
        when(gameState.getTopLocations())
                .thenReturn(List.of(source, first, validLater));

        MoveDrainRoutingPolicy.CantinaShuttle result =
                MoveDrainRoutingPolicy.cantinaShuttle(
                        gameState, source, mock(PhysicalCard.class), PLAYER,
                        "move through tatooine: docking bay to tatooine: mos eisley");

        assertFalse(result.pairMatched());
        assertSame(first, result.destination());
    }

    @Test
    public void shuttleBoundaryPreservesCombinedLadderTotal() {
        Harness harness = new Harness();
        PhysicalCard source = location("Tatooine: Cantina");
        PhysicalCard destination = location("Tatooine: Mos Eisley");
        PhysicalCard mover = card(PLAYER, CardCategory.CHARACTER);
        PhysicalCard remaining = card(PLAYER, CardCategory.CHARACTER);
        harness.locationsInOrder(source, destination);
        when(harness.gameState.getTopLocations())
                .thenReturn(List.of(source, destination));
        when(harness.gameState.getCardsAtLocation(source))
                .thenReturn(List.of(mover, remaining));
        harness.drain(source, 2.0f);
        harness.drain(destination, 1.0f);
        harness.adjacent(source, destination, true);

        float v85 = MoveDrainRoutingPolicy.uncontestedDeparture(
                harness.gameState, harness.game, source, PLAYER)
                .contribution().delta();
        float v2913 = MoveDrainRoutingPolicy.explicitDestinationDrain(
                harness.gameState, harness.game, source, PLAYER,
                "move to tatooine: mos eisley")
                .contribution().delta();
        float v73 = MoveDrainRoutingPolicy.cantinaShuttle(
                harness.gameState, source, mover, PLAYER,
                "move to tatooine: mos eisley")
                .contribution().delta();

        assertFloat(5560.0f, 6000.0f + v85 + v2913 + v73);
    }

    private static void assertShuttle(
            MoveDrainRoutingPolicy.CantinaShuttle result,
            PhysicalCard destination, int remaining, String sourceTitle) {
        assertTrue(result.pairMatched());
        assertTrue(result.contribution().applies());
        assertSame(destination, result.destination());
        assertEquals(remaining, result.sourceCharactersRemaining());
        assertFloat(400.0f, result.contribution().delta());
        assertEquals(
                "V73 SHUTTLE: Cantina ↔ Mos Eisley shuttle — drain BOTH this turn ("
                        + remaining + " chars stay at " + sourceTitle + ")",
                result.contribution().reason());
    }

    private static PhysicalCard location(String title) {
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getTitle()).thenReturn(title);
        return card;
    }

    private static PhysicalCard card(String owner, CardCategory category) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getOwner()).thenReturn(owner);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory()).thenReturn(category);
        return card;
    }

    private static void assertFloat(float expected, float actual) {
        assertEquals(expected, actual, 0.0001f);
    }

    private static final class Harness {
        private final GameState gameState = mock(GameState.class);
        private final SwccgGame game = mock(SwccgGame.class);
        private final ModifiersQuerying modifiers = mock(ModifiersQuerying.class);

        private Harness() {
            when(game.getModifiersQuerying()).thenReturn(modifiers);
        }

        private void locationsInOrder(PhysicalCard... locations) {
            when(gameState.getLocationsInOrder())
                    .thenReturn(List.of(locations));
        }

        private void drain(PhysicalCard location, float amount) {
            when(modifiers.getForceDrainAmount(
                    gameState, location, PLAYER)).thenReturn(amount);
        }

        private void adjacent(
                PhysicalCard source, PhysicalCard destination,
                boolean adjacent) {
            when(modifiers.isAdjacentSites(
                    gameState, source, destination)).thenReturn(adjacent);
        }
    }
}
