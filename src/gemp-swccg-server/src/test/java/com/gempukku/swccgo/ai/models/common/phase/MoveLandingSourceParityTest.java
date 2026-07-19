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

public class MoveLandingSourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void landingAnalysisHasOneSharedOwner() throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveLandingPolicy.evaluate("));
        assertFalse(move.contains("private void handleLandAction("));
        assertTrue(policy.contains("public static Evaluation evaluate("));
        assertTrue(policy.contains("Filters.aboard(card)"));
    }

    @Test
    public void adapterRetainsLadderAndLoggingOwnership() throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains("ladderVetoHard = true;"));
        assertTrue(move.contains(
                "ladderVetoHardReason = landing.reason();"));
        assertTrue(move.contains(
                "action.addReasoning(landing.reason(), landing.delta());"));
        assertTrue(move.contains(
                "[MoveEvaluator] V67f1: {} actual passengers aboard"));
        assertTrue(move.contains(
                "[MoveEvaluator] V49 LADDER VETO:"));
    }

    @Test
    public void policyPreservesLegacyOrderingAndBranches() throws IOException {
        String policy = policySource();
        int subtype = policy.indexOf(
                "if (subtype == CardSubtype.STARFIGHTER)");
        int passengerScan = policy.indexOf(
                "if (isStarship && !isStarfighter)", subtype);
        int nameFallback = policy.indexOf(
                "if (!isStarfighter && !isStarship)", passengerScan);
        int hardVeto = policy.indexOf(
                "if (isStarship && !hasPassengers)", nameFallback);
        int starfighterPenalty = policy.indexOf(
                "else if (isStarfighter)", hardVeto);

        assertTrue(subtype >= 0);
        assertTrue(passengerScan > subtype);
        assertTrue(nameFallback > passengerScan);
        assertTrue(hardVeto > nameFallback);
        assertTrue(starfighterPenalty > hardVeto);
    }

    @Test
    public void landingCallRemainsAtLegacyAdditivePosition()
            throws IOException {
        String move = evaluatorSource("rando");
        int takeOff = move.indexOf(
                "action.addReasoning(\"Take off (space deployment)\", 10.0f);");
        int landing = move.indexOf("MoveLandingPolicy.evaluate(", takeOff);
        int movePhase = move.indexOf(
                "if (context.getPhase() == Phase.MOVE)", landing);

        assertTrue(takeOff >= 0);
        assertTrue(landing > takeOff);
        assertTrue(movePhase > landing);
    }

    @Test
    public void policyContainsNoScoringTransportOrEngineMetadata()
            throws IOException {
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
                .resolve("MoveLandingPolicy.java"));
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
