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

public class MoveCardSelectionDrainRoutingSourceParityTest {
    @Test
    public void cardSelectionEvaluatorsStayNormalizedMirrors()
            throws IOException {
        assertEquals(normalize(cardSelectionSource("rando")),
                normalize(cardSelectionSource("chosenone")));
    }

    @Test
    public void fourDrainRoutingDecisionsHaveSharedOwners()
            throws IOException {
        String adapter = cardSelectionSource("rando");
        String drainPolicy = drainPolicySource();
        String transitPolicy = transitPolicySource();

        assertEquals(1, countOccurrences(
                adapter,
                "MoveDrainRoutingPolicy.contestOpponentDrain("));
        assertEquals(1, countOccurrences(
                adapter,
                "MoveTransitPolicy.isDrainTransitStagingSite("));
        assertEquals(1, countOccurrences(
                adapter,
                "MoveDrainRoutingPolicy.destinationDrain("));
        assertEquals(1, countOccurrences(
                adapter,
                "MoveDrainRoutingPolicy.moveFromDrain("));
        assertTrue(drainPolicy.contains(
                "public static Contribution contestOpponentDrain("));
        assertTrue(drainPolicy.contains(
                "public static DestinationDrain destinationDrain("));
        assertTrue(drainPolicy.contains(
                "public static Contribution moveFromDrain("));
        assertTrue(transitPolicy.contains(
                "public static boolean isDrainTransitStagingSite("));
    }

    @Test
    public void adapterRetainsReadsParsingLoggingAndActionMutation()
            throws IOException {
        String block = drainRoutingBlock(cardSelectionSource("rando"));

        assertTrue(block.contains("getForceDrainAmount("));
        assertTrue(block.contains("computeNetDrainBalance("));
        assertTrue(block.contains("gameState.getCardsAtLocation(location)"));
        assertTrue(block.contains("isBattleground(gameState, location, null)"));
        assertTrue(block.contains("context.getDecisionText()"));
        assertTrue(block.contains("java.util.regex.Pattern.compile("));
        assertTrue(block.contains("gameState.getAllPermanentCards()"));
        assertTrue(block.contains("fromBp.getIconCount("));
        assertTrue(block.contains("action.addReasoning("));
        assertTrue(block.contains("logger.warn("));
        assertTrue(block.contains("logger.info("));
        assertTrue(block.contains("logger.debug(\"V166 error: {}\""));
    }

    @Test
    public void adapterNoLongerOwnsDrainScoreMathOrTransitClassifier()
            throws IOException {
        String block = drainRoutingBlock(cardSelectionSource("rando"));

        assertFalse(block.contains("200.0f + Math.max("));
        assertFalse(block.contains("v67eExpectedDrain * 12.0f"));
        assertFalse(block.contains("-250.0f * dropAmt"));
        assertFalse(block.contains("contains(\"underground corridor\")"));
        assertFalse(block.contains(
                "V67n TRANSIT STAGING DEST: \" + title"));
        assertFalse(block.contains(
                "V67g ZERO DRAIN: \" + title"));
    }

    @Test
    public void cardSelectionDrainRulesRemainInLegacyOrder()
            throws IOException {
        String adapter = cardSelectionSource("rando");
        int v169 = adapter.indexOf(
                "MoveDestinationPolicy.safeRetreatDestination(");
        int v156 = adapter.indexOf("// === V156 JOIN-GROUP DEST", v169);
        int v166 = adapter.indexOf(
                "MoveDrainRoutingPolicy.contestOpponentDrain(", v156);
        int staging = adapter.indexOf(
                "MoveTransitPolicy.isDrainTransitStagingSite(", v166);
        int destinationDrain = adapter.indexOf(
                "MoveDrainRoutingPolicy.destinationDrain(", staging);
        int moveFromDrain = adapter.indexOf(
                "MoveDrainRoutingPolicy.moveFromDrain(", destinationDrain);
        int retreatToDrain = adapter.indexOf(
                "MoveDestinationPolicy.retreatToDrain(", moveFromDrain);
        int powerAware = adapter.indexOf(
                "MoveDestinationPolicy.powerAwareHiddenPathDestination(",
                retreatToDrain);

        assertTrue(v169 >= 0);
        assertTrue(v156 > v169);
        assertTrue(v166 > v156);
        assertTrue(staging > v166);
        assertTrue(destinationDrain > staging);
        assertTrue(moveFromDrain > destinationDrain);
        assertTrue(retreatToDrain > moveFromDrain);
        assertTrue(powerAware > retreatToDrain);
    }

    @Test
    public void sharedPoliciesContainNoAdapterOrDecisionTransport()
            throws IOException {
        String policies = drainPolicySource() + transitPolicySource();
        for (String forbidden : new String[]{
                "addReasoning", "logger.", "DecisionContext",
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata"}) {
            assertFalse(forbidden, policies.contains(forbidden));
        }
    }

    private static String drainRoutingBlock(String source) {
        int start = source.indexOf(
                "// MoveDrainRoutingPolicy owns V166 drain-contest scoring.");
        int end = source.indexOf(
                "// MoveDestinationPolicy owns V67au retreat-to-drain scoring.",
                start);
        return source.substring(start, end);
    }

    private static String cardSelectionSource(String bot)
            throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators")
                .resolve("CardSelectionEvaluator.java"));
    }

    private static String drainPolicySource() throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("MoveDrainRoutingPolicy.java"));
    }

    private static String transitPolicySource() throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("MoveTransitPolicy.java"));
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
