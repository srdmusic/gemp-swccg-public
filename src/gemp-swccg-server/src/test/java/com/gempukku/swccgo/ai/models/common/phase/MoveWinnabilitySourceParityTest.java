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

public class MoveWinnabilitySourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void v137HasOneSharedMatchAndDecisionOwner()
            throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveWinnabilityPolicy.actionTargetsLocation("));
        assertEquals(1, countOccurrences(
                move, "MoveWinnabilityPolicy.contested("));
        assertEquals(1, countOccurrences(
                move, "MoveWinnabilityPolicy.uncontestedBattleground("));
        assertFalse(move.contains("float v137Pen = -800.0f"));
        assertFalse(move.contains("int v137ProjectedAtDest ="));
        assertFalse(move.contains("if (!v137CanWin)"));
        assertTrue(policy.contains("-800.0f"));
        assertTrue(policy.contains("-1500.0f"));
        assertTrue(policy.contains("-500.0f"));
    }

    @Test
    public void adapterRetainsTopLocationAndProjectedTeamReads()
            throws IOException {
        String block = v137Block(evaluatorSource("rando"));

        assertTrue(block.contains("gameState.getTopLocations()"));
        assertTrue(block.contains("loc == currentLocation"));
        assertTrue(block.contains("game.getOpponent(playerId)"));
        assertEquals(3, countOccurrences(block,
                "getTotalPowerAtLocation("));
        assertEquals(6, countOccurrences(block,
                "gameState.getCardsAtLocation("));
        assertTrue(block.contains("hasAbilityAttribute()"));
        assertTrue(block.contains("hasForfeitAttribute()"));
        assertTrue(block.contains(
                "MovePredicates.canWinAt(game, gameState, playerId"));
        assertTrue(block.contains("V137 error: {}"));
    }

    @Test
    public void contestedBranchPreservesReadPredicateDecisionApplyOrder()
            throws IOException {
        String block = v137Block(evaluatorSource("rando"));
        int destination = block.indexOf(
                "MoveWinnabilityPolicy.actionTargetsLocation(");
        int opponentPower = block.indexOf(
                "float v137OppPower", destination);
        int contested = block.indexOf(
                "if (v137OppPower > 0)", opponentPower);
        int destinationPower = block.indexOf(
                "float v137OurPower", contested);
        int destinationAbility = block.indexOf(
                "gameState.getCardsAtLocation(v137Dest)", destinationPower);
        int sourcePower = block.indexOf(
                "v137OurPower +=", destinationAbility);
        int sourceAbility = block.indexOf(
                "gameState.getCardsAtLocation(currentLocation)", sourcePower);
        int forfeit = block.indexOf("float v137OurForfeit", sourceAbility);
        int canWin = block.indexOf("MovePredicates.canWinAt(", forfeit);
        int decision = block.indexOf(
                "MoveWinnabilityPolicy.contested(", canWin);
        int veto = block.indexOf("ladderCanWinVeto =", decision);
        int apply = block.indexOf("action.addReasoning(", veto);
        int log = block.indexOf("V137 UNWINNABLE MOVE: {}", apply);

        assertTrue(destination >= 0);
        assertTrue(opponentPower > destination);
        assertTrue(contested > opponentPower);
        assertTrue(destinationPower > contested);
        assertTrue(destinationAbility > destinationPower);
        assertTrue(sourcePower > destinationAbility);
        assertTrue(sourceAbility > sourcePower);
        assertTrue(forfeit > sourceAbility);
        assertTrue(canWin > forfeit);
        assertTrue(decision > canWin);
        assertTrue(veto > decision);
        assertTrue(apply > veto);
        assertTrue(log > apply);
    }

    @Test
    public void antiSoloBranchPreservesBattlegroundAndCountOrder()
            throws IOException {
        String block = v137Block(evaluatorSource("rando"));
        int uncontested = block.indexOf(
                "} else {\n                                    // V137 ANTI-SOLO");
        int battleground = block.indexOf(
                "isBattleground(gameState, v137Dest", uncontested);
        int bgGate = block.indexOf("if (v137DestBG)", battleground);
        int destinationCount = block.indexOf(
                "gameState.getCardsAtLocation(v137Dest)", bgGate);
        int sourceCount = block.indexOf(
                "gameState.getCardsAtLocation(currentLocation)",
                destinationCount);
        int decision = block.indexOf(
                "MoveWinnabilityPolicy.uncontestedBattleground(", sourceCount);
        int apply = block.indexOf("action.addReasoning(", decision);
        int log = block.indexOf("V137 ANTI-SOLO BG: {}", apply);

        assertTrue(uncontested >= 0);
        assertTrue(battleground > uncontested);
        assertTrue(bgGate > battleground);
        assertTrue(destinationCount > bgGate);
        assertTrue(sourceCount > destinationCount);
        assertTrue(decision > sourceCount);
        assertTrue(apply > decision);
        assertTrue(log > apply);
    }

    @Test
    public void policyContainsNoContextEngineOrDecisionTransport()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "DecisionContext", "GameState", "SwccgGame",
                "PhysicalCard", "MovePredicates", "EvaluatedAction",
                "RandoConfig", "addReasoning", "logger", "ladder",
                "PolicyOperation", "PolicyResult", "DecisionOrigin",
                "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef",
                "DeployDestinationRef", "DeployPhysicalCardRef",
                "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    private static String v137Block(String move) {
        int start = move.indexOf("// === V137");
        int end = move.indexOf("// === V29.13: HUNT DOWN", start);
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
                .resolve("MoveWinnabilityPolicy.java"));
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
