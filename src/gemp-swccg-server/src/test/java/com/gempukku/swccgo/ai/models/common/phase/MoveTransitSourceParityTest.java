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

public class MoveTransitSourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void transitRulesHaveOneSharedOwner() throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveTransitPolicy.pilotLock("));
        assertEquals(1, countOccurrences(
                move, "MoveTransitPolicy.movementTypes("));
        assertFalse(move.contains(
                "if (cardToMove != null && cardToMove.isPilotOf())"));
        assertFalse(move.contains(
                "if (actionLower.contains(\"shuttle\") || actionLower.contains(\"transport\"))"));
        assertTrue(policy.contains("public static PilotLock pilotLock("));
        assertTrue(policy.contains(
                "public static MovementTypes movementTypes("));
    }

    @Test
    public void adapterRetainsScoreAndLoggingOwnership() throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains(
                "action.addReasoning(pilotLock.contribution().reason(),"));
        assertTrue(move.contains(
                "action.addReasoning(defensiveShuttle.contribution().reason(),"));
        assertTrue(move.contains(
                "movementTypes.dockingBayTransit().reason()"));
        assertTrue(move.contains("movementTypes.takeOff().reason()"));
        assertTrue(move.contains(
                "V25 PILOT LOCK: {} is piloting {}"));
        assertTrue(move.contains(
                "V25 Defensive shuttle to {}"));
        assertTrue(move.contains(
                "V25 Shuttle without defensive need"));
    }

    @Test
    public void adapterCallsRemainInLegacyOrder() throws IOException {
        String move = evaluatorSource("rando");
        int deathStar = move.indexOf("// === V79 (Steve");
        int pilot = move.indexOf("MoveTransitPolicy.pilotLock(", deathStar);
        int lando = move.indexOf("// === V47: LANDO", pilot);
        int movementTypes = move.indexOf(
                "MoveTransitPolicy.movementTypes(", lando);
        int docking = move.indexOf(
                "movementTypes.dockingBayTransit().applies()", movementTypes);
        int takeOff = move.indexOf(
                "movementTypes.takeOff().applies()", docking);
        int landing = move.indexOf("MoveLandingPolicy.evaluate(", takeOff);

        assertTrue(deathStar >= 0);
        assertTrue(pilot > deathStar);
        assertTrue(lando > pilot);
        assertTrue(movementTypes > lando);
        assertTrue(docking > movementTypes);
        assertTrue(takeOff > docking);
        assertTrue(landing > takeOff);
    }

    @Test
    public void policyPreservesFirstMatchAndPrintedPowerGates()
            throws IOException {
        String policy = policySource();
        int locations = policy.indexOf(
                "for (PhysicalCard location : gameState.getLocationsInOrder())");
        int powerAttribute = policy.indexOf(
                "!blueprint.hasPowerAttribute()", locations);
        int threshold = policy.indexOf(
                "ourPower > 0 && theirPower >= ourPower * 2", powerAttribute);
        int firstMatchBreak = policy.indexOf("break;", threshold);

        assertTrue(locations >= 0);
        assertTrue(powerAttribute > locations);
        assertTrue(threshold > powerAttribute);
        assertTrue(firstMatchBreak > threshold);
    }

    @Test
    public void policyContainsNoScoringTransportOrEngineMetadata()
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
                .resolve("MoveTransitPolicy.java"));
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
