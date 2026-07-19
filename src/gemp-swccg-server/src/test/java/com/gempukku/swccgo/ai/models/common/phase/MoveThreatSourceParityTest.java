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

public class MoveThreatSourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void threatRulesHaveOneSharedOwner() throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveThreatPolicy.evaluate("));
        assertFalse(move.contains("private enum ThreatLevel"));
        assertFalse(move.contains("calculateThreatLevel("));
        assertTrue(policy.contains("public enum ThreatLevel"));
        assertTrue(policy.contains("public static Evaluation evaluate("));
        assertTrue(policy.contains("-1500.0f"));
        assertTrue(policy.contains("-500.0f"));
        assertTrue(policy.contains("20.0f"));
        assertTrue(policy.contains("150.0f"));
    }

    @Test
    public void adapterRetainsBoardLadderAndLoggingOwnership()
            throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains(
                "List<PhysicalCard> cardsAtLocation = gameState.getCardsAtLocation(location)"));
        assertTrue(move.contains(
                "action.addReasoning(threat.reason(), threat.delta())"));
        assertTrue(move.contains(
                "ladderClaimR3(\"THREAT RETREAT\")"));
        assertTrue(move.contains(
                "[MoveEvaluator] RETREAT recommended - outmatched by {}"));
        assertTrue(move.contains(
                "V37.1 STAY AND CRUSH at {}: power +{}"));
        assertTrue(move.contains(
                "V37.1 STAY AND FIGHT at {}: power +{}"));
    }

    @Test
    public void policyCallRemainsAfterPowerScanAndBeforeV85()
            throws IOException {
        String move = evaluatorSource("rando");
        int rankMethod = move.indexOf("private void rankMoveFromLocation(");
        int sourceScan = move.indexOf(
                "for (PhysicalCard card : cardsAtLocation)", rankMethod);
        int policyCall = move.indexOf(
                "MoveThreatPolicy.evaluate(", sourceScan);
        int v85 = move.indexOf("// === V85", policyCall);

        assertTrue(rankMethod >= 0);
        assertTrue(sourceScan > rankMethod);
        assertTrue(policyCall > sourceScan);
        assertTrue(v85 > policyCall);
    }

    @Test
    public void protectedMoveRulesRemainAdapterOwned() throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains(
                "oppWeaponBonusAt(gameState, currentLoc, v47Opp)"));
        assertTrue(move.contains(
                "v169EndangeredMover = v169Their > v169Our"));
        assertTrue(move.contains("// === V53b: HIDDEN PATH"));
        assertTrue(move.contains("// V60 FIX:"));
        assertTrue(move.contains("ladderFinalize(action)"));
        assertTrue(move.indexOf("ladderFinalize(action)")
                < move.indexOf("actions.add(action)"));
    }

    @Test
    public void policyContainsNoEngineDecisionMetadata()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "PolicyOperation", "PolicyResult", "DecisionContext",
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
                .resolve("MoveThreatPolicy.java"));
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
