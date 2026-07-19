package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResponsePolicySourceParityTest {
    @Test
    public void directResponseAdaptersStayMirrored() throws IOException {
        String rando = directResponseBlock(
                botSource("rando", "RandoCalAi.java"));
        String chosen = directResponseBlock(
                botSource("chosenone", "TheChosenOneAi.java"));
        assertEquals(rando, chosen);
    }

    @Test
    public void recognizedResponseShapesHaveOneSharedOwner()
            throws IOException {
        String rando = botSource("rando", "RandoCalAi.java");
        String chosen = botSource("chosenone", "TheChosenOneAi.java");
        String policy = policySource();

        for (String source : new String[]{rando, chosen}) {
            assertEquals(1, countOccurrences(source,
                    "ResponsePolicy.classify("));
            assertEquals(1, countOccurrences(source,
                    "ResponsePolicy.revertApproval("));
            assertEquals(1, countOccurrences(source,
                    "ResponsePolicy.yesNoIndexes("));
            assertEquals(1, countOccurrences(source,
                    "ResponsePolicy.shouldDeployUndercover("));
            assertEquals(1, countOccurrences(source,
                    "ResponsePolicy.scorePriorityCards("));
            assertFalse(source.contains("int yesIndex = 0"));
            assertFalse(source.contains("int v170YesIdx = 0"));
            assertFalse(source.contains("private int scorePriorityCards("));
        }

        assertTrue(policy.contains("enum Route"));
        assertTrue(policy.contains("return Route.LEGACY"));
        assertTrue(policy.contains("private static final int DAMAGE_CANCEL_SCORE = 100"));
        assertTrue(policy.contains("private static final int BARRIER_SCORE = 80"));
        assertTrue(policy.contains("private static final int SENSE_SCORE = 70"));
    }

    @Test
    public void retiredBotConfigScoresAreGone() throws IOException {
        for (String bot : new String[]{"rando", "chosenone"}) {
            String config = botSource(bot, "RandoConfig.java");
            assertFalse(config.contains("SCORE_DAMAGE_CANCEL"));
            assertFalse(config.contains("SCORE_BARRIER_USE"));
            assertFalse(config.contains("SCORE_SENSE_USE"));
        }
    }

    @Test
    public void pureResponsePolicyHasNoEngineOrGameStateSurface()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "AwaitingDecision", "DecisionContext", "GameState",
                "SwccgGame", "PhysicalCard", "EvaluatedAction",
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef",
                "DeployDestinationRef", "DeployPhysicalCardRef",
                "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    private static String directResponseBlock(String source) {
        int start = source.indexOf(
                "ResponsePolicy.Route responseRoute");
        int end = source.indexOf(
                "// V61 EPIC EVENT SAGA CHOICE", start);
        if (start < 0 || end < 0) {
            throw new AssertionError(
                    "Could not locate direct response adapter block");
        }
        return source.substring(start, end);
    }

    private static String botSource(String bot, String file)
            throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve(file));
    }

    private static String policySource() throws IOException {
        return Files.readString(mainJavaRoot().resolve(
                "com/gempukku/swccgo/ai/models/common/phase/ResponsePolicy.java"));
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
