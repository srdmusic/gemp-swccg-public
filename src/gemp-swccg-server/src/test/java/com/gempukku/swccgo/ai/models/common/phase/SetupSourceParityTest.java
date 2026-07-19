package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SetupSourceParityTest {
    @Test
    public void cardSelectionAdaptersStayNormalizedMirrors()
            throws IOException {
        assertEquals(normalizeBot(cardSelectionSource("rando")),
                normalizeBot(cardSelectionSource("chosenone")));
    }

    @Test
    public void actionTextAdaptersStayNormalizedMirrors()
            throws IOException {
        assertEquals(normalizeBot(actionTextSource("rando")),
                normalizeBot(actionTextSource("chosenone")));
    }

    @Test
    public void directSagaInterceptorsUseOneSharedChoiceOwner()
            throws IOException {
        String rando = sagaInterceptor(aiSource(
                "rando", "RandoCalAi.java"));
        String chosen = sagaInterceptor(aiSource(
                "chosenone", "TheChosenOneAi.java"));

        assertEquals(normalizeDirectAi(rando), normalizeDirectAi(chosen));
        assertEquals(1, countOccurrences(
                rando, "SetupPolicy.chooseSaga(deckName, results)"));
        assertFalse(rando.contains("int luke = -1"));
        assertFalse(rando.contains("boolean isTfismfChoice"));
    }

    @Test
    public void setupScoresHaveOneSharedPolicyOwner()
            throws IOException {
        String card = cardSelectionSource("rando");
        String action = actionTextSource("rando");
        String policy = policySource();

        for (String call : new String[]{
                "SetupPolicy.earlyStartingEffectBan(",
                "SetupPolicy.startingInterrupt(",
                "SetupPolicy.startingLocationText(",
                "SetupPolicy.startingEffectIdentity(",
                "SetupPolicy.startingEffectText(",
                "SetupPolicy.startingEffectDeck(",
                "SetupPolicy.startingEffectObjective(",
                "SetupPolicy.reserveStartingEffect("}) {
            assertTrue(call, card.contains(call));
        }
        assertTrue(action.contains("SetupPolicy.sagaChoice("));

        assertFalse(card.contains("V126a STARTING (battle starter)"));
        assertFalse(card.contains("V187 DUPLICATE STARTING EFFECT"));
        assertFalse(card.contains("v126cROTSOnTable"));
        assertFalse(action.contains("boolean isCorrectChoice"));
        assertTrue(policy.contains("-600.0f"));
        assertTrue(policy.contains("1500.0f"));
        assertTrue(policy.contains("1000.0f"));
    }

    @Test
    public void purePolicyHasNoGameAdapterOrEngineTransportTypes()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "DecisionContext", "GameState", "SwccgGame",
                "PhysicalCard", "SwccgCardBlueprint", "EvaluatedAction",
                "RandoConfig", "TheChosenOneConfig", "addReasoning",
                "logger", "DecisionOrigin", "DecisionActionSemantic",
                "DecisionWire", "PullDeployRef", "PullPhysicalCardRef",
                "DeployDestinationRef", "DeployPhysicalCardRef",
                "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    @Test
    public void setupProductionSourcesContainNoForbiddenEngineMetadata()
            throws IOException {
        String sources = String.join("\n",
                policySource(), factsSource(),
                cardSelectionSource("rando"),
                cardSelectionSource("chosenone"),
                actionTextSource("rando"),
                actionTextSource("chosenone"),
                aiSource("rando", "RandoCalAi.java"),
                aiSource("chosenone", "TheChosenOneAi.java"));
        for (String forbidden : new String[]{
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata"}) {
            assertFalse(forbidden, sources.contains(forbidden));
        }
    }

    private static String sagaInterceptor(String source) {
        int start = source.indexOf("// V61 EPIC EVENT SAGA CHOICE");
        int result = source.indexOf(
                "return String.valueOf(saga.index());", start);
        int end = source.indexOf("\n            }", result);
        if (start < 0 || result < 0 || end < 0) {
            throw new AssertionError("Could not locate V61 saga interceptor");
        }
        return source.substring(start, end + "\n            }".length());
    }

    private static String cardSelectionSource(String bot)
            throws IOException {
        return Files.readString(botRoot(bot)
                .resolve("evaluators/CardSelectionEvaluator.java"));
    }

    private static String actionTextSource(String bot)
            throws IOException {
        return Files.readString(botRoot(bot)
                .resolve("evaluators/ActionTextEvaluator.java"));
    }

    private static String aiSource(String bot, String file)
            throws IOException {
        return Files.readString(botRoot(bot).resolve(file));
    }

    private static String policySource() throws IOException {
        return Files.readString(commonPhaseRoot().resolve("SetupPolicy.java"));
    }

    private static String factsSource() throws IOException {
        return Files.readString(commonPhaseRoot().resolve("SetupFactsReader.java"));
    }

    private static Path botRoot(String bot) {
        return mainJavaRoot().resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot);
    }

    private static Path commonPhaseRoot() {
        return mainJavaRoot().resolve(
                "com/gempukku/swccgo/ai/models/common/phase");
    }

    private static Path mainJavaRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            Path repoLayout = cursor.resolve(
                    "src/gemp-swccg-server/src/main/java");
            if (Files.isDirectory(repoLayout)) {
                return repoLayout;
            }
            Path moduleLayout = cursor.resolve("src/main/java");
            if (Files.isDirectory(moduleLayout.resolve(
                    "com/gempukku/swccgo/ai/models"))) {
                return moduleLayout;
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError(
                "Could not locate gemp-swccg-server main/java");
    }

    private static String normalizeBot(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT")
                .replace("RandoConfig", "BotConfig")
                .replace("TheChosenOneConfig", "BotConfig");
    }

    private static String normalizeDirectAi(String source) {
        return normalizeBot(source)
                .replace("RandoCalAi", "BotAi")
                .replace("TheChosenOneAi", "BotAi");
    }

    private static int countOccurrences(String source, String needle) {
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }
}
