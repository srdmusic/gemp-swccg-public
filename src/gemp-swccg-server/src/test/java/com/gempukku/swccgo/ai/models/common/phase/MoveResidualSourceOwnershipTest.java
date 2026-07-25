package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MoveResidualSourceOwnershipTest {

    @Test
    public void moveAdaptersRemainStrictNormalizedMirrors()
            throws IOException {
        for (String file : new String[] {
                "MoveEvaluator.java",
                "ActionTextEvaluator.java",
                "CardSelectionEvaluator.java"}) {
            assertEquals(file,
                    normalize(evaluator("rando", file)),
                    normalize(evaluator("chosenone", file)));
        }
    }

    @Test
    public void everyResidualArmHasExactlyOneSharedCall()
            throws IOException {
        String move = evaluator("rando", "MoveEvaluator.java");
        String actionText = evaluator("rando", "ActionTextEvaluator.java");
        String selection = evaluator("rando", "CardSelectionEvaluator.java");

        assertExactlyOnce(move,
                "MoveDestinationPolicy.missingSourceLocation(");
        for (String call : new String[] {
                "MoveTransitPolicy.capacityChoice(",
                "MoveTransitPolicy.embark(",
                "MoveTransitPolicy.residualTransfer(",
                "MoveTransitPolicy.shipDock(",
                "MoveSpyFollowPolicy.breakCover("}) {
            assertExactlyOnce(actionText, call);
        }
        for (String call : new String[] {
                "MoveAbilityPolicy.weakSplit(",
                "MoveDestinationPolicy.icons(",
                "MoveAbilityPolicy.joinDestination(",
                "MoveDestinationPolicy.power(",
                "MoveDestinationPolicy.battleground(",
                "MoveObjectiveConsolidationPolicy.cloudCityDestination(",
                "MoveObjectiveConsolidationPolicy.hiddenPathSplit(",
                "MoveSpyFollowPolicy.dilution(",
                "MoveLandoStayPolicy.destinationSupport(",
                "MoveLandoStayPolicy.destinationStay(",
                "MoveTransitPolicy.spaceDestination(",
                "MoveDestinationPolicy.evazanCombo("}) {
            assertExactlyOnce(selection, call);
        }
    }

    @Test
    public void residualReasonsAndArithmeticHaveOneSharedOwner()
            throws IOException {
        String adapters = evaluator("rando", "MoveEvaluator.java")
                + evaluator("rando", "ActionTextEvaluator.java")
                + evaluator("rando", "CardSelectionEvaluator.java");
        String policies = phase("MoveAbilityPolicy.java")
                + phase("MoveDestinationPolicy.java")
                + phase("MoveObjectiveConsolidationPolicy.java")
                + phase("MoveSpyFollowPolicy.java")
                + phase("MoveLandoStayPolicy.java")
                + phase("MoveTransitPolicy.java");

        for (String reason : new String[] {
                "Card not at a location",
                "Pilot slot adds power to ship!",
                "Passenger gives NO power bonus!",
                "Usually avoid disembark/relocate/transfer",
                "Avoid ship-docking",
                "L1/L4 SPLIT (batch1b): weak mover to empty site would create TWO weak solos",
                "No icons at location - low value",
                "V29.7 Move to battleground — force drains!",
                "V24.9: Unoccupied CC site — free force drain if we move here!",
                "V47 LANDO STAY: Lando should stay put — moving wastes force and loses occupation!",
                "V24.14B VEHICLE TO SPACE: Vehicles don't belong in space!",
                "V24.3 EVAZAN COMBO: Move here — combo partner at this site for weapon kill combo!"}) {
            assertFalse(reason, adapters.contains("\"" + reason + "\""));
            assertTrue(reason, policies.contains(reason));
        }

        String moveDestination = moveDestinationSlice(
                evaluator("rando", "CardSelectionEvaluator.java"));
        for (String inlineArithmetic : new String[] {
                "action.addReasoning(\"We have power advantage here\"",
                "float v156JoinBonus = Math.min(",
                "float iconScore = theirIcons *",
                "action.addReasoning(\"V62 SPY DILUTION:",
                "action.addReasoning(\"V24.13 LANDO SUPPORT:",
                "action.addReasoning(\"V24.14B WEAPON CHAR TO SPACE:",
                "action.addReasoning(\"V24.3 EVAZAN COMBO:"}) {
            assertFalse(inlineArithmetic, moveDestination.contains(inlineArithmetic));
        }
    }

    @Test
    public void moveDestinationOrderRemainsFrozen() throws IOException {
        String source = moveDestinationSlice(
                evaluator("rando", "CardSelectionEvaluator.java"));

        assertInOrder(source,
                ".vetoMoveDestination(",
                ".vetoMoveOrigin(",
                "MoveAbilityPolicy.weakSplit(",
                "MoveDestinationPolicy.icons(",
                "MoveDestinationPolicy.safeRetreatDestination(",
                "MoveAbilityPolicy.joinDestination(",
                "MoveDrainRoutingPolicy.contestOpponentDrain(",
                "MoveDrainRoutingPolicy.destinationDrain(",
                "MoveDestinationPolicy.retreatToDrain(",
                "MoveDestinationPolicy.power(",
                "applyMoveBattlegroundPolicy(action, isBG, false)",
                "MoveObjectiveConsolidationPolicy.cloudCityDestination(",
                "MoveDestinationPolicy.powerAwareHiddenPathDestination(",
                "MoveObjectiveConsolidationPolicy.hiddenPathSplit(",
                "MoveSpyFollowPolicy.dilution(",
                "MoveLandoStayPolicy.destinationSupport(",
                "MoveLandoStayPolicy.destinationStay(",
                "MoveTransitPolicy.spaceDestination(",
                "MoveDestinationPolicy.hiddenPathPreFlipSuicide(",
                "actions.add(action);",
                "continue;",
                "MoveDestinationPolicy.spyAwareContest(",
                "MoveDestinationPolicy.wrongDirection(",
                "MoveDestinationPolicy.castleRetreat(",
                "MoveDestinationPolicy.evazanCombo(",
                "actions.add(action);");
    }

    @Test
    public void adapterControlAndObservationBoundariesRemainVisible()
            throws IOException {
        String actionText = evaluator("rando", "ActionTextEvaluator.java");
        String selection = moveDestinationSlice(
                evaluator("rando", "CardSelectionEvaluator.java"));

        assertInOrder(actionText,
                "textLower.contains(\"pilot capacity slot\")",
                "textLower.contains(\"passenger capacity slot\")",
                "MoveTransitPolicy.capacityChoice(",
                "action.setScore(capacity.replacementScore())",
                "actions.add(action);",
                "continue;");
        assertTrue(actionText.contains(
                "for (com.gempukku.swccgo.game.PhysicalCard pc : embarkGs.getAllPermanentCards())"));
        assertTrue(actionText.contains(
                "if (pcLoc != embarkLoc) continue;"));
        assertTrue(actionText.contains(
                "if (unmannedTitle == null) {\n"
                        + "                        unmannedTitle = pc.getTitle();"));
        assertTrue(actionText.contains(
                ".advancesRequiredCardDeployPrerequisiteAt("));
        assertTrue(actionText.contains(
                "} catch (Exception e) {\n            logger.debug(\"evaluateEmbark error:"));
        assertTrue(actionText.contains(
                "for (PhysicalCard loc : gameState.getTopLocations())"));

        assertTrue(selection.contains(
                "catch (Exception fsSplitE) { /* fail-open */ }"));
        assertTrue(selection.contains(
                "catch (Exception e) { /* ignore */ }"));
        assertTrue(selection.contains(
                "actions.add(action);\n                                    continue;"));
        assertTrue(selection.contains(
                "for (PhysicalCard c : destCards)"));
    }

    @Test
    public void battlegroundScoringRemainsInsideTheLegacyCatchEnvelope()
            throws IOException {
        String selection = moveDestinationSlice(
                evaluator("rando", "CardSelectionEvaluator.java"));
        String battleground = between(selection,
                "// V29.7: Bonus for battleground locations",
                "// === V24.9: PREFER UNOCCUPIED CC SITES");

        assertInOrder(battleground,
                "try {",
                ".isBattleground(gameState, location, null)",
                "applyMoveBattlegroundPolicy(action, isBG, false)",
                "} catch (Exception e) {",
                "title.toLowerCase().contains(\"battleground\")",
                "applyMoveBattlegroundPolicy(",
                "action, null, titleContainsBattleground)");
    }

    @Test
    public void forbiddenMoveMetadataAndRoutingSymbolsRemainAbsent()
            throws IOException {
        String changedProduction = phase("MoveAbilityPolicy.java")
                + phase("MoveDestinationPolicy.java")
                + phase("MoveObjectiveConsolidationPolicy.java")
                + phase("MoveSpyFollowPolicy.java")
                + phase("MoveLandoStayPolicy.java")
                + phase("MoveTransitPolicy.java")
                + evaluator("rando", "MoveEvaluator.java")
                + evaluator("rando", "ActionTextEvaluator.java")
                + evaluator("rando", "CardSelectionEvaluator.java");
        for (String forbidden : new String[] {
                "MoveKind",
                "MoveEngineActionMetadata",
                "initializeMoveAction",
                "getOtherSideBlueprintId",
                "setDecisionOrigin",
                "DecisionOrigin",
                "DecisionActionSemantic",
                "DecisionWire",
                "PullDeployRef",
                "PullPhysicalCardRef",
                "DeployDestinationRef",
                "DeployPhysicalCardRef",
                "DeployActionMetadata",
                "player-choice routing"}) {
            assertFalse(forbidden, changedProduction.contains(forbidden));
        }
    }

    private static String moveDestinationSlice(String source) {
        return between(source,
                "private List<EvaluatedAction> evaluateMoveDestination(",
                "private List<EvaluatedAction> evaluateCancelSelection(");
    }

    private static String evaluator(String bot, String file)
            throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators").resolve(file));
    }

    private static String phase(String file) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve(file));
    }

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
    }

    private static String between(
            String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from);
        assertTrue("missing start " + start, from >= 0);
        assertTrue("missing end " + end, to > from);
        return source.substring(from, to);
    }

    private static void assertExactlyOnce(String source, String needle) {
        assertEquals(needle, 1, occurrences(source, needle));
    }

    private static void assertInOrder(String source, String... needles) {
        int previous = -1;
        for (String needle : needles) {
            int next = source.indexOf(needle, previous + 1);
            assertTrue("missing or out of order: " + needle,
                    next > previous);
            previous = next;
        }
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
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
}
