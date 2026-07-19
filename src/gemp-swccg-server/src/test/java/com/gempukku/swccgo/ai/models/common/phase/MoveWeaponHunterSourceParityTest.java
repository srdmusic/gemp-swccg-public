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

public class MoveWeaponHunterSourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando", "MoveEvaluator.java")),
                normalize(evaluatorSource(
                        "chosenone", "MoveEvaluator.java")));
    }

    @Test
    public void weaponHunterHasOneSharedScoreOwner() throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveWeaponHunterPolicy.weaponFacts("));
        assertEquals(1, countOccurrences(
                move, "MoveWeaponHunterPolicy.profile("));
        assertEquals(1, countOccurrences(
                move, "MoveWeaponHunterPolicy.select("));
        assertFalse(move.contains("float attackScore = 60.0f"));
        assertFalse(move.contains("float bestAttackScore = 0"));
        assertFalse(move.contains("private static final float ICON_BONUS"));
        assertTrue(policy.contains("private static final float ICON_BONUS = 15.0f"));
        assertTrue(policy.contains("attackScore > bestAttackScore"));
    }

    @Test
    public void adapterRetainsAttachmentHandAndBoardReads()
            throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");

        assertTrue(move.contains("gameState.getAttachedCards(cardToMove)"));
        assertTrue(move.contains(
                "att.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.WEAPON"));
        assertTrue(move.contains("gameState.getHand(playerId)"));
        assertTrue(move.contains(
                "hTitle.contains(\"i have you now\")"));
        assertTrue(move.contains("gameState.getLocationsInOrder()"));
        assertTrue(move.contains("if (adjLocation == location) continue"));
        assertTrue(move.contains("gameState.getCardsAtLocation(adjLocation)"));
        assertTrue(move.contains("opponentId.equals(owner)"));
        assertTrue(move.contains("bp.hasPowerAttribute()"));
        assertTrue(move.contains(
                "card.getTitle().toLowerCase(Locale.ROOT).contains(\"luke\")"));
        assertTrue(move.contains("adjLocation.getBlueprint()"));
        assertTrue(move.contains("locBp.getIconCount(Icon.LIGHT_FORCE)"));
        assertTrue(move.contains("locBp.getIconCount(Icon.DARK_FORCE)"));
    }

    @Test
    public void adapterPreservesScanAndFactOrder() throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");
        int readiness = move.indexOf(
                "MoveUnarmedVaderPolicy.evaluate(");
        int weaponTitles = move.indexOf(
                "List<String> v297WeaponTitles", readiness);
        int attached = move.indexOf(
                "gameState.getAttachedCards(cardToMove)", weaponTitles);
        int weaponFacts = move.indexOf(
                "MoveWeaponHunterPolicy.weaponFacts(", attached);
        int hand = move.indexOf("gameState.getHand(playerId)", weaponFacts);
        int profile = move.indexOf(
                "MoveWeaponHunterPolicy.profile(", hand);
        int locations = move.indexOf(
                "gameState.getLocationsInOrder()", profile);
        int identitySkip = move.indexOf(
                "if (adjLocation == location) continue", locations);
        int cards = move.indexOf(
                "gameState.getCardsAtLocation(adjLocation)", identitySkip);
        int canBeat = move.indexOf("v297Profile.canBeat(", cards);
        int iconRead = move.indexOf(
                "adjLocation.getBlueprint()", canBeat);
        int fact = move.indexOf(
                "new MoveWeaponHunterPolicy.TargetFact(", iconRead);
        int select = move.indexOf(
                "MoveWeaponHunterPolicy.select(", fact);
        int spread = move.indexOf("// === SPREAD VIABILITY ===", select);

        assertTrue(readiness >= 0);
        assertTrue(weaponTitles > readiness);
        assertTrue(attached > weaponTitles);
        assertTrue(weaponFacts > attached);
        assertTrue(hand > weaponFacts);
        assertTrue(profile > hand);
        assertTrue(locations > profile);
        assertTrue(identitySkip > locations);
        assertTrue(cards > identitySkip);
        assertTrue(canBeat > cards);
        assertTrue(iconRead > canBeat);
        assertTrue(fact > iconRead);
        assertTrue(select > fact);
        assertTrue(spread > select);
    }

    @Test
    public void adapterRetainsAddLogAdjacencyAndLadderOrder()
            throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");
        int select = move.indexOf("MoveWeaponHunterPolicy.select(");
        int add = move.indexOf("action.addReasoning(", select);
        int log = move.indexOf(
                "[MoveEvaluator] ⚔️ {} — score {}", add);
        int ordinal = move.indexOf(
                "v297Evaluation.selectedTargetOrdinal()", log);
        int adjacency = move.indexOf(
                "isAdjacentSites(gameState, location, bestTargetLocCard)",
                ordinal);
        int claim = move.indexOf(
                "ladderClaimR2(\"V29.7 WEAPON HUNTER\"", adjacency);
        int remoteLog = move.indexOf(
                "LADDER: V29.7 no R2 claim", claim);
        int outerCatch = move.indexOf(
                "V29.7: Error in weapon hunter check", remoteLog);

        assertTrue(select >= 0);
        assertTrue(add > select);
        assertTrue(log > add);
        assertTrue(ordinal > log);
        assertTrue(adjacency > ordinal);
        assertTrue(claim > adjacency);
        assertTrue(remoteLog > claim);
        assertTrue(outerCatch > remoteLog);
    }

    @Test
    public void historicalTargetAndWeaponBoundariesStayVisible()
            throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");
        int weaponStart = move.indexOf("List<String> v297WeaponTitles");
        int weaponEnd = move.indexOf(
                "MoveWeaponHunterPolicy.weaponFacts(", weaponStart);
        String weaponRead = move.substring(weaponStart, weaponEnd);
        assertTrue(weaponRead.contains(
                "gameState.getAttachedCards(cardToMove)"));
        assertFalse(weaponRead.contains("permanentWeapon"));

        int targetStart = move.indexOf(
                "for (PhysicalCard adjLocation", weaponEnd);
        int targetEnd = move.indexOf(
                "MoveWeaponHunterPolicy.select(", targetStart);
        String targetRead = move.substring(targetStart, targetEnd);
        assertFalse(targetRead.contains("isUndercover"));
        assertFalse(targetRead.contains("isAdjacentSites"));
        assertTrue(targetRead.contains("v297Profile.canBeat("));
    }

    @Test
    public void otherOpportunityHuntAndBattleOwnersRemainUntouched()
            throws IOException {
        String move = evaluatorSource("rando", "MoveEvaluator.java");
        assertTrue(move.contains("MoveOpportunityPolicy.attack("));
        assertTrue(move.contains("MoveHuntTargetPolicy.evaluate("));

        String battle = Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("BattleDecisionPolicy.java"));
        assertTrue(battle.contains(
                "V29.7 WEAPON AWARENESS at {}"));
        assertTrue(battle.contains(
                "BattleInitiationPolicy.huntAggression("));
        String battleInitiation = Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("BattleInitiationPolicy.java"));
        assertTrue(battleInitiation.contains(
                "V29.9 HUNT DOWN: Armed Vader should FIGHT!"));
    }

    @Test
    public void policyContainsNoContextEngineOrDecisionTransport()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "DecisionContext", "GameState", "SwccgGame",
                "PhysicalCard", "SwccgCardBlueprint", "EvaluatedAction",
                "addReasoning", "logger", "ladderClaim", "ladderVeto",
                "PolicyOperation",
                "PolicyResult", "DecisionOrigin", "DecisionActionSemantic",
                "DecisionWire", "PullDeployRef", "PullPhysicalCardRef",
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
                .resolve("MoveWeaponHunterPolicy.java"));
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
