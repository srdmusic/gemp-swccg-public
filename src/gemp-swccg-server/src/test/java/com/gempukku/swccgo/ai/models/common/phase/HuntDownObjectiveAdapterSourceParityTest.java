package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HuntDownObjectiveAdapterSourceParityTest {

    @Test
    public void huntDownHighLevelAdaptersRemainNormalizedMirrors()
            throws IOException {
        for (String file : new String[] {
                "ActionTextEvaluator.java",
                "CardSelectionEvaluator.java",
                "DeployEvaluator.java",
                "MoveEvaluator.java"}) {
            assertEquals(file,
                    normalize(botSource("rando", file)),
                    normalize(botSource("chosenone", file)));
        }
    }

    @Test
    public void pullCandidateUsesTheStructuredPreFlipClassifier()
            throws IOException {
        String pull = methodSlice(
                botSource("rando", "CardSelectionEvaluator.java"),
                "private void applyCountedObjectivePullPolicy(",
                "private static boolean gameTextContainsShipName(");

        assertOrdered(pull,
                ".classifyPreFlipProgressCandidate(",
                "PullSelectionCandidatePolicy.scoreCountedObjectiveProgress(");
    }

    @Test
    public void parentAndChildDeployUseRuntimeActorDestinations()
            throws IOException {
        String parent = botSource("rando", "DeployEvaluator.java");
        String analyzer = methodSlice(
                commonSource("strategy/ObjectiveAnalyzer.java"),
                "public java.util.List<ScoreNote> "
                        + "getDeployObjectiveAdjustments(",
                "// My Lord DEPLOY magnitudes now read from the ACTIVE playbook");
        String child = methodSlice(
                botSource("rando", "CardSelectionEvaluator.java"),
                "private List<EvaluatedAction> evaluateDeployLocation(",
                "private List<EvaluatedAction> evaluateForceLoss(");

        assertTrue(parent.contains(
                ".getDeployObjectiveAdjustments("));
        assertOrdered(analyzer,
                "hasLegalPreFlipActorLocationDestination(",
                "\"OBJECTIVE ACTOR LOCATION: deploy '\"");
        assertOrdered(child,
                ".advancesPreFlipActorAtRuntimeLocation(",
                ".scoreActorRuntimeLocation(");
        assertTrue("Runtime actor progress must not double-stack the generic score",
                child.contains("boolean countedProgress =\n"
                        + "                                    !actorLocationProgress"));
    }

    @Test
    public void v25UsesRuntimeBattlegroundTruthInsteadOfPrintedDualIcons()
            throws IOException {
        String huntDeploy = sourceSlice(
                botSource("rando", "CardSelectionEvaluator.java"),
                "// === V25: HUNT DOWN",
                "// === V25: CLOUD CITY");

        assertTrue(huntDeploy.contains(
                ".advancesPreFlipActorAtRuntimeLocation("));
        assertTrue(huntDeploy.contains(
                "DeployObjectiveSitingPolicy.evaluateHuntDownCharacter("));
        assertFalse("Printed icons are not runtime battleground truth",
                huntDeploy.contains("getIconCount("));
        assertFalse("The old printed Light/Dark icon proxy must stay retired",
                huntDeploy.contains("Icon.LIGHT_FORCE")
                        || huntDeploy.contains("Icon.DARK_FORCE"));
    }

    @Test
    public void v51ProvesTheWholeLiveFlipBeforeApplyingItsBonus()
            throws IOException {
        String v51 = sourceSlice(
                botSource("rando", "DeployEvaluator.java"),
                "// === V51: VADER AGGRESSIVE FLIP ===",
                "// === V29.13: DEPLOY DIRECTLY TO OPPONENTS");

        assertOrdered(v51,
                ".wouldCompletePreFlipRequirementAt(",
                "DeployTacticalPolicy.scoreV51VaderFlip(");
        assertTrue(v51.contains(
                "new DeployTacticalPolicy.VaderFlipFacts("));
        assertTrue(v51.contains("completesObjective"));
    }

    @Test
    public void moveParentAndChildKeepRuntimeProgressAndFormationSafetyBoundaries()
            throws IOException {
        String parent = sourceSlice(
                botSource("rando", "MoveEvaluator.java"),
                "// Typed actor-gate objectives may require several ordinary",
                "// === V79");
        String child = methodSlice(
                botSource("rando", "CardSelectionEvaluator.java"),
                "private List<EvaluatedAction> evaluateMoveDestination(",
                "private List<EvaluatedAction> evaluateCancelSelection(");

        assertOrdered(parent,
                ".advancesPreFlipActorAtRuntimeLocation(",
                ".vetoMoveDestination(",
                ".vetoMoveOrigin(",
                ".objectiveActorLocationStart(");
        assertOrdered(child,
                ".vetoMoveDestination(",
                ".vetoMoveOrigin(",
                ".advancesPreFlipActorAtRuntimeLocation(",
                ".objectiveActorLocationDestination(");
        assertTrue(child.contains(
                "\"MOVE.OBJECTIVE.ACTOR_LOCATION_DESTINATION\""));
        String wrongDirection = sourceSlice(
                child,
                "MoveDestinationPolicy.wrongDirection(",
                "if (v41Direction.disposition()");
        assertTrue("Only terminal objective loss may bypass wrong-direction tactics",
                wrongDirection.contains(
                        "objectiveTerminalEscapeDestination"));
        assertFalse("Ordinary objective progress must remain overridable",
                wrongDirection.contains("objectiveActor"));
    }

    @Test
    public void lossAndForfeitReadStructuredActorPreservation()
            throws IOException {
        String forceLoss = methodSlice(
                botSource("rando", "CardSelectionEvaluator.java"),
                "private ForceLossPolicy.ObjectiveFlags "
                        + "forceLossObjectiveFlags(",
                "private List<EvaluatedAction> evaluateForfeit(");
        String forfeitFacts =
                commonSource("phase/BattleForfeitFacts.java");

        assertTrue(forceLoss.contains(
                ".classifyPreFlipProgressCandidate("));
        assertFalse(forceLoss.contains(
                ".matchesMissingPreFlipActorAtLocationRequirement("));
        assertTrue(forfeitFacts.contains(
                ".classifyGateFormationPieceIfRemoved("));
    }

    @Test
    public void battleReadsGlobalBlockersAndKeepsInquisitorsVirtualOnly()
            throws IOException {
        String battle =
                commonSource("phase/BattleDecisionPolicy.java");
        String objectiveBattle = sourceSlice(
                battle,
                "boolean exactStructuredPreFlipTarget = false;",
                "if (ourPower > 0 && theirPower > 0)");
        String inquisitor = sourceSlice(
                battle,
                "// === V35: INQUISITOR BATTLE DESTINY BONUS ===",
                "if (ourPower > 0 && theirPower > 0)");

        assertOrdered(objectiveBattle,
                ".isPreFlipBattleRemovableGlobalBlockerAt(",
                "ObjectiveBattlePolicy.evaluate(");
        assertTrue(objectiveBattle.contains(
                "globalObjectiveBlocker"));
        assertTrue(inquisitor.contains(
                ".isVirtualHuntDownObjective()"));
        assertFalse("Classic Hunt Down has no Inquisitor destiny text",
                inquisitor.contains(".isHuntDownV()"));
    }

    private static String botSource(String bot, String file)
            throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators").resolve(file));
    }

    private static String commonSource(String relative)
            throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common")
                .resolve(relative));
    }

    private static String methodSlice(
            String source, String startToken, String endToken) {
        return sourceSlice(source, startToken, endToken);
    }

    private static String sourceSlice(
            String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start);
        assertTrue("Missing source start token: " + startToken,
                start >= 0);
        assertTrue("Missing source end token: " + endToken,
                end > start);
        return source.substring(start, end);
    }

    private static void assertOrdered(
            String source, String... tokens) {
        int previous = -1;
        for (String token : tokens) {
            int current = source.indexOf(token, previous + 1);
            assertTrue("Missing or out-of-order source token: " + token,
                    current > previous);
            previous = current;
        }
    }

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
    }

    private static Path mainJavaRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            Path repositoryLayout = cursor.resolve(
                    "src/gemp-swccg-server/src/main/java");
            if (Files.isDirectory(repositoryLayout)) {
                return repositoryLayout;
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
}
