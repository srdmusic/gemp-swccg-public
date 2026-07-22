package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployMapuzoPlanDestinationSourceParityTest {

    @Test
    public void mapuzoAndPlanAdaptersRemainExactMirrors() throws IOException {
        assertEquals(normalize(packetSlice(evaluatorSource("rando"))),
                normalize(packetSlice(evaluatorSource("chosenone"))));
    }

    @Test
    public void sharedOwnersReplaceAllFourScoreBodies() throws IOException {
        String source = packetSlice(evaluatorSource("rando"));
        assertTrue(source.contains("DeploySitingPolicy.evaluateMapuzoDestination("));
        assertTrue(source.contains("DeployPlanPolicy.evaluateDestinationTarget("));
        assertTrue(source.contains("isCardSelectable(context, plannedTargetIndex)"));
        assertTrue(source.contains(
                "isOpponentUndercoverOnlyTarget(context, plannedTargetId)"));
        assertTrue(source.contains(
                "if (!isPlannedTarget || !plannedTargetSpyBlocked)"));
        assertTrue(source.contains(
                "plannedTargetOffered && !plannedTargetSpyBlocked"));
        for (String retired : new String[]{
                "action.addReasoning(\"V64 MAPUZO DEFENSE:",
                "action.addReasoning(\"V64 MAPUZO TRAP:",
                "action.addReasoning(\"PLANNED TARGET:",
                "action.addReasoning(\"Not planned target"}) {
            assertFalse(retired, source.contains(retired));
        }
        assertFalse("V275 must not add candidate short-circuiting",
                source.contains("continue;"));
    }

    @Test
    public void mapuzoAndPlanReadsStayInLegacyOrder() throws IOException {
        String source = packetSlice(evaluatorSource("rando"));
        int mapuzoGuard = source.indexOf("titleLower.contains(\"mapuzo\")");
        int opponent = source.indexOf(
                "getTotalPowerAtLocation(", mapuzoGuard);
        int blueprintGuard = source.indexOf(
                "if (deployingBlueprintId != null)", opponent);
        int blueprint = source.indexOf(
                "getBlueprintFromId(context, deployingBlueprintId)",
                blueprintGuard);
        int gameText = source.indexOf("deployBp.getGameText()", blueprint);
        int survivor = source.indexOf("contains(\"jedi survivor\")", gameText);
        int mapuzoPolicy = source.indexOf(
                "DeploySitingPolicy.evaluateMapuzoDestination(", survivor);
        int mapuzoLog = source.indexOf("logger.info", mapuzoPolicy);

        int planGuard = source.indexOf(
                "if (plannedTargetId != null)", mapuzoLog);
        int physicalComparison = source.indexOf(
                "cardId.equals(plannedTargetId)", planGuard);
        int apply = source.indexOf(
                "applyDeployPlanDestinationPolicy(action",
                physicalComparison);
        int planPolicy = source.indexOf(
                "DeployPlanPolicy.evaluateDestinationTarget(", apply);
        int planLog = source.indexOf("logger.info", planPolicy);

        assertTrue(mapuzoGuard >= 0 && opponent > mapuzoGuard
                && blueprintGuard > opponent && blueprint > blueprintGuard
                && gameText > blueprint && survivor > gameText
                && mapuzoPolicy > survivor && mapuzoLog > mapuzoPolicy
                && planGuard > mapuzoLog && physicalComparison > planGuard
                && apply > physicalComparison && planPolicy > apply
                && planLog > planPolicy);
    }

    @Test
    public void physicalDestinationParseStillPrecedesBothPolicies()
            throws IOException {
        String source = evaluatorSource("rando");
        int tempRoute = source.indexOf("cardId.startsWith(\"temp\")");
        int physicalLookup = source.indexOf(
                "gameState.findCardById(Integer.parseInt(cardId))", tempRoute);
        int mapuzo = source.indexOf(
                "DeploySitingPolicy.evaluateMapuzoDestination(", physicalLookup);
        int plan = source.indexOf(
                "DeployPlanPolicy.evaluateDestinationTarget(", mapuzo);
        assertTrue(tempRoute >= 0 && physicalLookup > tempRoute
                && mapuzo > physicalLookup && plan > mapuzo);

        for (String owner : new String[]{
                "DeploySitingPolicy.java", "DeployPlanPolicy.java"}) {
            String policy = Files.readString(mainJavaRoot().resolve(
                    "com/gempukku/swccgo/ai/models/common/phase/" + owner));
            for (String forbidden : new String[]{
                    "DecisionContext", "EvaluatedAction", "PhysicalCard",
                    "SwccgGame", "GameState", "ModifiersQuerying", "DeckOracle",
                    "ObjectiveAnalyzer", "DecisionOrigin", "DecisionActionSemantic",
                    "DecisionWire", "DeployActionMetadata"}) {
                assertFalse(owner + ": " + forbidden,
                        policy.contains(forbidden));
            }
        }
    }

    private static String packetSlice(String source) {
        return slice(source,
                "// === V64 MAPUZO JEDI-ONLY RULE ===",
                "// V24.14B: EARLY SPY DETECTION (UNIVERSAL)");
    }

    private static String slice(String source, String startMarker,
                                String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start);
        assertTrue(startMarker, start >= 0);
        assertTrue(endMarker, end > start);
        return source.substring(start, end);
    }

    private static String evaluatorSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/CardSelectionEvaluator.java"));
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

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
    }
}
