package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpaceDeploymentAllocationSourceOwnershipTest {

    @Test
    public void bothBotsRetainAllFourSpaceAllocationSeams()
            throws IOException {
        for (String bot : new String[]{"rando", "chosenone"}) {
            String parent = botSource(bot,
                    "evaluators/DeployEvaluator.java");
            String child = botSource(bot,
                    "evaluators/CardSelectionEvaluator.java");
            String planner = botSource(bot,
                    "strategy/DeployPhasePlanner.java");
            String actionText = botSource(bot,
                    "evaluators/ActionTextEvaluator.java");
            String legacy = legacySource(bot);

            assertEquals(bot + ": parent deploy", 1,
                    count(parent,
                            "SpaceDeploymentAllocationFactsReader.evaluateParent("));
            assertEquals(bot + ": child destination", 1,
                    count(child,
                            "SpaceDeploymentAllocationFactsReader\n"
                                    + "                            .evaluateDestination("));
            assertEquals(bot + ": planner routes", 2,
                    count(planner,
                            "SpaceDeploymentAllocationPolicy.evaluate("));
            assertEquals(bot + ": planner pilot selector", 3,
                    count(planner, "selectBestPilotForShip("));
            assertEquals(bot + ": planner source quality", 1,
                    count(planner, ".plannerPilotQualityTier("));
            assertEquals(bot + ": all planner pilot seams", 3,
                    count(planner, "pilotQualityTier("));
            assertEquals(bot + ": exact parent pilot assignment", 1,
                    count(parent, "readExactPilotAssignmentFacts("));
            assertEquals(bot + ": exact child, simultaneous, and embark assignments", 3,
                    count(child, "readExactPilotAssignmentFacts("));
            assertEquals(bot + ": exact planner eligibility seams", 2,
                    count(planner, ".isPlannerPilotEligible("));
            assertEquals(bot + ": planner intrinsic power owner", 1,
                    count(planner, ".powerAddedIfPiloting("));
            assertEquals(bot + ": embark starship bypass closed", 1,
                    count(actionText, "readExactPilotAssignmentFacts("));
            String endor = slice(planner,
                    "private DeploymentPlan generateEopEndorSystemPlan(",
                    "private DeploymentPlan generateEopBunkerGarrisonPlan(");
            assertTrue(bot + ": Endor solo uses paid deploy legality",
                    endor.contains("canDeployPaidDirectly("));
            String capital = slice(planner,
                    "private DeploymentPlan generateObjectiveCapitalPlan(",
                    "private DeploymentPlan generateRepilotPlan(");
            assertTrue(bot + ": capital legal pair",
                    capital.contains("selectBestPilotForShip("));
            assertTrue(bot + ": capital solo uses paid deploy legality",
                    capital.contains("canDeployPaidDirectly("));
            assertTrue(bot + ": capital verified package",
                    capital.contains("setVerifiedCrewPackage(true)"));
            assertTrue(bot + ": capital aboard binding",
                    capital.contains("setAboardShipBlueprintId("));
            String repilot = slice(planner,
                    "private DeploymentPlan generateRepilotPlan(",
                    "private record PersistentPlanSelection(");
            assertTrue(bot + ": repilot legal selector",
                    repilot.contains("selectBestPilotForShip("));
            assertTrue(bot + ": repilot aboard binding",
                    repilot.contains("setAboardShipBlueprintId("));
            assertTrue(bot + ": repilot uses paid deploy legality",
                    planner.contains(
                            "pilot, Filters.sameCardId(ship),\n"
                                    + "                                false, 0.0f)"));
            assertTrue(bot + ": package uses paid simultaneous legality",
                    planner.contains(
                            "ship, pilot, false, 0.0f,\n"
                                    + "                    Filters.sameCardId(location), false, 0.0f)"));
            assertEquals(bot + ": legacy coordinator", 1,
                    count(legacy, ".evaluateLegacyUnknownProjection("));
            assertEquals(bot + ": legacy cannot invent projection facts", 0,
                    count(legacy,
                            "SpaceDeploymentAllocationPolicy.evaluate("));
            assertEquals(bot + ": legacy shared score", 1,
                    count(legacy, ".scoreLegacyFallback(allocation)"));
        }
    }

    @Test
    public void inferredRepilotRequiresAnActualPilotIcon()
            throws IOException {
        String reader = commonPhaseSource(
                "SpaceDeploymentAllocationFactsReader.java");
        assertTrue(reader.contains(
                "deployingCard.getBlueprint().hasIcon(Icon.PILOT)"));
    }

    @Test
    public void pureOwnerHasNoEngineOrBotAdapterTypes()
            throws IOException {
        String policy = commonPhaseSource(
                "SpaceDeploymentAllocationPolicy.java");
        for (String forbidden : new String[]{
                "SwccgGame", "GameState", "PhysicalCard",
                "ModifiersQuerying", "AiBoardAnalyzer", "RandoConfig",
                "RandoCalAi", "TheChosenOneAi", "DecisionContext",
                "EvaluatedAction"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
        assertTrue(policy.contains("SYSTEM_ABILITY_TARGET = 4.0f"));
        assertTrue(policy.contains("PRESSURE_BOLSTER_LIMIT = 7.0f"));
    }

    private static String botSource(String bot, String relative)
            throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve(relative));
    }

    private static String legacySource(String bot) throws IOException {
        String file = bot.equals("rando")
                ? "RandoCalAi.java" : "TheChosenOneAi.java";
        return botSource(bot, file);
    }

    private static String commonPhaseSource(String file) throws IOException {
        return Files.readString(mainJavaRoot().resolve(
                "com/gempukku/swccgo/ai/models/common/phase").resolve(file));
    }

    private static int count(String source, String needle) {
        int occurrences = 0;
        int cursor = 0;
        while ((cursor = source.indexOf(needle, cursor)) >= 0) {
            occurrences++;
            cursor += needle.length();
        }
        return occurrences;
    }

    private static String slice(
            String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start);
        assertTrue(startMarker, start >= 0);
        assertTrue(endMarker, end > start);
        return source.substring(start, end);
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
