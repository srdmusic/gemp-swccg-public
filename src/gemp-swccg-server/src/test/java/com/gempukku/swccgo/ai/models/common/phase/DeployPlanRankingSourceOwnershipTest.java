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
                "Map<String, Float> abilityByLocation = new HashMap<>()",
                "for (DeploymentInstruction inst : plan.getInstructions())",
                "inst.getAbilityContribution(), Float::sum",
                "for (Map.Entry<String, Integer> entry : powerByLocation.entrySet())",
                "for (AiBoardAnalyzer.LocationAnalysis loc : locations)",
                "float postOurPower = targetLoc.ourPower + plannedPower",
                "float postOurAbility = targetLoc.ourAbility + plannedAbility",
                "targetLoc.theirCardCount == 0",
                "PublicImmediateReactAnalyzer.analyze(",
                "DeployPlanRankingPolicy.evaluateEndorAdjustment(",
                "hasPlannedSpyAtTarget(plan, locId)",
                "card.getBlueprint().hasKeyword(Keyword.SPY)",
                "inst.setAbilityContribution(\n            instructionAbilityContribution(card))",
                "shipInstruction.setAbilityContribution(\n                instructionAbilityContribution(best.ship.card))",
                "pilotInstruction.setAbilityContribution(\n                    instructionAbilityContribution(best.pilot.card))",
                "shipAbility + pilotAbility",
                "boolean includePermanentPilots = category == CardCategory.STARSHIP",
                "|| category == CardCategory.VEHICLE",
                "float ability = exactAbility(card, includePermanentPilots)",
                "Float.isFinite(ability) && ability > 0.0f",
                ".isObjectiveRelevantLocation(\n                                targetLoc.location, currentGame,",
                ".getLocationObjectiveBonus(\n                                    targetLoc.location, currentGame,",
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
                "EndorOperationsTacticalPolicy.isBunkerGarrisonPlan(",
                "ability = Math.min(inst.getPowerContribution(), 3)",
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

    @Test
    public void publicReactReaderUsesOnlyExactPublicExecutableEvidence()
            throws IOException {
        String reader = commonStrategySource(
                "PublicImmediateReactAnalyzer.java");
        for (String required : new String[]{
                "AiBoardAnalyzer.getCardCountAtLocation(",
                "getForceDrainAmount(",
                "isProhibitedFromForceDrainingAtLocation(",
                "ModifierType.MAY_NOT_REACT",
                "ModifierType.MAY_NOT_REACT_TO_LOCATION",
                "ModifierType.MAY_MOVE_AS_REACT_TO_LOCATION",
                "ModifierType.MAY_MOVE_OTHER_CARD_AS_REACT_TO_LOCATION",
                "Filters.sameCardId(target)",
                "getMoveUsingLandspeedAction(",
                "getMoveUsingHyperspeedAction(",
                "getMoveWithoutUsingHyperspeedAction(",
                "getMoveUsingSectorMovementAction(",
                "getLandAction(",
                "getTakeOffAction(",
                "getEnterStarshipOrVehicleSiteAction(",
                "getExitStarshipOrVehicleSiteAction(",
                "FormationSafety.weaponBonusOf(",
                "Float.isFinite(effectivePower)"}) {
            assertTrue(required, reader.contains(required));
        }
        for (String forbidden : new String[]{
                "getMoveAsReactAction(", "getMoveAsReactOption(",
                "getHand(", "getReserveDeck(", "getUsedPile(",
                "setBattleOrForceDrainLocation", "mayForceDrain("}) {
            assertFalse(forbidden, reader.contains(forbidden));
        }
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

    private static String commonStrategySource(String file) throws IOException {
        return Files.readString(mainJavaRoot().resolve(
                "com/gempukku/swccgo/ai/models/common/strategy")
                .resolve(file));
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
