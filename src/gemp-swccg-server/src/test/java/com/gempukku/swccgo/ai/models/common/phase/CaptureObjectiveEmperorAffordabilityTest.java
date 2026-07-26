package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.filters.Filters;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CaptureObjectiveEmperorAffordabilityTest {
    private static final String PLAYER = "player";

    @Test
    public void engineDeployabilityOwnsMinusTwoAndLiveAffordability() {
        Fixture fixture = fixture();
        int[] force = {0};
        boolean[] liveDeployable = {true};
        when(fixture.modifiers.getForceAvailableToUse(
                fixture.gameState, PLAYER))
                .thenAnswer(invocation -> force[0]);
        when(fixture.modifiers.isDeployable(
                eq(fixture.gameState),
                eq(fixture.objective),
                eq(fixture.emperor),
                eq(false), any(),
                eq(false), anyFloat(),
                any(), any(), any(), any(), any(),
                anyBoolean(), anyFloat()))
                .thenAnswer(invocation -> {
                    float objectiveAdjustment =
                            invocation.getArgument(
                                6, Float.class);
                    return liveDeployable[0]
                            && objectiveAdjustment == -2.0f
                            && fixture.modifiers
                                .getForceAvailableToUse(
                                    fixture.gameState, PLAYER)
                                >= 3;
                });

        assertFalse("Zero Force cannot receive the payoff or bypass",
                CaptureObjectiveFacts
                    .canAffordBhbmEmperorDownload(
                        fixture.game, PLAYER,
                        fixture.analyzer, fixture.objective));

        force[0] = 2;
        assertFalse("Two Force cannot pay the source-defined cost three",
                CaptureObjectiveFacts
                    .canAffordBhbmEmperorDownload(
                        fixture.game, PLAYER,
                        fixture.analyzer, fixture.objective));

        force[0] = 3;
        assertTrue("The back-side 9_109 deploy 5 minus 2 costs three",
                CaptureObjectiveFacts
                    .canAffordBhbmEmperorDownload(
                        fixture.game, PLAYER,
                        fixture.analyzer, fixture.objective));

        when(fixture.analyzer.isFlipped()).thenReturn(false);
        assertTrue("With no capture reserve, the front-side action costs three",
                CaptureObjectiveFacts
                    .canAffordBhbmEmperorDownload(
                        fixture.game, PLAYER,
                        fixture.analyzer, fixture.objective));

        force[0] = 10;
        liveDeployable[0] = false;
        assertFalse("Live engine restrictions dominate a printed estimate",
                CaptureObjectiveFacts
                    .canAffordBhbmEmperorDownload(
                        fixture.game, PLAYER,
                        fixture.analyzer, fixture.objective));
    }

    @Test
    public void bothSidesRequireExactOwnedBhbmSourceAndReserveCandidate() {
        Fixture fixture = fixture();
        when(fixture.modifiers.isDeployable(
                eq(fixture.gameState),
                eq(fixture.objective),
                eq(fixture.emperor),
                eq(false), any(),
                eq(false), eq(-2.0f),
                any(), any(), any(), any(), any(),
                anyBoolean(), anyFloat()))
                .thenReturn(true);

        when(fixture.analyzer.isFlipped()).thenReturn(false);
        assertTrue(CaptureObjectiveFacts
                .canAffordBhbmEmperorDownload(
                    fixture.game, PLAYER,
                    fixture.analyzer, fixture.objective));

        when(fixture.analyzer.isFlipped()).thenReturn(true);
        assertTrue(CaptureObjectiveFacts
                .canAffordBhbmEmperorDownload(
                    fixture.game, PLAYER,
                    fixture.analyzer, fixture.objective));

        when(fixture.objective.getOwner()).thenReturn("opponent");
        assertFalse(CaptureObjectiveFacts
                .canAffordBhbmEmperorDownload(
                    fixture.game, PLAYER,
                    fixture.analyzer, fixture.objective));

        when(fixture.objective.getOwner()).thenReturn(PLAYER);
        when(fixture.objective.getBlueprintId(true))
                .thenReturn("9_61");
        assertFalse(CaptureObjectiveFacts
                .canAffordBhbmEmperorDownload(
                    fixture.game, PLAYER,
                    fixture.analyzer, fixture.objective));

        when(fixture.objective.getBlueprintId(true))
                .thenReturn("9_151");
        when(fixture.gameState.getReserveDeck(PLAYER))
                .thenReturn(List.of());
        assertFalse(CaptureObjectiveFacts
                .canAffordBhbmEmperorDownload(
                    fixture.game, PLAYER,
                    fixture.analyzer, fixture.objective));
    }

    @Test
    public void frontSideLeavesTheNextCaptureMoveReserveIntact() {
        Fixture fixture = fixture();
        when(fixture.analyzer.isFlipped()).thenReturn(false);

        int[] force = {3};
        when(fixture.modifiers.getForceAvailableToUse(
                fixture.gameState, PLAYER))
                .thenAnswer(invocation -> force[0]);
        when(fixture.modifiers.isDeployable(
                eq(fixture.gameState),
                eq(fixture.objective),
                eq(fixture.emperor),
                eq(false), any(),
                eq(false), anyFloat(),
                any(), any(), any(), any(), any(),
                anyBoolean(), anyFloat()))
                .thenAnswer(invocation ->
                    invocation.getArgument(6, Float.class) == -2.0f
                    && force[0] >= 3);

        SwccgCardBlueprint objectiveBlueprint =
                mock(SwccgCardBlueprint.class);
        when(fixture.objective.getBlueprint())
                .thenReturn(objectiveBlueprint);
        when(objectiveBlueprint.getCardCategory())
                .thenReturn(CardCategory.OBJECTIVE);

        PhysicalCard luke = mock(PhysicalCard.class);
        PhysicalCard vader = mock(PhysicalCard.class);
        SwccgCardBlueprint lukeBlueprint =
                mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint vaderBlueprint =
                mock(SwccgCardBlueprint.class);
        PhysicalCard origin = site(
                "Tatooine: Cantina", 21);
        PhysicalCard captureSite = site(
                "Tatooine: Mos Eisley", 22);

        when(luke.getOwner()).thenReturn("opponent");
        when(luke.getPermanentCardId()).thenReturn(31);
        when(luke.getBlueprint()).thenReturn(lukeBlueprint);
        when(lukeBlueprint.getCardCategory())
                .thenReturn(CardCategory.CHARACTER);
        when(luke.getCardsEscorting()).thenReturn(List.of());
        when(vader.getOwner()).thenReturn(PLAYER);
        when(vader.getPermanentCardId()).thenReturn(32);
        when(vader.getBlueprint()).thenReturn(vaderBlueprint);
        when(vaderBlueprint.getCardCategory())
                .thenReturn(CardCategory.CHARACTER);
        when(vader.getAtLocation()).thenReturn(origin);
        when(vader.getCardsEscorting()).thenReturn(List.of());

        when(fixture.gameState.getAllPermanentCards())
                .thenReturn(List.of(
                    fixture.objective, luke,
                    vader, fixture.emperor));
        when(fixture.gameState.getLocationsInOrder())
                .thenReturn(List.of(origin, captureSite));
        when(fixture.gameState.findCardByPermanentId(32))
                .thenReturn(vader);
        when(fixture.gameState.isCardInPlayActive(
                any(PhysicalCard.class),
                anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean()))
                .thenAnswer(invocation ->
                    invocation.getArgument(0) == luke
                    || invocation.getArgument(0) == vader);

        when(fixture.modifiers.hasPersona(
                fixture.gameState, luke,
                Persona.LUKE)).thenReturn(true);
        when(fixture.modifiers.hasPersona(
                fixture.gameState, vader,
                Persona.VADER)).thenReturn(true);
        when(fixture.modifiers.getLocationThatCardIsPresentAt(
                fixture.gameState, luke))
                .thenReturn(captureSite);
        when(fixture.modifiers.getCardIsPresentAt(
                fixture.gameState, luke))
                .thenReturn(captureSite);
        when(fixture.modifiers.getLocationThatCardIsPresentAt(
                fixture.gameState, vader))
                .thenReturn(origin);
        when(fixture.modifiers.getLocationHere(
                fixture.gameState, vader))
                .thenReturn(origin);
        when(fixture.modifiers.getLocationHere(
                fixture.gameState, origin))
                .thenReturn(origin);
        when(fixture.modifiers.getLocationHere(
                fixture.gameState, captureSite))
                .thenReturn(captureSite);
        when(fixture.modifiers.getLandspeedRequired(
                fixture.gameState, vader,
                captureSite)).thenReturn(1);
        when(fixture.modifiers.getLandspeed(
                fixture.gameState, vader))
                .thenReturn(1.0f);
        when(fixture.modifiers.getMoveUsingLandspeedCost(
                fixture.gameState, vader,
                origin, captureSite,
                false, 0.0f)).thenReturn(1.0f);
        when(fixture.modifiers.canBeTargetedBy(
                any(GameState.class),
                any(PhysicalCard.class),
                any(PhysicalCard.class),
                anySet()))
                .thenReturn(true);
        when(fixture.modifiers.getDeployCost(
                same(fixture.gameState),
                same(fixture.objective),
                same(fixture.emperor),
                isNull(), eq(false), isNull(),
                eq(false), eq(-2.0f),
                isNull(), eq(true)))
                .thenReturn(3.0f);

        assertTrue("The engine owns the legal one-Force move",
                Filters.canMoveToUsingLandspeed(
                        PLAYER, vader,
                        false, false, false,
                        0.0f, null)
                    .accepts(
                        fixture.gameState,
                        fixture.modifiers,
                        captureSite));
        assertTrue("Vader arriving here triggers the exact capture",
                CaptureObjectiveFacts.guaranteesImmediateCaptureAt(
                        fixture.game, PLAYER,
                        fixture.analyzer,
                        vader, captureSite));
        assertEquals("The exact next Vader move reserves one Force",
                1, CaptureObjectiveFacts
                        .nextCaptureMoveForceReserve(
                            fixture.game, PLAYER,
                            fixture.analyzer,
                            fixture.emperor));
        assertFalse("F3 - C3 = 0, below the R1 capture reserve",
                CaptureObjectiveFacts
                    .canAffordBhbmEmperorDownload(
                        fixture.game, PLAYER,
                        fixture.analyzer, fixture.objective));

        force[0] = 4;
        assertTrue("F4 - C3 = 1, satisfying the R1 capture reserve",
                CaptureObjectiveFacts
                    .canAffordBhbmEmperorDownload(
                        fixture.game, PLAYER,
                        fixture.analyzer, fixture.objective));
    }

    private static Fixture fixture() {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers =
                mock(ModifiersQuerying.class);
        ObjectiveAnalyzer analyzer =
                mock(ObjectiveAnalyzer.class);
        PhysicalCard objective = mock(PhysicalCard.class);
        PhysicalCard emperor = mock(PhysicalCard.class);
        SwccgCardBlueprint emperorBlueprint =
                mock(SwccgCardBlueprint.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.isFlipped()).thenReturn(true);
        when(analyzer.getObjectiveBlueprintId())
                .thenReturn("9_151");

        when(objective.getOwner()).thenReturn(PLAYER);
        when(objective.getBlueprintId(true))
                .thenReturn("9_151");
        when(objective.getPermanentCardId()).thenReturn(11);
        when(gameState.findCardByPermanentId(11))
                .thenReturn(objective);

        when(emperor.getOwner()).thenReturn(PLAYER);
        when(emperor.getBlueprintId(true))
                .thenReturn("9_109");
        when(emperor.getTitles())
                .thenReturn(List.of("Emperor Palpatine"));
        when(emperor.getBlueprint())
                .thenReturn(emperorBlueprint);
        when(emperorBlueprint.getDeployCost())
                .thenReturn(5.0f);
        when(modifiers.hasPersona(
                gameState, emperor, Persona.SIDIOUS))
                .thenReturn(true);
        when(gameState.getReserveDeck(PLAYER))
                .thenReturn(List.of(emperor));

        return new Fixture(
                game, gameState, modifiers,
                analyzer, objective, emperor);
    }

    private static PhysicalCard site(
            String title, int permanentCardId) {
        PhysicalCard site = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint =
                mock(SwccgCardBlueprint.class);
        when(site.getTitle()).thenReturn(title);
        when(site.getTitles()).thenReturn(List.of(title));
        when(site.getPermanentCardId())
                .thenReturn(permanentCardId);
        when(site.getCardId()).thenReturn(permanentCardId);
        when(site.getAdditionalCardIds()).thenReturn(List.of());
        when(site.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory())
                .thenReturn(CardCategory.LOCATION);
        when(blueprint.getCardSubtype())
                .thenReturn(CardSubtype.SITE);
        return site;
    }

    private record Fixture(
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            ObjectiveAnalyzer analyzer,
            PhysicalCard objective,
            PhysicalCard emperor) {
    }
}
