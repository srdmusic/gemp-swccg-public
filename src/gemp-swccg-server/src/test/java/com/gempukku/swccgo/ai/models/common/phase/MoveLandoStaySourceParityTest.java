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

public class MoveLandoStaySourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void v47HasOneSharedClassificationAndDecisionOwner()
            throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveLandoStayPolicy.titleMarksLando("));
        assertEquals(1, countOccurrences(
                move, "MoveLandoStayPolicy.isCloudCitySite("));
        assertEquals(1, countOccurrences(
                move, "MoveLandoStayPolicy.evaluate("));
        assertFalse(move.contains(
                "locLower.contains(\"cloud city\")"));
        assertFalse(move.contains(
                "v47ObjectiveWantsLandoHere && v47Survivable"));
        assertTrue(policy.contains("contains(\"lando\")"));
        assertTrue(policy.contains("contains(\"cloud city\")"));
        assertTrue(policy.contains(
                "prefer staying for occupation"));
    }

    @Test
    public void adapterRetainsObjectiveAndSurvivabilityReads()
            throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains("context.getObjectiveAnalyzer()"));
        assertTrue(move.contains("v47Analyzer.isAnalyzed()"));
        assertTrue(move.contains(
                "v47Analyzer.needsBespinSystemPresence()"));
        assertTrue(move.contains(
                "v47Analyzer.isFlipBackProtectionLocation(currentLoc.getTitle())"));
        assertTrue(move.contains(
                "v47Analyzer.isObjectiveRelevantLocation(currentLoc.getTitle())"));
        assertTrue(move.contains(
                "V47 objective gate error: {}"));
        assertTrue(move.contains(
                "getTotalPowerAtLocation(\n                                gameState, currentLoc, playerId"));
        assertTrue(move.contains(
                "getTotalPowerAtLocation(\n                                gameState, currentLoc, v47Opp"));
        assertTrue(move.contains(
                "oppWeaponBonusAt(gameState, currentLoc, v47Opp)"));
        assertTrue(move.contains(
                "v47PowerDiff >= RandoConfig.BATTLE_DANGER_THRESHOLD"));
        assertTrue(move.contains(
                "V47 survivability gate error: {}"));
    }

    @Test
    public void adapterPreservesGateReadDecisionAndApplyOrder()
            throws IOException {
        String move = evaluatorSource("rando");
        int start = move.indexOf("// === V47: LANDO");
        int title = move.indexOf(
                "MoveLandoStayPolicy.titleMarksLando(", start);
        int location = move.indexOf(
                "PhysicalCard currentLoc = cardToMove.getAtLocation()", title);
        int site = move.indexOf(
                "MoveLandoStayPolicy.isCloudCitySite(", location);
        int objective = move.indexOf(
                "context.getObjectiveAnalyzer()", site);
        int power = move.indexOf(
                "getTotalPowerAtLocation(", objective);
        int weapons = move.indexOf(
                "oppWeaponBonusAt(", power);
        int decision = move.indexOf(
                "MoveLandoStayPolicy.evaluate(", weapons);
        int preferenceGate = move.indexOf(
                "if (v47Decision.applies())", decision);
        int reason = move.indexOf(
                "addObjectiveContribution(", preferenceGate);
        int appliedLog = move.indexOf(
                "V47 LANDO STAY: bounded objective preference", reason);
        int skippedLog = move.indexOf(
                "V47 LANDO STAY skipped at {}", appliedLog);
        int nextRule = move.indexOf(
                "// === V29: FORCE RESERVE CHECK", skippedLog);

        assertTrue(start >= 0);
        assertTrue(title > start);
        assertTrue(location > title);
        assertTrue(site > location);
        assertTrue(objective > site);
        assertTrue(power > objective);
        assertTrue(weapons > power);
        assertTrue(decision > weapons);
        assertTrue(preferenceGate > decision);
        assertTrue(reason > preferenceGate);
        assertTrue(appliedLog > reason);
        assertTrue(skippedLog > appliedLog);
        assertTrue(nextRule > skippedLog);
    }

    @Test
    public void v47IsABoundedObjectivePreferenceWithoutEarlyExit()
            throws IOException {
        String move = evaluatorSource("rando");
        int start = move.indexOf("// === V47: LANDO");
        int end = move.indexOf(
                "// === V29: FORCE RESERVE CHECK", start);
        String block = move.substring(start, end);

        assertTrue(block.contains("addObjectiveContribution("));
        assertTrue(block.contains(
                "MOVE.OBJECTIVE.TDIGWATT_LANDO_STAY"));
        assertFalse(block.contains("return;"));
        assertFalse(block.contains("continue;"));
        assertFalse(block.contains("ladderVetoHard = true"));
    }

    @Test
    public void policyContainsNoContextEngineOrDecisionTransport()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "DecisionContext", "GameState", "SwccgGame",
                "PhysicalCard", "EvaluatedAction", "ObjectiveAnalyzer",
                "RandoConfig", "addReasoning", "logger", "ladder",
                "PolicyOperation", "PolicyResult", "DecisionOrigin",
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
                .resolve(bot).resolve("evaluators")
                .resolve("MoveEvaluator.java"));
    }

    private static String policySource() throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("MoveLandoStayPolicy.java"));
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
