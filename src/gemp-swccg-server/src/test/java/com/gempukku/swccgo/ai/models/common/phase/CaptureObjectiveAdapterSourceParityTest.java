package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CaptureObjectiveAdapterSourceParityTest {

    @Test
    public void mirroredParentEvaluatorsRemainIdentical()
            throws IOException {
        for (String evaluator : new String[] {
                "ActionTextEvaluator.java",
                "DeployEvaluator.java",
                "MoveEvaluator.java",
                "PullPolicyAdapter.java"}) {
            assertEquals(evaluator,
                    normalize(botSource("rando", evaluator)),
                    normalize(botSource("chosenone", evaluator)));
        }
    }

    @Test
    public void payoffParentsRequireExactOwnedPhysicalSources()
            throws IOException {
        String actionText =
                botSource("rando",
                    "ActionTextEvaluator.java");
        String capture = slice(actionText,
                "var captureAnalyzer",
                "if (\"disembark\".equals");

        assertOrdered(capture,
                "gameState.findCardById(",
                "\"214_19\"",
                "\"9_61\"",
                "\"9_151\"",
                "\"deploy emperor from reserve deck\"",
                "scoreEmperorDownload(",
                "scorePayoff(");
        assertTrue(capture.contains(
                "CaptureObjectiveFacts.isOwnedExactSource("));
        assertTrue(capture.contains(
                "canAffordBhbmEmperorDownload("));
    }

    @Test
    public void emperorPullBypassIsClosedWhileSafetyGuardsRemain()
            throws IOException {
        String adapter =
                botSource("rando",
                    "PullPolicyAdapter.java");
        String route = slice(adapter,
                "public boolean objectiveRoutePullVetoBypass(",
                "};");
        String reader = commonSource(
                "phase/PullActionFactsReader.java");
        String policy = commonSource(
                "phase/PullActionPolicy.java");

        assertTrue(route.contains("return false;"));
        assertFalse(route.contains("CaptureObjectiveFacts.objectiveKind"));
        assertFalse(route.contains("canAffordBhbmEmperorDownload("));
        assertTrue(reader.contains(
                "&& !objectiveRoutePullVetoBypass\n"
                    + "                && cheapestCost != null"));
        assertTrue(policy.contains(
                "&& facts.cheapestTargetCost() != null"));
        assertFalse(policy.contains(
                "!facts.objectiveRoutePullVetoBypass()"));
    }

    @Test
    public void captureMoveBudgetAndBattleSafetyReachProductionConsumers()
            throws IOException {
        String deploy =
                botSource("rando",
                    "DeployEvaluator.java");
        String move =
                botSource("rando",
                    "MoveEvaluator.java");
        String battle = commonSource(
                "phase/BattleDecisionPolicy.java");

        assertOrdered(deploy,
                "CaptureObjectiveFacts",
                ".nextCaptureMoveForceReserve(",
                "CaptureDeployBudgetFactsReader",
                ".actionPayment(",
                "futureObligationDeployCost",
                "CAPTURE.BUDGET.UNKNOWN",
                "DeployBudgetPolicy.futureObligations(");
        assertTrue(move.contains(
                "hasFormationSafeLegalImmediateCaptureMoveDestination("));
        assertTrue(move.contains(
                "\"MOVE.OBJECTIVE.CAPTURE_ROUTE_PARENT\""));
        assertOrdered(battle,
                "nextCaptureMoveForceReserve(",
                "preserveObjectiveMoveForce(",
                "holdSoleVirtualCaptureEnablerBattle(",
                "scoreConflictBattle(");
    }

    @Test
    public void bothBotsSnapshotExactDeployActionPayments()
            throws IOException {
        String rando = aiSource(
                "rando", "RandoCalAi.java");
        String chosen = aiSource(
                "chosenone", "TheChosenOneAi.java");
        for (String source : new String[] {
                rando, chosen}) {
            assertOrdered(source,
                "CaptureDeployBudgetFactsReader",
                "ACTION_PAYMENTS_EXTRA",
                "snapshotExactNormalDeployPayments(",
                "decision, currentGame,",
                "playerId");
        }

        String reader = commonSource(
                "phase/CaptureDeployBudgetFactsReader.java");
        assertOrdered(reader,
                "AiActionSourceProvenance.actionForId(",
                "exactNormalCharacter(",
                "maximumExactNormalDeployPayment(",
                "getSpecialDeployCostEffect(",
                "Math.ceil(base) + extra");
    }

    @Test
    public void bhbmForceDripWiringIsMirroredSafeAndCaptureExclusive()
            throws IOException {
        String rando =
                botSource(
                    "rando",
                    "CardSelectionEvaluator.java");
        String chosen =
                botSource(
                    "chosenone",
                    "CardSelectionEvaluator.java");

        assertEquals(
                normalize(rando),
                normalize(chosen));
        assertOrdered(rando,
                "private boolean bhbmDeployFormationSafe(",
                "assessCharacterDeploy(",
                "DeployConstraint.ALLOW",
                "CandidateMechanism.DEPLOY");
        assertTrue(rando.contains(
                "ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA"));
        assertTrue(rando.contains(
                "bhbmActionSource(context)"));
        assertTrue(rando.contains(
                ".contains(\"using landspeed\")"));
        assertTrue(rando.contains(
                "CandidateMechanism.LANDSPEED"));
        assertFalse(rando.contains(
                "CandidateMechanism.OTHER"));
        assertTrue(rando.contains(
                "if (!guaranteedCaptureMoveDestination\n"
                    + "                                        && postFlipPayoff.applies())"));

        String provenance = commonSource(
                "phase/AiActionSourceProvenance.java");
        assertOrdered(provenance,
                "action.getActionSource()",
                "action.getActionAttachedToCard()");
    }

    @Test
    public void card10RelocationCannotMasqueradeAsLandspeedChild()
            throws IOException {
        String card10 = Files.readString(
                repoRoot().resolve(
                    "src/gemp-swccg-cards/src/main/java/"
                        + "com/gempukku/swccgo/cards/set10/"
                        + "light/Card10_010.java"));
        String evaluator = botSource(
                "rando",
                "CardSelectionEvaluator.java");

        assertTrue(card10.contains(
                "new RelocateBetweenLocationsEffect("));
        assertFalse(card10.contains(
                "MoveUsingLandspeedAction"));
        assertFalse(card10.contains(
                "using landspeed"));
        assertTrue(evaluator.contains(
                ".contains(\"using landspeed\")"));
    }

    @Test
    public void bhbmSetupPayoffsReachDeployMoveAndBattleConsumers()
            throws IOException {
        String randoSelection =
                botSource(
                    "rando",
                    "CardSelectionEvaluator.java");
        String chosenSelection =
                botSource(
                    "chosenone",
                    "CardSelectionEvaluator.java");
        String randoMovement =
                botSource(
                    "rando",
                    "CaptureMovementEvaluator.java");
        String chosenMovement =
                botSource(
                    "chosenone",
                    "CaptureMovementEvaluator.java");
        String randoDeploy =
                botSource(
                    "rando",
                    "DeployEvaluator.java");
        String chosenDeploy =
                botSource(
                    "chosenone",
                    "DeployEvaluator.java");
        String payoffReader = commonSource(
                "phase/BhbmSetupPayoffFactsReader.java");
        String battle = commonSource(
                "phase/BattleDecisionPolicy.java");

        assertEquals(
                normalize(randoSelection),
                normalize(chosenSelection));
        assertEquals(
                normalize(randoMovement),
                normalize(chosenMovement));
        assertEquals(
                normalize(randoDeploy),
                normalize(chosenDeploy));
        assertOrdered(randoSelection,
                "applyCaptureMoveDestination(",
                "scoreBhbmYourDestiny(",
                ".projectedVaderMoveFormationSafe(",
                "applyCaptureDeployDestination(",
                "scoreBhbmYourDestiny(",
                ".rewardsVaderForDeployAt(");
        assertOrdered(randoDeploy,
                "boolean bhbmYourDestiny",
                ".rewardsVaderForDeployAt(",
                ".hasLegalYourDestinyDeployDestination(",
                ".scoreBhbmYourDestiny(");
        assertOrdered(payoffReader,
                "projectedOwnedVader(",
                "CardCategory.VEHICLE",
                "Keyword.ENCLOSED",
                "Filters.filterActive(",
                "Filters.hasAboard(vader)",
                "hasOtherActiveFriendlyCharacterAboard(",
                "Filters.hasPermanentAboard(",
                ".getAboardCards(",
                "projectedVaderMoveFormationSafe(",
                "FormationSafety.vetoMoveDestination(",
                "currentlyRewardsVader(",
                "rewardsVaderForDeployAt(",
                "Filters.deployableToTarget(",
                "PhysicalCard effectiveSite",
                "FormationSafety.assessCharacterDeploy(",
                "FormationSafety.DeployConstraint.ALLOW",
                ".vetoMoveDestination(");
        assertOrdered(randoMovement,
                "BhbmSetupPayoffFactsReader",
                "ownsSpecialChildCaptureCredit(",
                ".projectedVaderMoveFormationSafe(",
                ".rewardsVaderAtBattleground(");
        assertFalse(randoMovement.contains(
                "child.mechanism()\n"
                    + "                    != CaptureMovementMechanismFactsReader\n"
                    + "                        .Mechanism.LANDSPEED"));
        assertOrdered(battle,
                "BhbmSetupPayoffFactsReader",
                ".insignificantRebellionActive(",
                "scoreConflictBattle(",
                "scoreBhbmBattleWin(");
    }

    private static String botSource(
            String bot,
            String evaluator) throws IOException {
        return Files.readString(repoRoot().resolve(
                "src/gemp-swccg-server/src/main/java/"
                    + "com/gempukku/swccgo/ai/models/")
                .resolve(bot)
                .resolve("evaluators")
                .resolve(evaluator));
    }

    private static String commonSource(
            String relative) throws IOException {
        return Files.readString(repoRoot().resolve(
                "src/gemp-swccg-server/src/main/java/"
                    + "com/gempukku/swccgo/ai/models/common/")
                .resolve(relative));
    }

    private static String aiSource(
            String bot,
            String filename) throws IOException {
        return Files.readString(repoRoot().resolve(
                "src/gemp-swccg-server/src/main/java/"
                    + "com/gempukku/swccgo/ai/models/")
                .resolve(bot)
                .resolve(filename));
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

    private static void assertOrdered(
            String source,
            String... tokens) {
        int previous = -1;
        for (String token : tokens) {
            int current =
                    source.indexOf(token, previous + 1);
            assertTrue("Missing or out-of-order token: " + token,
                    current > previous);
            previous = current;
        }
    }

    private static Path repoRoot() {
        Path cursor = Paths.get("")
                .toAbsolutePath().normalize();
        while (cursor != null) {
            if (Files.isDirectory(cursor.resolve(
                    "src/gemp-swccg-server/src/main/java"))
                    && Files.isDirectory(cursor.resolve(
                        "src/gemp-swccg-cards/src/main/java"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError(
                "Could not locate repository root");
    }

    private static String normalize(String source) {
        return source
                .replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
    }
}
