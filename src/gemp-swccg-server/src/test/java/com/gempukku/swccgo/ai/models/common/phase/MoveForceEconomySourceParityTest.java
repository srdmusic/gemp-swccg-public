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
    public void actionTextEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource(
                        "rando", "ActionTextEvaluator.java")),
                normalize(evaluatorSource(
                        "chosenone", "ActionTextEvaluator.java")));
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
    public void actionTextTransportGatesHaveOneSharedOwner() throws IOException {
        String actionText = evaluatorSource(
                "rando", "ActionTextEvaluator.java");
        String policy = Files.readString(commonPhaseRoot()
                .resolve("MoveForceEconomyPolicy.java"));

        assertEquals(1, countOccurrences(actionText,
                "MoveForceEconomyPolicy.odinNesloorFloor("));
        assertEquals(1, countOccurrences(actionText,
                "MoveForceEconomyPolicy.isOdinNesloorAction("));
        assertEquals(1, countOccurrences(actionText,
                "MoveForceEconomyPolicy.transportInterruptFloor("));
        assertEquals(1, countOccurrences(actionText,
                "MoveForceEconomyPolicy.isNamedTransportInterrupt("));
        assertEquals(1, countOccurrences(actionText,
                "MoveForceEconomyPolicy.isTransportInterruptAction("));
        assertFalse(actionText.contains("v134ActionMatches"));
        assertFalse(actionText.contains("v141IsTransport"));
        assertFalse(actionText.contains("v141ActionMatches"));
        assertTrue(policy.contains(
                "public static boolean isOdinNesloorAction("));
        assertTrue(policy.contains(
                "public static ActionGate odinNesloorFloor(int forcePile)"));
        assertTrue(policy.contains(
                "public static boolean isTransportInterruptAction("));
        assertTrue(policy.contains(
                "public static ActionGate transportInterruptFloor("));
        assertTrue(policy.contains("forcePile < 5"));
        assertTrue(policy.contains(
                "forcePile >= 4 && reserveDeckSize >= 1"));
        assertTrue(policy.contains("-100000.0f"));
        assertTrue(policy.contains("-2000.0f"));
    }

    @Test
    public void actionTextAdapterRetainsReadsLogsAndLegacyOrder()
            throws IOException {
        String actionText = evaluatorSource(
                "rando", "ActionTextEvaluator.java");
        int v87 = actionText.indexOf(
                "MoveTransitPolicy.capacitySlotSwap(");
        int v134Match = actionText.indexOf(
                "MoveForceEconomyPolicy.isOdinNesloorAction(", v87);
        int v134ForceRead = actionText.indexOf(
                "int v134ForcePile = context.getForcePileSize()", v134Match);
        int v134 = actionText.indexOf(
                "MoveForceEconomyPolicy.odinNesloorFloor(", v134ForceRead);
        int v134Score = actionText.indexOf(
                "action.addReasoning(v134Gate.reason()", v134);
        int v134Log = actionText.indexOf(
                "V134 ODIN NESLOOR BLOCK:", v134Score);
        int v141Match = actionText.indexOf(
                "MoveForceEconomyPolicy.isTransportInterruptAction(",
                v134Log);
        int v141ForceRead = actionText.indexOf(
                "int v141ForcePile = context.getForcePileSize()", v141Match);
        int v141 = actionText.indexOf(
                "MoveForceEconomyPolicy.transportInterruptFloor(",
                v141ForceRead);
        int v141Score = actionText.indexOf(
                "action.addReasoning(v141Gate.reason()", v141);
        int v141Log = actionText.indexOf(
                "V141 TRANSPORT BLOCK:", v141Score);
        int v142 = actionText.indexOf("// === V142", v141Log);

        assertTrue(actionText.contains(
                "context.getPhase() == Phase.MOVE"));
        assertTrue(actionText.contains(
                "gameState.findCardById(Integer.parseInt(cardId))"));
        assertTrue(actionText.contains(
                "v141Src.getBlueprint().getGameText()"));
        assertTrue(actionText.contains("context.getForcePileSize()"));
        assertTrue(actionText.contains("context.getReserveDeckSize()"));
        assertTrue(v87 >= 0);
        assertTrue(v134Match > v87);
        assertTrue(v134ForceRead > v134Match);
        assertTrue(v134 > v134ForceRead);
        assertTrue(v134Score > v134);
        assertTrue(v134Log > v134Score);
        assertTrue(v141Match > v134Log);
        assertTrue(v141ForceRead > v141Match);
        assertTrue(v141 > v141ForceRead);
        assertTrue(v141Score > v141);
        assertTrue(v141Log > v141Score);
        assertTrue(v142 > v141Log);
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
        return evaluatorSource(bot, "MoveEvaluator.java");
    }

    private static String evaluatorSource(
            String bot, String evaluator) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators").resolve(evaluator));
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
