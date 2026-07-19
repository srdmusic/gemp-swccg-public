package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MoveHuntTargetSourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void huntTargetHasOneSharedOwner() throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveHuntTargetPolicy.evaluate("));
        assertTrue(policy.contains("public static Evaluation evaluate("));
        int regionStart = move.indexOf("// === V29.12: HUNT DOWN");
        int regionEnd = move.indexOf("// === V137", regionStart);
        String region = move.substring(regionStart, regionEnd);
        assertFalse(region.contains("boolean v137bIsHunter ="));
        assertFalse(region.contains("boolean vaderArmed ="));
        assertFalse(region.contains("String bestTargetLoc ="));
        assertFalse(region.contains("String bestJediLoc ="));
    }

    @Test
    public void adaptersRetainObjectiveScoreLogAndLadderOwnership()
            throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains("context.getObjectiveAnalyzer()"));
        assertTrue(move.contains("huntMoveAnalyzer.isAnalyzed()"));
        assertTrue(move.contains("huntMoveAnalyzer.isHuntDownV()"));
        assertTrue(move.contains("huntTarget.contribution().reason()"));
        assertTrue(move.contains("logger.warn(\"V35 HUNT {}:"));
        assertTrue(move.contains("ladderClaimR2(\"V35 HUNT \""));

        int policyCall = move.indexOf("MoveHuntTargetPolicy.evaluate(");
        int score = move.indexOf("action.addReasoning(", policyCall);
        int log = move.indexOf("logger.warn(\"V35 HUNT {}:", score);
        int claim = move.indexOf("ladderClaimR2(\"V35 HUNT \"", log);
        assertTrue(policyCall >= 0);
        assertTrue(score > policyCall);
        assertTrue(log > score);
        assertTrue(claim > log);
    }

    @Test
    public void callRemainsBetweenLandingSafetyAndWinnabilityGate()
            throws IOException {
        String move = evaluatorSource("rando");
        int landingSafety = move.indexOf(
                "V135 SELF-MOVE-TO-FRIEND ALONE:");
        int policyCall = move.indexOf(
                "MoveHuntTargetPolicy.evaluate(", landingSafety);
        int winnability = move.indexOf("// === V137", policyCall);

        assertTrue(landingSafety >= 0);
        assertTrue(policyCall > landingSafety);
        assertTrue(winnability > policyCall);
    }

    @Test
    public void policyPreservesWeightsStrictTiesAndPartialScan()
            throws IOException {
        String policy = policySource();

        assertTrue(policy.contains("float bonus = jediTarget ? jediBonus : 200.0f"));
        assertTrue(policy.contains("opponentPower > bestTargetPower"));
        assertTrue(policy.contains("opponentPower > bestJediPower"));
        assertTrue(policy.contains("location == currentLocation"));
        assertEquals(1, countOccurrences(
                policy, "for (PhysicalCard location : gameState.getTopLocations())"));
        assertTrue(policy.contains(
                "Preserve V35's partial-result fail-open target scan."));
    }

    @Test
    public void protectedMoveMachineryRemainsAdapterOwned()
            throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains("// V60 FIX:"));
        assertTrue(move.contains("MovePredicates.canWinAt("));
        assertTrue(move.contains("ladderFinalize(action)"));
        assertFalse(move.contains("MovePhysicalCardResolver"));
    }

    @Test
    public void policyContainsNoAdapterOrEngineDecisionTransport()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "addReasoning", "ladderClaim", "ladderVeto", "logger.",
                "ObjectiveAnalyzer", "PolicyOperation", "PolicyResult",
                "DecisionContext", "DecisionOrigin",
                "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef",
                "DeployDestinationRef", "DeployPhysicalCardRef",
                "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    private static String evaluatorSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/MoveEvaluator.java"));
    }

    private static String policySource() throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("MoveHuntTargetPolicy.java"));
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

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT")
                .lines()
                .map(line -> line.stripLeading().startsWith("//")
                        ? line.stripLeading() : line)
                .collect(Collectors.joining("\n"));
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
