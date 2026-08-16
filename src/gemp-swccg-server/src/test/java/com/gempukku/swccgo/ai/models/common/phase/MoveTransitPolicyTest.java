package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MoveTransitPolicyTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";

    @Test
    public void drainTransitStagingClassifierPreservesNarrowTitleMatch() {
        assertTrue(MoveTransitPolicy.isDrainTransitStagingSite(
                "Mapuzo: Underground Corridor"));
        assertTrue(MoveTransitPolicy.isDrainTransitStagingSite(
                "UNDERGROUND CORRIDOR"));
        assertFalse(MoveTransitPolicy.isDrainTransitStagingSite(
                "Underground Hideout"));
        assertFalse(MoveTransitPolicy.isDrainTransitStagingSite(null));
    }

    @Test
    public void positiveHiddenPathTransitClassifierPreservesLegacyPatterns() {
        assertTrue(MoveTransitPolicy.isPositiveHiddenPathTransitAction(
                "move jedi survivor here to a site"));
        assertTrue(MoveTransitPolicy.isPositiveHiddenPathTransitAction(
                "move jedi knight from here to a site"));
        assertFalse(MoveTransitPolicy.isPositiveHiddenPathTransitAction(
                "move jedi survivor here"));
        assertFalse(MoveTransitPolicy.isPositiveHiddenPathTransitAction(
                "move survivor here to a site"));
        assertFalse(MoveTransitPolicy.isPositiveHiddenPathTransitAction(null));
    }

    @Test
    public void positiveHiddenPathTransitUsesBoundedObjectivePreference() {
        MoveTransitPolicy.Contribution hiddenPath =
                MoveTransitPolicy.positiveHiddenPathTransit(true);
        MoveTransitPolicy.Contribution fallback =
                MoveTransitPolicy.positiveHiddenPathTransit(false);

        assertTrue(hiddenPath.applies());
        assertEquals(
                "V60 HIDDEN PATH TRANSIT: Move Jedi OUT of Corridor to advance the objective",
                hiddenPath.reason());
        assertRawFloat(300.0f, hiddenPath.delta());
        assertTrue(fallback.applies());
        assertEquals("Move Jedi transit action — tactical mobility",
                fallback.reason());
        assertRawFloat(200.0f, fallback.delta());
        assertTrue(hiddenPath.delta() > fallback.delta());
    }

    @Test
    public void capacitySlotSwapPreservesBothDirectionsAndExactPenalty() {
        for (String action : new String[]{
                "move to passenger capacity slot",
                "move to pilot capacity slot"}) {
            MoveTransitPolicy.Contribution result =
                    MoveTransitPolicy.capacitySlotSwap(action);

            assertTrue(result.applies());
            assertEquals(
                    "V87 NO SWAP: pilot↔passenger capacity slot rearrangement is pointless — hard block",
                    result.reason());
            assertRawFloat(-3000.0f, result.delta());
        }
    }

    @Test
    public void capacitySlotSwapRejectsUnrelatedCapacityChoice() {
        MoveTransitPolicy.Contribution result =
                MoveTransitPolicy.capacitySlotSwap(
                        "choose passenger capacity slot");

        assertFalse(result.applies());
        assertNull(result.reason());
        assertRawFloat(0.0f, result.delta());
    }

    @Test
    public void nonPilotDoesNotReadAttachment() {
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.isPilotOf()).thenReturn(false);

        MoveTransitPolicy.PilotLock result =
                MoveTransitPolicy.pilotLock(card);

        assertFalse(result.contribution().applies());
        verify(card, never()).getAttachedTo();
    }

    @Test
    public void pilotLockPreservesNamesReasonAndRawDelta() {
        PhysicalCard pilot = mock(PhysicalCard.class);
        PhysicalCard ship = mock(PhysicalCard.class);
        when(pilot.isPilotOf()).thenReturn(true);
        when(pilot.getAttachedTo()).thenReturn(ship);
        when(pilot.getTitle()).thenReturn("Piett");
        when(ship.getTitle()).thenReturn("Executor");

        MoveTransitPolicy.PilotLock result =
                MoveTransitPolicy.pilotLock(pilot);

        assertTrue(result.contribution().applies());
        assertRawFloat(-500.0f, result.contribution().delta());
        assertEquals("Piett", result.pilotName());
        assertEquals("Executor", result.shipName());
        assertEquals(
                "V25 PILOT LOCK: Piett is piloting Executor — NEVER leave the ship!",
                result.contribution().reason());
    }

    @Test
    public void pilotLockPreservesFallbackNames() {
        PhysicalCard pilot = mock(PhysicalCard.class);
        when(pilot.isPilotOf()).thenReturn(true);
        when(pilot.getAttachedTo()).thenReturn(null);
        when(pilot.getTitle()).thenReturn(null);

        MoveTransitPolicy.PilotLock result =
                MoveTransitPolicy.pilotLock(pilot);

        assertEquals("pilot", result.pilotName());
        assertEquals("unknown ship", result.shipName());
        assertEquals(
                "V25 PILOT LOCK: pilot is piloting unknown ship — NEVER leave the ship!",
                result.contribution().reason());
    }

    @Test
    public void passengerCapacitySlotPreservesSkipBranch() {
        MoveTransitPolicy.CapacitySlot result =
                MoveTransitPolicy.capacitySlot(
                        "move to passenger capacity slot");

        assertEquals(MoveTransitPolicy.CapacitySlotBranch.PASSENGER_SKIP,
                result.branch());
        assertRawFloat(0.0f, result.baseScore());
        assertFalse(result.contribution().applies());
    }

    @Test
    public void pilotCapacitySlotPreservesBaseAndReasoningScores() {
        MoveTransitPolicy.CapacitySlot result =
                MoveTransitPolicy.capacitySlot(
                        "move to pilot capacity slot");

        assertEquals(MoveTransitPolicy.CapacitySlotBranch.PILOT_PREFER,
                result.branch());
        assertRawFloat(100.0f, result.baseScore());
        assertTrue(result.contribution().applies());
        assertEquals("Move to pilot slot - adds power!",
                result.contribution().reason());
        assertRawFloat(50.0f, result.contribution().delta());
    }

    @Test
    public void passengerCapacitySlotPreservesFirstBranchPrecedence() {
        MoveTransitPolicy.CapacitySlot result =
                MoveTransitPolicy.capacitySlot(
                        "passenger capacity slot then pilot capacity slot");

        assertEquals(MoveTransitPolicy.CapacitySlotBranch.PASSENGER_SKIP,
                result.branch());
    }

    @Test
    public void unrelatedMoveHasNoCapacitySlotContribution() {
        MoveTransitPolicy.CapacitySlot result =
                MoveTransitPolicy.capacitySlot("move using landspeed");

        assertEquals(MoveTransitPolicy.CapacitySlotBranch.NONE,
                result.branch());
        assertRawFloat(0.0f, result.baseScore());
        assertFalse(result.contribution().applies());
    }

    @Test
    public void actionTextCapacityChoicePreservesReplacementAndContribution() {
        MoveTransitPolicy.CapacityChoice pilot =
                MoveTransitPolicy.capacityChoice(true, false);
        MoveTransitPolicy.CapacityChoice passenger =
                MoveTransitPolicy.capacityChoice(false, true);

        assertEquals(MoveTransitPolicy.CapacitySlotBranch.PILOT_PREFER,
                pilot.branch());
        assertRawFloat(100.0f, pilot.replacementScore());
        assertRawFloat(100.0f, pilot.contribution().delta());
        assertRawFloat(200.0f,
                pilot.replacementScore() + pilot.contribution().delta());
        assertEquals(MoveTransitPolicy.CapacitySlotBranch.PASSENGER_SKIP,
                passenger.branch());
        assertRawFloat(-50.0f, passenger.replacementScore());
        assertRawFloat(-50.0f, passenger.contribution().delta());
        assertRawFloat(-100.0f,
                passenger.replacementScore()
                        + passenger.contribution().delta());
    }

    @Test
    public void actionTextCapacityChoicePreservesPilotFirstPrecedence() {
        MoveTransitPolicy.CapacityChoice both =
                MoveTransitPolicy.capacityChoice(true, true);

        assertEquals(MoveTransitPolicy.CapacitySlotBranch.PILOT_PREFER,
                both.branch());
        assertRawFloat(100.0f, both.replacementScore());
        assertRawFloat(100.0f, both.contribution().delta());
    }

    @Test
    public void embarkPreservesPowerBoundaryAndUnknownPowerEligibility() {
        MoveTransitPolicy.EmbarkEvaluation below =
                MoveTransitPolicy.embark(
                        true, true, true, 3.999f,
                        true, true, false, "Pilot", "Walker");
        MoveTransitPolicy.EmbarkEvaluation exact =
                MoveTransitPolicy.embark(
                        true, true, true, 4.0f,
                        true, true, false, "Pilot", "Walker");
        MoveTransitPolicy.EmbarkEvaluation unknown =
                MoveTransitPolicy.embark(
                        true, true, true, null,
                        true, true, false, "Pilot", "Walker");

        assertEquals(MoveTransitPolicy.EmbarkBranch.UNMANNED_TARGET,
                below.branch());
        assertRawFloat(500.0f, below.contribution().delta());
        assertEquals(MoveTransitPolicy.EmbarkBranch.POWER_FOUR_PLUS,
                exact.branch());
        assertRawFloat(0.0f, exact.contribution().delta());
        assertEquals(MoveTransitPolicy.EmbarkBranch.UNMANNED_TARGET,
                unknown.branch());
        assertRawFloat(500.0f, unknown.contribution().delta());
    }

    @Test
    public void embarkNeutralOutcomesRemainZero() {
        MoveTransitPolicy.EmbarkEvaluation nonPilot =
                MoveTransitPolicy.embark(
                        true, true, false, 2.0f,
                        true, true, false, "Passenger", "Walker");
        MoveTransitPolicy.EmbarkEvaluation noTarget =
                MoveTransitPolicy.embark(
                        true, true, true, 2.0f,
                        true, false, false, "Pilot", null);

        assertEquals(MoveTransitPolicy.EmbarkBranch.NON_PILOT,
                nonPilot.branch());
        assertRawFloat(0.0f, nonPilot.contribution().delta());
        assertEquals(MoveTransitPolicy.EmbarkBranch.NO_UNMANNED_TARGET,
                noTarget.branch());
        assertRawFloat(0.0f, noTarget.contribution().delta());
    }

    @Test
    public void embarkPreservesEveryLegacyNeutralReasonAndGuardPrecedence() {
        assertEmbark(
                MoveTransitPolicy.embark(
                        false, false, false, 9.0f,
                        false, false, true, null, null),
                MoveTransitPolicy.EmbarkBranch.ERROR,
                "Embark action (error)", 0.0f);
        assertEmbark(
                MoveTransitPolicy.embark(
                        false, false, false, 9.0f,
                        false, false, false, null, null),
                MoveTransitPolicy.EmbarkBranch.NO_CONTEXT,
                "Embark action (no context)", 0.0f);
        assertEmbark(
                MoveTransitPolicy.embark(
                        true, false, false, 9.0f,
                        false, false, false, null, null),
                MoveTransitPolicy.EmbarkBranch.NO_CARD,
                "Embark action (no card)", 0.0f);
        assertEmbark(
                MoveTransitPolicy.embark(
                        true, true, false, 9.0f,
                        false, false, false, "Passenger", null),
                MoveTransitPolicy.EmbarkBranch.NON_PILOT,
                "Embark action (non-pilot)", 0.0f);
        assertEmbark(
                MoveTransitPolicy.embark(
                        true, true, true, 4.75f,
                        false, false, false, "Pilot", null),
                MoveTransitPolicy.EmbarkBranch.POWER_FOUR_PLUS,
                "Embark action (skipped: power 4 — better as ground troop)",
                0.0f);
        assertEmbark(
                MoveTransitPolicy.embark(
                        true, true, true, 3.0f,
                        false, true, false, "Pilot", "Walker"),
                MoveTransitPolicy.EmbarkBranch.NO_LOCATION,
                "Embark action (no location)", 0.0f);
        assertEmbark(
                MoveTransitPolicy.embark(
                        true, true, true, 3.0f,
                        true, false, false, "Pilot", null),
                MoveTransitPolicy.EmbarkBranch.NO_UNMANNED_TARGET,
                "Embark action (no unmanned target at site)", 0.0f);
    }

    @Test
    public void embarkUnmannedTargetPreservesLegacyBoostAndReason() {
        assertEmbark(
                MoveTransitPolicy.embark(
                        true, true, true, 3.0f,
                        true, true, false, "Veers", "Blizzard 1"),
                MoveTransitPolicy.EmbarkBranch.UNMANNED_TARGET,
                "EMBARK PILOT: 'Veers' boarding unmanned 'Blizzard 1' — vehicle gets power & protection",
                500.0f);
    }

    @Test
    public void residualTransferAndShipDockPreserveLegacyPenalties() {
        MoveTransitPolicy.Contribution transfer =
                MoveTransitPolicy.residualTransfer();
        MoveTransitPolicy.Contribution shipDock =
                MoveTransitPolicy.shipDock();

        assertTrue(transfer.applies());
        assertEquals("Usually avoid disembark/relocate/transfer",
                transfer.reason());
        assertRawFloat(-50.0f, transfer.delta());
        assertTrue(shipDock.applies());
        assertEquals("Avoid ship-docking", shipDock.reason());
        assertRawFloat(-50.0f, shipDock.delta());
    }

    @Test
    public void systemDestinationPenaltiesStackIndependently() {
        MoveTransitPolicy.SpaceDestinationPenalties both =
                MoveTransitPolicy.spaceDestination(true, true, true);
        MoveTransitPolicy.SpaceDestinationPenalties ground =
                MoveTransitPolicy.spaceDestination(false, true, true);

        assertRawFloat(-300.0f,
                both.permanentWeaponCharacter().delta());
        assertRawFloat(-300.0f, both.vehicle().delta());
        assertRawFloat(-600.0f,
                both.permanentWeaponCharacter().delta()
                        + both.vehicle().delta());
        assertFalse(ground.permanentWeaponCharacter().applies());
        assertFalse(ground.vehicle().applies());
    }

    @Test
    public void defensiveShuttlePreservesExactTwoToOneBoundary() {
        PhysicalCard location = location("Cloud City: Upper Walkway");
        GameState gameState = gameState(
                List.of(location),
                List.of(List.of(powerCard(PLAYER, 3.0f),
                        powerCard(OPPONENT, 6.0f))));

        MoveTransitPolicy.MovementTypes result =
                MoveTransitPolicy.movementTypes(
                        "shuttle to cloud city: upper walkway",
                        gameState, PLAYER);

        MoveTransitPolicy.DefensiveShuttle shuttle =
                result.defensiveShuttle();
        assertTrue(result.shuttleAction());
        assertTrue(shuttle.contribution().applies());
        assertRawFloat(20.0f, shuttle.contribution().delta());
        assertEquals("Cloud City: Upper Walkway", shuttle.locationTitle());
        assertRawFloat(3.0f, shuttle.ourPower());
        assertRawFloat(6.0f, shuttle.theirPower());
        assertEquals(
                "V25 Defensive shuttle — opponent has 6 vs our 3 at Cloud City: Upper Walkway",
                shuttle.contribution().reason());
    }

    @Test
    public void defensiveShuttleRejectsBelowBoundaryAndZeroFriendlyPower() {
        PhysicalCard first = location("First Site");
        GameState below = gameState(
                List.of(first),
                List.of(List.of(powerCard(PLAYER, 3.0f),
                        powerCard(OPPONENT, 5.0f))));
        GameState zero = gameState(
                List.of(first),
                List.of(List.of(powerCard(OPPONENT, 8.0f))));

        assertFalse(MoveTransitPolicy.movementTypes(
                "transport to first site", below, PLAYER)
                .defensiveShuttle().contribution().applies());
        assertFalse(MoveTransitPolicy.movementTypes(
                "transport to first site", zero, PLAYER)
                .defensiveShuttle().contribution().applies());
    }

    @Test
    public void defensiveShuttleStopsAtFirstTextualLocation() {
        PhysicalCard first = location("First Site");
        PhysicalCard second = location("Second Site");
        GameState gameState = gameState(
                List.of(first, second),
                List.of(
                        List.of(powerCard(PLAYER, 4.0f),
                                powerCard(OPPONENT, 4.0f)),
                        List.of(powerCard(PLAYER, 2.0f),
                                powerCard(OPPONENT, 8.0f))));

        MoveTransitPolicy.MovementTypes result =
                MoveTransitPolicy.movementTypes(
                        "shuttle from first site to second site",
                        gameState, PLAYER);

        assertTrue(result.shuttleAction());
        assertFalse(result.defensiveShuttle().contribution().applies());
        assertNull(result.defensiveShuttle().locationTitle());
    }

    @Test
    public void matchingShuttleWithoutGameStateRemainsScoreNeutral() {
        MoveTransitPolicy.MovementTypes result =
                MoveTransitPolicy.movementTypes(
                        "shuttle a character", null, PLAYER);

        assertTrue(result.shuttleAction());
        assertFalse(result.defensiveShuttle().contribution().applies());
    }

    @Test
    public void genericDockingBayTextHasNoUnconditionalPositive() {
        MoveTransitPolicy.MovementTypes result =
                MoveTransitPolicy.movementTypes(
                        "docking bay transit and take off", null, PLAYER);

        assertFalse(result.shuttleAction());
        assertFalse(result.dockingBayTransit().applies());
        assertNull(result.dockingBayTransit().reason());
        assertRawFloat(0.0f, result.dockingBayTransit().delta());
        assertTrue(result.takeOff().applies());
        assertEquals("Take off (space deployment)",
                result.takeOff().reason());
        assertRawFloat(10.0f, result.takeOff().delta());
    }

    @Test
    public void hiddenPathSafehousePreservesBoundedTransitPreference() {
        PhysicalCard card = mover("Cal Kestis", "Mapuzo: Safehouse");

        MoveTransitPolicy.HiddenPathTransit result =
                MoveTransitPolicy.hiddenPathTransit(
                        "A Hidden Path / The Way Out",
                        card, "move using landspeed");

        assertEquals(MoveTransitPolicy.HiddenPathBranch.SAFEHOUSE_TO_CORRIDOR,
                result.branch());
        assertTrue(result.contribution().applies());
        assertRawFloat(300.0f, result.contribution().delta());
        assertEquals(
                "V53b HIDDEN PATH: prefer the free Safehouse to Corridor objective-progress move (+300)",
                result.contribution().reason());
        assertFalse(result.claimMandatoryTransit());
        assertEquals("V53b SAFEHOUSE→CORRIDOR", result.claimIdentity());
        assertFalse(result.hardVeto());
        assertEquals("Cal Kestis", result.characterName());
    }

    @Test
    public void hiddenPathCorridorUsesBoundedObjectivePreference() {
        PhysicalCard card = mover(
                "Kanan Jarrus", "Mapuzo: Underground Corridor");

        MoveTransitPolicy.HiddenPathTransit result =
                MoveTransitPolicy.hiddenPathTransit(
                        "a hidden path", card, "move");

        assertEquals(
                MoveTransitPolicy.HiddenPathBranch.CORRIDOR_LANDSPEED_BLOCK,
                result.branch());
        assertTrue(result.contribution().applies());
        assertRawFloat(-300.0f, result.contribution().delta());
        assertEquals(
                "V60 HIDDEN PATH LANDSPEED PREFERENCE: prefer the Corridor transit text instead of moving back to Mapuzo",
                result.contribution().reason());
        assertFalse(result.claimMandatoryTransit());
        assertFalse(result.hardVeto());
        assertNull(result.hardVetoReason());
        assertEquals("Kanan Jarrus", result.characterName());
    }

    @Test
    public void hiddenPathBroadUndergroundNameStillUsesBoundedPreference() {
        PhysicalCard card = mover("Jedi Survivor", "Underground Hideout");

        MoveTransitPolicy.HiddenPathTransit result =
                MoveTransitPolicy.hiddenPathTransit(
                        "A HIDDEN PATH", card,
                        "move using landspeed to safehouse");

        assertEquals(
                MoveTransitPolicy.HiddenPathBranch.CORRIDOR_LANDSPEED_BLOCK,
                result.branch());
        assertTrue(result.contribution().applies());
        assertRawFloat(-300.0f, result.contribution().delta());
        assertFalse(result.hardVeto());
    }

    @Test
    public void hiddenPathMapuzoPreservesObjectiveProgressTransit() {
        PhysicalCard card = mover("Cere Junda", "Mapuzo: Mining Village");

        MoveTransitPolicy.HiddenPathTransit result =
                MoveTransitPolicy.hiddenPathTransit(
                        "A Hidden Path", card,
                        "move using landspeed to jabiim");

        assertEquals(MoveTransitPolicy.HiddenPathBranch.MAPUZO_EXIT,
                result.branch());
        assertTrue(result.contribution().applies());
        assertRawFloat(300.0f, result.contribution().delta());
        assertEquals(
                "V53b HIDDEN PATH: Leaving Mapuzo via landspeed — objective progress!",
                result.contribution().reason());
        assertFalse(result.claimMandatoryTransit());
        assertEquals("V53b MAPUZO EXIT", result.claimIdentity());
        assertFalse(result.hardVeto());
    }

    @Test
    public void hiddenPathPreservesSourceBranchPrecedence() {
        MoveTransitPolicy.HiddenPathTransit safehouse =
                MoveTransitPolicy.hiddenPathTransit(
                        "A Hidden Path",
                        mover("Survivor", "Mapuzo: Safehouse"), "move");
        MoveTransitPolicy.HiddenPathTransit corridor =
                MoveTransitPolicy.hiddenPathTransit(
                        "A Hidden Path",
                        mover("Survivor", "Mapuzo: Underground Corridor"),
                        "move");

        assertEquals(MoveTransitPolicy.HiddenPathBranch.SAFEHOUSE_TO_CORRIDOR,
                safehouse.branch());
        assertEquals(
                MoveTransitPolicy.HiddenPathBranch.CORRIDOR_LANDSPEED_BLOCK,
                corridor.branch());
    }

    @Test
    public void hiddenPathNonLandspeedAndNullSourceRemainNeutral() {
        MoveTransitPolicy.HiddenPathTransit nonLandspeed =
                MoveTransitPolicy.hiddenPathTransit(
                        "A Hidden Path",
                        mover("Survivor", "Mapuzo: Safehouse"),
                        "move jedi survivor here to a site");
        PhysicalCard noSource = mock(PhysicalCard.class);
        when(noSource.getAtLocation()).thenReturn(null);
        when(noSource.getTitle()).thenReturn(null);
        MoveTransitPolicy.HiddenPathTransit nullSource =
                MoveTransitPolicy.hiddenPathTransit(
                        "A Hidden Path", noSource, "move");

        assertEquals(MoveTransitPolicy.HiddenPathBranch.NONE,
                nonLandspeed.branch());
        assertEquals(MoveTransitPolicy.HiddenPathBranch.NONE,
                nullSource.branch());
        assertEquals("character", nullSource.characterName());
    }

    @Test
    public void hiddenPathRequiresObjectiveBeforeReadingCard() {
        PhysicalCard card = mock(PhysicalCard.class);

        MoveTransitPolicy.HiddenPathTransit nullObjective =
                MoveTransitPolicy.hiddenPathTransit(
                        null, card, "move");
        MoveTransitPolicy.HiddenPathTransit otherObjective =
                MoveTransitPolicy.hiddenPathTransit(
                        "Hunt Down And Destroy The Jedi", card, "move");
        MoveTransitPolicy.HiddenPathTransit nullCard =
                MoveTransitPolicy.hiddenPathTransit(
                        "A Hidden Path", null, "move");

        assertEquals(MoveTransitPolicy.HiddenPathBranch.NONE,
                nullObjective.branch());
        assertEquals(MoveTransitPolicy.HiddenPathBranch.NONE,
                otherObjective.branch());
        assertEquals(MoveTransitPolicy.HiddenPathBranch.NONE,
                nullCard.branch());
        verify(card, never()).getAtLocation();
        verify(card, never()).getTitle();
    }

    private static PhysicalCard location(String title) {
        PhysicalCard location = mock(PhysicalCard.class);
        when(location.getTitle()).thenReturn(title);
        return location;
    }

    private static PhysicalCard mover(String title, String sourceTitle) {
        PhysicalCard card = mock(PhysicalCard.class);
        PhysicalCard source = location(sourceTitle);
        when(card.getTitle()).thenReturn(title);
        when(card.getAtLocation()).thenReturn(source);
        return card;
    }

    private static PhysicalCard powerCard(String owner, Float power) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getOwner()).thenReturn(owner);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.hasPowerAttribute()).thenReturn(true);
        when(blueprint.getPower()).thenReturn(power);
        return card;
    }

    private static GameState gameState(
            List<PhysicalCard> locations,
            List<List<PhysicalCard>> cardsByLocation) {
        GameState gameState = mock(GameState.class);
        when(gameState.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getLocationsInOrder()).thenReturn(locations);
        for (int index = 0; index < locations.size(); index++) {
            when(gameState.getCardsAtLocation(locations.get(index)))
                    .thenReturn(cardsByLocation.get(index));
        }
        return gameState;
    }

    private static void assertRawFloat(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(actual));
    }

    private static void assertEmbark(
            MoveTransitPolicy.EmbarkEvaluation evaluation,
            MoveTransitPolicy.EmbarkBranch branch,
            String reason, float delta) {
        assertEquals(branch, evaluation.branch());
        assertTrue(evaluation.contribution().applies());
        assertEquals(reason, evaluation.contribution().reason());
        assertRawFloat(delta, evaluation.contribution().delta());
    }
}
