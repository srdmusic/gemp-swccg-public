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

public class MoveSpyFollowSourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void spyFollowHasOneSharedOwner() throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveSpyFollowPolicy.evaluate("));
        assertTrue(policy.contains("public static Evaluation evaluate("));
        int start = move.indexOf("// === V53: SPY FOLLOW");
        int end = move.indexOf("// === V53b:", start);
        String region = move.substring(start, end);
        assertFalse(region.contains("float oppPowerHere ="));
        assertFalse(region.contains("boolean destHasOpponent ="));
        assertFalse(region.contains("for (PhysicalCard destLoc"));
    }

    @Test
    public void opponentSpyOnlyDestinationUsesSharedFactsAndKeepsLadderOwnership()
            throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveSpyFollowPolicy.opponentSpyOnlyDestination("));
        assertEquals(1, countOccurrences(
                move, "MoveSpyFollowPolicy.resolveDestination("));
        assertTrue(move.contains("ladderSpyBlockedDestination = true"));
        assertTrue(move.contains("if (ladderSpyBlockedDestination)"));
        assertTrue(policy.contains(
                "public static Contribution opponentSpyOnlyDestination("));
        assertTrue(policy.contains("-1500.0f, false"));
    }

    @Test
    public void adapterRetainsGateScoreLogCatchAndLadderOwnership()
            throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains(
                "cardToMove != null && cardToMove.isUndercover()"));
        assertTrue(move.contains("String spyPid = context.getPlayerId()"));
        assertTrue(move.contains("spyFollow.contribution().reason()"));
        assertTrue(move.contains("V53 SPY FOLLOW: {} following"));
        assertTrue(move.contains("V53 SPY STAY: {} trying"));
        assertTrue(move.contains("V53 SPY REPOSITION: {} moving"));
        assertTrue(move.contains("V53 SPY FOLLOW: Error:"));
        assertTrue(move.contains("spyFollow.contribution().claimDoctrineRank()"));
        assertTrue(move.contains("ladderClaimR2("));

        int policyCall = move.indexOf("MoveSpyFollowPolicy.evaluate(");
        int score = move.indexOf("action.addReasoning(", policyCall);
        int log = move.indexOf("V53 SPY FOLLOW: {} following", score);
        int claim = move.indexOf("ladderClaimR2(", log);
        int adapterCatch = move.indexOf("V53 SPY FOLLOW: Error:", claim);
        assertTrue(policyCall >= 0);
        assertTrue(score > policyCall);
        assertTrue(log > score);
        assertTrue(claim > log);
        assertTrue(adapterCatch > claim);
    }

    @Test
    public void callRemainsBetweenMaintenanceAndHiddenPathTransit()
            throws IOException {
        String move = evaluatorSource("rando");
        int maintenance = move.indexOf(
                "V27: Error checking maintenance during move:");
        int spy = move.indexOf(
                "MoveSpyFollowPolicy.evaluate(", maintenance);
        int hiddenPath = move.indexOf("// === V53b:", spy);

        assertTrue(maintenance >= 0);
        assertTrue(spy > maintenance);
        assertTrue(hiddenPath > spy);
    }

    @Test
    public void policyPreservesWeightsAndUnreachableOrdering()
            throws IOException {
        String policy = policySource();

        assertTrue(policy.contains("500.0f, true"));
        assertTrue(policy.contains("-300.0f, false"));
        assertTrue(policy.contains("400.0f, true"));
        int follow = policy.indexOf(
                "opponentPowerAtSource == 0.0f\n                && destinationHasOpponent");
        int stay = policy.indexOf(
                "opponentPowerAtSource > 0.0f\n                && !destinationHasOpponent", follow);
        int reposition = policy.indexOf(
                "destinationHasOpponent\n                && opponentPowerAtSource == 0.0f", stay);
        assertTrue(follow >= 0);
        assertTrue(stay > follow);
        assertTrue(reposition > stay);
    }

    @Test
    public void protectedMoveMachineryRemainsAdapterOwned()
            throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains(
                "// V60: Corridor landspeed receives a bounded -300 objective preference."));
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
                .resolve("MoveSpyFollowPolicy.java"));
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
