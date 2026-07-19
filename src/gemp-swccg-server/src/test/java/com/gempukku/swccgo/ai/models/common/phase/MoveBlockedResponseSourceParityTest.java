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

public class MoveBlockedResponseSourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando", "MoveEvaluator.java")),
                normalize(evaluatorSource(
                        "chosenone", "MoveEvaluator.java")));
    }

    @Test
    public void blockedResponseHasOneSharedClassifier() throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveBlockedResponsePolicy.matches("));
        assertEquals(1, countOccurrences(
                move, "MoveBlockedResponsePolicy.classify("));
        assertFalse(move.contains(
                "v169EndangeredMover = v169Their > v169Our"));
        assertTrue(policy.contains("public static boolean matches("));
        assertTrue(policy.contains("public static Evaluation classify("));
        assertTrue(policy.contains("opponentPower > ourPower"));
    }

    @Test
    public void adapterRetainsContextPowerCatchAndActionOwnership()
            throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");

        assertTrue(move.contains(
                "java.util.Set<String> v160MoveBlocked = context.getBlockedResponses()"));
        assertTrue(move.contains(
                "context.getGameState().findCardById(Integer.parseInt(cardIdStr))"));
        assertTrue(move.contains(
                ".getTotalPowerAtLocation(context.getGameState(), v169At, v169Pid, false, false)"));
        assertTrue(move.contains(
                ".getTotalPowerAtLocation(context.getGameState(), v169At, v169Opp, false, false)"));
        assertTrue(move.contains("catch (Exception ignore) { }"));
        assertTrue(move.contains(
                "new EvaluatedAction(actionId, ActionType.MOVE, 0.0f, actionText)"));
        assertTrue(move.contains(
                "blockedMove.addReasoning("));
        assertTrue(move.contains("actions.add(blockedMove)"));
    }

    @Test
    public void adapterPreservesBlockedGateReadAndControlFlowOrder()
            throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");
        int route = move.indexOf("if (!isMoveAction(actionText))");
        int match = move.indexOf(
                "MoveBlockedResponsePolicy.matches(", route);
        int cardRead = move.indexOf(
                "context.getGameState().findCardById", match);
        int ourPower = move.indexOf("v169Our = context.getGame()", cardRead);
        int theirPower = move.indexOf(
                "v169Their = context.getGame()", ourPower);
        int facts = move.indexOf(
                "v169PowerFactsAvailable = true", theirPower);
        int classify = move.indexOf(
                "MoveBlockedResponsePolicy.classify(", facts);
        int endangeredLog = move.indexOf(
                "V169 MoveEvaluator: endangered mover", classify);
        int blockedConstruction = move.indexOf(
                "EvaluatedAction blockedMove = new EvaluatedAction(",
                endangeredLog);
        int blockedReason = move.indexOf(
                "blockedMove.addReasoning(", blockedConstruction);
        int blockedLog = move.indexOf(
                "MoveEvaluator: actionId='{}' is in blockedResponses",
                blockedReason);
        int append = move.indexOf("actions.add(blockedMove)", blockedLog);
        int capacity = move.indexOf(
                "MoveTransitPolicy.capacitySlot(", append);

        assertTrue(route >= 0);
        assertTrue(match > route);
        assertTrue(cardRead > match);
        assertTrue(ourPower > cardRead);
        assertTrue(theirPower > ourPower);
        assertTrue(facts > theirPower);
        assertTrue(classify > facts);
        assertTrue(endangeredLog > classify);
        assertTrue(blockedConstruction > endangeredLog);
        assertTrue(blockedReason > blockedConstruction);
        assertTrue(blockedLog > blockedReason);
        assertTrue(append > blockedLog);
        assertTrue(capacity > append);
    }

    @Test
    public void hardBlockRemainsAdditiveAdapterAction() throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");
        String policy = policySource();

        assertTrue(policy.contains(
                "CANCEL-LOOP BLOCK: this move led to repeated Done-cancels — try something else (LADDER VETO)"));
        assertTrue(policy.contains("-100000.0f"));
        assertFalse(move.contains("blockedMove.hardVeto("));
        assertFalse(move.contains("ladderVetoHard = true;\n                    EvaluatedAction blockedMove"));
    }

    @Test
    public void actionTextRetainsSoleSoftRetryOwner() throws IOException {
        for (String bot : new String[]{"rando", "chosenone"}) {
            String actionText = evaluatorSource(
                    bot, "ActionTextEvaluator.java");
            assertTrue(actionText.contains(
                    "private static final int V169_SOFT_RETRY_BUDGET = 3"));
            assertTrue(actionText.contains(
                    "v167tl.contains(\"move using\") || v167tl.contains(\"transport\") || v167tl.contains(\"relocate\")"));
            assertTrue(actionText.contains(
                    "v169SoftRetryCounts.merge(v169Key, 1, Integer::sum)"));
            assertTrue(actionText.contains(
                    "v169Tries <= V169_SOFT_RETRY_BUDGET"));
            assertTrue(actionText.contains(
                    "V169: endangered mover, retreat must stay possible)\", -250.0f"));
            assertTrue(actionText.contains(
                    "V169 retry budget exhausted: no safe destination materialized)\", -100000.0f"));
        }
        String move = evaluatorSource("rando", "MoveEvaluator.java");
        assertFalse(move.contains(
                "retreat must stay possible)\", -250.0f"));
    }

    @Test
    public void policyContainsNoContextEngineOrDecisionTransport()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "DecisionContext", "GameState", "SwccgGame",
                "PhysicalCard", "EvaluatedAction", "addReasoning",
                "PolicyOperation", "PolicyResult", "DecisionOrigin",
                "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef",
                "DeployDestinationRef", "DeployPhysicalCardRef",
                "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
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
                .resolve("MoveBlockedResponsePolicy.java"));
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
