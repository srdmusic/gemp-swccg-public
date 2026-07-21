package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
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
            assertEquals("Neimoidian at naboo: theed palace throne room",
                    analyzer.getFlipGateActorRequirementLabel());
            assertTrue(analyzer.matchesFlipGateActorRequirement(
                    fixture.game, PLAYER_ID, fixture.candidate, fixture.throneRoom));
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
                fixture.game, PLAYER_ID, wrongActor, fixture.throneRoom));
        assertFalse(analyzer.advancesUnfilledFlipGateActorRequirement(
                fixture.game, PLAYER_ID, fixture.candidate, wrongDestination));

        ObjectiveAnalyzer flipped =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        Fixture flippedFixture = fixture(flipped, true, false);
        assertFalse(flipped.advancesUnfilledFlipGateActorRequirement(
                flippedFixture.game, PLAYER_ID,
                flippedFixture.candidate, flippedFixture.throneRoom));
    }

    private static Fixture fixture(
            ObjectiveAnalyzer analyzer, boolean flipped, boolean actorAlreadyThere) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard objective = mock(PhysicalCard.class);
        PhysicalCard candidate = mock(PhysicalCard.class);
        PhysicalCard existing = mock(PhysicalCard.class);
        PhysicalCard throneRoom = mock(PhysicalCard.class);
        SwccgCardBlueprint front = mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint back = mock(SwccgCardBlueprint.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
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
        when(existing.getOwner()).thenReturn(PLAYER_ID);
        when(throneRoom.getTitle()).thenReturn("Naboo: Theed Palace Throne Room");
        when(throneRoom.getTitles()).thenReturn(List.of("Naboo: Theed Palace Throne Room"));
        when(throneRoom.isBlownAway()).thenReturn(false);
        when(modifiers.isSpecies(gameState, candidate, Species.NEIMOIDIAN)).thenReturn(true);
        when(modifiers.isSpecies(gameState, existing, Species.NEIMOIDIAN)).thenReturn(true);
        when(modifiers.getLocationThatCardIsPresentAt(gameState, existing))
                .thenReturn(throneRoom);
        when(gameState.getAllPermanentCards()).thenReturn(actorAlreadyThere
                ? List.of(objective, existing) : List.of(objective));

        analyzer.analyze(game, PLAYER_ID, Side.DARK);
        return new Fixture(game, candidate, throneRoom);
    }

    private record Fixture(
            SwccgGame game, PhysicalCard candidate, PhysicalCard throneRoom) { }
}
