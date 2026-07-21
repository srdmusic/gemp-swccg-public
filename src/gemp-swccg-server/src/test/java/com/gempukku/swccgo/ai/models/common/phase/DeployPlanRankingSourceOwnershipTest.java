package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployPlanRankingSourceOwnershipTest {

    @Test
    public void plannersRemainExactNormalizedMirrors() throws IOException {
        assertEquals(normalize(plannerSource("rando")),
                normalize(plannerSource("chosenone")));
    }

    @Test
    public void plannersRetainReadsAndDelegateOnlyScalarRanking()
            throws IOException {
        String[] retained = {
                "if (plan == null || plan.getInstructions().isEmpty())",
                "Map<String, Integer> powerByLocation = new HashMap<>()",
                "Map<String, Integer> abilityByLocation = new HashMap<>()",
                "for (DeploymentInstruction inst : plan.getInstructions())",
                "if (ability == 0)",
                "ability = Math.min(inst.getPowerContribution(), 3)",
                "for (Map.Entry<String, Integer> entry : powerByLocation.entrySet())",
                "for (AiBoardAnalyzer.LocationAnalysis loc : locations)",
                "objectiveAnalyzer.isObjectiveRelevantLocation(locTitle)",
                "objectiveAnalyzer.getLocationObjectiveBonus(locTitle)",
                "LOG.warn(\"V22 PLAN SCORE: {} is objective-relevant, +{} to plan score\", locTitle, objBonus);",
                "DeployPlanRankingPolicy.evaluate(",
                "DeployPlanRankingPolicy.evaluateAdjunct(",
                "DeployPlanRankingPolicy.evaluateFlipGateFormation(",
                "DeployPlanRankingPolicy.apply(",
                "generateFlipGateFormationPlan(",
                "matchesFlipGateActorRequirement(",
                "hasFlipGateActorAtLocation(",
                "float planScore = isObjectiveFlipGateFormationPlan(bestPlan)",
                "float score = scoreObjectiveCapitalPlan(executorPlan, allLocations, turn)"};
        String[] extracted = {
                "score += inst.getPowerContribution() * 2",
                "50 + (powerAdvantage * 10) + denyDrainBonus + winControlBonus",
                "25 + (powerAdvantage * 5) + denyDrainBonus + winControlBonus",
                "score += 5 + denyDrainBonus",
                "score += 25;  // Can draw destiny",
                "score -= 20 + (ourPower * 2)",
                "float establishBonus = 40",
                "establishBonus -= 500",
                "scorePlan(executorPlan, allLocations, turn) + 200.0f"};
        String[] internalReasons = {
                "Base power value",
                "V22 objective-relevant location bonus",
                "FAVORABLE FIGHT",
                "MARGINAL FIGHT",
                "Can draw destiny",
                "Vulnerable",
                "EMPTY/ESTABLISH LOCATION",
                "V22 objective capital ship priority for Bespin",
                "V297 objective flip-gate actor and buddy formation"};

        for (String bot : new String[]{"rando", "chosenone"}) {
            String planner = plannerSource(bot);
            for (String fragment : retained) {
                assertTrue(bot + ": " + fragment, planner.contains(fragment));
            }
            for (String fragment : extracted) {
                assertFalse(bot + ": " + fragment, planner.contains(fragment));
            }
            for (String reason : internalReasons) {
                assertFalse(bot + ": internal reason " + reason,
                        planner.contains(reason));
            }
        }
    }

    @Test
    public void sharedOwnerIsPureAndApplySidePolicyRemainsSeparate()
            throws IOException {
        String ranking = commonPhaseSource("DeployPlanRankingPolicy.java");
        for (String forbidden : new String[]{
                "DeploymentPlan", "DeploymentInstruction", "LocationAnalysis",
                "ObjectiveAnalyzer", "SwccgGame", "PhysicalCard", "RandoConfig",
                "Decision" + "Origin", "DecisionAction" + "Semantic",
                "Decision" + "Wire", "PullDeploy" + "Ref",
                "PullPhysicalCard" + "Ref", "DeployDestination" + "Ref",
                "DeployPhysicalCard" + "Ref", "DeployAction" + "Metadata",
                "addReasoning", "hardVeto(", "defer(",
                "RandoLogger", "LOG."}) {
            assertFalse(forbidden, ranking.contains(forbidden));
        }

        String applySide = commonPhaseSource("DeployPlanPolicy.java");
        assertTrue(applySide.contains("DEPLOY_PLAN_POLICY"));
        assertTrue(applySide.contains("DEPLOY_PLAN_DESTINATION_POLICY"));
        assertFalse(applySide.contains("DeployPlanRankingPolicy"));
        assertFalse(applySide.contains("deploy-plan-ranking"));
    }

    private static String plannerSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("strategy/DeployPhasePlanner.java"));
    }

    private static String commonPhaseSource(String file) throws IOException {
        return Files.readString(mainJavaRoot().resolve(
                "com/gempukku/swccgo/ai/models/common/phase").resolve(file));
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
