package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpaceDeploymentAllocationPolicyTest {

    @Test
    public void buddyProgressRemainsAdmissibleUntilActualAbilityFour() {
        SpaceDeploymentAllocationPolicy.Evaluation progress = evaluate(
                2.0f, 3.0f, false, false,
                false, false, false);
        assertEquals(
                SpaceDeploymentAllocationPolicy.Outcome.BUDDY_PROGRESS,
                progress.outcome());
        assertFalse(hasDefer(progress.result().operations()));

        SpaceDeploymentAllocationPolicy.Evaluation completes = evaluate(
                3.99f, 4.0f, false, false,
                false, false, false);
        assertEquals(
                SpaceDeploymentAllocationPolicy.Outcome.BUDDY_COMPLETE,
                completes.outcome());
        assertFalse(hasDefer(completes.result().operations()));
        assertEquals("V298-space-buddy-complete",
                completes.result().operations().get(0).ruleArmId().id());
    }

    @Test
    public void actualAbilityFourDefersQuietExtraSpace() {
        for (float current : new float[]{4.0f, 4.01f, 7.0f}) {
            SpaceDeploymentAllocationPolicy.Evaluation evaluation = evaluate(
                    current, current + 2.0f, false, false,
                    false, false, false);
            assertEquals(
                    SpaceDeploymentAllocationPolicy.Outcome.GROUND_FIRST_AFTER_FOUR,
                    evaluation.outcome());
            assertTrue(hasDefer(evaluation.result().operations()));
            assertEquals("V298-space-ground-first",
                    evaluation.result().operations().get(0).ruleArmId().id());
            assertEquals(-1000,
                    SpaceDeploymentAllocationPolicy
                            .scoreLegacyFallback(evaluation));
        }
    }

    @Test
    public void permanentPilotAbilityIsAlreadyPartOfTheActualTotal() {
        SpaceDeploymentAllocationPolicy.Evaluation evaluation = evaluate(
                4.0f, 6.0f, false, false,
                false, false, false);

        assertEquals(
                "The adapter supplies engine total ability, including permanent pilots; "
                        + "the policy must not add them a second time",
                SpaceDeploymentAllocationPolicy.Outcome.GROUND_FIRST_AFTER_FOUR,
                evaluation.outcome());
        assertTrue(hasDefer(evaluation.result().operations()));
    }

    @Test
    public void boundedEnemyPressureCanBolsterOnlyBelowAbilitySeven() {
        SpaceDeploymentAllocationPolicy.Evaluation pressure = evaluate(
                4.0f, 6.0f, true, false,
                false, false, false);
        assertEquals(
                SpaceDeploymentAllocationPolicy.Outcome.PRESSURE_EXCEPTION,
                pressure.outcome());
        assertFalse(hasDefer(pressure.result().operations()));

        SpaceDeploymentAllocationPolicy.Evaluation oversizedBolster =
                evaluate(4.0f, 8.0f, true, false,
                        false, false, false);
        assertEquals(
                "Pressure may bolster a little, not jump past ability seven",
                SpaceDeploymentAllocationPolicy.Outcome.GROUND_FIRST_AFTER_FOUR,
                oversizedBolster.outcome());
        assertTrue(hasDefer(oversizedBolster.result().operations()));

        SpaceDeploymentAllocationPolicy.Evaluation alreadyBolstered = evaluate(
                7.0f, 9.0f, true, false,
                false, false, false);
        assertEquals(
                SpaceDeploymentAllocationPolicy.Outcome.GROUND_FIRST_AFTER_FOUR,
                alreadyBolstered.outcome());
        assertTrue(hasDefer(alreadyBolstered.result().operations()));
    }

    @Test
    public void legacyUnknownProjectionFailsClosedAfterAbilityFour() {
        SpaceDeploymentAllocationPolicy.Evaluation underFour =
                SpaceDeploymentAllocationPolicy
                        .evaluateLegacyUnknownProjection(
                                "legacy-under-four", 3.0f);
        assertEquals(
                SpaceDeploymentAllocationPolicy.Outcome.BUDDY_PROGRESS,
                underFour.outcome());
        assertFalse(hasDefer(underFour.result().operations()));

        SpaceDeploymentAllocationPolicy.Evaluation afterFour =
                SpaceDeploymentAllocationPolicy
                        .evaluateLegacyUnknownProjection(
                                "legacy-after-four", 4.0f);
        assertEquals(
                SpaceDeploymentAllocationPolicy.Outcome
                        .GROUND_FIRST_AFTER_FOUR,
                afterFour.outcome());
        assertTrue(hasDefer(afterFour.result().operations()));
        assertEquals(-1000, SpaceDeploymentAllocationPolicy
                .scoreLegacyFallback(afterFour));
    }

    @Test
    public void favorableBattleObjectiveRepilotAndTerminalDefenseRemainEligible() {
        assertException(
                SpaceDeploymentAllocationPolicy.Outcome.FAVORABLE_BATTLE_EXCEPTION,
                evaluate(7.0f, 9.0f, false, true,
                        false, false, false));
        assertException(
                SpaceDeploymentAllocationPolicy.Outcome.OBJECTIVE_EXCEPTION,
                evaluate(7.0f, 9.0f, false, false,
                        true, false, false));
        assertException(
                SpaceDeploymentAllocationPolicy.Outcome.REPILOT_EXCEPTION,
                evaluate(7.0f, 9.0f, false, false,
                        false, true, false));
        assertException(
                SpaceDeploymentAllocationPolicy.Outcome.TERMINAL_EXCEPTION,
                evaluate(7.0f, 9.0f, false, false,
                        false, false, true));
    }

    @Test
    public void nonSpaceRoutesRemainUntouched() {
        SpaceDeploymentAllocationPolicy.Evaluation evaluation =
                SpaceDeploymentAllocationPolicy.evaluate(
                        new SpaceDeploymentAllocationPolicy.Facts(
                                "ground", false, 9.0f, 11.0f,
                                true, true, true, true, true));
        assertEquals(SpaceDeploymentAllocationPolicy.Outcome.NO_SPACE_ROUTE,
                evaluation.outcome());
        assertTrue(evaluation.result().operations().isEmpty());
        assertEquals(0, SpaceDeploymentAllocationPolicy
                .scoreLegacyFallback(evaluation));
    }

    private static SpaceDeploymentAllocationPolicy.Evaluation evaluate(
            float currentAbility, float projectedAbility,
            boolean opponentPressure, boolean favorableBattle,
            boolean objectiveNeed, boolean orphanRepilot,
            boolean terminalDefense) {
        return SpaceDeploymentAllocationPolicy.evaluate(
                new SpaceDeploymentAllocationPolicy.Facts(
                        "space", true, currentAbility, projectedAbility,
                        opponentPressure, favorableBattle, objectiveNeed,
                        orphanRepilot, terminalDefense));
    }

    private static void assertException(
            SpaceDeploymentAllocationPolicy.Outcome expected,
            SpaceDeploymentAllocationPolicy.Evaluation evaluation) {
        assertEquals(expected, evaluation.outcome());
        assertFalse(hasDefer(evaluation.result().operations()));
    }

    private static boolean hasDefer(List<PolicyOperation> operations) {
        return operations.stream().anyMatch(operation ->
                operation.kind() == PolicyOperationKind.DEFER);
    }
}
