package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ObjectiveAnalyzerSharedGoldenTest {
    private static final String PLAYER_ID = "player";

    @Test
    public void bothFacadesExposeTheSharedPublicAndNestedTypeApi() throws Exception {
        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer rando =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer chosenOne =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer();

        assertEquals(ObjectiveAnalyzer.class, rando.getClass().getSuperclass());
        assertEquals(ObjectiveAnalyzer.class, chosenOne.getClass().getSuperclass());
        assertEquals(ObjectiveAnalyzer.ObjectivePlaybook.class,
                rando.getClass().getMethod("getActivePlaybook").getReturnType());
        assertEquals(String.class,
                rando.getClass().getMethod("getObjectiveBlueprintId").getReturnType());
        assertEquals(
                com.gempukku.swccgo.ai.models.common.playbook.ObjectiveProgressAssessment.class,
                rando.getClass().getMethod("assessDeployChild",
                        GameState.class, String.class,
                        PhysicalCard.class, PhysicalCard.class).getReturnType());
        Method deployAdjustments = rando.getClass().getMethod("getDeployObjectiveAdjustments",
                SwccgGame.class, GameState.class, String.class, PhysicalCard.class,
                SwccgCardBlueprint.class, String.class);
        assertEquals(List.class, deployAdjustments.getReturnType());
        ObjectiveAnalyzer.ScoreNote sampleNote = new ObjectiveAnalyzer.ScoreNote(1.0f, "sample");
        assertEquals(1.0f, sampleNote.score, 0.0f);
        assertEquals("sample", sampleNote.reason);

        ObjectiveAnalyzer.ObjectivePlaybook randoPlaybook = rando.getActivePlaybook();
        ObjectiveAnalyzer.ObjectivePlaybook chosenOnePlaybook = chosenOne.getActivePlaybook();
        assertEquals(randoPlaybook, chosenOnePlaybook);
        assertTrue(ObjectiveAnalyzer.class.getMethod("analyze", SwccgGame.class, String.class, Side.class)
                .getReturnType() == void.class);
        assertTrue(ObjectiveAnalyzer.class.getMethod("getDeployObjectiveAdjustments",
                SwccgGame.class, GameState.class, String.class, PhysicalCard.class,
                SwccgCardBlueprint.class, String.class).getReturnType().isAssignableFrom(List.class));
    }

    @Test
    public void facadeInstancesKeepMutableAnalysisStateIndependent() {
        com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer rando =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer chosenOne =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer();

        assertNotSame(rando, chosenOne);
        assertFalse(rando.isAnalyzed());
        assertFalse(chosenOne.isAnalyzed());

        analyze(rando, "12_179", "My Lord, Is That Legal?",
                "Deploy Galactic Senate. Flip this card if you have 3 senators at Galactic Senate.");

        assertTrue(rando.isAnalyzed());
        assertTrue(rando.isMyLord());
        assertFalse(chosenOne.isAnalyzed());
        assertFalse(chosenOne.isMyLord());
        assertNotNull(rando.getActivePlaybook());
        assertTrue(chosenOne.getActivePlaybook() == null);
    }

    @Test
    public void objectiveFixturesPreserveSharedIdentityAndProfileBehavior() {
        ObjectiveAnalyzer myLord = analyzed("12_179", "My Lord, Is That Legal?",
                "Deploy Galactic Senate. Flip this card if you have 3 senators at Galactic Senate.");
        ObjectiveAnalyzer invasion = analyzed("14_113", "Invasion",
                "Deploy Naboo system. Flip this card if you control Theed Palace Throne Room and Naboo system.");
        ObjectiveAnalyzer weHaveAPlan = analyzed("14_52", "We Have A Plan",
                "Flip this card if you control Theed Palace Throne Room with Amidala there.");
        ObjectiveAnalyzer endor = analyzed("8_167", "Endor Operations",
                "Deploy Endor system, Bunker and Landing Platform. Flip this card if Ominous Rumors and Establish Secret Base are both on table.");
        ObjectiveAnalyzer iwtm = analyzed("208_057", "I Want That Map",
                "Deploy Tuanul Village. Flip this card if your First Order characters control two battlegrounds.");
        ObjectiveAnalyzer hiddenPath = analyzed("226_028", "The Hidden Path",
                "Deploy Mining Village, Safehouse, Underground Corridor, and Fallen Order. Flip this card if Jedi occupy two non-Mapuzo sites.");
        ObjectiveAnalyzer tdigwatt = analyzed("109_012",
                "This Deal Is Getting Worse All The Time",
                "Deploy Bespin system. Flip this card if you occupy Bespin system.");
        ObjectiveAnalyzer tdigwattV = analyzed("226_012",
                "This Deal Is Getting Worse All The Time (V)",
                "Deploy Bespin system. Flip this card if you occupy Bespin system.");
        ObjectiveAnalyzer flippedTdigwatt = analyzed("109_012",
                "This Deal Is Getting Worse All The Time",
                "Deploy Bespin system. Flip this card if you occupy Bespin system.",
                true);
        ObjectiveAnalyzer otherBespin = analyzed("test_bespin",
                "A Different Bespin Objective",
                "Deploy Bespin system. Flip this card if you occupy Bespin system.");

        assertEquals("My Lord, Is That Legal?", myLord.getActivePlaybook().label);
        assertTrue(myLord.isMyLord());
        assertTrue(myLord.getFlipConditionText().contains("3 senators"));

        assertEquals("Invasion", invasion.getActivePlaybook().label);
        assertTrue(invasion.isInvasion());
        assertTrue(invasion.getFlipConditionText().contains("Theed Palace Throne Room"));

        assertEquals("We Have A Plan",
                weHaveAPlan.getActivePlaybook().label);
        assertTrue(weHaveAPlan.hasFlipGateActorRequirement());
        assertFalse(weHaveAPlan.hasCountedPreFlipActorRule());

        assertEquals("Endor Operations", endor.getActivePlaybook().label);
        assertTrue(endor.getRequiredCardsOnTable().contains("ominous rumors"));
        assertTrue(endor.getRequiredCardsOnTable().contains("establish secret base"));
        assertFalse(endor.hasFlipGateActorRequirement());
        assertFalse(endor.isActiveFlipGateLocationTitle("Endor: Bunker"));

        assertTrue(iwtm.isWantThatMap());
        assertTrue(iwtm.getIwtmSystemBpIds().contains("208_51"));
        assertEquals("starkiller base", iwtm.getIwtmSystemTitleFragment());
        assertEquals("the first order was just the beginning", iwtm.getIwtmPreferredStartingEffect());
        assertFalse(iwtm.isObjectiveRelevantLocation("Starkiller Base"));

        assertTrue(hiddenPath.getFlipConditionText().contains("Jedi occupy two non-Mapuzo sites"));
        assertTrue(tdigwatt.isTdigwatt());
        assertTrue(tdigwatt.isTdigwattPreFlip());
        assertTrue(tdigwattV.isTdigwatt());
        assertTrue(flippedTdigwatt.isTdigwatt());
        assertFalse(flippedTdigwatt.isTdigwattPreFlip());
        assertTrue(tdigwatt.needsBespinSystemPresence());
        assertFalse(otherBespin.isTdigwatt());
        assertTrue(otherBespin.needsBespinSystemPresence());
    }

    @Test
    public void iwtmUsesBattlegroundsForLiveRelevanceButNotSetupSystemName() {
        ObjectiveAnalyzer iwtm = analyzed("208_057", "I Want That Map",
                "Deploy Tuanul Village. Flip this card if your First Order characters control two battlegrounds.");
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = mock(GameState.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard battleground = mock(PhysicalCard.class);
        PhysicalCard nonBattlegroundSystem = mock(PhysicalCard.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(battleground.getTitle()).thenReturn("Starkiller Base: Forest");
        when(nonBattlegroundSystem.getTitle()).thenReturn("Starkiller Base");
        when(modifiers.isBattleground(gameState, battleground, null)).thenReturn(true);
        when(modifiers.isBattleground(gameState, nonBattlegroundSystem, null)).thenReturn(false);

        assertTrue(iwtm.isObjectiveRelevantLocation(battleground, game, PLAYER_ID));
        assertFalse(iwtm.isObjectiveRelevantLocation(nonBattlegroundSystem, game, PLAYER_ID));
    }

    @Test
    public void tdigwattIdentityResetsWhenADifferentObjectiveIsAnalyzed() {
        ObjectiveAnalyzer analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        analyze(analyzer, "109_012", "This Deal Is Getting Worse All The Time",
                "Deploy Bespin system. Flip this card if you occupy Bespin system.");
        assertTrue(analyzer.isTdigwatt());

        analyze(analyzer, "8_167", "Endor Operations",
                "Deploy Endor system. Flip this card if you occupy Endor system.");
        assertFalse(analyzer.isTdigwatt());
        assertFalse(analyzer.isTdigwattPreFlip());
    }

    @Test
    public void jsonRegistryRetainsCountsAndBlueprintThenTitleLookupOrder() throws Exception {
        ObjectiveAnalyzer analyzer = new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        Method profilesMethod = ObjectiveAnalyzer.class.getDeclaredMethod("profiles");
        profilesMethod.setAccessible(true);
        List<?> profiles = (List<?>) profilesMethod.invoke(analyzer);
        // Batch Seventeen (2026-07-27): the authored 601_146 profile, 58 -> 59.
        assertEquals(59, profiles.size());

        int enabled = 0;
        for (Object profile : profiles) {
            Field loaderEnabled = profile.getClass().getDeclaredField("loaderEnabled");
            loaderEnabled.setAccessible(true);
            if (Boolean.TRUE.equals(loaderEnabled.get(profile))) enabled++;
        }
        // Batch Fifteen (2026-07-27): 222_27 activated, 21 -> 22.
        // Batch Seventeen (2026-07-27): 10_26 activated and the new
        // 601_146 profile authored, 22 -> 24 enabled, 58 -> 59 profiles.
        // Batch Eighteen (2026-07-27): 110_4 and 12_180 activated,
        // 24 -> 26 enabled.
        assertEquals(26, enabled);
        assertEquals(33, profiles.size() - enabled);

        Method findProfile = ObjectiveAnalyzer.class.getDeclaredMethod("findProfile", String.class, String.class);
        findProfile.setAccessible(true);
        Object byBlueprint = findProfile.invoke(analyzer, "12_179", "unrelated title");
        Object byTitle = findProfile.invoke(analyzer, "unknown_blueprint", "The Hidden Path");
        assertEquals("My Lord, Is That Legal?", profileField(byBlueprint, "label"));
        assertEquals("The Hidden Path", profileField(byTitle, "label"));
    }

    @Test
    public void deployAdjustmentOrderAndExactReasonsMatchForBothFacades() {
        for (ObjectiveAnalyzer analyzer : List.of(
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer())) {
            GameState gameState = mock(GameState.class);
            SwccgGame game = mock(SwccgGame.class);
            ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
            PhysicalCard objective = mock(PhysicalCard.class);
            PhysicalCard senate = mock(PhysicalCard.class);
            PhysicalCard candidate = mock(PhysicalCard.class);
            SwccgCardBlueprint objectiveBlueprint = mock(SwccgCardBlueprint.class);
            SwccgCardBlueprint objectiveBack = mock(SwccgCardBlueprint.class);
            SwccgCardBlueprint candidateBlueprint = mock(SwccgCardBlueprint.class);
            SwccgCardBlueprint senateBlueprint = mock(SwccgCardBlueprint.class);

            when(game.getGameState()).thenReturn(gameState);
            when(game.getModifiersQuerying()).thenReturn(modifiers);
            when(game.getOpponent(PLAYER_ID)).thenReturn(null);
            when(gameState.getAllPermanentCards()).thenReturn(List.of(objective));
            when(gameState.getTopLocations()).thenReturn(List.of(senate));

            when(objective.getOwner()).thenReturn(PLAYER_ID);
            when(objective.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
            when(objective.getBlueprint()).thenReturn(objectiveBlueprint);
            when(objective.getOtherSideBlueprint()).thenReturn(objectiveBack);
            when(objective.getBlueprintId(true)).thenReturn("12_179");
            when(objective.isFlipped()).thenReturn(false);
            when(objectiveBlueprint.getTitle()).thenReturn("My Lord, Is That Legal?");
            when(objectiveBlueprint.getGameText()).thenReturn(
                    "Deploy Galactic Senate. Flip this card if you have 3 senators at Galactic Senate.");
            when(objectiveBlueprint.getCardCategory()).thenReturn(CardCategory.OBJECTIVE);
            when(objectiveBack.getGameText()).thenReturn("Flip this card if you do not have 3 senators at Galactic Senate.");

            when(senate.getTitle()).thenReturn("Coruscant: Galactic Senate");
            when(senate.getTitles()).thenReturn(List.of("Coruscant: Galactic Senate"));
            when(senate.isBlownAway()).thenReturn(false);
            when(senate.getBlueprint()).thenReturn(senateBlueprint);
            when(senateBlueprint.getCardSubtype()).thenReturn(CardSubtype.SITE);

            when(candidate.getTitle()).thenReturn("Test Character");
            when(candidate.getBlueprint()).thenReturn(candidateBlueprint);
            when(candidateBlueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
            when(candidateBlueprint.hasKeyword(com.gempukku.swccgo.common.Keyword.SENATOR)).thenReturn(false);
            when(candidateBlueprint.getLore()).thenReturn(null);

            analyzer.analyze(game, PLAYER_ID, Side.DARK);
            List<ObjectiveAnalyzer.ScoreNote> notes = analyzer.getDeployObjectiveAdjustments(
                    game, gameState, PLAYER_ID, candidate, candidateBlueprint,
                    "Deploy Test Character to Coruscant: Galactic Senate");

            assertEquals(2, notes.size());
            assertEquals(-2000.0f, notes.get(0).score, 0.0f);
            assertEquals("V110 MY LORD: HOLD non-senator 'Test Character' - no non-Senate site on table yet, would land at Senate",
                    notes.get(0).reason.replace('\u2014', '-'));
            assertEquals(-1500.0f, notes.get(1).score, 0.0f);
            assertEquals("V99 SENATE GUARD: non-senator 'Test Character' -> Galactic Senate (opp 0 <= my senator 0) - wasted, deploy elsewhere",
                    notes.get(1).reason.replace("\u2192", "->").replace('\u2014', '-'));
        }
    }

    @Test
    public void objectiveNeutralSenateGuardStillFiresForBothFacades() {
        for (ObjectiveAnalyzer analyzer : List.of(
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer())) {
            GameState gameState = mock(GameState.class);
            SwccgGame game = mock(SwccgGame.class);
            ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
            PhysicalCard senate = mock(PhysicalCard.class);
            PhysicalCard candidate = mock(PhysicalCard.class);
            SwccgCardBlueprint senateBlueprint = mock(SwccgCardBlueprint.class);
            SwccgCardBlueprint candidateBlueprint = mock(SwccgCardBlueprint.class);

            when(game.getModifiersQuerying()).thenReturn(modifiers);
            when(game.getOpponent(PLAYER_ID)).thenReturn(null);
            when(gameState.getTopLocations()).thenReturn(List.of(senate));
            when(gameState.getAllPermanentCards()).thenReturn(List.of());

            when(senate.getTitle()).thenReturn("Coruscant: Galactic Senate");
            when(senate.getTitles()).thenReturn(List.of("Coruscant: Galactic Senate"));
            when(senate.isBlownAway()).thenReturn(false);
            when(senate.getBlueprint()).thenReturn(senateBlueprint);
            when(senateBlueprint.getCardSubtype()).thenReturn(CardSubtype.SITE);

            when(candidate.getTitle()).thenReturn("Test Character");
            when(candidate.getBlueprint()).thenReturn(candidateBlueprint);
            when(candidateBlueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
            when(candidateBlueprint.hasKeyword(com.gempukku.swccgo.common.Keyword.SENATOR)).thenReturn(false);
            when(candidateBlueprint.getLore()).thenReturn(null);

            List<ObjectiveAnalyzer.ScoreNote> notes = analyzer.getDeployObjectiveAdjustments(
                    game, gameState, PLAYER_ID, candidate, candidateBlueprint,
                    "Deploy Test Character to Coruscant: Galactic Senate");

            assertFalse(analyzer.isAnalyzed());
            assertEquals(1, notes.size());
            assertEquals(-1500.0f, notes.get(0).score, 0.0f);
            assertEquals("V99 SENATE GUARD: non-senator 'Test Character' -> Galactic Senate (opp 0 <= my senator 0) - wasted, deploy elsewhere",
                    notes.get(0).reason.replace("\u2192", "->").replace('\u2014', '-'));
        }
    }

    private static String profileField(Object profile, String name) throws Exception {
        Field field = profile.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(profile);
    }

    private static ObjectiveAnalyzer analyzed(String blueprintId, String title, String gameText) {
        ObjectiveAnalyzer analyzer = new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        analyze(analyzer, blueprintId, title, gameText);
        return analyzer;
    }

    private static ObjectiveAnalyzer analyzed(
            String blueprintId, String title, String gameText, boolean flipped) {
        ObjectiveAnalyzer analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer();
        analyze(analyzer, blueprintId, title, gameText, flipped);
        return analyzer;
    }

    private static void analyze(ObjectiveAnalyzer analyzer, String blueprintId, String title, String gameText) {
        analyze(analyzer, blueprintId, title, gameText, false);
    }

    private static void analyze(
            ObjectiveAnalyzer analyzer, String blueprintId, String title,
            String gameText, boolean flipped) {
        SwccgCardBlueprint front = blueprint(title, gameText, CardCategory.OBJECTIVE);
        SwccgCardBlueprint back = blueprint(title + " Back", "Flip this card if the back condition is met.", CardCategory.OBJECTIVE);
        PhysicalCard objective = card(front, back, blueprintId, PLAYER_ID,
                Zone.SIDE_OF_TABLE, flipped);
        GameState gameState = mock(GameState.class);
        when(gameState.getAllPermanentCards()).thenReturn(List.of(objective));
        SwccgGame game = mock(SwccgGame.class);
        when(game.getGameState()).thenReturn(gameState);
        analyzer.analyze(game, PLAYER_ID, Side.DARK);
    }

    private static PhysicalCard card(SwccgCardBlueprint current, SwccgCardBlueprint opposite,
                                     String blueprintId, String owner, Zone zone, boolean flipped) {
        return (PhysicalCard) Proxy.newProxyInstance(
                PhysicalCard.class.getClassLoader(), new Class<?>[]{PhysicalCard.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getBlueprint": return current;
                        case "getOtherSideBlueprint": return opposite;
                        case "getBlueprintId": return blueprintId;
                        case "getOwner": return owner;
                        case "getZone": return zone;
                        case "isFlipped": return flipped;
                        default: return defaultValue(method.getReturnType());
                    }
                });
    }

    private static SwccgCardBlueprint blueprint(String title, String gameText, CardCategory category) {
        return (SwccgCardBlueprint) Proxy.newProxyInstance(
                SwccgCardBlueprint.class.getClassLoader(), new Class<?>[]{SwccgCardBlueprint.class},
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
}
