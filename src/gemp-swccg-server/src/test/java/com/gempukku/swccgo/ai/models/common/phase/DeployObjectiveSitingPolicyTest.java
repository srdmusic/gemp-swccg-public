package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeployObjectiveSitingPolicyTest {
    @Test
    public void emitsObjectiveSenatorTextAndGuardScoresInLegacyOrder() {
        List<PolicyOperation> operations = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", false, true, true, 150.0f,
                true, true, true, true,
                true, false, false, "galactic senate",
                0.0f, 0.0f));

        assertEquals(3, operations.size());
        assertOperation(operations.get(0), "V22-objective-location", 150.0f);
        assertOperation(operations.get(1), "V88-CS", 1500.0f);
        assertOperation(operations.get(2), "V88-text-named", 500.0f);
    }

    @Test
    public void wrongSiteSenatorKeepsDominantPenalty() {
        List<PolicyOperation> operations = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", false, true, false, 0.0f,
                true, true, true, false,
                false, false, false, "landing platform",
                0.0f, 0.0f));

        assertEquals(1, operations.size());
        assertOperation(operations.get(0), "V88-CS", -2000.0f);
    }

    @Test
    public void undercoverSpyDoesNotReceiveObjectivePresenceScore() {
        List<PolicyOperation> operations = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", true, true, true, 500.0f,
                false, false, true, false,
                false, false, false, "audience chamber",
                0.0f, 0.0f));
        assertTrue(operations.isEmpty());
    }

    @Test
    public void negativeTextAlwaysPenalizesButDoomedPositiveIsWithheld() {
        List<PolicyOperation> negative = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", false, false, false, 0.0f,
                false, false, true, false,
                true, true, true, "audience chamber",
                0.0f, 0.0f));
        assertEquals(1, negative.size());
        assertOperation(negative.get(0), "V88-text-named", -500.0f);

        List<PolicyOperation> doomed = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", false, false, false, 0.0f,
                false, false, true, false,
                true, false, true, "audience chamber",
                0.0f, 0.0f));
        assertTrue(doomed.isEmpty());
    }

    @Test
    public void senateGuardBlocksOnlyWhenDefenseIsNotNeeded() {
        List<PolicyOperation> blocked = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", false, false, false, 0.0f,
                false, false, true, true,
                false, false, false, "galactic senate",
                4.0f, 4.0f));
        assertEquals(1, blocked.size());
        assertOperation(blocked.get(0), "V99-CS", -1500.0f);

        List<PolicyOperation> defense = evaluate(new DeployObjectiveSitingPolicy.Facts(
                "a", false, false, false, 0.0f,
                false, false, true, true,
                false, false, false, "galactic senate",
                5.0f, 4.0f));
        assertTrue(defense.isEmpty());
    }

    @Test
    public void directObjectivePrioritiesKeepDistinctRuleArms() {
        PolicyOperation army = DeployObjectiveSitingPolicy.scoreCloudCityArmy(
                new DeployObjectiveSitingPolicy.CloudCityArmyFacts(
                        "a", "Cloud City: Downtown Plaza"))
                .operations().get(0);
        PolicyOperation objective = DeployObjectiveSitingPolicy.scoreObjectiveFirst(
                new DeployObjectiveSitingPolicy.ObjectiveFirstFacts(
                        "a", "Cloud City: Downtown Plaza"))
                .operations().get(0);
        PolicyOperation key = DeployObjectiveSitingPolicy.scoreKeyCharacter(
                new DeployObjectiveSitingPolicy.KeyCharacterFacts(
                        "a", "Darth Vader"))
                .operations().get(0);

        assertOperation(army, "V51-CC-ARMY", 500.0f);
        assertOperation(objective, "V51-OBJ-FIRST", 300.0f);
        assertOperation(key, "V67ak", 800.0f);
    }

    @Test
    public void cloudCityEnginePreservesBlockedPriorityAndSafeBranches() {
        DeployObjectiveSitingPolicy.CloudCityEngineEvaluation blocked =
                DeployObjectiveSitingPolicy.evaluateCloudCityEngine(
                        new DeployObjectiveSitingPolicy.CloudCityEngineFacts(
                                "a", "Cloud City Occupation", false, false));
        DeployObjectiveSitingPolicy.CloudCityEngineEvaluation priority =
                DeployObjectiveSitingPolicy.evaluateCloudCityEngine(
                        new DeployObjectiveSitingPolicy.CloudCityEngineFacts(
                                "a", "Cloud City Occupation", true, false));
        DeployObjectiveSitingPolicy.CloudCityEngineEvaluation safe =
                DeployObjectiveSitingPolicy.evaluateCloudCityEngine(
                        new DeployObjectiveSitingPolicy.CloudCityEngineFacts(
                                "a", "Cloud City Occupation", true, true));

        assertEquals(DeployObjectiveSitingPolicy.CloudCityEngineOutcome.BLOCKED,
                blocked.outcome());
        assertOperation(blocked.result().operations().get(0), "V22.7", -800.0f);
        assertEquals(DeployObjectiveSitingPolicy.CloudCityEngineOutcome.ENGINE_PRIORITY,
                priority.outcome());
        assertOperation(priority.result().operations().get(0), "V24", 300.0f);
        assertEquals(DeployObjectiveSitingPolicy.CloudCityEngineOutcome.SAFE,
                safe.outcome());
        assertOperation(safe.result().operations().get(0), "V22.7", 50.0f);
    }

    @Test
    public void gherantKeepsExecutorSitePriority() {
        PolicyOperation operation = DeployObjectiveSitingPolicy.scoreGherant(
                new DeployObjectiveSitingPolicy.GherantFacts("a"))
                .operations().get(0);
        assertOperation(operation, "V24.1", 150.0f);
    }

    @Test
    public void landoWinsLegacyPrecedenceWhenBothNamesAppear() {
        DeployObjectiveSitingPolicy.LandoLobotEvaluation safe =
                DeployObjectiveSitingPolicy.evaluateLandoLobot(
                        new DeployObjectiveSitingPolicy.LandoLobotFacts(
                                "a", true, true, true));
        DeployObjectiveSitingPolicy.LandoLobotEvaluation blocked =
                DeployObjectiveSitingPolicy.evaluateLandoLobot(
                        new DeployObjectiveSitingPolicy.LandoLobotFacts(
                                "a", true, true, false));

        assertEquals(DeployObjectiveSitingPolicy.LandoLobotOutcome.LANDO_SAFE,
                safe.outcome());
        assertOperation(safe.result().operations().get(0),
                "V29.2-LANDO", 200.0f);
        assertEquals(DeployObjectiveSitingPolicy.LandoLobotOutcome.LANDO_BLOCKED,
                blocked.outcome());
        assertOperation(blocked.result().operations().get(0),
                "V47-LANDO", -9999.0f);
    }

    @Test
    public void lobotKeepsSafeAndSoloScores() {
        DeployObjectiveSitingPolicy.LandoLobotEvaluation safe =
                DeployObjectiveSitingPolicy.evaluateLandoLobot(
                        new DeployObjectiveSitingPolicy.LandoLobotFacts(
                                "a", false, true, true));
        DeployObjectiveSitingPolicy.LandoLobotEvaluation blocked =
                DeployObjectiveSitingPolicy.evaluateLandoLobot(
                        new DeployObjectiveSitingPolicy.LandoLobotFacts(
                                "a", false, true, false));

        assertOperation(safe.result().operations().get(0),
                "V29.2-LOBOT", 150.0f);
        assertOperation(blocked.result().operations().get(0),
                "V47-LOBOT", -9999.0f);
    }

    @Test
    public void preFlipDefenseKeepsHuntDownTierLadder() {
        assertFlipScore(250.0f, 5, false, false);
        assertFlipScore(500.0f, 5, true, false);
        assertFlipScore(800.0f, 3, true, false);
        assertFlipScore(1000.0f, 3, true, true);
    }

    @Test
    public void preFlipSpreadAndPostFlipHoldRemainExclusive() {
        DeployObjectiveSitingPolicy.FlipSitingEvaluation spread =
                DeployObjectiveSitingPolicy.evaluateFlipSiting(
                        new DeployObjectiveSitingPolicy.FlipSitingFacts(
                                "a", false, 0, false, false,
                                1, 2, false, false, false));
        DeployObjectiveSitingPolicy.FlipSitingEvaluation hold =
                DeployObjectiveSitingPolicy.evaluateFlipSiting(
                        new DeployObjectiveSitingPolicy.FlipSitingFacts(
                                "a", true, 0, false, false,
                                3, 0, false, true, true));

        assertEquals(DeployObjectiveSitingPolicy.FlipSitingOutcome.PREFLIP_SPREAD,
                spread.outcome());
        assertOperation(spread.result().operations().get(0),
                "V31-PREFLIP", -50.0f);
        assertEquals(DeployObjectiveSitingPolicy.FlipSitingOutcome.POSTFLIP_HOLD,
                hold.outcome());
        assertOperation(hold.result().operations().get(0),
                "V31-POSTFLIP", 200.0f);
    }

    @Test
    public void postFlipThirdObjectiveLocationStaysNeutral() {
        DeployObjectiveSitingPolicy.FlipSitingEvaluation neutral =
                DeployObjectiveSitingPolicy.evaluateFlipSiting(
                        new DeployObjectiveSitingPolicy.FlipSitingFacts(
                                "a", true, 0, false, false,
                                3, 0, false, false, true));

        assertEquals(
                DeployObjectiveSitingPolicy.FlipSitingOutcome.POSTFLIP_THIRD_NEUTRAL,
                neutral.outcome());
        assertOperation(neutral.result().operations().get(0),
                "V40-POSTFLIP", 0.0f);
    }

    private static List<PolicyOperation> evaluate(DeployObjectiveSitingPolicy.Facts facts) {
        return DeployObjectiveSitingPolicy.evaluate(facts).operations();
    }

    private static void assertFlipScore(float expected, int turnNumber,
                                        boolean huntDown, boolean inquisitor) {
        DeployObjectiveSitingPolicy.FlipSitingEvaluation evaluation =
                DeployObjectiveSitingPolicy.evaluateFlipSiting(
                        new DeployObjectiveSitingPolicy.FlipSitingFacts(
                                "a", false, turnNumber, huntDown, inquisitor,
                                1, 2, true, false, false));
        assertEquals(DeployObjectiveSitingPolicy.FlipSitingOutcome.PREFLIP_DEFEND,
                evaluation.outcome());
        assertOperation(evaluation.result().operations().get(0),
                "V36", expected);
    }

    private static void assertOperation(PolicyOperation operation,
                                        String ruleId, float delta) {
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(delta, operation.delta(), 0.0f);
    }
}
