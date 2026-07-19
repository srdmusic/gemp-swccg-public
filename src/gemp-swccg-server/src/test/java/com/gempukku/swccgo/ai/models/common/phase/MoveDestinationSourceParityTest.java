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

public class MoveDestinationSourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void destinationRulesHaveOneSharedOwner() throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveDestinationPolicy.landedShipEscape("));
        assertEquals(1, countOccurrences(
                move, "MoveDestinationPolicy.destinationContest("));
        assertTrue(policy.contains(
                "public static LandedShipEscape landedShipEscape("));
        assertTrue(policy.contains(
                "public static DestinationContest destinationContest("));
    }

    @Test
    public void adaptersRetainScoreLadderVetoAndLoggingOwnership()
            throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains(
                "escape.contribution().reason()"));
        assertTrue(move.contains(
                "ladderClaimR3(\"V91 ESCAPE LANDED SHIP\")"));
        assertTrue(move.contains(
                "destination.contestContribution().reason()"));
        assertTrue(move.contains(
                "ladderClaimR2(\"V34 CONTEST\""));
        assertTrue(move.contains(
                "ladderClaimR2(\"V111 BG ADVANCE\""));
        assertTrue(move.contains("ladderWrongDirVeto = true"));
        assertTrue(move.contains("ladderVetoHard = true"));
        assertTrue(move.contains(
                "V38.3 CASTLE RETREAT BLOCKED (LADDER VETO)"));
    }

    @Test
    public void callsRemainAtLegacyPositions() throws IOException {
        String move = evaluatorSource("rando");
        int explicitDrain = move.indexOf(
                "MoveDrainRoutingPolicy.explicitDestinationDrain(");
        int escape = move.indexOf(
                "MoveDestinationPolicy.landedShipEscape(", explicitDrain);
        int shuttle = move.indexOf(
                "MoveDrainRoutingPolicy.cantinaShuttle(", escape);
        int contest = move.indexOf(
                "MoveDestinationPolicy.destinationContest(", shuttle);
        int methodEnd = move.indexOf(
                "// Default: not a good time to move", contest);

        assertTrue(explicitDrain >= 0);
        assertTrue(escape > explicitDrain);
        assertTrue(shuttle > escape);
        assertTrue(contest > shuttle);
        assertTrue(methodEnd > contest);
    }

    @Test
    public void policyPreservesIndependentScansAndPredicates()
            throws IOException {
        String policy = policySource();

        assertEquals(1, countOccurrences(
                policy, "gameState.getAllPermanentCards()"));
        assertEquals(3, countOccurrences(
                policy, "gameState.getLocationsInOrder()"));
        assertTrue(policy.contains(
                "cardLocation == location"));
        assertTrue(policy.contains(
                "actionLower.contains(locationName)"));
        assertTrue(policy.contains(
                "opponentPowerAtDestination > 0"));
        assertTrue(policy.contains(
                "opponentPower > opponentUncontestedPower"));
        assertTrue(policy.contains(
                "destinationTitle.contains(\"mustafar\")"));
    }

    @Test
    public void protectedMoveMachineryRemainsUntouched()
            throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains("// V60 FIX:"));
        assertTrue(move.contains("MovePredicates.canWinAt("));
        assertTrue(move.contains("oppWeaponBonusAt("));
        assertTrue(move.contains("ladderFinalize(action)"));
        assertFalse(move.contains("MovePhysicalCardResolver"));
    }

    @Test
    public void policyContainsNoAdapterOrEngineDecisionTransport()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "addReasoning", "ladderClaim", "ladderVeto", "logger.",
                "PolicyOperation", "PolicyResult", "DecisionContext",
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata"}) {
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
                .resolve("MoveDestinationPolicy.java"));
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
