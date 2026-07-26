package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CaptureVirtualHutSoleEnablerFactsTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";

    @Test
    public void twoEligibleImperialsAtOnePlatformStillMakeItSole() {
        Fixture fixture = fixture();
        enable(fixture, fixture.first,
                fixture.landingA);
        enable(fixture, fixture.second,
                fixture.landingA);

        assertTrue(CaptureObjectiveFacts
                .isSoleVirtualHutCaptureEnablerLocation(
                    fixture.game, PLAYER,
                    fixture.analyzer,
                    fixture.landingA));
    }

    @Test
    public void oneEligibleImperialAtEachPlatformIsNotSole() {
        Fixture fixture = fixture();
        enable(fixture, fixture.first,
                fixture.landingA);
        enable(fixture, fixture.second,
                fixture.landingB);

        assertFalse(CaptureObjectiveFacts
                .isSoleVirtualHutCaptureEnablerLocation(
                    fixture.game, PLAYER,
                    fixture.analyzer,
                    fixture.landingA));
        assertFalse(CaptureObjectiveFacts
                .isSoleVirtualHutCaptureEnablerLocation(
                    fixture.game, PLAYER,
                    fixture.analyzer,
                    fixture.landingB));
    }

    @Test
    public void ineligibleInactiveAndTitleImpostorsDoNotCount() {
        Fixture fixture = fixture();
        enable(fixture, fixture.first,
                fixture.landingA);

        fixture.active.add(fixture.second);
        fixture.locations.put(
                fixture.second,
                fixture.landingA);
        fixture.canEscort.put(
                fixture.second, false);

        fixture.locations.put(
                fixture.inactive,
                fixture.landingA);
        fixture.canEscort.put(
                fixture.inactive, true);

        fixture.active.add(fixture.titleImpostor);
        fixture.locations.put(
                fixture.titleImpostor,
                fixture.landingA);
        fixture.canEscort.put(
                fixture.titleImpostor, true);

        assertTrue(CaptureObjectiveFacts
                .isSoleVirtualHutCaptureEnablerLocation(
                    fixture.game, PLAYER,
                    fixture.analyzer,
                    fixture.landingA));
        assertFalse("The sole physical enabler is not at this platform",
                CaptureObjectiveFacts
                    .isSoleVirtualHutCaptureEnablerLocation(
                        fixture.game, PLAYER,
                        fixture.analyzer,
                        fixture.landingB));

        fixture.active.remove(fixture.first);
        assertFalse("Inactive and title-only cards cannot enable capture",
                CaptureObjectiveFacts
                    .isSoleVirtualHutCaptureEnablerLocation(
                        fixture.game, PLAYER,
                        fixture.analyzer,
                        fixture.landingA));
    }

    private static void enable(
            Fixture fixture,
            PhysicalCard imperial,
            PhysicalCard location) {
        fixture.active.add(imperial);
        fixture.canEscort.put(imperial, true);
        fixture.locations.put(imperial, location);
    }

    private static Fixture fixture() {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers =
                mock(ModifiersQuerying.class);
        ObjectiveAnalyzer analyzer =
                mock(ObjectiveAnalyzer.class);

        PhysicalCard objective = objective();
        PhysicalCard luke = character(
                "Luke Skywalker", 11,
                PLAYER, true);
        PhysicalCard first = character(
                "Stormtrooper", 20,
                OPPONENT, true);
        PhysicalCard second = character(
                "Officer", 21,
                OPPONENT, true);
        PhysicalCard inactive = character(
                "Scout Trooper", 22,
                OPPONENT, true);
        PhysicalCard titleImpostor = character(
                "Imperial Stormtrooper", 23,
                OPPONENT, false);
        PhysicalCard hut = site(
                "Endor: Chief Chirpa's Hut (V)",
                "214_19", 101);
        PhysicalCard landingA = site(
                "Endor: Landing Platform (Docking Bay)",
                "8_76", 102);
        PhysicalCard landingB = site(
                "Endor: Landing Platform (Docking Bay)",
                "8_76", 103);

        List<PhysicalCard> permanents =
                new ArrayList<>(List.of(
                    objective, luke, first, second,
                    inactive, titleImpostor));
        Set<PhysicalCard> active =
                new HashSet<>(Set.of(luke));
        Map<PhysicalCard, Boolean> canEscort =
                new HashMap<>();
        Map<PhysicalCard, PhysicalCard> locations =
                new HashMap<>();
        locations.put(luke, hut);

        Map<Integer, PhysicalCard> cardsById =
                new HashMap<>();
        for (PhysicalCard card : List.of(
                objective, luke, first, second,
                inactive, titleImpostor,
                hut, landingA, landingB)) {
            cardsById.put(
                    card.getPermanentCardId(),
                    card);
        }

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying())
                .thenReturn(modifiers);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getAllPermanentCards())
                .thenAnswer(invocation -> permanents);
        when(gameState.getLocationsInOrder())
                .thenReturn(List.of(
                    hut, landingA, landingB));
        when(gameState.findCardByPermanentId(any()))
                .thenAnswer(invocation -> cardsById.get(
                    invocation.getArgument(0)));
        when(gameState.isCardInPlayActive(
                any(PhysicalCard.class),
                anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean()))
                .thenAnswer(invocation ->
                    active.contains(
                        invocation.getArgument(0)));

        when(analyzer.isAnalyzed()).thenReturn(true);
        when(analyzer.isFlipped()).thenReturn(false);
        when(analyzer.getObjectiveBlueprintId())
                .thenReturn("9_61");
        when(modifiers.hasPersona(
                gameState, luke,
                Persona.LUKE)).thenReturn(true);
        for (PhysicalCard imperial : List.of(
                first, second, inactive)) {
            when(modifiers.hasIcon(
                    gameState, imperial,
                    Icon.IMPERIAL)).thenReturn(true);
        }
        when(modifiers.canEscortCaptive(
                any(GameState.class),
                any(PhysicalCard.class),
                any(PhysicalCard.class),
                anyBoolean(), anyBoolean(),
                anyBoolean()))
                .thenAnswer(invocation ->
                    canEscort.getOrDefault(
                        invocation.getArgument(1),
                        false));
        when(modifiers.getLocationThatCardIsPresentAt(
                any(GameState.class),
                any(PhysicalCard.class)))
                .thenAnswer(invocation ->
                    locations.get(
                        invocation.getArgument(1)));
        when(modifiers.getLocationHere(
                any(GameState.class),
                any(PhysicalCard.class)))
                .thenAnswer(invocation -> {
                    PhysicalCard card =
                            invocation.getArgument(1);
                    if (card == hut
                            || card == landingA
                            || card == landingB) {
                        return card;
                    }
                    return locations.get(card);
                });
        when(luke.getBlueprint()
                .getValidMoveTargetFilter(
                    PLAYER, game, luke, false))
                .thenReturn(Filters.any);

        return new Fixture(
                game, gameState, modifiers,
                analyzer, first, second,
                inactive, titleImpostor,
                landingA, landingB,
                active, canEscort, locations);
    }

    private static PhysicalCard objective() {
        PhysicalCard card = card(
                "There Is Good In Him", 10,
                PLAYER, Zone.SIDE_OF_TABLE,
                CardCategory.OBJECTIVE);
        when(card.getBlueprintId(true))
                .thenReturn("9_61");
        return card;
    }

    private static PhysicalCard character(
            String title,
            int id,
            String owner,
            boolean imperialIcon) {
        PhysicalCard card = card(
                title, id, owner,
                Zone.AT_LOCATION,
                CardCategory.CHARACTER);
        when(card.getCardsEscorting())
                .thenReturn(List.of());
        if (!imperialIcon) {
            when(card.getBlueprintId(true))
                    .thenReturn("test_impostor");
        }
        return card;
    }

    private static PhysicalCard site(
            String title,
            String blueprintId,
            int id) {
        PhysicalCard card = card(
                title, id, null,
                Zone.LOCATIONS,
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

    private record Fixture(
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            ObjectiveAnalyzer analyzer,
            PhysicalCard first,
            PhysicalCard second,
            PhysicalCard inactive,
            PhysicalCard titleImpostor,
            PhysicalCard landingA,
            PhysicalCard landingB,
            Set<PhysicalCard> active,
            Map<PhysicalCard, Boolean> canEscort,
            Map<PhysicalCard, PhysicalCard> locations) {
    }
}
