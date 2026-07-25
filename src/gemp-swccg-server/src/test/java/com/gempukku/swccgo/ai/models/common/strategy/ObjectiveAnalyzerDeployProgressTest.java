package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.playbook.ObjectiveProgressAssessment;
import com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ObjectiveAnalyzerDeployProgressTest {
    private static final String PLAYER_ID = "player";

    @Test
    public void noPhysicalObjectiveProducesNoObjective() {
        ObjectiveAnalyzer analyzer = new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        GameState gameState = mock(GameState.class);
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        SwccgGame game = mock(SwccgGame.class);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying())
                .thenReturn(mock(ModifiersQuerying.class));

        ObjectiveProgressAssessment assessment = analyzer.assessDeployChild(
                game, PLAYER_ID, null, null);

        assertEquals(ObjectiveProgressAssessment.Outcome.NO_OBJECTIVE, assessment.outcome());
    }

    @Test
    public void endorRequiredCardAdvancesOneMissingRequirement() {
        PhysicalCard ominousRumors = card("Ominous Rumors", "8_125", CardCategory.EFFECT,
                PLAYER_ID, Zone.RESERVE_DECK, false);
        PhysicalCard endorSystem = card("Endor", "8_128", CardCategory.LOCATION,
                PLAYER_ID, Zone.LOCATIONS, false);
        Fixture fixture = endorFixture(false, List.of());
        when(fixture.gameState.getCardPile(PLAYER_ID, Zone.RESERVE_DECK))
                .thenReturn(List.of(ominousRumors));
        when(fixture.gameState.getTopLocations()).thenReturn(List.of(endorSystem));

        ObjectiveProgressAssessment assessment = fixture.analyzer.assessDeployChild(
                fixture.game, PLAYER_ID, ominousRumors, endorSystem);

        assertEquals(ObjectiveProgressAssessment.Outcome.ADVANCES_MISSING_REQUIREMENT,
                assessment.outcome());
        assertEquals(Set.of("ominous rumors", "establish secret base"),
                assessment.missingRequirements());
        assertEquals(Set.of("ominous rumors"), assessment.advancedRequirements());
    }

    @Test
    public void finalEndorRequiredCardCompletesModeledFlipRequirements() {
        PhysicalCard ominousRumors = card("Ominous Rumors", "8_125", CardCategory.EFFECT,
                PLAYER_ID, Zone.SIDE_OF_TABLE, false);
        PhysicalCard establishSecretBase = card("Establish Secret Base (V)", "207_25",
                CardCategory.EFFECT, PLAYER_ID, Zone.RESERVE_DECK, false);
        PhysicalCard bunker = card("Endor: Bunker", "8_129", CardCategory.LOCATION,
                PLAYER_ID, Zone.LOCATIONS, false);
        Fixture fixture = endorFixture(false, List.of(ominousRumors));
        when(fixture.gameState.getCardPile(PLAYER_ID, Zone.RESERVE_DECK))
                .thenReturn(List.of(establishSecretBase));
        when(fixture.gameState.getTopLocations()).thenReturn(List.of(bunker));

        ObjectiveProgressAssessment assessment = fixture.analyzer.assessDeployChild(
                fixture.game, PLAYER_ID, establishSecretBase, bunker);

        assertEquals(ObjectiveProgressAssessment.Outcome.COMPLETES_FLIP_NOW,
                assessment.outcome());
        assertEquals(Set.of("ominous rumors"), assessment.satisfiedRequirements());
        assertEquals(Set.of("establish secret base"), assessment.missingRequirements());
        assertEquals(Set.of("establish secret base"), assessment.advancedRequirements());
    }

    @Test
    public void ambiguousPhysicalChildFailsClosed() {
        Fixture fixture = endorFixture(false, List.of());
        PhysicalCard bunker = card("Endor: Bunker", "8_129", CardCategory.LOCATION,
                PLAYER_ID, Zone.LOCATIONS, false);
        when(fixture.gameState.getTopLocations()).thenReturn(List.of(bunker));

        ObjectiveProgressAssessment assessment = fixture.analyzer.assessDeployChild(
                fixture.game, PLAYER_ID, null, bunker);

        assertEquals(ObjectiveProgressAssessment.Outcome.UNPROVEN, assessment.outcome());
        assertTrue(assessment.evidence().contains("unique physical card"));
    }

    @Test
    public void physicalCardOutsideLiveCandidateCollectionsFailsClosed() {
        Fixture fixture = endorFixture(false, List.of());
        PhysicalCard ominousRumors = card("Ominous Rumors", "8_125", CardCategory.EFFECT,
                PLAYER_ID, Zone.RESERVE_DECK, false);
        PhysicalCard endorSystem = card("Endor", "8_128", CardCategory.LOCATION,
                PLAYER_ID, Zone.LOCATIONS, false);
        when(fixture.gameState.getTopLocations()).thenReturn(List.of(endorSystem));

        ObjectiveProgressAssessment assessment = fixture.analyzer.assessDeployChild(
                fixture.game, PLAYER_ID, ominousRumors, endorSystem);

        assertEquals(ObjectiveProgressAssessment.Outcome.UNPROVEN, assessment.outcome());
        assertTrue(assessment.evidence().contains("exact live child"));
    }

    @Test
    public void destinationOutsideLiveTopLocationsFailsClosed() {
        Fixture fixture = endorFixture(false, List.of());
        PhysicalCard ominousRumors = card("Ominous Rumors", "8_125", CardCategory.EFFECT,
                PLAYER_ID, Zone.RESERVE_DECK, false);
        PhysicalCard liveEndorSystem = card("Endor", "8_128", CardCategory.LOCATION,
                PLAYER_ID, Zone.LOCATIONS, false);
        PhysicalCard disconnectedEndorSystem = card("Endor", "8_128", CardCategory.LOCATION,
                PLAYER_ID, Zone.LOCATIONS, false);
        when(fixture.gameState.getCardPile(PLAYER_ID, Zone.RESERVE_DECK))
                .thenReturn(List.of(ominousRumors));
        when(fixture.gameState.getTopLocations()).thenReturn(List.of(liveEndorSystem));

        ObjectiveProgressAssessment assessment = fixture.analyzer.assessDeployChild(
                fixture.game, PLAYER_ID, ominousRumors, disconnectedEndorSystem);

        assertEquals(ObjectiveProgressAssessment.Outcome.UNPROVEN, assessment.outcome());
        assertTrue(assessment.evidence().contains("exact live top location"));
    }

    @Test
    public void duplicateBlueprintCandidatesCannotResolvePhysicalChild() throws Exception {
        PhysicalCard first = card("Ominous Rumors", "8_125", CardCategory.EFFECT,
                PLAYER_ID, Zone.HAND, false);
        PhysicalCard second = card("Ominous Rumors", "8_125", CardCategory.EFFECT,
                PLAYER_ID, Zone.RESERVE_DECK, false);
        GameState gameState = mock(GameState.class);
        when(gameState.getHand(PLAYER_ID)).thenReturn(List.of(first));
        when(gameState.getCardPile(PLAYER_ID, Zone.RESERVE_DECK)).thenReturn(List.of(second));
        when(gameState.getAllStackedCards()).thenReturn(List.of());

        com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator evaluator =
                new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator();
        java.lang.reflect.Method resolver = evaluator.getClass().getDeclaredMethod(
                "findUniqueDeployingCard", DecisionContext.class, GameState.class,
                String.class, String.class);
        resolver.setAccessible(true);

        assertEquals(null, resolver.invoke(
                evaluator, null, gameState, PLAYER_ID, "8_125"));
    }

    @Test
    public void unmodeledObjectiveFailsClosed() {
        PhysicalCard objective = objective("12_179", "My Lord, Is That Legal?",
                "Deploy Galactic Senate. Flip this card if you have 3 senators at Galactic Senate.",
                false);
        GameState gameState = mock(GameState.class);
        when(gameState.getAllPermanentCards()).thenReturn(List.of(objective));
        SwccgGame game = mock(SwccgGame.class);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying())
                .thenReturn(mock(ModifiersQuerying.class));
        ObjectiveAnalyzer analyzer = new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        analyzer.analyze(game, PLAYER_ID, Side.DARK);
        PhysicalCard candidate = card("Test Senator", "test_1", CardCategory.CHARACTER,
                PLAYER_ID, Zone.HAND, false);
        PhysicalCard destination = card("Coruscant: Galactic Senate", "12_180",
                CardCategory.LOCATION, PLAYER_ID, Zone.LOCATIONS, false);
        when(gameState.getHand(PLAYER_ID)).thenReturn(List.of(candidate));
        when(gameState.getTopLocations()).thenReturn(List.of(destination));

        ObjectiveProgressAssessment assessment = analyzer.assessDeployChild(
                game, PLAYER_ID, candidate, destination);

        assertEquals(ObjectiveProgressAssessment.Outcome.UNPROVEN, assessment.outcome());
    }

    private static Fixture endorFixture(boolean flipped, List<PhysicalCard> otherCards) {
        PhysicalCard objective = objective("8_167", "Endor Operations",
                "Deploy Endor system, Bunker and Landing Platform. Flip this card if Ominous Rumors and Establish Secret Base are both on table.",
                flipped);
        List<PhysicalCard> permanents = new ArrayList<>();
        permanents.add(objective);
        permanents.addAll(otherCards);

        GameState gameState = mock(GameState.class);
        when(gameState.getAllPermanentCards()).thenReturn(permanents);
        SwccgGame game = mock(SwccgGame.class);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying())
                .thenReturn(mock(ModifiersQuerying.class));
        when(gameState.isCardInPlayActive(
                any(PhysicalCard.class),
                anyBoolean(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyBoolean())).thenReturn(true);
        ObjectiveAnalyzer analyzer = new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        analyzer.analyze(game, PLAYER_ID, Side.DARK);
        return new Fixture(analyzer, game, gameState);
    }

    private static PhysicalCard objective(
            String blueprintId, String title, String gameText, boolean flipped) {
        SwccgCardBlueprint front = blueprint(title, gameText, CardCategory.OBJECTIVE);
        SwccgCardBlueprint back = blueprint(title + " Back",
                "Place out of play if an Endor location is blown away.", CardCategory.OBJECTIVE);
        return physicalCard(front, back, blueprintId, PLAYER_ID, Zone.SIDE_OF_TABLE, flipped);
    }

    private static PhysicalCard card(
            String title, String blueprintId, CardCategory category,
            String owner, Zone zone, boolean flipped) {
        return physicalCard(blueprint(title, "", category), null,
                blueprintId, owner, zone, flipped);
    }

    private static PhysicalCard physicalCard(
            SwccgCardBlueprint current, SwccgCardBlueprint opposite,
            String blueprintId, String owner, Zone zone, boolean flipped) {
        return (PhysicalCard) Proxy.newProxyInstance(
                PhysicalCard.class.getClassLoader(), new Class<?>[]{PhysicalCard.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getBlueprint": return current;
                        case "getOtherSideBlueprint": return opposite;
                        case "getBlueprintId": return blueprintId;
                        case "getTitle": return current != null ? current.getTitle() : null;
                        case "getOwner": return owner;
                        case "getZone": return zone;
                        case "isFlipped": return flipped;
                        default: return defaultValue(method.getReturnType());
                    }
                });
    }

    private static SwccgCardBlueprint blueprint(
            String title, String gameText, CardCategory category) {
        return (SwccgCardBlueprint) Proxy.newProxyInstance(
                SwccgCardBlueprint.class.getClassLoader(),
                new Class<?>[]{SwccgCardBlueprint.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getTitle": return title;
                        case "getGameText": return gameText;
                        case "getCardCategory": return category;
                        default: return defaultValue(method.getReturnType());
                    }
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == char.class) return '\0';
        return null;
    }

    private record Fixture(
            ObjectiveAnalyzer analyzer, SwccgGame game,
            GameState gameState) { }
}
