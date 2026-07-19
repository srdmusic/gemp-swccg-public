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
        assertEquals(1, countOccurrences(
                move, "MoveThreatPolicy.flee("));
        assertFalse(move.contains("private enum ThreatLevel"));
        assertFalse(move.contains("calculateThreatLevel("));
        assertFalse(move.contains(
                "theirPower - myPower > POWER_DIFF_FOR_FLEE"));
        assertTrue(policy.contains("public enum ThreatLevel"));
        assertTrue(policy.contains("public static Evaluation evaluate("));
        assertTrue(policy.contains(
                "public static FleeEvaluation flee("));
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
        assertTrue(move.contains(
                "action.addReasoning(flee.reason(), flee.delta())"));
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
    public void fleeCallRemainsAfterV85AndBeforeAttack()
            throws IOException {
        String move = evaluatorSource("rando");
        int rankMethod = move.indexOf("private void rankMoveFromLocation(");
        int v85 = move.indexOf("// === V85", rankMethod);
        int flee = move.indexOf("MoveThreatPolicy.flee(", v85);
        int fleeScore = move.indexOf(
                "action.addReasoning(flee.reason(), flee.delta())", flee);
        int attack = move.indexOf(
                "// === OFFENSIVE ATTACK OPPORTUNITY ===", fleeScore);

        assertTrue(v85 > rankMethod);
        assertTrue(flee > v85);
        assertTrue(fleeScore > flee);
        assertTrue(attack > fleeScore);
    }

    @Test
    public void fleeOwnerPreservesStrictGateFormulaAndCap()
            throws IOException {
        String policy = policySource();
        int method = policy.indexOf(
                "public static FleeEvaluation flee(");
        int difference = policy.indexOf(
                "float disadvantage = opponentPower - ourPower", method);
        int strictGate = policy.indexOf(
                "disadvantage > powerDifferenceThreshold", difference);
        int opponentGate = policy.indexOf(
                "opponentPower > 0.0f", strictGate);
        int reason = policy.indexOf(
                "\"Outmatched by \" + (int) disadvantage + \" - should flee\"",
                opponentGate);
        int formula = policy.indexOf(
                "goodDelta * Math.min(disadvantage / 2.0f, 5.0f)",
                reason);

        assertTrue(method >= 0);
        assertTrue(difference > method);
        assertTrue(strictGate > difference);
        assertTrue(opponentGate > strictGate);
        assertTrue(reason > opponentGate);
        assertTrue(formula > reason);
    }

    @Test
    public void protectedMoveRulesRemainAdapterOwned() throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains(
                "oppWeaponBonusAt(gameState, currentLoc, v47Opp)"));
        assertTrue(move.contains(
                "v169PowerFactsAvailable = true"));
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
