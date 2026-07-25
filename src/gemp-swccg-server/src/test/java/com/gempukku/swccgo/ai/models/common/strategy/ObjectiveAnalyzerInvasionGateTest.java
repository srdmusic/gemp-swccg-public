package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Zone;
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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ObjectiveAnalyzerInvasionGateTest {
    private static final String PLAYER_ID = "player";

    @Test
    public void invasionProfileHydratesTheExactActorGateForBothFacades() {
        for (ObjectiveAnalyzer analyzer : List.of(
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer())) {
            Fixture fixture = fixture(analyzer, false, false);

            assertEquals("naboo: theed palace throne room",
                    analyzer.getFlipCriticalControlSite());
            assertEquals(1600.0f,
                    analyzer.getActivePlaybook().weights.deployFlipGateSite, 0.0f);
            assertTrue(analyzer.hasFlipGateActorRequirement());
            assertTrue(analyzer.isActiveFlipGateLocationTitle(
                    "Naboo: Theed Palace Throne Room"));
            assertFalse(analyzer.isActiveFlipGateLocationTitle(
                    "Naboo: Theed Palace Generator"));
            assertEquals("Neimoidian at naboo: theed palace throne room",
                    analyzer.getFlipGateActorRequirementLabel());
            assertTrue(analyzer.matchesFlipGateActorRequirement(
                    fixture.game, PLAYER_ID, fixture.candidate));
            assertTrue(analyzer.isFlipGateLocation(
                    fixture.game, PLAYER_ID, fixture.throneRoom));
            assertTrue(analyzer.matchesFlipGateActorRequirement(
                    fixture.game, PLAYER_ID, fixture.candidate, fixture.throneRoom));
            assertEquals(0, analyzer.countFlipGateActorsAtLocation(
                    fixture.game, PLAYER_ID, fixture.throneRoom));
            assertFalse(analyzer.hasFlipGateActorAtLocation(
                    fixture.game, PLAYER_ID, fixture.throneRoom));
            assertTrue(analyzer.advancesUnfilledFlipGateActorRequirement(
                    fixture.game, PLAYER_ID, fixture.candidate, fixture.throneRoom));
        }
    }

    @Test
    public void actorGateClosesWhenANeimoidianIsAlreadyAtTheThroneRoom() {
        ObjectiveAnalyzer analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        Fixture fixture = fixture(analyzer, false, true);
        PhysicalCard wrongLocation = mock(PhysicalCard.class);
        when(wrongLocation.getTitles()).thenReturn(List.of("Naboo: Swamp"));

        assertTrue(analyzer.hasFlipGateActorAtLocation(
                fixture.game, PLAYER_ID, fixture.throneRoom));
        assertEquals(1, analyzer.countFlipGateActorsAtLocation(
                fixture.game, PLAYER_ID, fixture.throneRoom));
        assertFalse(analyzer.hasFlipGateActorAtLocation(
                fixture.game, PLAYER_ID, wrongLocation));
        assertFalse(analyzer.advancesUnfilledFlipGateActorRequirement(
                fixture.game, PLAYER_ID, fixture.candidate, fixture.throneRoom));
    }

    @Test
    public void actorGateFailsClosedForWrongActorWrongDestinationAndPostFlip() {
        ObjectiveAnalyzer analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        Fixture fixture = fixture(analyzer, false, false);
        PhysicalCard wrongActor = mock(PhysicalCard.class);
        PhysicalCard wrongDestination = mock(PhysicalCard.class);
        when(wrongDestination.getTitles()).thenReturn(List.of("Naboo: Swamp"));

        assertFalse(analyzer.advancesUnfilledFlipGateActorRequirement(
                fixture.game, PLAYER_ID, wrongActor, fixture.throneRoom));
        assertFalse(analyzer.matchesFlipGateActorRequirement(
                fixture.game, PLAYER_ID, wrongActor));
        assertFalse(analyzer.matchesFlipGateActorRequirement(
                fixture.game, PLAYER_ID, wrongActor, fixture.throneRoom));
        assertFalse(analyzer.isFlipGateLocation(
                fixture.game, PLAYER_ID, wrongDestination));
        assertFalse(analyzer.advancesUnfilledFlipGateActorRequirement(
                fixture.game, PLAYER_ID, fixture.candidate, wrongDestination));

        ObjectiveAnalyzer flipped =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        Fixture flippedFixture = fixture(flipped, true, false);
        assertFalse(flipped.advancesUnfilledFlipGateActorRequirement(
                flippedFixture.game, PLAYER_ID,
                flippedFixture.candidate, flippedFixture.throneRoom));
        assertFalse(flipped.isActiveFlipGateLocationTitle(
                "Naboo: Theed Palace Throne Room"));
    }

    @Test
    public void structuredFrontRuleRequiresBothExactControlHalves() {
        ObjectiveAnalyzer emptyAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        Fixture empty = fixture(
                emptyAnalyzer, false, false,
                false, false, false, false);
        ObjectiveAnalyzer.FlipLocationRuleState emptyState =
                onlyState(emptyAnalyzer, empty, "preFlip", "flip");
        assertFalse(emptyState.conditionSatisfied());
        assertEquals(2, emptyState.missingAlternatives().size());

        ObjectiveAnalyzer noActorAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        Fixture noActor = fixture(
                noActorAnalyzer, false, false,
                true, true, false, false);
        ObjectiveAnalyzer.FlipLocationRuleState noActorState =
                onlyState(noActorAnalyzer, noActor, "preFlip", "flip");
        assertFalse(noActorState.conditionSatisfied());
        assertEquals(1, noActorState.satisfiedAlternatives().size());
        assertTrue(noActorAnalyzer.isMissingPreFlipRequirementAt(
                noActor.game, PLAYER_ID, noActor.throneRoom));

        ObjectiveAnalyzer throneOnlyAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        Fixture throneOnly = fixture(
                throneOnlyAnalyzer, false, true,
                true, false, false, false);
        ObjectiveAnalyzer.FlipLocationRuleState throneOnlyState =
                onlyState(throneOnlyAnalyzer, throneOnly, "preFlip", "flip");
        assertFalse(throneOnlyState.conditionSatisfied());
        assertEquals(1, throneOnlyState.satisfiedAlternatives().size());
        assertTrue(throneOnlyAnalyzer.isMissingPreFlipRequirementAt(
                throneOnly.game, PLAYER_ID, throneOnly.nabooSystem));

        ObjectiveAnalyzer completeAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        Fixture complete = fixture(
                completeAnalyzer, false, true,
                true, true, false, false);
        ObjectiveAnalyzer.FlipLocationRuleState completeState =
                onlyState(completeAnalyzer, complete, "preFlip", "flip");
        assertTrue(completeState.conditionSatisfied());
        assertEquals(2, completeState.satisfiedAlternatives().size());
        assertTrue(completeState.missingAlternatives().isEmpty());
        assertFalse(completeAnalyzer.isMissingPreFlipRequirementAt(
                complete.game, PLAYER_ID, complete.throneRoom));
        assertFalse(completeAnalyzer.isMissingPreFlipRequirementAt(
                complete.game, PLAYER_ID, complete.nabooSystem));
    }

    @Test
    public void typedRulesMakeOnlyTheExactFrontAndBackLocationsRelevant() {
        ObjectiveAnalyzer front =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        Fixture frontFixture = fixture(front, false, false);

        assertTrue(front.isObjectiveRelevantLocation("Naboo"));
        assertTrue(front.isObjectiveRelevantLocation(
                "Naboo: Theed Palace Throne Room"));
        assertFalse(front.isObjectiveRelevantLocation("Naboo: Swamp"));
        assertFalse(front.isObjectiveRelevantLocation(
                "Naboo: Theed Palace Generator"));
        assertFalse(front.isObjectiveRelevantLocation(
                "Death Star II: Throne Room"));
        assertTrue(front.isPreFlipFlipRequirementLocation(
                frontFixture.game, PLAYER_ID, frontFixture.nabooSystem));
        assertTrue(front.isPreFlipFlipRequirementLocation(
                frontFixture.game, PLAYER_ID, frontFixture.throneRoom));
        assertFalse(front.isPreFlipFlipRequirementLocation(
                frontFixture.game, PLAYER_ID, frontFixture.generator));
        assertTrue(front.isPreFlipPlainControlRequirementLocation(
                frontFixture.game, PLAYER_ID, frontFixture.nabooSystem));
        assertFalse(front.isPreFlipPlainControlRequirementLocation(
                frontFixture.game, PLAYER_ID, frontFixture.throneRoom));

        ObjectiveAnalyzer back =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer();
        Fixture backFixture = fixture(back, true, false);

        assertTrue(back.isFlipBackProtectionLocation("Naboo"));
        assertTrue(back.isFlipBackProtectionLocation(
                "Naboo: Theed Palace Throne Room"));
        assertFalse(back.isFlipBackProtectionLocation("Naboo: Swamp"));
        assertFalse(back.isFlipBackProtectionLocation(
                "Naboo: Theed Palace Generator"));
        assertFalse(back.isFlipBackProtectionLocation(
                "Death Star II: Throne Room"));
        assertTrue(back.hasStructuredFlipBackLocationRules());
        assertTrue(back.isFlipBackProtectionLocation(
                backFixture.nabooSystem, backFixture.game, PLAYER_ID));
        assertTrue(back.isFlipBackProtectionLocation(
                backFixture.throneRoom, backFixture.game, PLAYER_ID));
        assertFalse(back.isFlipBackProtectionLocation(
                backFixture.generator, backFixture.game, PLAYER_ID));
    }

    @Test
    public void structuredBackRuleUsesEitherExactOpponentControlBranch() {
        ObjectiveAnalyzer safeAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        Fixture safe = fixture(
                safeAnalyzer, true, true,
                true, true, false, false);
        assertFalse(onlyState(
                safeAnalyzer, safe, "postFlip", "flipBack")
                .conditionSatisfied());

        ObjectiveAnalyzer systemLostAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        Fixture systemLost = fixture(
                systemLostAnalyzer, true, true,
                true, false, true, false);
        ObjectiveAnalyzer.FlipLocationRuleState systemLostState =
                onlyState(systemLostAnalyzer, systemLost, "postFlip", "flipBack");
        assertTrue(systemLostState.conditionSatisfied());
        assertEquals(1, systemLostState.satisfiedAlternatives().size());

        ObjectiveAnalyzer throneLostAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer();
        Fixture throneLost = fixture(
                throneLostAnalyzer, true, true,
                true, false, false, true);
        ObjectiveAnalyzer.FlipLocationRuleState throneLostState =
                onlyState(throneLostAnalyzer, throneLost, "postFlip", "flipBack");
        assertTrue(throneLostState.conditionSatisfied());
        assertEquals(1, throneLostState.satisfiedAlternatives().size());
    }

    @Test
    public void freeSidiousUploadExposesTheFundedMissingActorPath() {
        ObjectiveAnalyzer analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        Fixture fixture = fixture(analyzer, false, false);
        PhysicalCard sidious = mock(PhysicalCard.class);
        SwccgCardBlueprint sidiousBlueprint = mock(SwccgCardBlueprint.class);
        when(sidious.getBlueprint()).thenReturn(sidiousBlueprint);
        when(sidiousBlueprint.getGameText()).thenReturn(
                "Once per game, may [upload] Always Two There Are or a Neimoidian.");
        when(fixture.game.getGameState().getReserveDeck(PLAYER_ID))
                .thenReturn(List.of(fixture.candidate));

        assertEquals(Integer.valueOf(3),
                analyzer.getFlipGateActorEnablerFutureDeployCost(
                        fixture.game, PLAYER_ID, sidious));
        assertTrue(analyzer.isFlipGateActorUploadIntoHandAction(
                fixture.game, PLAYER_ID, sidious,
                "Take card into hand from Reserve Deck"));
        assertFalse(analyzer.isFlipGateActorUploadIntoHandAction(
                fixture.game, PLAYER_ID, sidious,
                "Deploy"));

        when(sidiousBlueprint.getGameText()).thenReturn(
                "Use 1 Force to [upload] a Neimoidian.");
        assertNull(analyzer.getFlipGateActorEnablerFutureDeployCost(
                fixture.game, PLAYER_ID, sidious));
    }

    @Test
    public void nabooSystemIdentifiesOnlyTheSoleIndependentControlSource() {
        ObjectiveAnalyzer analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        Fixture fixture = fixture(
                analyzer, false, false,
                false, true, false, false);
        GameState gameState = fixture.game.getGameState();
        ModifiersQuerying modifiers = fixture.game.getModifiersQuerying();
        PhysicalCard flagship = mock(PhysicalCard.class);
        when(flagship.getOwner()).thenReturn(PLAYER_ID);
        when(flagship.isUndercover()).thenReturn(false);
        when(modifiers.hasAbility(gameState, flagship, true)).thenReturn(true);
        when(modifiers.getLocationThatCardIsPresentAt(gameState, flagship))
                .thenReturn(fixture.nabooSystem);
        when(gameState.getAllPermanentCards()).thenReturn(List.of(flagship));

        assertTrue(analyzer.isSoleControlSourceAtRequiredLocation(
                fixture.game, PLAYER_ID, flagship, fixture.nabooSystem));

        PhysicalCard secondShip = mock(PhysicalCard.class);
        when(secondShip.getOwner()).thenReturn(PLAYER_ID);
        when(secondShip.isUndercover()).thenReturn(false);
        when(modifiers.hasAbility(gameState, secondShip, true)).thenReturn(true);
        when(modifiers.getLocationThatCardIsPresentAt(gameState, secondShip))
                .thenReturn(fixture.nabooSystem);
        when(gameState.getAllPermanentCards())
                .thenReturn(List.of(flagship, secondShip));

        assertFalse(analyzer.isSoleControlSourceAtRequiredLocation(
                fixture.game, PLAYER_ID, flagship, fixture.nabooSystem));
        assertFalse(analyzer.isSoleControlSourceAtRequiredLocation(
                fixture.game, PLAYER_ID, flagship, fixture.throneRoom));
    }

    private static ObjectiveAnalyzer.FlipLocationRuleState onlyState(
            ObjectiveAnalyzer analyzer, Fixture fixture,
            String phase, String purpose) {
        List<ObjectiveAnalyzer.FlipLocationRuleState> states =
                analyzer.assessFlipLocationRules(
                        fixture.game, PLAYER_ID, phase, purpose);
        assertEquals(1, states.size());
        return states.get(0);
    }

    private static Fixture fixture(
            ObjectiveAnalyzer analyzer, boolean flipped, boolean actorAlreadyThere) {
        return fixture(analyzer, flipped, actorAlreadyThere,
                false, false, false, false);
    }

    private static Fixture fixture(
            ObjectiveAnalyzer analyzer,
            boolean flipped,
            boolean actorAlreadyThere,
            boolean controlsThrone,
            boolean controlsSystem,
            boolean opponentControlsSystem,
            boolean opponentControlsThrone) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard objective = mock(PhysicalCard.class);
        PhysicalCard candidate = mock(PhysicalCard.class);
        PhysicalCard existing = mock(PhysicalCard.class);
        PhysicalCard throneRoom = mock(PhysicalCard.class);
        PhysicalCard nabooSystem = mock(PhysicalCard.class);
        PhysicalCard generator = mock(PhysicalCard.class);
        SwccgCardBlueprint front = mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint back = mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint candidateBlueprint = mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint throneBlueprint = mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint systemBlueprint = mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint generatorBlueprint = mock(SwccgCardBlueprint.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getOpponent(PLAYER_ID)).thenReturn("opponent");
        when(objective.getOwner()).thenReturn(PLAYER_ID);
        when(objective.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(objective.getBlueprint()).thenReturn(front);
        when(objective.getOtherSideBlueprint()).thenReturn(back);
        when(objective.getBlueprintId(true)).thenReturn("14_113");
        when(objective.isFlipped()).thenReturn(flipped);
        when(front.getTitle()).thenReturn("Invasion");
        when(front.getGameText()).thenReturn(
                "Deploy Naboo system. Flip this card if you control Theed Palace Throne Room "
                        + "(with a Neimoidian there) and Naboo system.");
        when(front.getCardCategory()).thenReturn(CardCategory.OBJECTIVE);
        when(back.getTitle()).thenReturn("In Complete Control");
        when(back.getGameText()).thenReturn(
                "Flip this card if opponent controls Naboo system or Theed Palace Throne Room.");

        when(candidate.getOwner()).thenReturn(PLAYER_ID);
        when(candidate.getBlueprint()).thenReturn(candidateBlueprint);
        when(candidateBlueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        when(candidateBlueprint.getDeployCost()).thenReturn(3.0f);
        when(existing.getOwner()).thenReturn(PLAYER_ID);
        when(existing.getZone()).thenReturn(Zone.AT_LOCATION);
        when(existing.isUndercover()).thenReturn(false);
        setActive(gameState, existing, true);
        when(throneRoom.getBlueprint()).thenReturn(throneBlueprint);
        when(throneRoom.getTitle()).thenReturn("Naboo: Theed Palace Throne Room");
        when(throneRoom.getTitles()).thenReturn(List.of("Naboo: Theed Palace Throne Room"));
        when(throneRoom.isBlownAway()).thenReturn(false);
        when(throneBlueprint.getCardCategory()).thenReturn(CardCategory.LOCATION);
        when(throneBlueprint.getCardSubtype()).thenReturn(CardSubtype.SITE);
        when(nabooSystem.getBlueprint()).thenReturn(systemBlueprint);
        when(nabooSystem.getTitle()).thenReturn("Naboo");
        when(nabooSystem.getTitles()).thenReturn(List.of("Naboo"));
        when(nabooSystem.isBlownAway()).thenReturn(false);
        when(systemBlueprint.getCardCategory()).thenReturn(CardCategory.LOCATION);
        when(systemBlueprint.getCardSubtype()).thenReturn(CardSubtype.SYSTEM);
        when(generator.getBlueprint()).thenReturn(generatorBlueprint);
        when(generator.getTitle()).thenReturn("Naboo: Theed Palace Generator");
        when(generator.getTitles()).thenReturn(
                List.of("Naboo: Theed Palace Generator"));
        when(generator.isBlownAway()).thenReturn(false);
        when(generatorBlueprint.getCardCategory()).thenReturn(CardCategory.LOCATION);
        when(generatorBlueprint.getCardSubtype()).thenReturn(CardSubtype.SITE);
        when(modifiers.isSpecies(gameState, candidate, Species.NEIMOIDIAN)).thenReturn(true);
        when(modifiers.isSpecies(gameState, existing, Species.NEIMOIDIAN)).thenReturn(true);
        when(modifiers.getLocationThatCardIsPresentAt(gameState, existing))
                .thenReturn(throneRoom);
        when(modifiers.controlsLocation(
                gameState, throneRoom, PLAYER_ID,
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(controlsThrone);
        when(modifiers.controlsLocation(
                gameState, nabooSystem, PLAYER_ID,
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(controlsSystem);
        when(modifiers.controlsLocation(
                gameState, nabooSystem, "opponent",
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(opponentControlsSystem);
        when(modifiers.controlsLocation(
                gameState, throneRoom, "opponent",
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(opponentControlsThrone);
        when(gameState.getLocationsInOrder()).thenReturn(
                List.of(nabooSystem, throneRoom, generator));
        when(gameState.getTopLocations()).thenReturn(
                List.of(nabooSystem, throneRoom, generator));
        when(gameState.getCardsAtLocation(throneRoom)).thenReturn(
                actorAlreadyThere ? List.of(existing) : List.of());
        when(gameState.getCardsAtLocation(nabooSystem)).thenReturn(List.of());
        when(gameState.getCardsAtLocation(generator)).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(actorAlreadyThere
                ? List.of(objective, existing) : List.of(objective));

        analyzer.analyze(game, PLAYER_ID, Side.DARK);
        return new Fixture(
                game, candidate, throneRoom, nabooSystem, generator);
    }

    private static void setActive(
            GameState gameState, PhysicalCard card, boolean active) {
        when(gameState.isCardInPlayActive(
                card, false, false, false, false,
                false, false, false, false)).thenReturn(active);
        when(gameState.isCardInPlayActive(
                card, true, false, false, false,
                false, false, false, false)).thenReturn(active);
    }

    private record Fixture(
            SwccgGame game,
            PhysicalCard candidate,
            PhysicalCard throneRoom,
            PhysicalCard nabooSystem,
            PhysicalCard generator) { }
}
