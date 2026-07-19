package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployFormationTopologySourceParityTest {

    private static final String SLICE_START = "// No opponent power - uncontested";
    private static final String SLICE_END =
            "// === V29.7: BATTLEGROUND PREFERENCE FOR CHARACTER DEPLOYMENT ===";

    @Test
    public void randoAndChosenOneFormationTopologyAdaptersStayExactMirrors()
            throws IOException {
        assertEquals(normalize(slice(source("rando"))),
                normalize(slice(source("chosenone"))));
    }

    @Test
    public void sharedPolicyOwnsFormationTopologyScoresWithoutChangingReads()
            throws IOException {
        String adapter = slice(source("rando"));

        assertTrue(adapter.contains(".evaluateEmptyDestinationTopology("));
        assertTrue(adapter.contains(".evaluateReinforcementTopology("));
        assertTrue(adapter.contains("DeployFormationSitingPolicy.evaluateBuddyTopology("));

        assertFalse(adapter.contains("float concPenalty ="));
        assertFalse(adapter.contains("float reinforceBonus ="));
        assertFalse(adapter.contains("action.addReasoning("));
        assertFalse(adapter.contains("actions.add("));
        assertEquals(8, countOccurrences(adapter, "continue;"));
        assertEquals(2, countOccurrences(adapter, "break;"));

        int concentrationScan = adapter.indexOf("gameState.getTopLocations()");
        int concentrationCatch = adapter.indexOf("catch (Exception e)", concentrationScan);
        int emptyPolicy = adapter.indexOf(".evaluateEmptyDestinationTopology(",
                concentrationCatch);
        int emptyLog = adapter.indexOf("logger.warn(\"V29 CONCENTRATE:", emptyPolicy);
        int characterCount = adapter.indexOf("int ourCharsHere = 0;", emptyLog);
        int deficit = adapter.indexOf("float v67bnDeficit =", characterCount);
        int escapeScan = adapter.indexOf("gameState.getAllPermanentCards()", deficit);
        int escapeCatch = adapter.indexOf("catch (Exception eEsc)", escapeScan);
        int reinforcementPolicy = adapter.indexOf(".evaluateReinforcementTopology(",
                escapeCatch);
        int reinforcementLog = adapter.indexOf(
                "logger.warn(\"V67bn REINFORCE OUTGUNNED:", reinforcementPolicy);
        int buddyRead = adapter.indexOf("String locOwner = location.getOwner();",
                reinforcementLog);
        int buddyPolicy = adapter.indexOf(
                "DeployFormationSitingPolicy.evaluateBuddyTopology(", buddyRead);

        assertTrue(concentrationScan >= 0);
        assertTrue(concentrationCatch > concentrationScan);
        assertTrue(emptyPolicy > concentrationCatch);
        assertTrue(emptyLog > emptyPolicy);
        assertTrue(characterCount > emptyLog);
        assertTrue(deficit > characterCount);
        assertTrue(escapeScan > deficit);
        assertTrue(escapeCatch > escapeScan);
        assertTrue(reinforcementPolicy > escapeCatch);
        assertTrue(reinforcementLog > reinforcementPolicy);
        assertTrue(buddyRead > reinforcementLog);
        assertTrue(buddyPolicy > buddyRead);
    }

    @Test
    public void pureOwnerContainsNoEngineDecisionMetadata() throws IOException {
        String policy = Files.readString(mainJavaRoot().resolve(
                "com/gempukku/swccgo/ai/models/common/phase/DeployFormationSitingPolicy.java"));
        for (String forbidden : new String[]{
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    private static String source(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/CardSelectionEvaluator.java"));
    }

    private static String slice(String source) {
        int start = source.indexOf(SLICE_START);
        int end = source.indexOf(SLICE_END, start);
        assertTrue(start >= 0);
        assertTrue(end > start);
        return source.substring(start, end);
    }

    private static Path mainJavaRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            Path repoLayout = cursor.resolve("src/gemp-swccg-server/src/main/java");
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
        throw new AssertionError("Could not locate gemp-swccg-server main/java");
    }

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
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
