package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeploySitingSourceCharacterizationTest {

    @Test
    public void deploySitingAdaptersStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando", "DeployEvaluator.java")),
                normalize(evaluatorSource("chosenone", "DeployEvaluator.java")));
        assertEquals(normalize(evaluatorSource("rando", "CardSelectionEvaluator.java")),
                normalize(evaluatorSource("chosenone", "CardSelectionEvaluator.java")));
    }

    @Test
    public void sharedPoliciesOwnExtractedSitingOperations() throws IOException {
        String deploy = evaluatorSource("rando", "DeployEvaluator.java");
        String destination = evaluatorSource("rando", "CardSelectionEvaluator.java");

        assertTrue(deploy.contains("DeploySitingPolicy.evaluateDirect("));
        assertTrue(destination.contains("DeploySitingPolicy.evaluateDestination("));
        assertTrue(destination.contains("DeployObjectiveSitingPolicy.evaluate("));
        assertTrue(destination.contains("DeployTacticalPolicy.scoreV166ContestDrain("));
        assertTrue(destination.contains("DeployTacticalPolicy.scoreV169ProtectEndangered("));
        assertTrue(destination.contains("DeployTacticalPolicy.scoreV170SpyDrainBlock("));
        assertTrue(destination.contains("DeployTacticalPolicy.scoreV171V172Contact("));
        assertTrue(destination.contains(
                "DeployFormationSitingPolicy.evaluateCommittedReinforcement("));
        assertTrue(destination.contains(
                "DeployFormationSitingPolicy.evaluateBuddyTopology("));
        for (String call : new String[]{
                "DeployFormationSitingPolicy.evaluateLegacySolo(",
                "DeployFormationSitingPolicy.evaluateStrongReinforcement(",
                "DeployFormationSitingPolicy.evaluateBuddySeek(",
                "DeployFormationSitingPolicy.evaluateHuntGrouping(",
                "DeployFormationSitingPolicy.scoreHighDrainSite(",
                "DeployFormationSitingPolicy.scoreGoodDrainSite(",
                "DeployFormationSitingPolicy.evaluatePositiveFormation("}) {
            assertTrue(call, deploy.contains(call));
        }

        assertFalse(deploy.contains("pairedDeployPossible"));
        assertFalse(deploy.contains("V38 SOLO CAUTION: %s (power %d) solo"));
        assertFalse(deploy.contains("V35.1 HUNT GROUP+ENGAGE: Deploy %s"));
        assertFalse(deploy.contains("V51 BUDDY DESTINY: Ability %.0f"));
    }

    @Test
    public void retiredSitingBodiesCannotReenterTheDecisionTree() throws IOException {
        String destination = evaluatorSource("rando", "CardSelectionEvaluator.java");
        assertFalse(destination.contains("if (false /* V122 SUPERSEDED V136 */"));
        assertFalse(destination.contains("if (false /* V67as SUPERSEDED V136 */"));
        assertFalse(destination.contains("V75 KILL-BOX OVERRIDE:"));
        assertFalse(destination.contains("V67bj DON'T BAIT (uncommitted):"));
        assertTrue(destination.contains(
                "V67as, including nested V67br/V75/V67bj, became unreachable"));
    }

    @Test
    public void objectiveProgressFactsStayShadowOnlyAtPhysicalDeployRoutes() throws IOException {
        String deploy = evaluatorSource("rando", "DeployEvaluator.java");
        String destination = evaluatorSource("rando", "CardSelectionEvaluator.java");
        String actionText = evaluatorSource("rando", "ActionTextEvaluator.java");

        assertEquals(0, countOccurrences(deploy, ".assessDeployChild("));
        assertEquals(1, countOccurrences(destination, ".assessDeployChild("));
        assertEquals(0, countOccurrences(actionText, ".assessDeployChild("));
        assertTrue(destination.contains("V214 DEPLOY CHILD OBJECTIVE FACTS"));
    }

    @Test
    public void sitingPoliciesContainNoEngineDecisionMetadata() throws IOException {
        String combined = commonPhaseSource("DeploySitingPolicy.java")
                + commonPhaseSource("DeployObjectiveSitingPolicy.java")
                + commonPhaseSource("DeployTacticalPolicy.java")
                + commonPhaseSource("DeployFormationSitingPolicy.java");
        for (String forbidden : new String[]{
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata"}) {
            assertFalse(forbidden, combined.contains(forbidden));
        }
    }

    private static String evaluatorSource(String bot, String fileName)
            throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators").resolve(fileName));
    }

    private static String commonPhaseSource(String fileName) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve(fileName));
    }

    private static Path mainJavaRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            Path repoLayout = cursor.resolve("src/gemp-swccg-server/src/main/java");
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
        throw new AssertionError("Could not locate gemp-swccg-server main/java");
    }

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
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
