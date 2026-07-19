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
        assertEquals(1, countOccurrences(
                move, "MoveTransitPolicy.hiddenPathTransit("));
        assertFalse(move.contains(
                "if (cardToMove != null && cardToMove.isPilotOf())"));
        assertFalse(move.contains(
                "if (actionLower.contains(\"shuttle\") || actionLower.contains(\"transport\"))"));
        assertTrue(policy.contains("public static PilotLock pilotLock("));
        assertTrue(policy.contains(
                "public static MovementTypes movementTypes("));
        assertTrue(policy.contains(
                "public static HiddenPathTransit hiddenPathTransit("));

        int hiddenPathStart = move.indexOf(
                "// === V53b: HIDDEN PATH MANDATORY JEDI TRANSIT ===");
        int hiddenPathEnd = move.indexOf(
                "// T4.1 (2026-07-06): LADDER FINALIZER", hiddenPathStart);
        String hiddenPathRegion = move.substring(
                hiddenPathStart, hiddenPathEnd);
        assertFalse(hiddenPathRegion.contains(
                "PhysicalCard srcLoc = cardToMove.getAtLocation()"));
        assertFalse(hiddenPathRegion.contains(
                "srcName.contains(\"safehouse\")"));
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
        assertTrue(move.contains(
                "hpMoveAnalyzer != null && hpMoveAnalyzer.isAnalyzed()"));
        assertTrue(move.contains(
                "hiddenPath.contribution().reason()"));
        assertTrue(move.contains("hiddenPath.hardVeto()"));
        assertTrue(move.contains(
                "ladderClaimR4Transit(hiddenPath.claimIdentity())"));
        assertTrue(move.contains(
                "V53b HIDDEN PATH: {} MUST landspeed"));
        assertTrue(move.contains(
                "V60 HIDDEN PATH: {} BLOCKED landspeed"));
        assertTrue(move.contains(
                "V53b HIDDEN PATH: {} leaving Mapuzo"));
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
        int spy = move.indexOf("MoveSpyFollowPolicy.evaluate(", landing);
        int hiddenPath = move.indexOf(
                "MoveTransitPolicy.hiddenPathTransit(", spy);
        int hiddenPathScore = move.indexOf(
                "action.addReasoning(", hiddenPath);
        int hiddenPathVeto = move.indexOf(
                "if (hiddenPath.hardVeto())", hiddenPathScore);
        int hiddenPathClaim = move.indexOf(
                "ladderClaimR4Transit(hiddenPath.claimIdentity())",
                hiddenPathVeto);
        int hiddenPathLog = move.indexOf(
                "V53b HIDDEN PATH: {} MUST landspeed", hiddenPathClaim);
        int finalizer = move.indexOf("ladderFinalize(action)", hiddenPathLog);

        assertTrue(deathStar >= 0);
        assertTrue(pilot > deathStar);
        assertTrue(lando > pilot);
        assertTrue(movementTypes > lando);
        assertTrue(docking > movementTypes);
        assertTrue(takeOff > docking);
        assertTrue(landing > takeOff);
        assertTrue(spy > landing);
        assertTrue(hiddenPath > spy);
        assertTrue(hiddenPathScore > hiddenPath);
        assertTrue(hiddenPathVeto > hiddenPathScore);
        assertTrue(hiddenPathClaim > hiddenPathVeto);
        assertTrue(hiddenPathLog > hiddenPathClaim);
        assertTrue(finalizer > hiddenPathLog);
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
    public void policyPreservesHiddenPathBranchOrderAndExactWeights()
            throws IOException {
        String policy = policySource();
        int objectiveGate = policy.indexOf(
                ".contains(\"hidden path\")");
        int sourceRead = policy.indexOf(
                "PhysicalCard sourceLocation = cardToMove.getAtLocation()",
                objectiveGate);
        int landspeed = policy.indexOf(
                "actionLower.contains(\"move using landspeed\")",
                sourceRead);
        int safehouse = policy.indexOf(
                "sourceName.contains(\"safehouse\") && landspeed",
                landspeed);
        int corridor = policy.indexOf(
                "sourceName.contains(\"underground corridor\")",
                safehouse);
        int mapuzo = policy.indexOf(
                "sourceName.contains(\"mapuzo\") && landspeed",
                corridor);

        assertTrue(objectiveGate >= 0);
        assertTrue(sourceRead > objectiveGate);
        assertTrue(landspeed > sourceRead);
        assertTrue(safehouse > landspeed);
        assertTrue(corridor > safehouse);
        assertTrue(mapuzo > corridor);
        assertEquals(2, countOccurrences(policy, "800.0f"));
        assertTrue(policy.contains("V53b SAFEHOUSE→CORRIDOR"));
        assertTrue(policy.contains("V53b MAPUZO EXIT"));
        assertTrue(policy.contains(
                "V60 HIDDEN PATH LANDSPEED BLOCK:"));
    }

    @Test
    public void positiveHiddenPathActionTextTransitRemainsSeparate()
            throws IOException {
        for (String bot : new String[]{"rando", "chosenone"}) {
            String actionText = evaluatorSource(
                    bot, "ActionTextEvaluator.java");
            assertTrue(actionText.contains(
                    "textLower.contains(\"move jedi survivor here to a site\")"));
            assertTrue(actionText.contains(
                    "V60 HIDDEN PATH TRANSIT: Move Jedi OUT of Corridor — flips objective! (R4 band)\", 20000.0f"));
            assertTrue(actionText.contains(
                    "Move Jedi transit action — tactical mobility\", 200.0f"));
        }
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
        return evaluatorSource(bot, "MoveEvaluator.java");
    }

    private static String evaluatorSource(
            String bot, String evaluator) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators").resolve(evaluator));
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
