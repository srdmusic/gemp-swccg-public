package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployActionTextSourceParityTest {

    @Test
    public void deployActionTextScoresAndReasonsHaveOnePureOwner()
            throws IOException {
        String policy = policySource();
        String[] methods = {
                "evaluateAmsd(", "scoreDockingBay(",
                "scoreVaderCastle(", "scoreDiningRoomLando(",
                "scoreBespinShip(", "scoreSimultaneousDeploy(",
                "scoreMainGenerator(", "scoreGenericDeploy(",
                "scoreGenericPlayCard("};
        String[] retiredMutations = {
                "action.addReasoning(\"V24 AMSD BLOCKED:",
                "action.addReasoning(\"V24.10 AMSD BLOCKED:",
                "action.addReasoning(\"V45 AMSD UNAFFORDABLE:",
                "action.addReasoning(\"V29.7 DOCKING BAY:",
                "action.addReasoning(\"V29.7 FIRST DOCKING BAY:",
                "action.addReasoning(\"V25 HUNT DOWN:",
                "action.addReasoning(\"Deploy Vader from reserve",
                "action.addReasoning(\"V29.6 DINING ROOM:",
                "action.addReasoning(\"Dining Room: Deploy Lando",
                "action.addReasoning(\"V22.5 CRITICAL: Deploy ship",
                "action.addReasoning(\"V22.5: Deploy ship",
                "action.addReasoning(\"V22.5: Deploy pilot+ship",
                "V160 PUSH TARGET THE MAIN GENERATOR: deck's flip engine",
                "action.addReasoning(\"Never put projection on side of table",
                "action.addReasoning(\"Deploy on location/table",
                "action.addReasoning(\"Special battleground deploy",
                "action.addReasoning(\"No Force available - can't play cards!",
                "action.addReasoning(\"Very low Force (",
                "action.addReasoning(\"Generic play card — moderate priority"};

        for (String method : methods) {
            assertTrue(method, policy.contains(method));
        }
        for (String bot : new String[] {"rando", "chosenone"}) {
            String adapter = adapterSource(bot);
            for (String method : methods) {
                assertTrue(bot + ": " + method,
                        adapter.contains("DeployActionTextPolicy." + method));
            }
            for (String retired : retiredMutations) {
                assertFalse(bot + ": " + retired, adapter.contains(retired));
            }
            assertTrue(adapter.contains("applyDeployActionTextPolicy(action,"));
        }
    }

    @Test
    public void adaptersRetainGameOracleObjectiveAndMutationBoundaries()
            throws IOException {
        String[] retained = {
                "getLocationsInOrder()", "hasAmsdFailedThisTurn(",
                "isCardInHand(\"Admiral Piett\")",
                "isCardInReserve(\"Executor\")",
                "context.getForcePileSize()", "recordAmsdFailedOnTurn(",
                "getTopLocations()", "getCardsAtLocation(",
                "isVaderOnTable(", "needsBespinSystemPresence()",
                "getTotalPowerAtLocation(", "V29.4 AMSD DIAGNOSTIC"};
        for (String bot : new String[] {"rando", "chosenone"}) {
            String adapter = adapterSource(bot);
            for (String read : retained) {
                assertTrue(bot + ": " + read, adapter.contains(read));
            }
            assertTrue(adapter.contains(
                    "== DeployActionTextPolicy.AdapterStep.CONTINUE_ACTION"));
        }
    }

    @Test
    public void residualAdaptersRetainRecognitionReadsAndSkipOrder()
            throws IOException {
        for (String bot : new String[] {"rando", "chosenone"}) {
            String adapter = adapterSource(bot);

            String v160 = slice(adapter,
                    "// === V160 (Steve, 2026-05-29):",
                    "// === V158 RESERVE-DEPLOY BYPASS GUARD");
            assertOrdered(v160,
                    "textLower.contains(\"target the main generator\")",
                    "context.getObjectiveAnalyzer()",
                    "v160OA.isAnalyzed()",
                    "v160OA.isShieldWillBeDown()",
                    "DeployActionTextPolicy.scoreMainGenerator(",
                    "logger.warn(\"V160 SHIELD WILL BE DOWN:");
            assertFalse(v160.contains("action.addReasoning("));

            int skipGate = adapter.indexOf(
                    "// ========== Skip ALL Deploy Actions ==========");
            int skipContinue = adapter.indexOf("continue;", skipGate);
            int genericDeploy = adapter.indexOf(
                    "DeployActionTextPolicy.scoreGenericDeploy(", skipContinue);
            assertTrue(bot, skipGate >= 0);
            assertTrue(bot, skipContinue > skipGate);
            assertTrue(bot, genericDeploy > skipContinue);

            int stackedShield = adapter.indexOf(
                    "ShieldPolicy.isStackedPileShieldSource(");
            int playCardHelper = adapter.indexOf(
                    "evaluatePlayCard(action, context)", stackedShield);
            int forceRead = adapter.indexOf(
                    "int forcePile = context.getForcePileSize();",
                    playCardHelper);
            int playCardPolicy = adapter.indexOf(
                    "DeployActionTextPolicy.scoreGenericPlayCard(", forceRead);
            assertTrue(bot, stackedShield >= 0);
            assertTrue(bot, playCardHelper > stackedShield);
            assertTrue(bot, forceRead > playCardHelper);
            assertTrue(bot, playCardPolicy > forceRead);
        }
    }

    @Test
    public void deployParentAndDestinationAdaptersHaveNoHiddenScores()
            throws IOException {
        for (String bot : new String[] {"rando", "chosenone"}) {
            String destination = slice(evaluatorSource(bot,
                            "CardSelectionEvaluator.java"),
                    "private List<EvaluatedAction> evaluateDeployLocation(",
                    "// ═══ SECTION: FORCE-LOSS");
            for (String mutation : new String[] {
                    "action.addReasoning(", "action.setScore(",
                    "action.hardVeto(", "action.defer("}) {
                assertFalse(bot + ": " + mutation,
                        destination.contains(mutation));
            }

            String parent = evaluatorSource(bot, "DeployEvaluator.java");
            assertFalse(bot, parent.contains("action.addReasoning(\""));
            assertEquals(bot, 1, count(parent,
                    "action.addReasoning(note.reason, note.score);"));
        }
    }

    @Test
    public void pureOwnerHasNoEngineOrForbiddenMetadataDependencies()
            throws IOException {
        String sources = policySource() + factsSource();
        for (String forbidden : new String[] {
                "DecisionContext", "EvaluatedAction", "PhysicalCard",
                "SwccgGame", "GameState", "ModifiersQuerying", "DeckOracle",
                "ObjectiveAnalyzer", "DecisionOrigin", "DecisionActionSemantic",
                "DecisionWire", "PullDeployRef", "PullPhysicalCardRef",
                "DeployDestinationRef", "DeployPhysicalCardRef",
                "DeployActionMetadata", "hardVeto(", "defer("}) {
            assertFalse(forbidden, sources.contains(forbidden));
        }
    }

    private static String policySource() throws IOException {
        return Files.readString(
                phaseRoot().resolve("DeployActionTextPolicy.java"));
    }

    private static String factsSource() throws IOException {
        return Files.readString(
                phaseRoot().resolve("DeployActionTextFacts.java"));
    }

    private static String adapterSource(String bot) throws IOException {
        return evaluatorSource(bot, "ActionTextEvaluator.java");
    }

    private static String evaluatorSource(String bot, String file)
            throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators").resolve(file));
    }

    private static String slice(String source, String start, String end) {
        int startAt = source.indexOf(start);
        int endAt = source.indexOf(end, startAt);
        assertTrue(start, startAt >= 0);
        assertTrue(end, endAt > startAt);
        return source.substring(startAt, endAt);
    }

    private static void assertOrdered(String source, String... tokens) {
        int cursor = -1;
        for (String token : tokens) {
            int next = source.indexOf(token, cursor + 1);
            assertTrue(token, next > cursor);
            cursor = next;
        }
    }

    private static int count(String source, String token) {
        int count = 0;
        int cursor = 0;
        while ((cursor = source.indexOf(token, cursor)) >= 0) {
            count++;
            cursor += token.length();
        }
        return count;
    }

    private static Path phaseRoot() {
        return mainJavaRoot().resolve(
                "com/gempukku/swccgo/ai/models/common/phase");
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
        throw new AssertionError("Could not locate gemp-swccg-server main/java");
    }
}
