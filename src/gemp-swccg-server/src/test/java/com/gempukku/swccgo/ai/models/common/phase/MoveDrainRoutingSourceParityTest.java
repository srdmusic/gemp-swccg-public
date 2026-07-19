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

public class MoveDrainRoutingSourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void actionTextEvaluatorsStayNormalizedMirrors()
            throws IOException {
        assertEquals(normalize(actionTextSource("rando")),
                normalize(actionTextSource("chosenone")));
    }

    @Test
    public void threeDrainRulesHaveOneSharedOwner() throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(
                move, "MoveDrainRoutingPolicy.uncontestedDeparture("));
        assertEquals(1, countOccurrences(
                move, "MoveDrainRoutingPolicy.explicitDestinationDrain("));
        assertEquals(1, countOccurrences(
                move, "MoveDrainRoutingPolicy.cantinaShuttle("));
        assertTrue(policy.contains(
                "public static UncontestedDeparture uncontestedDeparture("));
        assertTrue(policy.contains(
                "public static ExplicitDestinationDrain explicitDestinationDrain("));
        assertTrue(policy.contains(
                "public static CantinaShuttle cantinaShuttle("));
    }

    @Test
    public void moveToHereDrainGuardHasOneSharedOwner() throws IOException {
        String actionText = actionTextSource("rando");
        String policy = policySource();

        assertEquals(1, countOccurrences(actionText,
                "MoveDrainRoutingPolicy.isMoveToHereAction("));
        assertEquals(1, countOccurrences(actionText,
                "MoveDrainRoutingPolicy.moveToHereDrain("));
        assertTrue(policy.contains(
                "public static boolean isMoveToHereAction("));
        assertTrue(policy.contains(
                "public static MoveToHereDrain moveToHereDrain("));
        assertFalse(actionText.contains(
                "V67ae MOVE-TO-NON-DRAIN: '%s' destination has 0 opp icons"));
        assertFalse(actionText.contains(
                "V67ae RETREAT EXEMPT: '%s' hopelessly outgunned"));
    }

    @Test
    public void moveToHereAdapterRetainsBoardReadsLogsAndFailureBoundary()
            throws IOException {
        String actionText = actionTextSource("rando");

        assertTrue(actionText.contains("gameState.findCardById("));
        assertTrue(actionText.contains("getIconCount("));
        assertTrue(actionText.contains("gameState.getTopLocations()"));
        assertTrue(actionText.contains("getTotalPowerAtLocation("));
        assertTrue(actionText.contains("gameState.getCardsAtLocation(rl)"));
        assertTrue(actionText.contains("gameState.getAttachedCards(rc)"));
        assertTrue(actionText.contains(
                "V67ae RETREAT EXEMPT: doomed={} dest={} — skipping -300"));
        assertTrue(actionText.contains(
                "V67ae MOVE-TO-NON-DRAIN: action='{}' dest={} 0-drain — penalize free retreat (-300)"));
        assertTrue(actionText.contains(
                "catch (Exception e) { /* fail-open: no exemption */ }"));
        assertTrue(actionText.contains(
                "catch (Exception e) { logger.debug(\"V67ae error: {}\", e.getMessage()); }"));
        assertTrue(actionText.contains(
                "action.addReasoning(\"V67ae move-to-here action — see drain analysis\", 0.0f);"));
    }

    @Test
    public void adaptersRetainScoreLogLadderAndExceptionOwnership()
            throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains(
                "action.addReasoning(\n                        v85.contribution().reason()"));
        assertTrue(move.contains(
                "V85 UNCONTESTED CHECK: Error: {}"));
        assertTrue(move.contains(
                "action.addReasoning(\n                        drain.contribution().reason()"));
        assertTrue(move.contains(
                "ladderClaimR2(\"V29.13 GOOD DRAIN\""));
        assertTrue(move.contains(
                "V29.13 DRAIN CHECK: Error: {}"));
        assertTrue(move.contains(
                "action.addReasoning(\n                        shuttle.contribution().reason()"));
        assertTrue(move.contains(
                "ladderClaimR2(\"V73 SHUTTLE\""));
        assertTrue(move.contains(
                "V73 SHUTTLE check error: {}"));
    }

    @Test
    public void drainCallsRemainAtThreeLegacyPositions() throws IOException {
        String move = evaluatorSource("rando");
        int threat = move.indexOf("MoveThreatPolicy.evaluate(");
        int v85 = move.indexOf(
                "MoveDrainRoutingPolicy.uncontestedDeparture(", threat);
        int flee = move.indexOf("// === FLEE LOGIC", v85);
        int spread = move.indexOf("MoveOpportunityPolicy.spread(", flee);
        int explicitDrain = move.indexOf(
                "MoveDrainRoutingPolicy.explicitDestinationDrain(", spread);
        int v91 = move.indexOf("// === V91", explicitDrain);
        int shuttle = move.indexOf(
                "MoveDrainRoutingPolicy.cantinaShuttle(", v91);
        int v34 = move.indexOf("// === V34", shuttle);

        assertTrue(threat >= 0);
        assertTrue(v85 > threat);
        assertTrue(flee > v85);
        assertTrue(spread > flee);
        assertTrue(explicitDrain > spread);
        assertTrue(v91 > explicitDrain);
        assertTrue(shuttle > v91);
        assertTrue(v34 > shuttle);
    }

    @Test
    public void policyPreservesThreeDifferentLocationScans()
            throws IOException {
        String policy = policySource();

        assertEquals(2, countOccurrences(
                policy, "gameState.getLocationsInOrder()"));
        assertEquals(1, countOccurrences(
                policy, "gameState.getTopLocations()"));
        assertEquals(1, countOccurrences(
                policy, "isAdjacentSites("));
        assertTrue(policy.contains(
                "if (adjacentDrain > bestAdjacentDrain)"));
        assertTrue(policy.contains(
                "actionTextLower.contains(locationName)"));
        assertTrue(policy.contains(
                "actionDisplayLower.contains(locationTitleLower)"));
    }

    @Test
    public void destinationRulesYieldToSharedPolicyWhileFinalizerStaysOwned()
            throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains(
                "MoveDestinationPolicy.destinationContest("));
        assertTrue(move.contains("ladderClaimR2(\"V34 CONTEST\""));
        assertTrue(move.contains("ladderClaimR2(\"V111 BG ADVANCE\""));
        assertTrue(move.contains("ladderWrongDirVeto = true"));
        assertTrue(move.contains("ladderVetoHard = true"));
        assertTrue(move.contains("// V60 FIX:"));
        assertTrue(move.contains("ladderFinalize(action)"));
        assertTrue(move.contains("MovePredicates.canWinAt("));
    }

    @Test
    public void policyContainsNoAdapterOrEngineDecisionTransport()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "addReasoning", "ladderClaim", "logger.",
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

    private static String actionTextSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/ActionTextEvaluator.java"));
    }

    private static String policySource() throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("MoveDrainRoutingPolicy.java"));
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
