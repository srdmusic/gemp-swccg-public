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
    public void contestOpponentDrainPreservesSoftestSiteCurve() {
        MoveDrainRoutingPolicy.Contribution oneCard =
                MoveDrainRoutingPolicy.contestOpponentDrain(
                        "Cloud City: Guest Quarters",
                        4.0f, 2, 2, 1);
        MoveDrainRoutingPolicy.Contribution twoCards =
                MoveDrainRoutingPolicy.contestOpponentDrain(
                        "Cloud City: Guest Quarters",
                        4.0f, 2, 2, 2);
        MoveDrainRoutingPolicy.Contribution threeCards =
                MoveDrainRoutingPolicy.contestOpponentDrain(
                        "Cloud City: Guest Quarters",
                        4.0f, 2, 2, 3);
        MoveDrainRoutingPolicy.Contribution fourCards =
                MoveDrainRoutingPolicy.contestOpponentDrain(
                        "Cloud City: Guest Quarters",
                        4.0f, 2, 2, 4);
        MoveDrainRoutingPolicy.Contribution fiveCards =
                MoveDrainRoutingPolicy.contestOpponentDrain(
                        "Cloud City: Guest Quarters",
                        4.0f, 2, 2, 5);

        assertFloat(350.0f, oneCard.delta());
        assertFloat(300.0f, twoCards.delta());
        assertFloat(250.0f, threeCards.delta());
        assertFloat(200.0f, fourCards.delta());
        assertFloat(200.0f, fiveCards.delta());
        assertEquals(
                "V166 CONTEST DRAIN: opponent out-draining (net>=2) — contest Cloud City: Guest Quarters (their drain 2, 1 opp cards)",
                oneCard.reason());
    }

    @Test
    public void contestOpponentDrainPreservesStrictGates() {
        assertFalse(MoveDrainRoutingPolicy.contestOpponentDrain(
                "Site", 0.0f, 2, 2, 1).applies());
        assertFalse(MoveDrainRoutingPolicy.contestOpponentDrain(
                "Site", Float.NaN, 2, 2, 1).applies());
        assertFalse(MoveDrainRoutingPolicy.contestOpponentDrain(
                "Site", 1.0f, 0, 2, 1).applies());
        assertFalse(MoveDrainRoutingPolicy.contestOpponentDrain(
                "Site", 1.0f, 2, 1, 1).applies());
        assertTrue(MoveDrainRoutingPolicy.contestOpponentDrain(
                "Site", 0.0001f, 1, 2, 0).applies());
        assertFloat(400.0f, MoveDrainRoutingPolicy.contestOpponentDrain(
                "Site", 0.0001f, 1, 2, 0).delta());
    }

    @Test
    public void destinationDrainPreservesPositiveDrainPrecedenceAndFormula() {
        MoveDrainRoutingPolicy.DestinationDrain result =
                MoveDrainRoutingPolicy.destinationDrain(
                        "Mapuzo: Underground Corridor",
                        2.5f, true);

        assertEquals(
                MoveDrainRoutingPolicy.DestinationDrainBranch.DRAIN_POTENTIAL,
                result.branch());
        assertFloat(30.0f, result.contribution().delta());
        assertEquals(
                "V67e DRAIN POTENTIAL: drain 2.5 at Mapuzo: Underground Corridor = +30 opponent force loss",
                result.contribution().reason());
    }

    @Test
    public void destinationDrainPreservesBattlegroundFormattedBoundary() {
        MoveDrainRoutingPolicy.DestinationDrain result =
                MoveDrainRoutingPolicy.destinationDrain(
                        "Battleground", 1.25f, false);

        assertFloat(15.0f, result.contribution().delta());
        assertEquals(
                "V67e DRAIN POTENTIAL: drain 1.3 at Battleground = +15 opponent force loss",
                result.contribution().reason());
    }

    @Test
    public void destinationDrainPreservesTransitAndZeroDrainBranches() {
        MoveDrainRoutingPolicy.DestinationDrain transit =
                MoveDrainRoutingPolicy.destinationDrain(
                        "Mapuzo: Underground Corridor",
                        0.0f, true);
        MoveDrainRoutingPolicy.DestinationDrain zero =
                MoveDrainRoutingPolicy.destinationDrain(
                        "Cloud City: Upper Plaza Corridor",
                        0.0f, false);

        assertEquals(
                MoveDrainRoutingPolicy.DestinationDrainBranch.TRANSIT_STAGING,
                transit.branch());
        assertFloat(300.0f, transit.contribution().delta());
        assertEquals(
                "V67n TRANSIT STAGING DEST: Mapuzo: Underground Corridor is the Hidden Path transit hub; prefer routing Jedi through here (+300 objective preference)",
                transit.contribution().reason());
        assertEquals(
                MoveDrainRoutingPolicy.DestinationDrainBranch.ZERO_DRAIN,
                zero.branch());
        assertFloat(-200.0f, zero.contribution().delta());
        assertEquals(
                "V67g ZERO DRAIN: Cloud City: Upper Plaza Corridor has no opponent force icons — wasted move!",
                zero.contribution().reason());
    }

    @Test
    public void moveFromDrainPreservesStrictDropAndExemptions() {
        MoveDrainRoutingPolicy.Contribution drop =
                MoveDrainRoutingPolicy.moveFromDrain(
                        true, false,
                        "Cloud City: Guest Quarters", 3,
                        "Cloud City: Upper Plaza Corridor", 1);

        assertTrue(drop.applies());
        assertFloat(-500.0f, drop.delta());
        assertEquals(
                "V67g MOVE-FROM-DRAIN: leaving Cloud City: Guest Quarters (drain 3) for Cloud City: Upper Plaza Corridor (drain 1) — losing 2 drain!",
                drop.reason());
        assertFalse(MoveDrainRoutingPolicy.moveFromDrain(
                false, false, "Source", 3, "Destination", 1)
                .applies());
        assertFalse(MoveDrainRoutingPolicy.moveFromDrain(
                true, true, "Source", 3, "Destination", 1)
                .applies());
        assertFalse(MoveDrainRoutingPolicy.moveFromDrain(
                true, false, "Source", 2, "Destination", 2)
                .applies());
        assertFalse(MoveDrainRoutingPolicy.moveFromDrain(
                true, false, "Source", 1, "Destination", 2)
                .applies());
    }

    @Test
    public void moveToHereClassificationPreservesLegacyPatterns() {
        assertTrue(MoveDrainRoutingPolicy.isMoveToHereAction(
                "move from cloud city to here"));
        assertTrue(MoveDrainRoutingPolicy.isMoveToHereAction(
                "move to here"));
        assertTrue(MoveDrainRoutingPolicy.isMoveToHereAction(
                "relocate to here"));
        assertFalse(MoveDrainRoutingPolicy.isMoveToHereAction(
                "move from cloud city"));
        assertFalse(MoveDrainRoutingPolicy.isMoveToHereAction(
                "relocate to cloud city"));
        assertFalse(MoveDrainRoutingPolicy.isMoveToHereAction(null));
    }

    @Test
    public void moveToHereDrainPreservesNonZeroDrainNoOp() {
        MoveDrainRoutingPolicy.MoveToHereDrain result =
                MoveDrainRoutingPolicy.moveToHereDrain(
                        "Mustafar: Vader's Castle", 1,
                        false, null);

        assertEquals(MoveDrainRoutingPolicy.MoveToHereDrainBranch.NONE,
                result.branch());
        assertFalse(result.contribution().applies());
        assertFloat(0.0f, result.contribution().delta());
        assertNull(result.contribution().reason());
    }

    @Test
    public void moveToHereDrainPreservesRetreatExemption() {
        MoveDrainRoutingPolicy.MoveToHereDrain result =
                MoveDrainRoutingPolicy.moveToHereDrain(
                        "Mustafar: Vader's Castle", 0,
                        true, "Cloud City: Lower Corridor");

        assertEquals(
                MoveDrainRoutingPolicy.MoveToHereDrainBranch.RETREAT_EXEMPT,
                result.branch());
        assertTrue(result.contribution().applies());
        assertFloat(0.0f, result.contribution().delta());
        assertEquals(
                "V67ae RETREAT EXEMPT: 'Cloud City: Lower Corridor' hopelessly outgunned (gap >= 6, V33 standard) — retreat to non-drain allowed",
                result.contribution().reason());
    }

    @Test
    public void moveToHereDrainPreservesZeroDrainPenalty() {
        MoveDrainRoutingPolicy.MoveToHereDrain result =
                MoveDrainRoutingPolicy.moveToHereDrain(
                        "Mustafar: Vader's Castle", 0,
                        false, null);

        assertEquals(
                MoveDrainRoutingPolicy.MoveToHereDrainBranch.ZERO_DRAIN_PENALTY,
                result.branch());
        assertTrue(result.contribution().applies());
        assertFloat(-300.0f, result.contribution().delta());
        assertEquals(
                "V67ae MOVE-TO-NON-DRAIN: 'Mustafar: Vader's Castle' destination has 0 opp icons — losing drain pressure for a 'safe' retreat!",
                result.contribution().reason());
    }

    @Test
    public void blockedDrainEscapePreservesMoverEligibility() {
        assertTrue(MoveDrainRoutingPolicy.allowsBlockedDrainEscapeMover(
                false, false));
        assertFalse(MoveDrainRoutingPolicy.allowsBlockedDrainEscapeMover(
                true, false));
        assertFalse(MoveDrainRoutingPolicy.allowsBlockedDrainEscapeMover(
                false, true));
        assertFalse(MoveDrainRoutingPolicy.allowsBlockedDrainEscapeMover(
                true, true));
    }

    @Test
    public void blockedDrainEscapePreservesSpyAndEnemyBonuses() {
        MoveDrainRoutingPolicy.BlockedDrainEscape spy =
                MoveDrainRoutingPolicy.blockedDrainEscape(
                        "Cloud City: Guest Quarters",
                        true, true, true);
        MoveDrainRoutingPolicy.BlockedDrainEscape enemy =
                MoveDrainRoutingPolicy.blockedDrainEscape(
                        "Cloud City: Guest Quarters",
                        true, true, false);

        assertTrue(spy.contribution().applies());
        assertTrue(spy.undercoverSpyBlock());
        assertFloat(250.0f, spy.contribution().delta());
        assertEquals(
                "V35.4: UNDERCOVER SPY blocking drain at Cloud City: Guest Quarters — move away to drain elsewhere!",
                spy.contribution().reason());
        assertTrue(enemy.contribution().applies());
        assertFalse(enemy.undercoverSpyBlock());
        assertFloat(150.0f, enemy.contribution().delta());
        assertEquals(
                "V35.4: Enemy presence blocking drain at Cloud City: Guest Quarters — move away to drain elsewhere!",
                enemy.contribution().reason());
    }

    @Test
    public void blockedDrainEscapePreservesPresenceGates() {
        assertFalse(MoveDrainRoutingPolicy.blockedDrainEscape(
                "Site", false, true, true)
                .contribution().applies());
        assertFalse(MoveDrainRoutingPolicy.blockedDrainEscape(
                "Site", true, false, false)
                .contribution().applies());
        assertTrue(MoveDrainRoutingPolicy.blockedDrainEscape(
                "Site", true, false, true)
                .contribution().applies());
    }

    @Test
    public void vaderCastleRetreatPreservesMatchersAndMustafarGate() {
        assertTrue(MoveDrainRoutingPolicy.isVaderCastleRetreatAction(
                "transport vader to vader's castle"));
        assertTrue(MoveDrainRoutingPolicy.isVaderCastleRetreatAction(
                "transport to mustafar"));
        assertFalse(MoveDrainRoutingPolicy.isVaderCastleRetreatAction(
                "transport vader to executor"));
        assertFalse(MoveDrainRoutingPolicy.isVaderCastleRetreatAction(null));
        assertTrue(MoveDrainRoutingPolicy.isMustafarLocation(
                "Mustafar: Vader's Castle"));
        assertFalse(MoveDrainRoutingPolicy.isMustafarLocation(
                "Cloud City: Guest Quarters"));
        assertFalse(MoveDrainRoutingPolicy.isMustafarLocation(null));
    }

    @Test
    public void vaderCastleRetreatPreservesDrainPenaltyAndAdditiveStacks() {
        MoveDrainRoutingPolicy.Contribution retreat =
                MoveDrainRoutingPolicy.vaderCastleRetreat(
                        "Cloud City: Guest Quarters", 2);

        assertTrue(retreat.applies());
        assertFloat(-300.0f, retreat.delta());
        assertEquals(
                "V29.7 VADER RETREAT: Vader is draining 2 at Cloud City: Guest Quarters — DON'T retreat to Mustafar!",
                retreat.reason());
        assertFalse(MoveDrainRoutingPolicy.vaderCastleRetreat(
                "Cloud City: Guest Quarters", 0).applies());
        assertFalse(MoveDrainRoutingPolicy.vaderCastleRetreat(
                "Cloud City: Guest Quarters", -1).applies());

        float spyEscape = MoveDrainRoutingPolicy.blockedDrainEscape(
                "Cloud City: Guest Quarters", true, true, true)
                .contribution().delta();
        float enemyEscape = MoveDrainRoutingPolicy.blockedDrainEscape(
                "Cloud City: Guest Quarters", true, true, false)
                .contribution().delta();
        assertFloat(-50.0f, spyEscape + retreat.delta());
        assertFloat(-150.0f, enemyEscape + retreat.delta());
    }

    @Test
    public void destinationDrainPreservesLegacyAdditiveStacks() {
        float ordinaryZeroDrain =
                MoveDrainRoutingPolicy.destinationDrain(
                        "Zero", 0.0f, false)
                        .contribution().delta();
        float twoIconDrop = MoveDrainRoutingPolicy.moveFromDrain(
                true, false, "Source", 2, "Zero", 0)
                .delta();
        float transit = MoveDrainRoutingPolicy.destinationDrain(
                "Mapuzo: Underground Corridor", 0.0f, true)
                .contribution().delta();
        float transitDrop = MoveDrainRoutingPolicy.moveFromDrain(
                true, true, "Source", 2,
                "Mapuzo: Underground Corridor", 0)
                .delta();
        float contest = MoveDrainRoutingPolicy.contestOpponentDrain(
                "Printed Zero", 4.0f, 1, 2, 1).delta();

        assertFloat(-700.0f, ordinaryZeroDrain + twoIconDrop);
        assertFloat(300.0f, transit + transitDrop);
        assertFloat(150.0f, contest + ordinaryZeroDrain);
    }

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

        assertFloat(560.0f, 1000.0f + v85 + v2913 + v73);
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
