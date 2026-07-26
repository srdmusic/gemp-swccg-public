package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShieldSourceOwnershipTest {

    @Test
    public void strategyOwnsCatalogStateAndScoresWithoutBoardReads() throws IOException {
        String strategy = commonStrategySource("ShieldStrategy.java");

        assertTrue(strategy.contains("private static final Map<String, ShieldInfo> DARK_SHIELDS"));
        assertTrue(strategy.contains("public float scoreShield("));
        assertTrue(strategy.contains("public int minTurnToPlay("));
        assertFalse(strategy.contains("GameState"));
        assertFalse(strategy.contains("SwccgGame"));
        assertFalse(strategy.contains("ShieldFacts.fourthSlotFacts("));
        assertFalse(strategy.contains("getAllPermanentCards("));
        assertFalse(strategy.contains("getTopLocations("));
    }

    @Test
    public void policyIsTheSingleOrderedCandidateAdjustmentOwner() throws IOException {
        String policy = commonPhaseSource("ShieldPolicy.java");
        String signature =
                "public static PolicyResult shieldCandidateAdjustments(";
        int compatibilityOverload = policy.indexOf(signature);
        String compatibility = method(policy, signature);
        String candidate = method(
                policy.substring(compatibilityOverload + signature.length()),
                signature);

        assertEquals(2, occurrences(policy, signature));
        assertTrue(compatibility.contains("return shieldCandidateAdjustments("));
        assertFalse(policy.contains("shieldSelectionAdjustments("));
        assertFalse(policy.contains("reserveBattleOrderAdjustments("));
        assertEquals(1, occurrences(candidate, "\"V53-shield-min-turn\""));
        assertTrue(candidate.indexOf("battleOrderPlanRedundancyGate(")
                < candidate.indexOf("turnNumber < minTurnToPlay"));
        assertTrue(candidate.indexOf("turnNumber < minTurnToPlay")
                < candidate.indexOf("shieldsOnTable >= 3"));
        assertTrue(candidate.indexOf("shieldsOnTable >= 3")
                < candidate.indexOf("addBattleOrderSelection("));
        assertTrue(candidate.contains("TraceOutputKind.VETO, -5000.0f"));
        assertFalse(policy.contains("hardVeto("));
        assertFalse(policy.contains("defer("));
        assertFalse(policy.contains("PolicyOperation.set("));
    }

    @Test
    public void mirroredCandidateAdaptersApplyOnePerCandidateLedgerOnce() throws IOException {
        String rando = evaluatorSource("rando");
        String chosen = evaluatorSource("chosenone");
        assertEquals(normalize(rando), normalize(chosen));

        for (String evaluator : new String[] {rando, chosen}) {
            String reserve = method(evaluator,
                    "private List<EvaluatedAction> evaluateReserveDeckSelection(");
            String dedicated = method(evaluator,
                    "private List<EvaluatedAction> evaluateShieldSelection(");

            assertCandidateLedgerOnce(reserve);
            assertCandidateLedgerOnce(dedicated);
            assertTrue(reserve.contains(
                    "if (pullCategory == CardCategory.DEFENSIVE_SHIELD)"));
            assertTrue(reserve.indexOf("cardTitle = pullBlueprint.getTitle();")
                    < reserve.indexOf("new EvaluatedAction("));
            assertFalse(reserve.contains("scoreShield(blueprintId, blueprintId"));
            assertTrue(dedicated.contains("action.setScore(shieldScore);"));
            assertTrue(dedicated.contains(
                    "action.addReasoning(\"Defensive shield (no strategy)\", 50.0f);"));
            assertTrue(reserve.contains("ShieldFacts.shieldsOnTable("));
            assertTrue(dedicated.contains("ShieldFacts.shieldsOnTable("));
            assertFalse(reserve.contains("shieldsRemaining("));
            assertFalse(dedicated.contains("shieldsRemaining("));
        }
    }

    @Test
    public void retiredProductionFieldsRemainCommentsWithZeroLiveUsages() throws IOException {
        String production = allAiProductionSource();
        for (String retired : new String[] {
                "playIfWeHave", "hasShieldsToPlay"}) {
            int commentMarkers = 0;
            for (String line : production.lines().toList()) {
                if (line.contains(retired)) {
                    assertTrue(retired + " must remain comment-only",
                            line.stripLeading().startsWith("//"));
                    commentMarkers++;
                }
            }
            assertTrue(retired + " retirement marker missing", commentMarkers > 0);
        }
    }

    private static void assertCandidateLedgerOnce(String method) {
        assertEquals(1, occurrences(method,
                "PolicyContributionLedger shieldLedger = new PolicyContributionLedger("));
        assertEquals(1, occurrences(method,
                "shieldLedger.register(ShieldPolicy.shieldCandidateAdjustments("));
        assertEquals(1, occurrences(method,
                "PolicyOperationAdapter.apply(action, shieldLedger);"));
        assertTrue(method.indexOf(
                "shieldLedger.register(ShieldPolicy.shieldCandidateAdjustments(")
                < method.indexOf("PolicyOperationAdapter.apply(action, shieldLedger);"));
    }

    private static String evaluatorSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/CardSelectionEvaluator.java"));
    }

    private static String commonPhaseSource(String file) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase").resolve(file));
    }

    private static String commonStrategySource(String file) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/strategy").resolve(file));
    }

    private static String allAiProductionSource() throws IOException {
        StringBuilder source = new StringBuilder();
        try (var files = Files.walk(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java"))
                    .sorted().toList()) {
                source.append(Files.readString(file));
            }
        }
        return source.toString();
    }

    private static Path mainJavaRoot() {
        return Path.of("src", "main", "java");
    }

    private static String method(String source, String signature) {
        int from = source.indexOf(signature);
        assertTrue(from >= 0);
        int open = source.indexOf('{', from);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char current = source.charAt(i);
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return source.substring(from, i + 1);
            }
        }
        throw new AssertionError("Unterminated method: " + signature);
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }

    private static String normalize(String source) {
        return source.replace("ai.models.rando", "ai.models.BOT")
                .replace("ai.models.chosenone", "ai.models.BOT")
                .replace("Rando", "Robot")
                .replace("ChosenOne", "Robot");
    }
}
