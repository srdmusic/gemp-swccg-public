package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CaptureObjectiveCriticalCopyFactsTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";

    @Test
    public void exactBhbmRouteBeatsUnaffordableHandOnBothFaces() {
        EmperorFixture fixture = emperorFixture();

        for (boolean flipped : List.of(false, true)) {
            when(fixture.analyzer.isFlipped())
                    .thenReturn(flipped);
            String objectiveId = flipped
                    ? "9_151_BACK" : "9_151";
            when(fixture.analyzer.getObjectiveBlueprintId())
                    .thenReturn(objectiveId);
            when(fixture.objective.getBlueprintId(true))
                    .thenReturn(objectiveId);
            fixture.force[0] = 4;
            assertOnlyPreferred(
                    fixture, fixture.reserve);

            fixture.force[0] = 6;
            assertOnlyPreferred(
                    fixture, fixture.reserve);
        }
    }

    @Test
    public void noExecutableEmperorProtectsNoPhysicalCopy() {
        EmperorFixture fixture = emperorFixture();
        fixture.force[0] = 3;

        assertOnlyPreferred(fixture, null);
    }

    @Test
    public void activeSidiousBlocksEveryEmperorCopy() {
        EmperorFixture fixture = emperorFixture();
        PhysicalCard active = character(
                "Lord Sidious", 40, PLAYER,
                Zone.AT_LOCATION);
        when(active.getBlueprintId(true))
                .thenReturn("208_35");
        when(fixture.modifiers.hasPersona(
                fixture.gameState, active,
                Persona.SIDIOUS)).thenReturn(true);
        when(fixture.gameState.getAllPermanentCards())
                .thenReturn(List.of(
                    fixture.objective,
                    fixture.handLowId,
                    fixture.handHighId,
                    fixture.reserve,
                    active));
        setActiveCards(
                fixture.gameState, Set.of(active));
        fixture.force[0] = 6;

        assertOnlyPreferred(fixture, null);
    }

    @Test
    public void activeEmperorIsTheOnlyProtectedSidiousCopy() {
        EmperorFixture fixture = emperorFixture();
        PhysicalCard active = emperor(
                "The Emperor", 40,
                Zone.AT_LOCATION);
        when(active.getBlueprintId(true))
                .thenReturn("10_51");
        when(fixture.modifiers.hasPersona(
                fixture.gameState, active,
                Persona.SIDIOUS)).thenReturn(true);
        when(fixture.gameState.getAllPermanentCards())
                .thenReturn(List.of(
                    fixture.objective,
                    fixture.handLowId,
                    fixture.handHighId,
                    fixture.reserve,
                    active));
        setActiveCards(
                fixture.gameState, Set.of(active));
        fixture.force[0] = 6;

        assertEquals(
                CaptureObjectivePolicy.CriticalRole
                    .PAYOFF_CARD,
                preferred(fixture, active));
        assertOnlyPreferred(fixture, null);
    }

    @Test
    public void equalHandRoutesUseLiveCostThenPermanentId() {
        EmperorFixture fixture = emperorFixture();
        when(fixture.gameState.getAllPermanentCards())
                .thenReturn(List.of(
                    fixture.objective,
                    fixture.handLowId,
                    fixture.handHighId));
        when(fixture.gameState.getReserveDeck(PLAYER))
                .thenReturn(List.of());
        fixture.force[0] = 6;

        when(fixture.modifiers.getDeployCost(
                fixture.gameState,
                fixture.handHighId))
                .thenReturn(5.0f);
        assertEquals(
                CaptureObjectivePolicy.CriticalRole
                    .PAYOFF_CARD,
                preferred(fixture,
                    fixture.handHighId));
        assertNull(preferred(
                fixture, fixture.handLowId));

        when(fixture.modifiers.getDeployCost(
                fixture.gameState,
                fixture.handHighId))
                .thenReturn(6.0f);
        assertEquals(
                CaptureObjectivePolicy.CriticalRole
                    .PAYOFF_CARD,
                preferred(fixture,
                    fixture.handLowId));
        assertNull(preferred(
                fixture, fixture.handHighId));
    }

    @Test
    public void tigihProtectsExecutableIftcHandNotReserveCopy() {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers =
                mock(ModifiersQuerying.class);
        ObjectiveAnalyzer analyzer =
                mock(ObjectiveAnalyzer.class);
        PhysicalCard objective = objective(
                "9_61", 10);
        PhysicalCard hand = card(
                "I Feel The Conflict", 20,
                PLAYER, Zone.HAND,
                CardCategory.EFFECT);
        PhysicalCard reserve = card(
                "I Feel The Conflict", 21,
                PLAYER, Zone.RESERVE_DECK,
                CardCategory.EFFECT);
        Map<Integer, PhysicalCard> cards =
                Map.of(
                    10, objective,
                    20, hand,
                    21, reserve);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying())
                .thenReturn(modifiers);
        when(gameState.getAllPermanentCards())
                .thenReturn(List.of(
                    objective, hand, reserve));
        when(gameState.findCardByPermanentId(any()))
                .thenAnswer(invocation -> {
                    Integer id = invocation.getArgument(0);
                    return id != null
                            ? cards.get(id) : null;
                });
        setActiveCards(gameState, Set.of());
        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.isFlipped()).thenReturn(false);
        when(analyzer.getObjectiveBlueprintId())
                .thenReturn("9_61");
        when(modifiers.isDeployable(
                eq(gameState), eq(hand), eq(hand),
                eq(false), any(), eq(false),
                eq(0.0f), any(), any(), any(),
                any(), any(), anyBoolean(),
                anyFloat())).thenReturn(true);
        when(modifiers.getDeployCost(
                gameState, hand)).thenReturn(3.0f);

        assertTrue(Filters.I_Feel_The_Conflict
                .accepts(
                    gameState, modifiers, hand));
        assertTrue(Filters.deployable(
                    null, null, false, 0.0f)
                .accepts(
                    gameState, modifiers, hand));
        assertEquals(
                CaptureObjectivePolicy.CriticalRole
                    .PAYOFF_CARD,
                CaptureObjectiveFacts
                    .preferredCriticalLossRole(
                        game, PLAYER, analyzer,
                        hand));
        assertNull(CaptureObjectiveFacts
                .preferredCriticalLossRole(
                    game, PLAYER, analyzer,
                    reserve));
    }

    @Test
    public void activeRouteVaderBeatsStrandedLowerIdDuplicate() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers =
                mock(ModifiersQuerying.class);
        ObjectiveAnalyzer analyzer =
                mock(ObjectiveAnalyzer.class);

        PhysicalCard objective = objective(
                "9_151", 10);
        PhysicalCard luke = character(
                "Luke Skywalker", 11,
                OPPONENT, Zone.AT_LOCATION);
        PhysicalCard stranded = character(
                "Darth Vader", 20,
                PLAYER, Zone.AT_LOCATION);
        PhysicalCard routed = character(
                "Darth Vader", 30,
                PLAYER, Zone.AT_LOCATION);
        PhysicalCard origin = site(
                "Tatooine: Cantina", 101);
        PhysicalCard captureSite = site(
                "Tatooine: Mos Eisley", 102);
        PhysicalCard unrelated = site(
                "Cloud City: West Gallery", 103);

        Map<Integer, PhysicalCard> cards = new HashMap<>();
        for (PhysicalCard card : List.of(
                objective, luke, stranded, routed,
                origin, captureSite, unrelated)) {
            cards.put(card.getPermanentCardId(), card);
        }
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying())
                .thenReturn(modifiers);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getAllPermanentCards())
                .thenReturn(List.of(
                    objective, luke, stranded, routed));
        when(gameState.getLocationsInOrder())
                .thenReturn(List.of(
                    origin, captureSite, unrelated));
        when(gameState.findCardByPermanentId(any()))
                .thenAnswer(invocation -> cards.get(
                    invocation.getArgument(0)));
        setActiveCards(
                gameState,
                Set.of(luke, stranded, routed));

        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.isFlipped()).thenReturn(false);
        when(analyzer.getObjectiveBlueprintId())
                .thenReturn("9_151");
        when(modifiers.hasPersona(
                gameState, luke,
                Persona.LUKE)).thenReturn(true);
        when(modifiers.hasPersona(
                gameState, stranded,
                Persona.VADER)).thenReturn(true);
        when(modifiers.hasPersona(
                gameState, routed,
                Persona.VADER)).thenReturn(true);
        when(modifiers.canBeTargetedBy(
                any(GameState.class),
                any(PhysicalCard.class),
                any(PhysicalCard.class),
                anySet())).thenReturn(true);

        when(routed.getAtLocation())
                .thenReturn(origin);
        when(stranded.getAtLocation())
                .thenReturn(unrelated);
        when(modifiers.getLocationThatCardIsPresentAt(
                gameState, luke))
                .thenReturn(captureSite);
        when(modifiers.getCardIsPresentAt(
                gameState, luke))
                .thenReturn(captureSite);
        when(modifiers.getLocationThatCardIsPresentAt(
                gameState, routed))
                .thenReturn(origin);
        when(modifiers.getLocationThatCardIsPresentAt(
                gameState, stranded))
                .thenReturn(unrelated);
        when(modifiers.getLocationHere(
                gameState, routed)).thenReturn(origin);
        when(modifiers.getLocationHere(
                gameState, stranded)).thenReturn(unrelated);
        for (PhysicalCard location : List.of(
                origin, captureSite, unrelated)) {
            when(modifiers.getLocationHere(
                    gameState, location))
                    .thenReturn(location);
        }
        when(modifiers.getLandspeedRequired(
                gameState, routed,
                captureSite)).thenReturn(1);
        when(modifiers.getLandspeedRequired(
                gameState, stranded,
                captureSite)).thenReturn(null);
        when(modifiers.getLandspeed(
                gameState, routed)).thenReturn(1.0f);
        when(modifiers.getForceAvailableToUse(
                gameState, PLAYER)).thenReturn(10);

        assertTrue(CaptureObjectiveFacts
                .guaranteesImmediateCaptureAt(
                    game, PLAYER, analyzer,
                    routed, captureSite));
        assertTrue(CaptureObjectiveFacts
                .hasLegalImmediateCaptureMoveDestination(
                    game, PLAYER, analyzer,
                    routed));
        assertFalse(CaptureObjectiveFacts
                .hasLegalImmediateCaptureMoveDestination(
                    game, PLAYER, analyzer,
                    stranded));
        assertFalse(CaptureObjectiveFacts
                .hasLegalCaptureApproachMoveDestination(
                    game, PLAYER, analyzer,
                    stranded));
        assertNull(CaptureObjectiveFacts
                .preferredCriticalLossRole(
                    game, PLAYER, analyzer,
                    stranded));
        assertEquals(
                CaptureObjectivePolicy.CriticalRole
                    .CAPTURE_PIECE,
                CaptureObjectiveFacts
                    .preferredCriticalLossRole(
                        game, PLAYER, analyzer,
                        routed));
    }

    @Test
    public void bhbmDuelPayoffAcceptsOpponentOwnedThroneRoom() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers =
                mock(ModifiersQuerying.class);
        ObjectiveAnalyzer analyzer =
                mock(ObjectiveAnalyzer.class);
        PhysicalCard objective = objective(
                "9_151_BACK", 10);
        PhysicalCard luke = character(
                "Luke Skywalker", 11,
                OPPONENT, Zone.AT_LOCATION);
        PhysicalCard vader = character(
                "Darth Vader", 12,
                PLAYER, Zone.AT_LOCATION);
        PhysicalCard throneRoom = card(
                "Death Star II: Throne Room", 101,
                OPPONENT, Zone.LOCATIONS,
                CardCategory.LOCATION);
        when(throneRoom.getBlueprint()
                .getCardSubtype())
                .thenReturn(CardSubtype.SITE);
        when(luke.isCaptive()).thenReturn(true);
        when(luke.getEscort()).thenReturn(vader);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying())
                .thenReturn(modifiers);
        when(gameState.getAllPermanentCards())
                .thenReturn(List.of(
                    objective, luke, vader));
        setActiveCards(
                gameState, Set.of(luke, vader));
        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.isFlipped()).thenReturn(true);
        when(analyzer.getObjectiveBlueprintId())
                .thenReturn("9_151_BACK");
        when(modifiers.hasPersona(
                gameState, luke,
                Persona.LUKE)).thenReturn(true);
        when(modifiers.hasPersona(
                gameState, vader,
                Persona.VADER)).thenReturn(true);

        assertEquals(OPPONENT, throneRoom.getOwner());
        assertEquals(
                ObjectiveAnalyzer.ObjectivePostFlipPayoffRole
                    .PRIMARY,
                CaptureObjectiveFacts.bhbmDuelPayoffRoleAt(
                    game, PLAYER, analyzer,
                    vader, throneRoom));
    }

    @Test
    public void stableBackCountsLastVaderAboardCarrierAsPhysicalGroup() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers =
                mock(ModifiersQuerying.class);
        ObjectiveAnalyzer analyzer =
                mock(ObjectiveAnalyzer.class);
        PhysicalCard objective = objective(
                "9_151_BACK", 10);
        PhysicalCard luke = character(
                "Luke Skywalker", 11,
                OPPONENT, Zone.AT_LOCATION);
        PhysicalCard carrier = card(
                "Blizzard 1", 12,
                PLAYER, Zone.AT_LOCATION,
                CardCategory.VEHICLE);
        PhysicalCard vader = character(
                "Darth Vader", 13,
                PLAYER, Zone.AT_LOCATION);
        PhysicalCard duplicate = character(
                "Darth Vader", 14,
                PLAYER, Zone.AT_LOCATION);
        PhysicalCard destination = site(
                "Hoth: Echo Docking Bay", 101);
        when(vader.getAttachedTo())
                .thenReturn(carrier);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying())
                .thenReturn(modifiers);
        when(gameState.getAllPermanentCards())
                .thenReturn(List.of(
                    objective, luke,
                    carrier, vader));
        setActiveCards(
                gameState,
                Set.of(luke, carrier,
                    vader, duplicate));
        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.isFlipped()).thenReturn(true);
        when(analyzer.getObjectiveBlueprintId())
                .thenReturn("9_151_BACK");
        when(modifiers.hasPersona(
                gameState, luke,
                Persona.LUKE)).thenReturn(true);
        when(modifiers.hasPersona(
                gameState, vader,
                Persona.VADER)).thenReturn(true);
        when(modifiers.hasPersona(
                gameState, duplicate,
                Persona.VADER)).thenReturn(true);
        when(modifiers.isPresentWith(
                gameState, vader, luke))
                .thenReturn(true);
        when(modifiers.isPresentWith(
                gameState, duplicate, luke))
                .thenReturn(true);

        assertTrue(CaptureObjectiveFacts
                .wouldBreakStableBackIfRemoved(
                    game, PLAYER, analyzer,
                    carrier));
        assertTrue(CaptureObjectiveFacts
                .wouldBreakStableBackByMovingTo(
                    game, PLAYER, analyzer,
                    carrier, destination));
        assertEquals(
                CaptureObjectivePolicy.CriticalRole
                    .CAPTURE_PIECE,
                CaptureObjectiveFacts
                    .preferredCriticalLossRole(
                        game, PLAYER, analyzer,
                        vader));

        when(gameState.getAllPermanentCards())
                .thenReturn(List.of(
                    objective, luke,
                    carrier, vader, duplicate));

        assertFalse(CaptureObjectiveFacts
                .wouldBreakStableBackIfRemoved(
                    game, PLAYER, analyzer,
                    carrier));
        assertFalse(CaptureObjectiveFacts
                .wouldBreakStableBackByMovingTo(
                    game, PLAYER, analyzer,
                    carrier, destination));
        assertNull(CaptureObjectiveFacts
                .preferredCriticalLossRole(
                    game, PLAYER, analyzer,
                    vader));
        assertNull(CaptureObjectiveFacts
                .preferredCriticalLossRole(
                    game, PLAYER, analyzer,
                    duplicate));
    }

    private static void assertOnlyPreferred(
            EmperorFixture fixture,
            PhysicalCard expected) {
        int protectedCopies = 0;
        for (PhysicalCard candidate : List.of(
                fixture.handLowId,
                fixture.handHighId,
                fixture.reserve)) {
            CaptureObjectivePolicy.CriticalRole role =
                    preferred(fixture, candidate);
            if (candidate == expected
                    && expected != null) {
                assertEquals(
                    CaptureObjectivePolicy.CriticalRole
                        .PAYOFF_CARD,
                    role);
            } else {
                assertNull(role);
            }
            if (role != null) {
                protectedCopies++;
            }
        }
        assertEquals(expected != null ? 1 : 0,
                protectedCopies);
    }

    private static CaptureObjectivePolicy.CriticalRole
            preferred(
                    EmperorFixture fixture,
                    PhysicalCard card) {
        return CaptureObjectiveFacts
                .preferredCriticalLossRole(
                    fixture.game, PLAYER,
                    fixture.analyzer, card);
    }

    private static EmperorFixture emperorFixture() {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers =
                mock(ModifiersQuerying.class);
        ObjectiveAnalyzer analyzer =
                mock(ObjectiveAnalyzer.class);
        PhysicalCard objective = objective(
                "9_151", 10);
        PhysicalCard handLowId = emperor(
                "The Emperor", 20,
                Zone.HAND);
        PhysicalCard handHighId = emperor(
                "The Emperor", 21,
                Zone.HAND);
        PhysicalCard reserve = emperor(
                "The Emperor", 30,
                Zone.RESERVE_DECK);
        int[] force = {4};

        Map<Integer, PhysicalCard> cards = new HashMap<>();
        for (PhysicalCard card : List.of(
                objective, handLowId,
                handHighId, reserve)) {
            cards.put(card.getPermanentCardId(), card);
        }
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying())
                .thenReturn(modifiers);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getAllPermanentCards())
                .thenReturn(List.of(
                    objective, handLowId,
                    handHighId, reserve));
        when(gameState.getReserveDeck(PLAYER))
                .thenReturn(List.of(reserve));
        when(gameState.findCardByPermanentId(any()))
                .thenAnswer(invocation -> cards.get(
                    invocation.getArgument(0)));
        setActiveCards(gameState, Set.of());

        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.isFlipped()).thenReturn(false);
        when(analyzer.getObjectiveBlueprintId())
                .thenReturn("9_151");
        when(reserve.getBlueprintId(true))
                .thenReturn("10_51");
        for (PhysicalCard emperor : List.of(
                handLowId, handHighId, reserve)) {
            when(modifiers.hasPersona(
                    gameState, emperor,
                    Persona.SIDIOUS)).thenReturn(true);
        }
        when(modifiers.getForceAvailableToUse(
                gameState, PLAYER))
                .thenAnswer(invocation -> force[0]);
        when(modifiers.getDeployCost(
                gameState, handLowId))
                .thenReturn(6.0f);
        when(modifiers.getDeployCost(
                gameState, handHighId))
                .thenReturn(6.0f);
        when(modifiers.getDeployCost(
                eq(gameState),
                eq(objective), eq(reserve),
                any(), eq(false), any(),
                eq(false), eq(-2.0f),
                any(), eq(true)))
                .thenReturn(4.0f);
        when(modifiers.isDeployable(
                eq(gameState), any(), any(),
                eq(false), any(), eq(false),
                anyFloat(), any(), any(), any(),
                any(), any(), anyBoolean(),
                anyFloat()))
                .thenAnswer(invocation -> {
                    PhysicalCard source =
                            invocation.getArgument(1);
                    PhysicalCard candidate =
                            invocation.getArgument(2);
                    float adjustment =
                            invocation.getArgument(
                                6, Float.class);
                    if ((candidate == handLowId
                            || candidate == handHighId)
                            && source == candidate) {
                        return adjustment == 0.0f
                                && force[0] >= 6;
                    }
                    return candidate == reserve
                            && source == objective
                            && adjustment == -2.0f
                            && force[0] >= 4;
                });

        return new EmperorFixture(
                game, gameState, modifiers,
                analyzer, objective,
                handLowId, handHighId,
                reserve, force);
    }

    private static PhysicalCard objective(
            String blueprintId, int id) {
        PhysicalCard card = card(
                "Objective", id, PLAYER,
                Zone.SIDE_OF_TABLE,
                CardCategory.OBJECTIVE);
        when(card.getBlueprintId(true))
                .thenReturn(blueprintId);
        return card;
    }

    private static PhysicalCard emperor(
            String title, int id, Zone zone) {
        PhysicalCard card = character(
                title, id, PLAYER, zone);
        when(card.getBlueprint().getDeployCost())
                .thenReturn(6.0f);
        return card;
    }

    private static PhysicalCard character(
            String title,
            int id,
            String owner,
            Zone zone) {
        PhysicalCard card = card(
                title, id, owner, zone,
                CardCategory.CHARACTER);
        when(card.getCardsEscorting())
                .thenReturn(List.of());
        return card;
    }

    private static PhysicalCard site(
            String title, int id) {
        PhysicalCard card = card(
                title, id, null,
                Zone.LOCATIONS,
                CardCategory.LOCATION);
        when(card.getBlueprint()
                .getCardSubtype())
                .thenReturn(CardSubtype.SITE);
        return card;
    }

    private static PhysicalCard card(
            String title,
            int id,
            String owner,
            Zone zone,
            CardCategory category) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint =
                mock(SwccgCardBlueprint.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles())
                .thenReturn(List.of(title));
        when(card.getPermanentCardId())
                .thenReturn(id);
        when(card.getCardId()).thenReturn(id);
        when(card.getAdditionalCardIds())
                .thenReturn(List.of());
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.isBlownAway()).thenReturn(false);
        when(blueprint.getCardCategory())
                .thenReturn(category);
        return card;
    }

    private static void setActiveCards(
            GameState gameState,
            Set<PhysicalCard> active) {
        when(gameState.isCardInPlayActive(
                any(PhysicalCard.class),
                anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean()))
                .thenAnswer(invocation ->
                    active.contains(
                        invocation.getArgument(0)));
    }

    private record EmperorFixture(
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            ObjectiveAnalyzer analyzer,
            PhysicalCard objective,
            PhysicalCard handLowId,
            PhysicalCard handHighId,
            PhysicalCard reserve,
            int[] force) {
    }
}
