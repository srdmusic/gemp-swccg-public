package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CaptureObjectiveRouteFactsTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";

    @Test
    public void bhbmUsesOnlyLegalStrictlyCloserVaderHops() {
        Fixture fixture = fixture(
                CaptureObjectivePolicy.ObjectiveKind.BHBM);

        assertTrue(CaptureObjectiveFacts
                .advancesCaptureApproachByLandspeed(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor, fixture.halfway));
        assertTrue(CaptureObjectiveFacts
                .hasLegalCaptureApproachMoveDestination(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor));

        assertFalse("Staying at the origin is not progress",
                CaptureObjectiveFacts
                    .advancesCaptureApproachByLandspeed(
                        fixture.game, PLAYER, fixture.analyzer,
                        fixture.actor, fixture.origin));
        assertFalse("The final capture site belongs to the R4 route",
                CaptureObjectiveFacts
                    .advancesCaptureApproachByLandspeed(
                        fixture.game, PLAYER, fixture.analyzer,
                        fixture.actor, fixture.captureSite));
        assertTrue(CaptureObjectiveFacts.guaranteesImmediateCaptureAt(
                fixture.game, PLAYER, fixture.analyzer,
                fixture.actor, fixture.captureSite));
        assertFalse(CaptureObjectiveFacts
                .advancesCaptureApproachByLandspeed(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor, fixture.farther));
        assertFalse("A legal site in another system is not closer",
                CaptureObjectiveFacts
                .advancesCaptureApproachByLandspeed(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor, fixture.unrelated));

        when(fixture.modifiers.getLandspeedRequired(
                fixture.gameState, fixture.actor,
                fixture.halfway)).thenReturn(null);
        assertFalse("An offered-looking but illegal hop must fail closed",
                CaptureObjectiveFacts
                    .advancesCaptureApproachByLandspeed(
                        fixture.game, PLAYER, fixture.analyzer,
                        fixture.actor, fixture.halfway));
    }

    @Test
    public void bhbmApproachHonorsEverySourceCaptureRestriction() {
        Fixture fixture = fixture(
                CaptureObjectivePolicy.ObjectiveKind.BHBM);

        PhysicalCard wrongActor = character(
                "Admiral Piett", 52, PLAYER);
        setActive(fixture.gameState, wrongActor);
        when(wrongActor.getAtLocation())
                .thenReturn(fixture.origin);
        fixture.cardsById.put(52, wrongActor);
        assertFalse(CaptureObjectiveFacts
                .advancesCaptureApproachByLandspeed(
                    fixture.game, PLAYER, fixture.analyzer,
                    wrongActor, fixture.halfway));

        when(fixture.actor.isLeavingTable()).thenReturn(true);
        assertFalse(CaptureObjectiveFacts
                .advancesCaptureApproachByLandspeed(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor, fixture.halfway));
        when(fixture.actor.isLeavingTable()).thenReturn(false);

        when(fixture.actor.getCardsEscorting())
                .thenReturn(List.of(fixture.target));
        assertFalse(CaptureObjectiveFacts
                .advancesCaptureApproachByLandspeed(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor, fixture.halfway));
        when(fixture.actor.getCardsEscorting())
                .thenReturn(List.of());

        when(fixture.target.isCaptive()).thenReturn(true);
        assertFalse(CaptureObjectiveFacts
                .advancesCaptureApproachByLandspeed(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor, fixture.halfway));
        when(fixture.target.isCaptive()).thenReturn(false);

        when(fixture.modifiers.hasGameTextModification(
                fixture.gameState, fixture.objective,
                ModifyGameTextType
                    .BRING_HIM_BEFORE_ME__MAY_NOT_CAPTURE_LUKE))
                .thenReturn(true);
        assertFalse(CaptureObjectiveFacts
                .advancesCaptureApproachByLandspeed(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor, fixture.halfway));
        when(fixture.modifiers.hasGameTextModification(
                fixture.gameState, fixture.objective,
                ModifyGameTextType
                    .BRING_HIM_BEFORE_ME__MAY_NOT_CAPTURE_LUKE))
                .thenReturn(false);

        when(fixture.analyzer.isFlipped()).thenReturn(true);
        assertFalse(CaptureObjectiveFacts
                .advancesCaptureApproachByLandspeed(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor, fixture.halfway));
    }

    @Test
    public void bhbmRetargetedLeiaIsNotBlockedByLukeOnlyRestriction() {
        Fixture fixture = fixture(
                CaptureObjectivePolicy.ObjectiveKind.BHBM);
        PhysicalCard leia = character(
                "Princess Leia", 54, OPPONENT);
        setActive(fixture.gameState, leia);
        fixture.cardsById.put(54, leia);
        when(fixture.modifiers.hasPersona(
                fixture.gameState, leia,
                Persona.LEIA)).thenReturn(true);
        when(fixture.modifiers.getLocationThatCardIsPresentAt(
                fixture.gameState, leia))
                .thenReturn(fixture.captureSite);
        when(fixture.modifiers.getCardIsPresentAt(
                fixture.gameState, leia))
                .thenReturn(fixture.captureSite);
        when(fixture.gameState.getAllPermanentCards())
                .thenReturn(List.of(
                    fixture.objective, fixture.target,
                    fixture.actor, fixture.imperial, leia));
        when(fixture.modifiers.hasGameTextModification(
                fixture.gameState, fixture.objective,
                ModifyGameTextType
                    .BRING_HIM_BEFORE_ME__TARGETS_LEIA_INSTEAD_OF_LUKE))
                .thenReturn(true);
        when(fixture.modifiers.hasGameTextModification(
                fixture.gameState, fixture.objective,
                ModifyGameTextType
                    .BRING_HIM_BEFORE_ME__MAY_NOT_CAPTURE_LUKE))
                .thenReturn(true);

        assertTrue(CaptureObjectiveFacts
                .advancesCaptureApproachByLandspeed(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor, fixture.halfway));
        assertTrue(CaptureObjectiveFacts.guaranteesImmediateCaptureAt(
                fixture.game, PLAYER, fixture.analyzer,
                fixture.actor, fixture.captureSite));
    }

    @Test
    public void tigihRoutesOnlyItsExactLukeTowardAnEligibleImperial() {
        Fixture fixture = fixture(
                CaptureObjectivePolicy.ObjectiveKind.TIGIH);

        assertTrue(CaptureObjectiveFacts
                .advancesCaptureApproachByLandspeed(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor, fixture.halfway));
        assertFalse("The final capture site belongs to the R4 route",
                CaptureObjectiveFacts
                    .advancesCaptureApproachByLandspeed(
                        fixture.game, PLAYER, fixture.analyzer,
                        fixture.actor, fixture.captureSite));
        assertTrue(CaptureObjectiveFacts.guaranteesImmediateCaptureAt(
                fixture.game, PLAYER, fixture.analyzer,
                fixture.actor, fixture.captureSite));

        PhysicalCard otherLuke = character(
                "Luke Skywalker", 53, PLAYER);
        setActive(fixture.gameState, otherLuke);
        when(fixture.modifiers.hasPersona(
                fixture.gameState, otherLuke,
                Persona.LUKE)).thenReturn(true);
        when(otherLuke.getAtLocation())
                .thenReturn(fixture.origin);
        fixture.cardsById.put(53, otherLuke);
        assertFalse("A same-persona copy is not the objective target",
                CaptureObjectiveFacts
                    .advancesCaptureApproachByLandspeed(
                        fixture.game, PLAYER, fixture.analyzer,
                        otherLuke, fixture.halfway));

        when(fixture.modifiers.canEscortCaptive(
                fixture.gameState, fixture.imperial,
                fixture.target, true, false, false))
                .thenReturn(false);
        assertFalse("No eligible Imperial means no source-backed route",
                CaptureObjectiveFacts
                    .advancesCaptureApproachByLandspeed(
                        fixture.game, PLAYER, fixture.analyzer,
                        fixture.actor, fixture.halfway));

        when(fixture.modifiers.canEscortCaptive(
                fixture.gameState, fixture.imperial,
                fixture.target, true, false, false))
                .thenReturn(true);
        when(fixture.modifiers.hasPersona(
                fixture.gameState, fixture.target,
                Persona.LUKE)).thenReturn(false);
        assertFalse("The objective target must still be Luke",
                CaptureObjectiveFacts
                    .advancesCaptureApproachByLandspeed(
                        fixture.game, PLAYER, fixture.analyzer,
                        fixture.actor, fixture.halfway));
    }

    @Test
    public void freeVirtualHutCaptureSuppressesPaidApproachAndReserve() {
        Fixture fixture = fixture(
                CaptureObjectivePolicy.ObjectiveKind.TIGIH);
        when(fixture.origin.getTitle())
                .thenReturn("Endor: Chief Chirpa's Hut (V)");
        when(fixture.origin.getTitles())
                .thenReturn(List.of(
                    "Endor: Chief Chirpa's Hut (V)"));
        when(fixture.origin.getBlueprintId(true))
                .thenReturn("214_19");
        when(fixture.captureSite.getTitle())
                .thenReturn("Endor: Landing Platform (Docking Bay)");
        when(fixture.captureSite.getTitles())
                .thenReturn(List.of(
                    "Endor: Landing Platform (Docking Bay)"));
        when(fixture.captureSite.getBlueprintId(true))
                .thenReturn("8_76");
        when(fixture.actor.getBlueprint()
                .getValidMoveTargetFilter(
                    PLAYER, fixture.game,
                    fixture.actor, false))
                .thenReturn(Filters.any);

        assertTrue(CaptureObjectiveFacts
                .virtualHutActionGuaranteesCapture(
                    fixture.game, PLAYER,
                    fixture.analyzer));
        assertFalse(CaptureObjectiveFacts
                .advancesCaptureApproachByLandspeed(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor, fixture.halfway));
        assertEquals(0,
                CaptureObjectiveFacts.nextCaptureMoveForceReserve(
                    fixture.game, PLAYER, fixture.analyzer,
                    null));
    }

    @Test
    public void forceReserveIncludesTheCheapestCloserHop() {
        Fixture fixture = fixture(
                CaptureObjectivePolicy.ObjectiveKind.BHBM);
        when(fixture.modifiers.getMoveUsingLandspeedCost(
                fixture.gameState, fixture.actor,
                fixture.origin, fixture.halfway,
                false, 0.0f)).thenReturn(1.01f);

        assertEquals(2,
                CaptureObjectiveFacts.nextCaptureMoveForceReserve(
                    fixture.game, PLAYER, fixture.analyzer,
                    null));
        assertEquals("Do not reserve Force from the capture actor itself",
                0,
                CaptureObjectiveFacts.nextCaptureMoveForceReserve(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor));
    }

    @Test
    public void forceReserveFallsBackToTheDirectCaptureMove() {
        Fixture fixture = fixture(
                CaptureObjectivePolicy.ObjectiveKind.BHBM);
        when(fixture.modifiers.getLandspeedRequired(
                fixture.gameState, fixture.actor,
                fixture.halfway)).thenReturn(null);

        assertEquals(4,
                CaptureObjectiveFacts.nextCaptureMoveForceReserve(
                    fixture.game, PLAYER, fixture.analyzer,
                    null));
    }

    @Test
    public void unknownLandspeedCostDoesNotInventAFreeReserve() {
        Fixture fixture = fixture(
                CaptureObjectivePolicy.ObjectiveKind.BHBM);
        when(fixture.modifiers.getMoveUsingLandspeedCost(
                fixture.gameState, fixture.actor,
                fixture.origin, fixture.halfway,
                false, 0.0f)).thenReturn(Float.NaN);
        when(fixture.modifiers.getMoveUsingLandspeedCost(
                fixture.gameState, fixture.actor,
                fixture.origin, fixture.captureSite,
                false, 0.0f)).thenReturn(Float.NaN);

        assertEquals("Unknown remains unknown",
                0,
                CaptureObjectiveFacts.nextCaptureMoveForceReserve(
                    fixture.game, PLAYER, fixture.analyzer,
                    null));
    }

    @Test
    public void deployParentRequiresTheExactCaptureDestinationToBeFormationSafe() {
        Fixture fixture = fixture(
                CaptureObjectivePolicy.ObjectiveKind.BHBM);
        when(fixture.actor.getZone()).thenReturn(Zone.HAND);
        when(fixture.actor.getBlueprint()
                .hasPowerAttribute()).thenReturn(true);
        when(fixture.actor.getBlueprint()
                .getPower()).thenReturn(3.0f);
        when(fixture.actor.getBlueprint()
                .hasAbilityAttribute()).thenReturn(true);
        when(fixture.actor.getBlueprint()
                .getAbility()).thenReturn(3.0f);
        when(fixture.actor.getBlueprint()
                .getDeployCost()).thenReturn(4.0f);
        when(fixture.gameState.getForcePileSize(PLAYER))
                .thenReturn(10);
        when(fixture.modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(),
                anyBoolean(), anyFloat(), any(), any(), any(),
                any(), any(), any(), anyBoolean(), anyFloat()))
                .thenReturn(true);

        assertFalse("A legal but unsupported weak solo destination"
                        + " cannot grant the mandatory deploy parent",
                CaptureObjectiveFacts.guaranteesImmediateCaptureAt(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor, fixture.captureSite));
        assertFalse(CaptureObjectiveFacts
                .hasLegalImmediateCaptureDeployDestination(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor));

        when(fixture.actor.getBlueprint()
                .getAbility()).thenReturn(4.0f);
        assertTrue("A destiny-eligible actor has a known ALLOW verdict",
                CaptureObjectiveFacts.guaranteesImmediateCaptureAt(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor, fixture.captureSite));
        assertTrue(CaptureObjectiveFacts
                .hasLegalImmediateCaptureDeployDestination(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor));
    }

    @Test
    public void bhbmDeployPursuesAnOpenVehicleAtTheTargetsSite() {
        Fixture fixture = fixture(
                CaptureObjectivePolicy.ObjectiveKind.BHBM);
        PhysicalCard openVehicle = card(
                "Bantha", 107, PLAYER,
                CardCategory.VEHICLE);
        when(fixture.modifiers
                .getLocationThatCardIsPresentAt(
                    fixture.gameState, openVehicle))
                .thenReturn(fixture.captureSite);
        when(fixture.gameState.getAllPermanentCards())
                .thenReturn(List.of(
                    fixture.objective, fixture.target,
                    fixture.actor, fixture.imperial,
                    openVehicle));
        when(fixture.actor.getZone()).thenReturn(Zone.HAND);
        when(fixture.actor.getBlueprint()
                .hasPowerAttribute()).thenReturn(true);
        when(fixture.actor.getBlueprint()
                .getPower()).thenReturn(6.0f);
        when(fixture.actor.getBlueprint()
                .hasAbilityAttribute()).thenReturn(true);
        when(fixture.actor.getBlueprint()
                .getAbility()).thenReturn(6.0f);
        when(fixture.actor.getBlueprint()
                .getDeployCost()).thenReturn(6.0f);
        when(fixture.gameState.getForcePileSize(PLAYER))
                .thenReturn(10);
        when(fixture.modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(),
                anyBoolean(), anyFloat(), any(), any(), any(),
                any(), any(), any(), anyBoolean(), anyFloat()))
                .thenReturn(true);

        assertTrue(CaptureObjectiveFacts
                .guaranteesImmediateCaptureAt(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor, openVehicle));
        assertTrue(CaptureObjectiveFacts
                .hasLegalImmediateCaptureDeployDestination(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor));

        when(fixture.modifiers.hasKeyword(
                fixture.gameState, openVehicle,
                Keyword.ENCLOSED)).thenReturn(true);
        assertFalse("Vader inside an enclosed vehicle is not present"
                        + " with the target at the outer site",
                CaptureObjectiveFacts
                    .guaranteesImmediateCaptureAt(
                        fixture.game, PLAYER, fixture.analyzer,
                        fixture.actor, openVehicle));
    }

    @Test
    public void bhbmDoesNotTreatAnEnclosedTargetAsPresentAtTheOuterSite() {
        Fixture fixture = fixture(
                CaptureObjectivePolicy.ObjectiveKind.BHBM);
        PhysicalCard enclosedVehicle = card(
                "Blizzard 1", 108, OPPONENT,
                CardCategory.VEHICLE);
        when(fixture.modifiers.getCardIsPresentAt(
                fixture.gameState, fixture.target))
                .thenReturn(enclosedVehicle);

        assertFalse(CaptureObjectiveFacts
                .guaranteesImmediateCaptureAt(
                    fixture.game, PLAYER, fixture.analyzer,
                    fixture.actor, fixture.captureSite));
    }

    @Test
    public void forceReserveIncludesExactVadersCastleCostButNotUnknownCost() {
        Fixture fixture = fixture(
                CaptureObjectivePolicy.ObjectiveKind.BHBM);
        PhysicalCard castle = site(
                "Mustafar: Vader's Castle",
                "209_50", 106);
        fixture.cardsById.put(
                castle.getPermanentCardId(), castle);
        when(fixture.gameState.getAllPermanentCards())
                .thenReturn(List.of(
                    fixture.objective, fixture.target,
                    fixture.actor, castle));
        when(fixture.gameState.getLocationsInOrder())
                .thenReturn(List.of(
                    castle, fixture.captureSite));
        when(fixture.actor.getAtLocation())
                .thenReturn(castle);
        when(fixture.modifiers
                .getLocationThatCardIsPresentAt(
                    fixture.gameState,
                    fixture.actor)).thenReturn(castle);
        when(fixture.modifiers.getLocationHere(
                fixture.gameState,
                fixture.actor)).thenReturn(castle);
        when(fixture.modifiers.getLocationHere(
                fixture.gameState, castle))
                .thenReturn(castle);
        when(fixture.modifiers.isBattleground(
                fixture.gameState,
                fixture.captureSite, null))
                .thenReturn(true);
        when(fixture.actor.getBlueprint()
                .getValidMoveTargetFilter(
                    PLAYER, fixture.game,
                    fixture.actor, false))
                .thenReturn(Filters.any);
        when(fixture.modifiers.getLandspeedRequired(
                fixture.gameState, fixture.actor,
                fixture.captureSite)).thenReturn(null);
        when(fixture.modifiers
                .getMoveUsingLocationTextCost(
                    fixture.gameState,
                    fixture.actor, castle,
                    fixture.captureSite,
                    1.0f, 0.0f))
                .thenReturn(1.0f);

        assertTrue("The exact source-backed Castle move is legal",
                Filters.canMoveToUsingLocationText(
                        fixture.actor, false,
                        1.0f, 0.0f)
                    .accepts(
                        fixture.gameState,
                        fixture.modifiers,
                        fixture.captureSite));
        assertEquals("Vader's Castle preserves its exact one Force",
                1, CaptureObjectiveFacts
                    .nextCaptureMoveForceReserve(
                        fixture.game, PLAYER,
                        fixture.analyzer, null));
        assertEquals("Do not reserve Force from the spending actor itself",
                0, CaptureObjectiveFacts
                    .nextCaptureMoveForceReserve(
                        fixture.game, PLAYER,
                        fixture.analyzer,
                        fixture.actor));

        when(fixture.modifiers
                .getMoveUsingLocationTextCost(
                    fixture.gameState,
                    fixture.actor, castle,
                    fixture.captureSite,
                    1.0f, 0.0f))
                .thenReturn(Float.NaN);
        assertEquals("Unknown special-route cost must not be invented",
                0, CaptureObjectiveFacts
                    .nextCaptureMoveForceReserve(
                        fixture.game, PLAYER,
                        fixture.analyzer, null));
    }

    private static Fixture fixture(
            CaptureObjectivePolicy.ObjectiveKind kind) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers =
                mock(ModifiersQuerying.class);
        ObjectiveAnalyzer analyzer =
                mock(ObjectiveAnalyzer.class);

        String objectiveId =
                kind == CaptureObjectivePolicy.ObjectiveKind.TIGIH
                    ? "9_61" : "9_151";
        PhysicalCard objective = objective(objectiveId, 10);
        PhysicalCard target = character(
                "Luke Skywalker", 20,
                kind == CaptureObjectivePolicy.ObjectiveKind.TIGIH
                    ? PLAYER : OPPONENT);
        PhysicalCard vader = character(
                "Darth Vader", 30, PLAYER);
        PhysicalCard imperial = character(
                "Stormtrooper", 40, OPPONENT);
        PhysicalCard origin = site(
                "Tatooine: Cantina", "1_290", 101);
        PhysicalCard halfway = site(
                "Tatooine: Mos Eisley", "1_295", 102);
        PhysicalCard captureSite = site(
                "Tatooine: Docking Bay 94", "1_291", 103);
        PhysicalCard farther = site(
                "Tatooine: Lars' Moisture Farm", "1_294", 104);
        // ILLEGAL-BY-DESIGN negative control: a cross-system site must be
        // rejected even when mocked movement numbers make it look cheap.
        PhysicalCard unrelated = site(
                "Cloud City: West Gallery", "7_274", 105);

        PhysicalCard actor =
                kind == CaptureObjectivePolicy.ObjectiveKind.TIGIH
                    ? target : vader;
        Map<Integer, PhysicalCard> cardsById =
                new HashMap<>();
        for (PhysicalCard card : List.of(
                objective, target, vader, imperial,
                origin, halfway, captureSite,
                farther, unrelated)) {
            cardsById.put(card.getPermanentCardId(), card);
        }

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getAllPermanentCards()).thenReturn(
                List.of(objective, target, vader, imperial));
        when(gameState.getLocationsInOrder()).thenReturn(
                List.of(origin, halfway, captureSite,
                        farther, unrelated));
        when(gameState.findCardByPermanentId(any(Integer.class)))
                .thenAnswer(invocation -> cardsById.get(
                        invocation.getArgument(0, Integer.class)));

        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.isFlipped()).thenReturn(false);
        when(analyzer.getObjectiveBlueprintId())
                .thenReturn(objectiveId);

        setActive(gameState, target);
        setActive(gameState, vader);
        setActive(gameState, imperial);
        when(modifiers.hasPersona(
                gameState, target, Persona.LUKE))
                .thenReturn(true);
        when(modifiers.hasPersona(
                gameState, vader, Persona.VADER))
                .thenReturn(true);
        when(modifiers.hasIcon(
                gameState, imperial, Icon.IMPERIAL))
                .thenReturn(true);
        when(modifiers.canEscortCaptive(
                gameState, imperial, target,
                true, false, false)).thenReturn(true);
        when(modifiers.canBeTargetedBy(
                any(GameState.class),
                any(PhysicalCard.class),
                any(PhysicalCard.class),
                anySet()))
                .thenReturn(true);

        when(actor.getAtLocation()).thenReturn(origin);
        when(modifiers.getLocationThatCardIsPresentAt(
                gameState, actor)).thenReturn(origin);
        when(modifiers.getLocationThatCardIsPresentAt(
                gameState, target)).thenReturn(
                    kind
                        == CaptureObjectivePolicy.ObjectiveKind.TIGIH
                        ? origin : captureSite);
        when(modifiers.getCardIsPresentAt(
                gameState, target)).thenReturn(
                    kind
                        == CaptureObjectivePolicy.ObjectiveKind.TIGIH
                        ? origin : captureSite);
        when(modifiers.getLocationThatCardIsPresentAt(
                gameState, imperial)).thenReturn(captureSite);
        when(modifiers.getLocationHere(
                gameState, actor)).thenReturn(origin);
        for (PhysicalCard location : List.of(
                origin, halfway, captureSite,
                farther, unrelated)) {
            when(modifiers.getLocationHere(
                    gameState, location)).thenReturn(location);
        }
        when(modifiers.getSitesBetween(
                gameState, origin, captureSite))
                .thenReturn(List.of(halfway));

        when(modifiers.getLandspeedRequired(
                gameState, actor, halfway)).thenReturn(1);
        when(modifiers.getLandspeedRequired(
                gameState, actor, captureSite)).thenReturn(2);
        when(modifiers.getLandspeedRequired(
                gameState, actor, farther)).thenReturn(1);
        when(modifiers.getLandspeedRequired(
                gameState, actor, unrelated)).thenReturn(1);
        when(modifiers.getLandspeed(
                gameState, actor)).thenReturn(2.0f);
        when(modifiers.getForceAvailableToUse(
                gameState, PLAYER)).thenReturn(10);
        when(modifiers.getMoveUsingLandspeedCost(
                gameState, actor, origin,
                halfway, false, 0.0f)).thenReturn(2.0f);
        when(modifiers.getMoveUsingLandspeedCost(
                gameState, actor, origin,
                captureSite, false, 0.0f)).thenReturn(4.0f);
        when(modifiers.getMoveUsingLandspeedCost(
                gameState, actor, origin,
                farther, false, 0.0f)).thenReturn(1.0f);
        when(modifiers.getMoveUsingLandspeedCost(
                gameState, actor, origin,
                unrelated, false, 0.0f)).thenReturn(1.0f);

        return new Fixture(
                game, gameState, modifiers, analyzer,
                cardsById, objective, target, actor,
                imperial, origin, halfway,
                captureSite, farther, unrelated);
    }

    private static PhysicalCard objective(
            String blueprintId, int id) {
        PhysicalCard card = card(
                "Objective", id, PLAYER,
                CardCategory.OBJECTIVE);
        when(card.getBlueprintId(true))
                .thenReturn(blueprintId);
        return card;
    }

    private static PhysicalCard character(
            String title, int id, String owner) {
        PhysicalCard card = card(
                title, id, owner,
                CardCategory.CHARACTER);
        when(card.getCardsEscorting()).thenReturn(List.of());
        return card;
    }

    private static PhysicalCard site(
            String title, String blueprintId, int id) {
        PhysicalCard card = card(
                title, id, null,
                CardCategory.LOCATION);
        when(card.getBlueprintId(true))
                .thenReturn(blueprintId);
        when(card.getBlueprint()
                .getCardSubtype())
                .thenReturn(CardSubtype.SITE);
        return card;
    }

    private static PhysicalCard card(
            String title,
            int id,
            String owner,
            CardCategory category) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint =
                mock(SwccgCardBlueprint.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.getPermanentCardId()).thenReturn(id);
        when(card.getCardId()).thenReturn(id);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        when(card.getOwner()).thenReturn(owner);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.isBlownAway()).thenReturn(false);
        when(blueprint.getCardCategory()).thenReturn(category);
        return card;
    }

    private static void setActive(
            GameState gameState,
            PhysicalCard card) {
        when(gameState.isCardInPlayActive(
                any(PhysicalCard.class),
                anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean()))
                .thenAnswer(invocation ->
                    invocation.getArgument(0) == card
                    || invocation.getArgument(0)
                        != null);
    }

    private record Fixture(
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            ObjectiveAnalyzer analyzer,
            Map<Integer, PhysicalCard> cardsById,
            PhysicalCard objective,
            PhysicalCard target,
            PhysicalCard actor,
            PhysicalCard imperial,
            PhysicalCard origin,
            PhysicalCard halfway,
            PhysicalCard captureSite,
            PhysicalCard farther,
            PhysicalCard unrelated) {
    }
}
