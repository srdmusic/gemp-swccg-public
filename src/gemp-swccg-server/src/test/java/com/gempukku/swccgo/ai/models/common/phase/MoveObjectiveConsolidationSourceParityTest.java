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

public class MoveObjectiveConsolidationSourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void objectiveConsolidationHasOneSharedOwner()
            throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveObjectiveConsolidationPolicy.preFlip("));
        assertEquals(1, countOccurrences(
                move, "MoveObjectiveConsolidationPolicy.postFlipPhysical("));
        assertEquals(0, countOccurrences(
                move, "MoveObjectiveConsolidationPolicy.postFlip("));
        assertTrue(policy.contains("public static Evaluation preFlip("));
        assertTrue(policy.contains("public static Evaluation postFlip("));
        assertTrue(policy.contains(
                "public static Evaluation postFlipPhysical("));

        int start = move.indexOf("// V22.5: PRE-FLIP");
        int end = move.indexOf("} else {", start);
        String region = move.substring(start, end);
        assertFalse(region.contains("int preFlipOurChars ="));
        assertFalse(region.contains("float opponentTotalPower ="));
        assertFalse(region.contains("float worstDeficit ="));
        assertFalse(region.contains("String weakestLoc ="));
    }

    @Test
    public void adaptersRetainObjectiveScoreLogAndLadderOwnership()
            throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains("moveObjAnalyzer.isAnalyzed()"));
        assertTrue(move.contains("!moveObjAnalyzer.isFlipped()"));
        assertTrue(move.contains("moveObjAnalyzer.isFlipped()"));
        assertTrue(move.contains(
                "location -> moveObjAnalyzer\n"
                        + "                                    .isFlipBackProtectionLocation(\n"
                        + "                                        location, game, playerId)"));
        assertFalse(move.contains(
                "moveObjAnalyzer::isFlipBackProtectionLocation"));
        assertTrue(move.contains("preFlip.contribution().reason()"));
        assertTrue(move.contains("postFlip.contribution().reason()"));
        assertTrue(move.contains(
                "V22.5 CONSOLIDATE PRE-FLIP:"));
        assertTrue(move.contains("V22.2 PROTECT:"));
        assertTrue(move.contains(
                "ladderClaimR2(\n                                \"V22.5 PRE-FLIP CONSOLIDATE\""));
        assertTrue(move.contains(
                "\"V22.2 POST-FLIP REINFORCE\""));
        assertTrue(move.contains("Could not sum opponent power:"));
        assertTrue(move.contains(
                "Could not analyze protection locations:"));
    }

    @Test
    public void callsRemainBetweenHuntGroupAndMovementTypeScoring()
            throws IOException {
        String move = evaluatorSource("rando");
        int huntGroup = move.indexOf("MoveHuntGroupPolicy.evaluate(");
        int preFlip = move.indexOf(
                "MoveObjectiveConsolidationPolicy.preFlip(", huntGroup);
        int postFlip = move.indexOf(
                "MoveObjectiveConsolidationPolicy.postFlipPhysical(", preFlip);
        int movementTypes = move.indexOf(
                "MoveTransitPolicy.movementTypes(", postFlip);

        assertTrue(huntGroup >= 0);
        assertTrue(preFlip > huntGroup);
        assertTrue(postFlip > preFlip);
        assertTrue(movementTypes > postFlip);
    }

    @Test
    public void policyPreservesThresholdsAndFactOrder()
            throws IOException {
        String policy = policySource();

        for (String value : new String[]{
                "160.0f : 100.0f", "60.0f", "-30.0f",
                "-80.0f", "-120.0f", "-160.0f",
                "80.0f", "120.0f", "160.0f"}) {
            assertTrue(value, policy.contains(value));
        }
        assertTrue(policy.contains("opponentPower > ownPower * 2.0f"));
        assertTrue(policy.contains("opponentPower > ownPower * 3.0f"));
        assertTrue(policy.contains("opponentPower > ownPower * 1.5f"));
        assertTrue(policy.contains("deficit > worstProtectionDeficit"));

        int preTitle = policy.indexOf(
                "String currentLocationName = currentLocation.getTitle();");
        int preOpponent = policy.indexOf(
                "String opponentId = game.getOpponent(playerId);",
                preTitle);
        int preCards = policy.indexOf(
                "gameState.getCardsAtLocation(currentLocation)",
                preOpponent);
        assertTrue(preTitle >= 0);
        assertTrue(preOpponent > preTitle);
        assertTrue(preCards > preOpponent);
    }

    @Test
    public void powerBearingCardsAreNotNarrowedToCharacters()
            throws IOException {
        String policy = policySource();
        assertTrue(policy.contains("hasPowerAttribute()"));
        assertFalse(policy.contains("CardCategory.CHARACTER"));
    }

    @Test
    public void protectedMoveMachineryRemainsAdapterOwned()
            throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains("// V60 FIX:"));
        assertTrue(move.contains("MovePredicates.canWinAt("));
        assertTrue(move.contains("ladderFinalize(action)"));
        assertFalse(move.contains("MovePhysicalCardResolver"));
    }

    @Test
    public void policyContainsNoAdapterOrEngineDecisionTransport()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "addReasoning", "ladderClaim", "ladderVeto", "logger.",
                "ObjectiveAnalyzer", "PolicyOperation", "PolicyResult",
                "DecisionContext", "DecisionOrigin",
                "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef",
                "DeployDestinationRef", "DeployPhysicalCardRef",
                "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    private static String evaluatorSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/MoveEvaluator.java"));
    }

    private static String policySource() throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("MoveObjectiveConsolidationPolicy.java"));
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
