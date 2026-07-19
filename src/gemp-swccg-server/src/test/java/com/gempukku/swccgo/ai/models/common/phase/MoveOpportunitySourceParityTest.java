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

public class MoveOpportunitySourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void opportunityAnalysisHasOneSharedOwner() throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(move, "MoveOpportunityPolicy.attack("));
        assertEquals(1, countOccurrences(move, "MoveOpportunityPolicy.spread("));
        assertFalse(move.contains("private AttackAnalysis analyzeAttackOpportunity"));
        assertFalse(move.contains("private SpreadAnalysis analyzeSpreadViability"));
        assertFalse(move.contains("private int getOpponentIcons"));
        assertFalse(move.contains("private int getMyIcons"));
        assertTrue(policy.contains("public static AttackAnalysis attack("));
        assertTrue(policy.contains("public static SpreadAnalysis spread("));
    }

    @Test
    public void adaptersRetainScoresLogsAndLadderOwnership() throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains(
                "action.addReasoning(attack.reason, attack.score);"));
        assertTrue(move.contains(
                "action.addReasoning(\"Possible attack (no drain icons)\", 15.0f);"));
        assertTrue(move.contains(
                "action.addReasoning(spread.reason, spread.score);"));
        assertTrue(move.contains(
                "action.addReasoning(\"Can't spread: \" + spread.reason, BAD_DELTA);"));
        assertTrue(move.contains(
                "isAdjacentSites(\n                            gameState, location, attack.targetLocation)"));
        assertTrue(move.contains(
                "ladderClaimR2(\"ATTACK\", attack.score, 0.0f, true);"));
        assertTrue(move.contains(
                "ladderClaimR2(\"SPREAD\", spread.score, 0.0f,"));
    }

    @Test
    public void policyCallsRemainInLegacyOrder() throws IOException {
        String move = evaluatorSource("rando");
        int attack = move.indexOf("MoveOpportunityPolicy.attack(");
        int weaponHunter = move.indexOf("// === V29.7: WEAPON HUNTER", attack);
        int spread = move.indexOf("MoveOpportunityPolicy.spread(", weaponHunter);
        int drainModifier = move.indexOf(
                "// === V29.13: FORCE DRAIN MODIFIER CHECK", spread);

        assertTrue(attack >= 0);
        assertTrue(weaponHunter > attack);
        assertTrue(spread > weaponHunter);
        assertTrue(drainModifier > spread);
    }

    @Test
    public void policyPreservesAsymmetricLegacyReads() throws IOException {
        String policy = policySource();

        assertTrue(policy.contains("if (card.isUndercover())"));
        assertEquals(1, countOccurrences(policy, "if (card.isUndercover())"));
        assertTrue(policy.contains(
                "int myIcons = ownIcons(targetLocation.getBlueprint(), mySide);"));
        assertTrue(policy.contains("if (score > bestScore)"));
    }

    @Test
    public void policyContainsNoScoringTransportOrEngineMetadata() throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "PolicyOperation", "PolicyResult",
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata"}) {
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
                .resolve("MoveOpportunityPolicy.java"));
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
