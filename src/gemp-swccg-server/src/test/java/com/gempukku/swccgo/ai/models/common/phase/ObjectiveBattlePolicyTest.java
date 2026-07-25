package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectiveBattlePolicyTest {

    @Test
    public void exactMissingSafeContestReceivesTypedBonusAtBoundary() {
        PolicyResult result = evaluate(
                true, true, true, false, true,
                -2.0f, 3, 6.0f, 7.0f);

        assertEquals(1, result.operations().size());
        PolicyOperation operation = result.operations().get(0);
        assertEquals(
                ObjectiveBattlePolicy.REQUIRED_LOCATION_CONTEST_RULE_ID,
                operation.ruleArmId().id());
        assertEquals(TraceDomainId.BATTLE_INITIATION, operation.domainId());
        assertEquals(TraceOutputKind.BANDED, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(
                ObjectiveBattlePolicy.REQUIRED_LOCATION_CONTEST_BONUS,
                operation.delta(), 0.0f);
    }

    @Test
    public void everyPositiveSafetyFactIsRequired() {
        assertEmpty(evaluate(
                false, true, true, false, true,
                0.0f, 3, 6.0f, 6.0f));
        assertEmpty(evaluate(
                true, false, true, false, true,
                0.0f, 3, 6.0f, 6.0f));
        assertEmpty(evaluate(
                true, true, false, false, true,
                0.0f, 3, 6.0f, 6.0f));
        assertEmpty(evaluate(
                true, true, true, true, true,
                0.0f, 3, 6.0f, 6.0f));
        assertEmpty(evaluate(
                true, true, true, false, false,
                0.0f, 3, 6.0f, 6.0f));
        assertEmpty(evaluate(
                true, true, true, false, true,
                -2.01f, 3, 6.0f, 6.0f));
    }

    @Test
    public void lowReserveRequiresRawOverpowerMarginOfEight() {
        assertEmpty(evaluate(
                true, true, true, false, true,
                7.9f, 2, 11.9f, 4.0f));
        assertEquals(1, evaluate(
                true, true, true, false, true,
                8.0f, 2, 12.0f, 4.0f).operations().size());
    }

    @Test
    public void v25SuicideRemainsExcludedEvenWhenEffectiveDiffPasses() {
        assertEmpty(evaluate(
                true, true, true, false, true,
                -2.0f, 3, 3.0f, 7.0f));
    }

    @Test
    public void armedTerminalObjectiveBattleIsVetoedUnlessClearlySafe() {
        PolicyResult risky =
                ObjectiveBattlePolicy
                    .evaluateTerminalObjectiveHazard(
                        "battle", true, false, 10.0f);

        assertEquals(1, risky.operations().size());
        PolicyOperation veto = risky.operations().get(0);
        assertEquals(
                ObjectiveBattlePolicy
                    .TERMINAL_OBJECTIVE_BATTLE_RULE_ID,
                veto.ruleArmId().id());
        assertEquals(
                PolicyOperationKind.HARD_VETO,
                veto.kind());

        assertEquals(1, ObjectiveBattlePolicy
                .evaluateTerminalObjectiveHazard(
                    "battle", true, true, 1.99f)
                .operations().size());
        assertEmpty(ObjectiveBattlePolicy
                .evaluateTerminalObjectiveHazard(
                    "battle", false, false, -20.0f));
    }

    @Test
    public void armedTerminalObjectiveBattleOpensAtSafeMargin() {
        assertEmpty(ObjectiveBattlePolicy
                .evaluateTerminalObjectiveHazard(
                    "battle", true, true, 2.0f));
    }

    @Test
    public void battleCannotSpendTheExactObjectiveMoveReserve() {
        PolicyResult blocked =
                ObjectiveBattlePolicy
                    .preserveObjectiveMoveForce(
                        "battle", 1, 1, 1.0f);

        assertEquals(1, blocked.operations().size());
        PolicyOperation veto = blocked.operations().get(0);
        assertEquals(
                ObjectiveBattlePolicy
                    .OBJECTIVE_MOVE_FORCE_RESERVE_RULE_ID,
                veto.ruleArmId().id());
        assertEquals(
                PolicyOperationKind.HARD_VETO,
                veto.kind());

        assertEmpty(ObjectiveBattlePolicy
                .preserveObjectiveMoveForce(
                    "battle", 1, 2, 1.0f));
        assertEmpty(ObjectiveBattlePolicy
                .preserveObjectiveMoveForce(
                    "battle", 1, 1, 0.0f));
        assertEmpty(ObjectiveBattlePolicy
                .preserveObjectiveMoveForce(
                    "battle", 0, 0, 1.0f));
    }

    private static PolicyResult evaluate(
            boolean exactStructuredPreFlipTarget,
            boolean missingSelfControl,
            boolean bothSidesPresent,
            boolean formationSafetyVeto,
            boolean predictorSafe,
            float effectiveDiff,
            int reserveDeckSize,
            float ourPower,
            float theirPower) {
        return ObjectiveBattlePolicy.evaluate(
                new ObjectiveBattlePolicy.Facts(
                        "battle",
                        exactStructuredPreFlipTarget,
                        missingSelfControl,
                        bothSidesPresent,
                        formationSafetyVeto,
                        predictorSafe,
                        effectiveDiff,
                        reserveDeckSize,
                        ourPower,
                        theirPower));
    }

    private static void assertEmpty(PolicyResult result) {
        assertTrue(result.operations().isEmpty());
    }
}
