package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.cards.set109.dark.Card109_012;
import com.gempukku.swccgo.cards.set109.dark.Card109_012_BACK;
import com.gempukku.swccgo.cards.set226.dark.Card226_012;
import com.gempukku.swccgo.cards.set226.dark.Card226_012_BACK;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Source-backed AI contract for the two mechanically different TDIGWATT
 * printings. The card blueprints are the authority, not copied workbook text.
 */
public class TdigwattObjectiveAiSourceContractTest {
    private static final String PLAYER_ID = "player";

    @Test
    public void actualCardSourcesProduceDistinctClassicAndVirtualFactsForBothBots() {
        Snapshot classicRando = analyze(
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                "109_12", new Card109_012(), new Card109_012_BACK());
        Snapshot classicChosenOne = analyze(
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer(),
                "109_12", new Card109_012(), new Card109_012_BACK());
        Snapshot virtualRando = analyze(
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                "226_12", new Card226_012(), new Card226_012_BACK());
        Snapshot virtualChosenOne = analyze(
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer(),
                "226_12", new Card226_012(), new Card226_012_BACK());

        assertEquals(classicRando, classicChosenOne);
        assertEquals(virtualRando, virtualChosenOne);

        assertEquals("109_12", classicRando.blueprintId());
        assertTrue(classicRando.tdigwatt());
        assertTrue(classicRando.preFlip());
        assertTrue(classicRando.requiresOccupy());
        assertFalse(classicRando.requiresControl());
        assertFalse(classicRando.forbidsExecutor());
        assertFalse(classicRando.jsonHydrated());
        assertEquals(Set.of("dark deal"), classicRando.requiredCards());
        assertEquals(Set.of(
                "bespin system",
                "bespin: cloud city",
                "dark deal",
                "cloud city occupation"),
                classicRando.pullableCards());
        assertTrue(classicRando.flipText().contains(
                "Dark Deal on table"));
        assertTrue(classicRando.flipText().contains(
                "occupy Bespin System"));
        assertTrue(classicRando.flipText().contains(
                "Bespin: Cloud City"));
        assertTrue(classicRando.flipBackText().contains(
                "Dark Deal is canceled"));
        assertTrue(classicRando.flipBackText().contains(
                "opponent controls Bespin"));
        assertTrue(classicRando.flipBackText().contains(
                "Bespin is 'blown away'"));

        assertEquals("226_12", virtualRando.blueprintId());
        assertTrue(virtualRando.tdigwatt());
        assertTrue(virtualRando.preFlip());
        assertFalse(virtualRando.requiresOccupy());
        assertTrue(virtualRando.requiresControl());
        assertTrue(virtualRando.forbidsExecutor());
        assertFalse(virtualRando.jsonHydrated());
        assertEquals(Set.of(), virtualRando.requiredCards());
        assertEquals(Set.of(
                "dark deal",
                "vader's bounty",
                "bespin"),
                virtualRando.pullableCards());
        assertTrue(virtualRando.flipText().contains(
                "control 3 Bespin locations"));
        assertTrue(virtualRando.flipText().contains(
                "opponent controls fewer than 3 Bespin locations"));
        assertTrue(virtualRando.flipBackText().contains(
                "opponent controls more Bespin locations than you"));
    }

    @Test
    public void sourceDerivedRetentionFactsDoNotConflateTheTwoPrintings() {
        ObjectiveAnalyzer classic =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        ObjectiveAnalyzer virtual =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();

        analyze(classic, "109_12",
                new Card109_012(), new Card109_012_BACK());
        analyze(virtual, "226_12",
                new Card226_012(), new Card226_012_BACK());

        assertTrue(classic.isRequiredCardForFlip("Dark Deal"));
        assertFalse(virtual.isRequiredCardForFlip("Dark Deal"));
        assertTrue(classic.isPullableCard("Bespin: Cloud City"));
        assertTrue(classic.isPullableCard("Cloud City Occupation"));
        assertFalse(classic.isPullableCard("Vader's Bounty"));
        assertTrue(virtual.isPullableCard("Bespin"));
        assertTrue(virtual.isPullableCard("Vader's Bounty"));
        assertFalse(virtual.isPullableCard("Cloud City Occupation"));
    }

    @Test
    public void exactBlueprintLookupSeparatesProfilesThatShareTheirPrintedTitle()
            throws Exception {
        ObjectiveAnalyzer analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        Method findProfile = ObjectiveAnalyzer.class.getDeclaredMethod(
                "findProfile", String.class, String.class);
        findProfile.setAccessible(true);

        String sharedTitle = new Card109_012().getTitle();
        assertEquals(sharedTitle, new Card226_012().getTitle());

        Object classic = findProfile.invoke(
                analyzer, "109_12", sharedTitle);
        Object virtual = findProfile.invoke(
                analyzer, "226_12", sharedTitle);

        assertEquals(List.of("109_12", "109_12_BACK"),
                field(classic, "blueprintIds"));
        assertEquals(List.of("226_12", "226_12_BACK"),
                field(virtual, "blueprintIds"));
        assertFalse(Boolean.TRUE.equals(
                field(classic, "loaderEnabled")));
        assertFalse(Boolean.TRUE.equals(
                field(virtual, "loaderEnabled")));
    }

    private static Snapshot analyze(
            ObjectiveAnalyzer analyzer,
            String blueprintId,
            SwccgCardBlueprint front,
            SwccgCardBlueprint back) {
        PhysicalCard objective = objectiveCard(
                front, back, blueprintId);
        GameState gameState = mock(GameState.class);
        when(gameState.getAllPermanentCards())
                .thenReturn(List.of(objective));
        SwccgGame game = mock(SwccgGame.class);
        when(game.getGameState()).thenReturn(gameState);

        analyzer.analyze(game, PLAYER_ID, Side.DARK);
        return new Snapshot(
                analyzer.getObjectiveBlueprintId(),
                analyzer.isTdigwatt(),
                analyzer.isTdigwattPreFlip(),
                analyzer.requiresOccupy(),
                analyzer.requiresControl(),
                analyzer.objectiveForbidsDeployingExecutor(),
                analyzer.isHydratedFromJson(),
                analyzer.getRequiredCardsOnTable(),
                analyzer.getPullableCards(),
                analyzer.getFlipConditionText(),
                analyzer.getFlipBackConditionText());
    }

    private static PhysicalCard objectiveCard(
            SwccgCardBlueprint front,
            SwccgCardBlueprint back,
            String blueprintId) {
        return (PhysicalCard) Proxy.newProxyInstance(
                PhysicalCard.class.getClassLoader(),
                new Class<?>[]{PhysicalCard.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getBlueprint" -> front;
                    case "getOtherSideBlueprint" -> back;
                    case "getBlueprintId" -> blueprintId;
                    case "getOwner" -> PLAYER_ID;
                    case "getZone" -> Zone.SIDE_OF_TABLE;
                    case "isFlipped" -> false;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object field(Object target, String name)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
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

    private record Snapshot(
            String blueprintId,
            boolean tdigwatt,
            boolean preFlip,
            boolean requiresOccupy,
            boolean requiresControl,
            boolean forbidsExecutor,
            boolean jsonHydrated,
            Set<String> requiredCards,
            Set<String> pullableCards,
            String flipText,
            String flipBackText) {
    }
}
