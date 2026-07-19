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

public class MoveUnarmedVaderSourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando", "MoveEvaluator.java")),
                normalize(evaluatorSource(
                        "chosenone", "MoveEvaluator.java")));
    }

    @Test
    public void unarmedVaderHasOneSharedScoreOwner() throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveUnarmedVaderPolicy.evaluate("));
        assertFalse(move.contains(
                "action.addReasoning(\"V29.9 UNARMED VADER"));
        assertTrue(policy.contains("Branch.EQUIP_FIRST"));
        assertTrue(policy.contains("-250.0f"));
        assertTrue(policy.contains("Branch.UNARMED"));
        assertTrue(policy.contains("-100.0f"));
    }

    @Test
    public void adapterRetainsExactCardAndHandReads() throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");

        assertTrue(move.contains(
                "cardToMove.getTitle().toLowerCase(Locale.ROOT)"));
        assertTrue(move.contains(
                "gameState.getAttachedCards(cardToMove)"));
        assertTrue(move.contains(
                "att.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.WEAPON"));
        assertTrue(move.contains("gameState.getHand(playerId)"));
        assertTrue(move.contains(
                "hCard.getTitle().toLowerCase(Locale.ROOT).contains(\"lightsaber\")"));
        assertTrue(move.contains(
                "V29.9: Error checking Vader weapon status: {}"));
        assertTrue(move.contains(
                "v29Readiness.reason(), v29Readiness.delta()"));
    }

    @Test
    public void adapterPreservesGateScanApplyLogAndHunterOrder()
            throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");
        int outerGate = move.indexOf(
                "if (!theirHasCards && myPower >= ATTACK_MIN_POWER"
                        + " && myCardCount == 1 && cardToMove != null)");
        int title = move.indexOf(
                "String preCharTitle", outerGate);
        int attached = move.indexOf(
                "gameState.getAttachedCards(cardToMove)", title);
        int hand = move.indexOf("gameState.getHand(playerId)", attached);
        int classify = move.indexOf(
                "MoveUnarmedVaderPolicy.evaluate(", hand);
        int add = move.indexOf("action.addReasoning(", classify);
        int equipLog = move.indexOf(
                "Vader has no weapon but lightsaber in hand", add);
        int unarmedLog = move.indexOf(
                "Vader has no weapon and none in hand", equipLog);
        int catchBlock = move.indexOf(
                "V29.9: Error checking Vader weapon status", unarmedLog);
        int hunter = move.indexOf(
                "List<String> v297WeaponTitles", catchBlock);

        assertTrue(outerGate >= 0);
        assertTrue(title > outerGate);
        assertTrue(attached > title);
        assertTrue(hand > attached);
        assertTrue(classify > hand);
        assertTrue(add > classify);
        assertTrue(equipLog > add);
        assertTrue(unarmedLog > equipLog);
        assertTrue(catchBlock > unarmedLog);
        assertTrue(hunter > catchBlock);
    }

    @Test
    public void readinessRemainsAdditiveWithoutLadderClaim()
            throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");
        int start = move.indexOf(
                "MoveUnarmedVaderPolicy.Evaluation v29Readiness");
        int end = move.indexOf(
                "V29.9: Error checking Vader weapon status", start);
        String block = move.substring(start, end);

        assertTrue(block.contains("action.addReasoning("));
        assertFalse(block.contains("ladderClaim"));
        assertFalse(block.contains("ladderVeto"));
        assertFalse(block.contains("return;"));
        assertFalse(block.contains("continue;"));
    }

    @Test
    public void weaponHunterAndBattleV299OwnersRemainPresent()
            throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");
        assertTrue(move.contains("MoveWeaponHunterPolicy.select("));
        String hunterPolicy = Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("MoveWeaponHunterPolicy.java"));
        assertTrue(hunterPolicy.contains(
                "V29.7 WEAPON HUNTER: %s + %s should CHALLENGE LUKE at %s!"));
        assertTrue(move.contains(
                "ladderClaimR2(\"V29.7 WEAPON HUNTER\""));

        String battle = Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("BattleDecisionPolicy.java"));
        assertTrue(battle.contains(
                "BattleInitiationPolicy.barrierRisk("));
        assertTrue(battle.contains(
                "BattleInitiationPolicy.huntAggression("));
        String battleInitiation = Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("BattleInitiationPolicy.java"));
        assertTrue(battleInitiation.contains(
                "V29.9 BARRIER RISK: If opponent Barriers Vader"));
        assertTrue(battleInitiation.contains(
                "V29.9 HUNT DOWN: Armed Vader should FIGHT!"));
    }

    @Test
    public void policyContainsNoContextEngineOrDecisionTransport()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "DecisionContext", "GameState", "SwccgGame",
                "PhysicalCard", "EvaluatedAction", "addReasoning",
                "logger", "ladder", "PolicyOperation", "PolicyResult",
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
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
                .resolve("MoveUnarmedVaderPolicy.java"));
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
