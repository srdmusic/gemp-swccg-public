package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
                "scoreBespinShip(", "scoreSimultaneousDeploy("};
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
                "action.addReasoning(\"V22.5: Deploy pilot+ship"};

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
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/ActionTextEvaluator.java"));
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
