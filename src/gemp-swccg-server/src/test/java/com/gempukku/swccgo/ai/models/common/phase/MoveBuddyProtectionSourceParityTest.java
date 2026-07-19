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

public class MoveBuddyProtectionSourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void v27AndV59HaveOneSharedDecisionOwner()
            throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveBuddyProtectionPolicy.hasBuddyPair("));
        assertEquals(1, countOccurrences(
                move, "MoveBuddyProtectionPolicy.needsPowerAnalysis("));
        assertEquals(1, countOccurrences(
                move, "MoveBuddyProtectionPolicy.evaluate("));
        assertFalse(move.contains("boolean doomed = enemyThreat"));
        assertFalse(move.contains("float buddyPenalty = -150.0f"));
        assertTrue(policy.contains("Branch.DOOMED_ESCAPE"));
        assertTrue(policy.contains("200.0f"));
        assertTrue(policy.contains("-150.0f"));
        assertTrue(policy.contains("-250.0f"));
        assertTrue(policy.contains("-400.0f"));
    }

    @Test
    public void adapterRetainsCardBlueprintAndPowerReads()
            throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains(
                "gameState.getCardsAtLocation(currentLocation)"));
        assertTrue(move.contains("playerId.equals(card.getOwner())"));
        assertTrue(move.contains(
                "CardCategory.CHARACTER"));
        assertTrue(move.contains("c != cardToMove"));
        assertTrue(move.contains("allyBp.hasPowerAttribute()"));
        assertTrue(move.contains("allyBp.getPower()"));
        assertTrue(move.contains("allyBp.hasAbilityAttribute()"));
        assertTrue(move.contains("allyBp.getAbility()"));
        assertTrue(move.contains("game.getOpponent(playerId)"));
        assertEquals(2, countOccurrences(
                buddyBlock(move), "getTotalPowerAtLocation("));
        assertTrue(move.contains(
                "RandoConfig.MIN_SOLO_DEPLOY_POWER"));
    }

    @Test
    public void adapterPreservesReadDecisionApplyAndLogOrder()
            throws IOException {
        String move = evaluatorSource("rando");
        int start = move.indexOf("// === V27: BUDDY PROTECTION");
        int cards = move.indexOf(
                "gameState.getCardsAtLocation(currentLocation)", start);
        int pair = move.indexOf(
                "MoveBuddyProtectionPolicy.hasBuddyPair(", cards);
        int ally = move.indexOf("remainingAlly = c", pair);
        int allyPower = move.indexOf("allyBp.getPower()", ally);
        int allyAbility = move.indexOf("allyBp.getAbility()", allyPower);
        int opponent = move.indexOf("game.getOpponent(playerId)", allyAbility);
        int theirPower = move.indexOf(
                "getTotalPowerAtLocation(", opponent);
        int needs = move.indexOf(
                "MoveBuddyProtectionPolicy.needsPowerAnalysis(", theirPower);
        int ourPower = move.indexOf(
                "getTotalPowerAtLocation(", needs);
        int decision = move.indexOf(
                "MoveBuddyProtectionPolicy.evaluate(", ourPower);
        int doomed = move.indexOf(
                "MoveBuddyProtectionPolicy.Branch.DOOMED_ESCAPE", decision);
        int doomedApply = move.indexOf("action.addReasoning(", doomed);
        int claim = move.indexOf(
                "ladderClaimR3(\"V59 DOOMED ESCAPE\")", doomedApply);
        int doomedLog = move.indexOf("V59 DOOMED: {} at {}", claim);
        int protect = move.indexOf(
                "MoveBuddyProtectionPolicy.Branch.BUDDY_PROTECT", doomedLog);
        int protectApply = move.indexOf("action.addReasoning(", protect);
        int protectLog = move.indexOf(
                "V27 BUDDY PROTECT: {} moving from {}", protectApply);
        int nextRule = move.indexOf("// === V32: ABILITY", protectLog);

        assertTrue(start >= 0);
        assertTrue(cards > start);
        assertTrue(pair > cards);
        assertTrue(ally > pair);
        assertTrue(allyPower > ally);
        assertTrue(allyAbility > allyPower);
        assertTrue(opponent > allyAbility);
        assertTrue(theirPower > opponent);
        assertTrue(needs > theirPower);
        assertTrue(ourPower > needs);
        assertTrue(decision > ourPower);
        assertTrue(doomed > decision);
        assertTrue(doomedApply > doomed);
        assertTrue(claim > doomedApply);
        assertTrue(doomedLog > claim);
        assertTrue(protect > doomedLog);
        assertTrue(protectApply > protect);
        assertTrue(protectLog > protectApply);
        assertTrue(nextRule > protectLog);
    }

    @Test
    public void expensiveOurPowerReadRemainsInsideNeedGate()
            throws IOException {
        String move = evaluatorSource("rando");
        String block = buddyBlock(move);
        int theirPower = block.indexOf("float theirPowerHere = 0");
        int needs = block.indexOf(
                "MoveBuddyProtectionPolicy.needsPowerAnalysis(", theirPower);
        int ourPower = block.indexOf("float ourPowerHere = 0", needs);

        assertTrue(theirPower >= 0);
        assertTrue(needs > theirPower);
        assertTrue(ourPower > needs);
    }

    @Test
    public void policyContainsNoContextEngineOrDecisionTransport()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "DecisionContext", "GameState", "SwccgGame",
                "PhysicalCard", "SwccgCardBlueprint", "EvaluatedAction",
                "RandoConfig", "addReasoning", "logger", "ladder",
                "PolicyOperation", "PolicyResult", "DecisionOrigin",
                "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef",
                "DeployDestinationRef", "DeployPhysicalCardRef",
                "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    private static String buddyBlock(String move) {
        int start = move.indexOf("// === V27: BUDDY PROTECTION");
        int end = move.indexOf("// === V32: ABILITY", start);
        return move.substring(start, end);
    }

    private static String evaluatorSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators")
                .resolve("MoveEvaluator.java"));
    }

    private static String policySource() throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("MoveBuddyProtectionPolicy.java"));
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
