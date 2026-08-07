package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CaptureMovementMechanismSourceParityTest {

    @Test
    public void mirroredBotsLatchAndConsumeEveryStockMovementPrompt()
            throws IOException {
        String rando = botMain(
                "rando", "RandoCalAi.java");
        String chosen = botMain(
                "chosenone", "TheChosenOneAi.java");
        String randoLatch = slice(
                rando,
                "private void rememberSelectedMoveCard(",
                "private void rememberSelectedLostPileDeployCard(");
        String chosenLatch = slice(
                chosen,
                "private void rememberSelectedMoveCard(",
                "private void rememberSelectedLostPileDeployCard(");
        String randoChild = slice(
                rando,
                "String promptLower =",
                "if ((pendingObjectiveDeployingCardId != null");
        String chosenChild = slice(
                chosen,
                "String promptLower =",
                "if ((pendingObjectiveDeployingCardId != null");
        String randoDeploy = slice(
                rando,
                "private void rememberSelectedDeployCard(",
                "/**\n     * Try to use the evaluator system");
        String chosenDeploy = slice(
                chosen,
                "private void rememberSelectedDeployCard(",
                "/**\n     * Try to use the evaluator system");
        String randoDeployChild = slice(
                rando,
                "if ((pendingObjectiveDeployingCardId != null",
                "evalContext.setActivationAmountDecision(");
        String chosenDeployChild = slice(
                chosen,
                "if ((pendingObjectiveDeployingCardId != null",
                "evalContext.setActivationAmountDecision(");

        assertEquals(randoLatch, chosenLatch);
        assertEquals(randoChild, chosenChild);
        assertEquals(randoDeploy, chosenDeploy);
        assertEquals(randoDeployChild, chosenDeployChild);
        for (String parent : new String[] {
                "move using landspeed",
                "move using hyperspeed",
                "embark",
                "disembark",
                "shuttle"}) {
            assertTrue("Missing parent movement provenance: " + parent,
                    randoLatch.contains(parent));
        }
        for (String prompt : new String[] {
                "choose where to move",
                "choose where to embark",
                "choose where to disembark",
                "choose where to shuttle"}) {
            assertTrue("Missing child movement provenance: " + prompt,
                    randoChild.contains(prompt));
        }
        assertTrue(randoChild.contains(
                "pendingMovePhysicalCardId = null"));
        assertTrue(randoLatch.contains(
                "AiActionSourceProvenance"));
        assertTrue(randoChild.contains(
                "ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA"));
        assertTrue(randoChild.contains(
                "pendingMoveActionSourcePermanentCardId = null"));
        assertOrdered(randoChild,
                "boolean agentsOfBlackSunMoveMechanismChild",
                "&& objectiveAnalyzer != null",
                "&& objectiveAnalyzer",
                ".isActiveAgentsOfBlackSunBountyMoveAction(",
                "if (!agentsOfBlackSunMoveMechanismChild)",
                "pendingMovePhysicalCardId = null");
        assertFalse("The new context receives its analyzer later in the builder",
                randoChild.contains(
                    "evalContext.getObjectiveAnalyzer()\n"
                        + "                    .isActiveAgentsOfBlackSunBountyMoveAction("));
        assertTrue(randoDeploy.contains(
                "AiActionSourceProvenance"));
        assertTrue(randoDeployChild.contains(
                "ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA"));
        assertTrue(randoDeployChild.contains(
                "pendingDeployActionSourcePermanentCardId = null"));
    }

    @Test
    public void mirroredCardSelectionRoutesAllStockMovementDestinations()
            throws IOException {
        for (String bot : new String[] {
                "rando", "chosenone"}) {
            String source = botEvaluator(
                    bot, "CardSelectionEvaluator.java");
            String routing = slice(
                    source,
                    "Route to specific handlers",
                    "return evaluateMoveDestination(context);");
            String embark = slice(
                    source,
                    "evaluateObjectiveEmbarkTarget(",
                    "private void applyMoveBattlegroundPolicy(");

            assertTrue(routing.contains(
                    "choose where to embark"));
            assertTrue(routing.contains(
                    "where to shuttle"));
            assertTrue(routing.contains(
                    "where to disembark"));
            assertTrue(embark.contains(
                    "applyCaptureMoveDestination("));
            String formation = slice(
                    source,
                    "// FORMATION SAFETY (2026-07-11c): L4",
                    "// BATCH1b (2026-07-12");
            assertTrue(formation.contains(
                    "FormationSafety\n"
                        + "                                .vetoMoveDestination("));
            assertFalse(
                    "Guaranteed capture must not bypass formation safety",
                    formation.contains(
                        "!guaranteedCaptureMoveDestination"));
        }
    }

    @Test
    public void mirroredSpecialMovementEvaluatorIsRegisteredAndIdentical()
            throws IOException {
        String rando = botEvaluator(
                "rando", "CaptureMovementEvaluator.java");
        String chosen = botEvaluator(
                "chosenone", "CaptureMovementEvaluator.java");
        assertEquals(
                normalizeBotPackage(rando),
                normalizeBotPackage(chosen));

        for (String bot : new String[] {
                "rando", "chosenone"}) {
            String combined = botEvaluator(
                    bot, "CombinedEvaluator.java");
            assertTrue(combined.contains(
                    "new CaptureMovementEvaluator()"));
        }

        for (String mechanism : new String[] {
                "DOCKING_BAY_TRANSIT",
                "VADERS_CASTLE",
                "MACHINATION_RELOCATE",
                "TRANSPORT"}) {
            assertTrue(
                    "Missing exact Capture mechanism: "
                        + mechanism,
                    rando.contains(mechanism));
        }
        String reader = Files.readString(
                repoRoot().resolve(
                    "src/gemp-swccg-server/src/main/java/"
                        + "com/gempukku/swccgo/ai/models/common/phase/"
                        + "CaptureMovementMechanismFactsReader.java"));
        assertTrue(reader.contains("RISE_RECALL"));
        assertTrue(reader.contains("RISE_RELOCATE"));
        assertTrue(rando.contains(
                "hasAdmissibleCaptureRoute()"));
        assertTrue(rando.contains(
                "ownsSpecialParentCaptureCredit(mechanism)"));
        assertTrue(rando.contains(
                "applyStableBackHold("));
        assertTrue(rando.contains(
                "applyFormationHold("));
        assertTrue(rando.contains(
                ".Route::formationBlocked"));
        assertTrue(rando.contains(
                ".Route::hardBlocked"));
        assertFalse(rando.contains(
                ".Route::formationSafe))"));
    }

    @Test
    public void parentAndReserveShareFormationSafeCaptureReachability()
            throws IOException {
        for (String bot : new String[] {
                "rando", "chosenone"}) {
            String move = botEvaluator(
                    bot, "MoveEvaluator.java");
            assertTrue(move.contains(
                    ".hasFormationSafeLegalImmediateCaptureMoveDestination("));
        }

        String facts = Files.readString(
                repoRoot().resolve(
                    "src/gemp-swccg-server/src/main/java/"
                        + "com/gempukku/swccgo/ai/models/common/phase/"
                        + "CaptureObjectiveFacts.java"));
        String reserve = slice(
                facts,
                "public static int nextCaptureMoveForceReserve(",
                "private static boolean hasEligibleImperialAt(");
        assertTrue(reserve.contains(
                "formationSafeMoveDestination("));

        String reader = Files.readString(
                repoRoot().resolve(
                    "src/gemp-swccg-server/src/main/java/"
                        + "com/gempukku/swccgo/ai/models/common/phase/"
                        + "CaptureMovementMechanismFactsReader.java"));
        assertTrue(reader.contains(
                "CaptureObjectiveFacts.advancesCaptureApproachAt("));
        assertTrue(reader.contains(
                "postActionCaptureRelationshipProven("));
        assertTrue(reader.contains(
                ".getCardIsPresentAt("));
        assertTrue(reader.contains(
                "Keyword.ENCLOSED"));
        assertTrue(reader.contains(
                ".getLocationThatCardIsAt("));
        assertTrue(reader.contains(
                ".getLocationThatCardIsPresentAt("));
        assertTrue(reader.contains(
                "BhbmSetupPayoffFactsReader\n"
                    + "                .projectedOwnedVader("));
        assertTrue(reader.contains(
                ".projectedVaderMoveFormationSafe("));

        assertOrdered(facts,
                ".projectedOwnedVader(",
                ".proposedPresentAt(",
                ".getCardIsPresentAt(",
                "captureDeployFormationSafeIfNeeded(");
        assertTrue(facts.contains(
                ".getAllPermanentCards()"));
        assertTrue(facts.contains(
                "Filters.deployableToTarget("));
    }

    @Test
    public void bothSidesTransportSourcesShareExactDecisionContracts()
            throws IOException {
        for (String source : new String[] {
                "set1/light/Card1_097.java",
                "set1/dark/Card1_243.java",
                "set12/light/Card12_065.java",
                "set12/dark/Card12_150.java",
                "set209/light/Card209_021.java",
                "set209/dark/Card209_048.java"}) {
            String card = Files.readString(
                    repoRoot().resolve(
                        "src/gemp-swccg-cards/src/main/java/"
                            + "com/gempukku/swccgo/cards/")
                        .resolve(source));
            assertTrue(source, card.contains(
                    "action.setText(\"'Transport' characters\")"));
            assertTrue(source, card.contains(
                    "\"Choose site to 'transport' from\""));
            assertTrue(source, card.contains(
                    "\"Choose site to 'transport' to\""));
            assertTrue(source, card.contains(
                    "\"Choose characters to 'transport'\""));
        }

        String reader = Files.readString(
                repoRoot().resolve(
                    "src/gemp-swccg-server/src/main/java/"
                        + "com/gempukku/swccgo/ai/models/common/phase/"
                        + "CaptureMovementMechanismFactsReader.java"));
        for (String blueprint : new String[] {
                "\"1_97\"", "\"1_243\"",
                "\"12_65\"", "\"12_150\"",
                "\"209_21\"", "\"209_48\""}) {
            assertTrue(
                    "Missing transport source " + blueprint,
                    reader.contains(blueprint));
        }
        assertTrue(reader.contains(
                "ALL_SITE_TRANSPORT_SOURCES"));
        assertTrue(reader.contains(
                "EXTERIOR_TRANSPORT_SOURCES"));
        assertTrue(reader.contains(
                "EXTERIOR_OR_BATTLEGROUND_TRANSPORT_SOURCES"));

        String randoCombined = botEvaluator(
                "rando", "CombinedEvaluator.java");
        String chosenCombined = botEvaluator(
                "chosenone", "CombinedEvaluator.java");
        assertEquals(
                normalizeBotPackage(randoCombined),
                normalizeBotPackage(chosenCombined));
        assertTrue(randoCombined.contains(
                "pendingCaptureMovementOriginCardId"));
        assertTrue(randoCombined.contains(
                "SELECTED_ORIGIN_CARD_ID_EXTRA"));
    }

    private static String botMain(
            String bot, String file) throws IOException {
        return Files.readString(repoRoot().resolve(
                "src/gemp-swccg-server/src/main/java/"
                    + "com/gempukku/swccgo/ai/models/")
                .resolve(bot)
                .resolve(file));
    }

    private static void assertOrdered(
            String source,
            String... needles) {
        int cursor = -1;
        for (String needle : needles) {
            int next = source.indexOf(
                    needle, cursor + 1);
            assertTrue(
                    "Missing or out-of-order source token: "
                        + needle,
                    next > cursor);
            cursor = next;
        }
    }

    private static String botEvaluator(
            String bot, String file) throws IOException {
        return Files.readString(repoRoot().resolve(
                "src/gemp-swccg-server/src/main/java/"
                    + "com/gempukku/swccgo/ai/models/")
                .resolve(bot)
                .resolve("evaluators")
                .resolve(file));
    }

    private static String slice(
            String source,
            String startToken,
            String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start);
        assertTrue(start >= 0);
        assertTrue(end > start);
        return source.substring(start, end);
    }

    private static String normalizeBotPackage(
            String source) {
        return source.replace(
                "models.rando",
                "models.bot")
            .replace(
                "models.chosenone",
                "models.bot");
    }

    private static Path repoRoot() {
        Path cursor = Paths.get("")
                .toAbsolutePath().normalize();
        while (cursor != null) {
            if (Files.isDirectory(cursor.resolve(
                    "src/gemp-swccg-server/src/main/java"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError(
                "Could not locate repository root");
    }
}
