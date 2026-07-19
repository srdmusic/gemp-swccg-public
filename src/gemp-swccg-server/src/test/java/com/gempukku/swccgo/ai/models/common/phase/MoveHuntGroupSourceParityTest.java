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

public class MoveHuntGroupSourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void huntGroupHasOneSharedOwner() throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveHuntGroupPolicy.evaluate("));
        assertTrue(policy.contains(
                "public static Evaluation evaluate("));
        int regionStart = move.indexOf(
                "// === V29.13: HUNT DOWN");
        int regionEnd = move.indexOf("// V22.5: PRE-FLIP", regionStart);
        String region = move.substring(regionStart, regionEnd);
        assertFalse(region.contains("String movingCardTitle ="));
        assertFalse(region.contains("float bestAllyPower = 0"));
        assertFalse(region.contains("boolean huntingOpponents = false"));
        assertFalse(region.contains("PhysicalCard vaderLoc = null"));
    }

    @Test
    public void adaptersRetainObjectiveScoreLogCatchAndLadderOwnership()
            throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains(
                "huntMoveGroupAnalyzer.isHuntDownV()"));
        assertTrue(move.contains(
                "huntGroup.contribution().reason()"));
        assertTrue(move.contains(
                "V29.13 HUNT GROUP: Vader moving to allies"));
        assertTrue(move.contains(
                "V29.13 HUNT SCATTER: Vader moving away"));
        assertTrue(move.contains(
                "ladderClaimR2(\"V29.13 HUNT GROUP MOVE (Vader→allies)\""));
        assertTrue(move.contains(
                "ladderClaimR2(\"V29.13 HUNT GROUP MOVE (→Vader)\""));
        assertTrue(move.contains(
                "logger.debug(\"V29.13 HUNT GROUP MOVE: Error:"));

        int policyCall = move.indexOf("MoveHuntGroupPolicy.evaluate(");
        int scoreApplication = move.indexOf(
                "action.addReasoning(", policyCall);
        int positiveLog = move.indexOf(
                "V29.13 HUNT GROUP: Vader moving to allies", scoreApplication);
        int positiveClaim = move.indexOf(
                "ladderClaimR2(\"V29.13 HUNT GROUP MOVE (Vader→allies)\"",
                positiveLog);
        int adapterCatch = move.indexOf(
                "V29.13 HUNT GROUP MOVE: Error:", positiveClaim);
        assertTrue(policyCall >= 0);
        assertTrue(scoreApplication > policyCall);
        assertTrue(positiveLog > scoreApplication);
        assertTrue(positiveClaim > positiveLog);
        assertTrue(adapterCatch > positiveClaim);
    }

    @Test
    public void callRemainsBetweenV137AndPreFlipConsolidation()
            throws IOException {
        String move = evaluatorSource("rando");
        int antiSolo = move.indexOf("V137 ANTI-SOLO BG:");
        int huntGroup = move.indexOf("MoveHuntGroupPolicy.evaluate(",
                antiSolo);
        int preFlip = move.indexOf("// V22.5: PRE-FLIP", huntGroup);

        assertTrue(antiSolo >= 0);
        assertTrue(huntGroup > antiSolo);
        assertTrue(preFlip > huntGroup);
    }

    @Test
    public void policyPreservesBranchesWeightsAndScanShapes()
            throws IOException {
        String policy = policySource();

        for (String branch : new String[]{
                "HUNTER_TOWARD_ALLIES", "HUNTER_AWAY_FROM_ALLIES",
                "ALLY_AWAY_FROM_HUNTER", "ALLY_TOWARD_HUNTER",
                "ALLY_ELSEWHERE"}) {
            assertTrue(branch, policy.contains(branch));
        }
        for (String delta : new String[]{
                "200.0f", "50.0f", "-200.0f",
                "-250.0f", "250.0f", "-100.0f"}) {
            assertTrue(delta, policy.contains(delta));
        }
        assertEquals(2, countOccurrences(
                policy, "gameState.getTopLocations()"));
        assertEquals(1, countOccurrences(
                policy, "gameState.getAllPermanentCards()"));
        assertTrue(policy.contains(
                "if (allyPowerHere > bestAllyPower)"));
        assertTrue(policy.contains(
                "currentLocation == hunterLocation"));
    }

    @Test
    public void protectedMoveMachineryRemainsAdapterOwned()
            throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains("// V60 FIX:"));
        assertTrue(move.contains("MovePredicates.canWinAt("));
        assertTrue(move.contains("oppWeaponBonusAt("));
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
                .resolve("MoveHuntGroupPolicy.java"));
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
