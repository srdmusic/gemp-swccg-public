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

public class MovePostFlipConsolidationSourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void v31HasOneSharedMatchAndWeakestDecisionOwner()
            throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(2, countOccurrences(
                move, "MovePostFlipConsolidationPolicy.isObjectiveLocation("));
        assertEquals(1, countOccurrences(
                move, "MovePostFlipConsolidationPolicy.evaluate("));
        assertFalse(move.contains("String weakestObjLoc = null"));
        assertFalse(move.contains("float weakestPwr = Float.MAX_VALUE"));
        assertFalse(move.contains("frag.toLowerCase(Locale.ROOT)"));
        assertTrue(policy.contains("Float.MAX_VALUE"));
        assertTrue(policy.contains("entry.getValue() < weakestPower"));
        assertTrue(policy.contains("200.0f"));
    }

    @Test
    public void adapterRetainsObjectiveAndBoardPowerReads()
            throws IOException {
        String block = v31Block(evaluatorSource("rando"));

        assertTrue(block.contains("context.getObjectiveAnalyzer()"));
        assertTrue(block.contains(
                "moveConsolidateAnalyzer.isAnalyzed()"));
        assertTrue(block.contains(
                "moveConsolidateAnalyzer.isFlipped()"));
        assertTrue(block.contains(
                "moveConsolidateAnalyzer.getFlipConditionLocationFragments()"));
        assertTrue(block.contains(
                "moveConsolidateAnalyzer.isFlipBackProtectionLocation("));
        assertTrue(block.contains("currentLocationMustBeHeld"));
        assertTrue(block.contains("gameState.getTopLocations()"));
        assertTrue(block.contains(
                "getTotalPowerAtLocation(\n                                        gameState, loc, playerId"));
        assertTrue(block.contains(
                "if (pwr > 0) objPowerMap.put(loc.getTitle(), pwr)"));
        assertTrue(block.contains(
                "V31 MOVE CONSOLIDATE: Error: {}"));
    }

    @Test
    public void adapterPreservesGateReadDecisionApplyClaimAndCatchOrder()
            throws IOException {
        String block = v31Block(evaluatorSource("rando"));
        int analyzer = block.indexOf("context.getObjectiveAnalyzer()");
        int analyzed = block.indexOf(
                "moveConsolidateAnalyzer.isAnalyzed()", analyzer);
        int flipped = block.indexOf(
                "moveConsolidateAnalyzer.isFlipped()", analyzed);
        int tryBlock = block.indexOf("try {", flipped);
        int fragments = block.indexOf(
                "getFlipConditionLocationFragments()", tryBlock);
        int currentMatch = block.indexOf(
                "MovePostFlipConsolidationPolicy.isObjectiveLocation(",
                fragments);
        int locations = block.indexOf(
                "gameState.getTopLocations()", currentMatch);
        int locationMatch = block.indexOf(
                "MovePostFlipConsolidationPolicy.isObjectiveLocation(",
                currentMatch + 1);
        int power = block.indexOf(
                "getTotalPowerAtLocation(", locationMatch);
        int decision = block.indexOf(
                "MovePostFlipConsolidationPolicy.evaluate(", power);
        int apply = block.indexOf("action.addReasoning(", decision);
        int log = block.indexOf(
                "V31 POST-FLIP CONSOLIDATE: {} should leave", apply);
        int claim = block.indexOf(
                "ladderClaimR2(\"V31 POST-FLIP CONSOLIDATE\"", log);
        int catchBlock = block.indexOf(
                "V31 MOVE CONSOLIDATE: Error", claim);

        assertTrue(analyzer >= 0);
        assertTrue(analyzed > analyzer);
        assertTrue(flipped > analyzed);
        assertTrue(tryBlock > flipped);
        assertTrue(fragments > tryBlock);
        assertTrue(currentMatch > fragments);
        assertTrue(locations > currentMatch);
        assertTrue(locationMatch > locations);
        assertTrue(power > locationMatch);
        assertTrue(decision > power);
        assertTrue(apply > decision);
        assertTrue(log > apply);
        assertTrue(claim > log);
        assertTrue(catchBlock > claim);
    }

    @Test
    public void occupiedPowerMapRemainsInsertionOrderedAndPositiveOnly()
            throws IOException {
        String block = v31Block(evaluatorSource("rando"));

        assertTrue(block.contains(
                "java.util.Map<String, Float> objPowerMap = new java.util.LinkedHashMap<>()"));
        assertTrue(block.contains("if (pwr > 0)"));
        assertFalse(block.contains("new java.util.HashMap"));
    }

    @Test
    public void policyContainsNoContextEngineOrDecisionTransport()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "DecisionContext", "GameState", "SwccgGame",
                "PhysicalCard", "ObjectiveAnalyzer", "EvaluatedAction",
                "RandoConfig", "addReasoning", "logger", "ladder",
                "PolicyOperation", "PolicyResult", "DecisionOrigin",
                "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef",
                "DeployDestinationRef", "DeployPhysicalCardRef",
                "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    private static String v31Block(String move) {
        int start = move.indexOf("// === V31: POST-FLIP");
        int end = move.indexOf("// === V37: NEVER MOVE", start);
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
                .resolve("MovePostFlipConsolidationPolicy.java"));
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
