package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BattleInitiationSourceParityTest {

    @Test
    public void initiationConditionsWeightsAndReasonsHaveOnePureOwner()
            throws IOException {
        String coordinator = coordinatorSource();
        String policy = policySource();

        for (String retired : new String[] {
                "private static final int FAVORABLE_THRESHOLD",
                "private static final int MARGINAL_THRESHOLD",
                "private static final float ABILITY_BATTLE_MAX_POWER_DEFICIT",
                "private static final int MIN_RESERVE_FOR_BATTLE",
                "No favorable battles available - don't initiate",
                "V34 MUST-FIGHT: Opponent draining from",
                "V61 RESERVE EMPTY: 0 cards in Reserve",
                "V27.1 DRAW THEIR FIRE: Opponent has DTF on table!",
                "Behind on life force - slightly more aggressive"}) {
            assertFalse(retired, coordinator.contains(retired));
            assertTrue(retired, policy.contains(retired));
        }

        for (String call : new String[] {
                "BattleInitiationPolicy.barrierRisk(",
                "BattleInitiationPolicy.huntAggression(",
                "BattleInitiationPolicy.inquisitorDestiny(",
                "BattleInitiationPolicy.prediction(",
                "BattleInitiationPolicy.specificBattle(",
                "BattleInitiationPolicy.fallbackLocation(",
                "BattleInitiationPolicy.scanOutcome(",
                "BattleInitiationPolicy.mustFight(",
                "BattleInitiationPolicy.reserve(",
                "BattleInitiationPolicy.interruptForce(",
                "BattleInitiationPolicy.lifeForce("}) {
            assertTrue(call, coordinator.contains(call));
        }
    }

    @Test
    public void coordinatorRetainsFactsPredictionSafetyLogsAndMutation()
            throws IOException {
        String coordinator = coordinatorSource();

        assertTrue(coordinator.contains("SwccgGame game = context.getGame()"));
        assertTrue(coordinator.contains("GameState gameState = context.getGameState()"));
        assertTrue(coordinator.contains("for (PhysicalCard"));
        assertTrue(coordinator.contains("context.predictBattle("));
        assertTrue(coordinator.contains("FormationSafety"));
        assertTrue(coordinator.contains("action.hardVeto("));
        assertTrue(coordinator.contains("logger.warn("));
        assertTrue(coordinator.contains("addReasoning(contribution.reason(), contribution.delta())"));
    }

    @Test
    public void purePolicyHasNoEngineOrDecisionMetadataDependencies()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[] {
                "DecisionContext", "EvaluatedAction", "PhysicalCard",
                "SwccgGame", "GameState", "ModifiersQuerying",
                "hardVeto(", "defer(",
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    @Test
    public void bothBattleAdaptersUseTheSameSharedCoordinator()
            throws IOException {
        for (String bot : new String[] {"rando", "chosenone"}) {
            String evaluator = evaluatorSource(bot);
            assertTrue(evaluator.contains(
                    "BattleDecisionPolicy.canEvaluate(adapt(context))"));
            assertTrue(evaluator.contains(
                    "BattleDecisionPolicy.evaluate(adapt(context))"));
            assertTrue(evaluator.contains(
                    "for (BattleDecisionPolicy.ScoredAction result"));
        }
    }

    private static String coordinatorSource() throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("BattleDecisionPolicy.java"));
    }

    private static String policySource() throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("BattleInitiationPolicy.java"));
    }

    private static String evaluatorSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/BattleEvaluator.java"));
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
