package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RetiredScaffoldingSourceOwnershipTest {

    @Test
    public void disabledChaosLaneIsCommentOnlyAndRouteVocabularyIsStable() throws IOException {
        String rando = botSource("rando", "RandoCalAi.java");
        String chosen = botSource("chosenone", "TheChosenOneAi.java");
        String configs = botSource("rando", "RandoConfig.java")
                + botSource("chosenone", "RandoConfig.java");
        String route = commonSource("trace/TraceRoute.java");

        for (String source : new String[] {rando, chosen}) {
            assertTrue(source.contains("V295: the disabled 0% chaos bypass was retired"));
            assertTrue(source.contains("String evaluatorResult = tryEvaluators("));
            assertCommentOnly(source, "shouldApplyChaos");
            assertCommentOnly(source, "Chaos mode: selecting random action");
            assertCommentOnly(source, "private Random random");
            assertCommentOnly(source, "import java.util.Random");
            assertCommentOnly(source, "TraceRoute.CHAOS_FALLBACK");
        }
        assertCommentOnly(configs, "CHAOS_PERCENT");
        assertTrue(route.contains("V295 RETIRED: route vocabulary retained"));
        assertTrue(route.contains("CHAOS_FALLBACK,"));
    }

    @Test
    public void deadObjectiveHandlerWiringIsCommentOnlyButHistoricalSourceRemains() throws IOException {
        for (String bot : new String[] {"rando", "chosenone"}) {
            String ai = botSource(bot, bot.equals("rando")
                    ? "RandoCalAi.java" : "TheChosenOneAi.java");
            String context = botSource(bot, "evaluators/DecisionContext.java");
            String historical = botSource(bot, "strategy/ObjectiveHandler.java");

            assertCommentOnly(ai, "ObjectiveHandler");
            assertCommentOnly(ai, "objectiveHandler");
            assertCommentOnly(context, "ObjectiveHandler");
            assertCommentOnly(context, "objectiveHandler");
            assertTrue(historical.contains("DEAD CODE"));
            assertTrue(historical.contains("V295 retired its inert"));
            assertTrue(historical.contains("class ObjectiveHandler"));
        }
    }

    @Test
    public void onlyProvenUnusedEvaluatorRngFieldsAreCommentedOut() throws IOException {
        for (String evaluator : new String[] {
                "evaluators/CardSelectionEvaluator.java",
                "evaluators/CombinedEvaluator.java"}) {
            String rando = botSource("rando", evaluator);
            String chosen = botSource("chosenone", evaluator);
            assertEquals(normalize(rando), normalize(chosen));
            assertCommentOnly(rando, "import java.util.Random");
            assertCommentOnly(rando, "Random random");
            assertCommentOnly(chosen, "import java.util.Random");
            assertCommentOnly(chosen, "Random random");
        }

        for (String liveOwner : new String[] {
                "AstrogatorPersonality.java",
                "HolidayOverlay.java",
                "evaluators/BattlePredictor.java"}) {
            for (String bot : new String[] {"rando", "chosenone"}) {
                String source = botSource(bot, liveOwner);
                assertTrue(liveOwner, source.contains("import java.util.Random"));
                assertTrue(liveOwner, source.contains("random"));
            }
        }
    }

    @Test
    public void liveBlockedResponseProtectionRemainsIntact() throws IOException {
        for (String bot : new String[] {"rando", "chosenone"}) {
            String tracker = botSource(bot, "DecisionTracker.java");
            String context = botSource(bot, "evaluators/DecisionContext.java");
            String move = botSource(bot, "evaluators/MoveEvaluator.java");

            assertTrue(tracker.contains("public Set<String> getBlockedResponses("));
            assertTrue(tracker.contains("turnBlockedActions.computeIfAbsent("));
            assertTrue(context.contains("private Set<String> blockedResponses"));
            assertTrue(context.contains("public void setBlockedResponses("));
            assertTrue(move.contains("MoveBlockedResponsePolicy.classify("));
        }
    }

    @Test
    public void retiredPacketContainsNoEngineMetadataDependencies() throws IOException {
        String changedProduction = commonSource("trace/TraceRoute.java");
        for (String bot : new String[] {"rando", "chosenone"}) {
            changedProduction += botSource(bot, bot.equals("rando")
                    ? "RandoCalAi.java" : "TheChosenOneAi.java");
            changedProduction += botSource(bot, "RandoConfig.java");
            changedProduction += botSource(bot, "evaluators/CardSelectionEvaluator.java");
            changedProduction += botSource(bot, "evaluators/CombinedEvaluator.java");
            changedProduction += botSource(bot, "evaluators/DecisionContext.java");
            changedProduction += botSource(bot, "strategy/ObjectiveHandler.java");
        }

        for (String forbidden : new String[] {
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata",
                "getOtherSideBlueprintId", "setDecisionOrigin",
                "initializeMoveAction", "MoveKind", "MoveEngineActionMetadata"}) {
            assertFalse(forbidden, changedProduction.contains(forbidden));
        }
    }

    private static String botSource(String bot, String file) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve(file));
    }

    private static String commonSource(String file) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common").resolve(file));
    }

    private static Path mainJavaRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            Path repoLayout = cursor.resolve("src/gemp-swccg-server/src/main/java");
            if (Files.isDirectory(repoLayout)) {
                return repoLayout;
            }
            Path moduleLayout = cursor.resolve("src/main/java");
            if (Files.isDirectory(moduleLayout.resolve("com/gempukku/swccgo/ai/models"))) {
                return moduleLayout;
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError("Could not locate gemp-swccg-server main/java");
    }

    private static String normalize(String source) {
        return source.replace("ai.models.rando", "ai.models.BOT")
                .replace("ai.models.chosenone", "ai.models.BOT")
                .replace("Rando", "Robot")
                .replace("ChosenOne", "Robot");
    }

    private static void assertCommentOnly(String source, String symbol) {
        int matches = 0;
        for (String line : source.lines().toList()) {
            if (line.contains(symbol)) {
                String stripped = line.stripLeading();
                assertTrue(symbol + " must remain comment-only",
                        stripped.startsWith("//") || stripped.startsWith("/**")
                                || stripped.startsWith("*"));
                matches++;
            }
        }
        assertTrue(symbol + " retirement marker missing", matches > 0);
    }
}
