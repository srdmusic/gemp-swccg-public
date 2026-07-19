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

public class MoveForceEconomySourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void forceEconomyScoresHaveOneSharedOwner() throws IOException {
        String move = evaluatorSource("rando");
        String policy = Files.readString(commonPhaseRoot()
                .resolve("MoveForceEconomyPolicy.java"));

        assertEquals(1, countOccurrences(move,
                "MoveForceEconomyPolicy.reserve("));
        assertEquals(1, countOccurrences(move,
                "MoveForceEconomyPolicy.maintenance("));
        assertFalse(move.contains("V29 FORCE RESERVE: Only %d Force"));
        assertFalse(move.contains("V27 MAINTENANCE: Need %d Force"));
        assertTrue(policy.contains("V29 FORCE RESERVE: Only %d Force"));
        assertTrue(policy.contains("V27 MAINTENANCE: Need %d Force"));
    }

    @Test
    public void policyCallsRemainInLegacyAdditiveOrder() throws IOException {
        String move = evaluatorSource("rando");
        int reserve = move.indexOf("MoveForceEconomyPolicy.reserve(");
        int strategic = move.indexOf("rankMoveFromLocation(", reserve);
        int movePhase = move.indexOf("action.addReasoning(\"Move phase\"", strategic);
        int maintenance = move.indexOf(
                "MoveForceEconomyPolicy.maintenance(", movePhase);
        int spyFollow = move.indexOf("// === V53: SPY FOLLOW", maintenance);

        assertTrue(reserve >= 0);
        assertTrue(strategic > reserve);
        assertTrue(movePhase > strategic);
        assertTrue(maintenance > movePhase);
        assertTrue(spyFollow > maintenance);
    }

    @Test
    public void movePolicyContainsNoEngineDecisionMetadata() throws IOException {
        String policy = Files.readString(commonPhaseRoot()
                .resolve("MoveForceEconomyPolicy.java"));
        for (String forbidden : new String[]{
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

    private static Path commonPhaseRoot() {
        return mainJavaRoot().resolve("com/gempukku/swccgo/ai/models/common/phase");
    }

    private static Path mainJavaRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            Path repoLayout = cursor.resolve("src/gemp-swccg-server/src/main/java");
            if (Files.isDirectory(repoLayout)) return repoLayout;
            Path moduleLayout = cursor.resolve("src/main/java");
            if (Files.isDirectory(moduleLayout.resolve(
                    "com/gempukku/swccgo/ai/models"))) {
                return moduleLayout;
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError("Could not locate gemp-swccg-server main/java");
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
