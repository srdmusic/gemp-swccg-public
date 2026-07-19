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

public class MoveAbilitySourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void v32AndV156HaveOneSharedBranchAndScoreOwner()
            throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveAbilityPolicy.analyze("));
        assertEquals(1, countOccurrences(
                move, "MoveAbilityPolicy.destinyDanger("));
        assertEquals(1, countOccurrences(
                move, "MoveAbilityPolicy.soloEscape("));
        assertEquals(1, countOccurrences(
                move, "MoveAbilityPolicy.isUncontested("));
        assertEquals(1, countOccurrences(
                move, "MoveAbilityPolicy.canJoinGroup("));
        assertEquals(1, countOccurrences(
                move, "MoveAbilityPolicy.joinGroup("));
        assertFalse(move.contains("float abilityPenalty = -300.0f"));
        assertFalse(move.contains(
                "friendlyCharsHere == 1 && totalAbilityHere < 4.0f"));
        assertFalse(move.contains(
                "v156OppPowerHere == 0f && !cardToMove.isUndercover()"));
        assertTrue(policy.contains("-300.0f"));
        assertTrue(policy.contains("-500.0f"));
        assertTrue(policy.contains("50.0f"));
        assertTrue(policy.contains("250.0f"));
    }

    @Test
    public void adapterRetainsSiteBlueprintAndAbilityReads()
            throws IOException {
        String move = evaluatorSource("rando");
        String block = abilityBlock(move);

        assertTrue(block.contains(
                "currentLocation.getBlueprint().getCardSubtype()"));
        assertTrue(block.contains("CardSubtype.SITE"));
        assertTrue(block.contains(
                "cardToMove.getBlueprint().hasAbilityAttribute()"));
        assertTrue(block.contains(
                "cardToMove.getBlueprint().getAbility()"));
        assertTrue(block.contains(
                "gameState.getCardsAtLocation(currentLocation)"));
        assertTrue(block.contains("playerId.equals(c.getOwner())"));
        assertTrue(block.contains("CardCategory.CHARACTER"));
        assertTrue(block.contains("c.getBlueprint().getAbility()"));
    }

    @Test
    public void v32PreservesAnalyzeLazyPowerApplyAndSoloOrder()
            throws IOException {
        String move = evaluatorSource("rando");
        int start = move.indexOf("// === V32: ABILITY >= 4");
        int scan = move.indexOf(
                "gameState.getCardsAtLocation(currentLocation)", start);
        int analyze = move.indexOf("MoveAbilityPolicy.analyze(", scan);
        int danger = move.indexOf(
                "MoveAbilityPolicy.Branch.DESTINY_DANGER", analyze);
        int opponent = move.indexOf("game.getOpponent(playerId)", danger);
        int power = move.indexOf("getTotalPowerAtLocation(", opponent);
        int dangerScore = move.indexOf(
                "MoveAbilityPolicy.destinyDanger(", power);
        int dangerApply = move.indexOf("action.addReasoning(", dangerScore);
        int dangerLog = move.indexOf(
                "V32 ABILITY MOVE BLOCK", dangerApply);
        int solo = move.indexOf(
                "MoveAbilityPolicy.Branch.SOLO_ESCAPE", dangerLog);
        int soloScore = move.indexOf(
                "MoveAbilityPolicy.soloEscape(", solo);
        int soloApply = move.indexOf("action.addReasoning(", soloScore);
        int joinComment = move.indexOf("V156 JOIN-GROUP", soloApply);

        assertTrue(start >= 0);
        assertTrue(scan > start);
        assertTrue(analyze > scan);
        assertTrue(danger > analyze);
        assertTrue(opponent > danger);
        assertTrue(power > opponent);
        assertTrue(dangerScore > power);
        assertTrue(dangerApply > dangerScore);
        assertTrue(dangerLog > dangerApply);
        assertTrue(solo > dangerLog);
        assertTrue(soloScore > solo);
        assertTrue(soloApply > soloScore);
        assertTrue(joinComment > soloApply);
    }

    @Test
    public void v156RetainsObjectiveAdjacencyAndAbilityTotalReads()
            throws IOException {
        String move = evaluatorSource("rando");
        String block = abilityBlock(move);

        assertTrue(block.contains("context.getObjectiveAnalyzer()"));
        assertTrue(block.contains("v156Oa.isAnalyzed()"));
        assertTrue(block.contains(
                "v156Oa.isObjectiveRelevantLocation(currentLocation.getTitle())"));
        assertTrue(block.contains(
                "CharacterDeploySiteEvaluator\n                                                .isV156FlipNotReady(gameState, playerId)"));
        assertTrue(block.contains("cardToMove.isUndercover()"));
        assertTrue(block.contains("MovePredicates\n                                            .bestJoinDestination("));
        assertTrue(block.contains("MovePredicates\n                                                .siteAbilityTotal("));
        assertTrue(block.contains("V156 JOIN-GROUP error: {}"));
    }

    @Test
    public void uncontestedGateStillShortCircuitsUndercoverRead()
            throws IOException {
        String block = abilityBlock(evaluatorSource("rando"));
        int uncontested = block.indexOf(
                "MoveAbilityPolicy.isUncontested(v156OppPowerHere)");
        int andGate = block.indexOf("&& MoveAbilityPolicy.canJoinGroup(",
                uncontested);
        int undercover = block.indexOf(
                "cardToMove.isUndercover()", andGate);
        int ready = block.indexOf("v156AtReadyFlipSite", undercover);

        assertTrue(uncontested >= 0);
        assertTrue(andGate > uncontested);
        assertTrue(undercover > andGate);
        assertTrue(ready > undercover);
    }

    @Test
    public void joinDecisionAppliesBeforeR2ClaimAndLog()
            throws IOException {
        String block = abilityBlock(evaluatorSource("rando"));
        int destination = block.indexOf(
                ".bestJoinDestination(");
        int total = block.indexOf(".siteAbilityTotal(", destination);
        int decision = block.indexOf(
                "MoveAbilityPolicy.joinGroup(", total);
        int apply = block.indexOf("action.addReasoning(", decision);
        int claim = block.indexOf(
                "ladderClaimR2(", apply);
        int log = block.indexOf(
                "V156 JOIN-GROUP: {} (ability {})", claim);

        assertTrue(destination >= 0);
        assertTrue(total > destination);
        assertTrue(decision > total);
        assertTrue(apply > decision);
        assertTrue(claim > apply);
        assertTrue(log > claim);
    }

    @Test
    public void policyContainsNoContextEngineOrDecisionTransport()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "DecisionContext", "GameState", "SwccgGame",
                "PhysicalCard", "ObjectiveAnalyzer", "MovePredicates",
                "CharacterDeploySiteEvaluator", "EvaluatedAction",
                "RandoConfig", "addReasoning", "logger", "ladder",
                "PolicyOperation", "PolicyResult", "DecisionOrigin",
                "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef",
                "DeployDestinationRef", "DeployPhysicalCardRef",
                "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    private static String abilityBlock(String move) {
        int start = move.indexOf("// === V32: ABILITY >= 4");
        int end = move.indexOf("// === V33: ABILITY 7", start);
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
                .resolve("MoveAbilityPolicy.java"));
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
