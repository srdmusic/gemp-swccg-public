package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NoMoneyNoPartsObjectivePolicyTest {

    @Test
    public void ordinaryDeployGetsBoundedMosEspaMovePenalty() {
        var penalized = NoMoneyNoPartsObjectivePolicy
                .preserveMoveForceForOrdinaryDeploy(
                    "deploy", true, 1, 1, 1, 1);
        assertEquals(1, penalized.operations().size());
        assertEquals(PolicyOperationKind.ADD,
                penalized.operations().get(0).kind());
        assertEquals(-300.0f,
                penalized.operations().get(0).delta(), 0.0f);
        assertEquals("OBJECTIVE.NO_MONEY.MOVE_FORCE_RESERVE",
                penalized.operations().get(0).ruleArmId().id());

        assertTrue(NoMoneyNoPartsObjectivePolicy
                .preserveMoveForceForOrdinaryDeploy(
                    "deploy", true, 1, 2, 1, 1)
                .operations().isEmpty());
        assertTrue(NoMoneyNoPartsObjectivePolicy
                .preserveMoveForceForOrdinaryDeploy(
                    "deploy", true, 1, 1, 0, 0)
                .operations().isEmpty());
        assertTrue(NoMoneyNoPartsObjectivePolicy
                .preserveMoveForceForOrdinaryDeploy(
                    "deploy", true, 0, 1, 1, 1)
                .operations().isEmpty());
    }

    @Test
    public void unknownPaidDeployGetsBoundedPenaltyWhileMoveReserveIsLive() {
        var penalized = NoMoneyNoPartsObjectivePolicy
                .preserveMoveForceForOrdinaryDeploy(
                    "deploy", true, 1, 2, null, 2);
        assertEquals(1, penalized.operations().size());
        assertEquals(PolicyOperationKind.ADD,
                penalized.operations().get(0).kind());
        assertEquals(-300.0f,
                penalized.operations().get(0).delta(), 0.0f);
        assertEquals("OBJECTIVE.NO_MONEY.MOVE_PAYMENT_UNKNOWN",
                penalized.operations().get(0).ruleArmId().id());

        assertTrue("A conservative fallback that leaves the reserve is allowed",
                NoMoneyNoPartsObjectivePolicy
                    .preserveMoveForceForOrdinaryDeploy(
                        "deploy", true, 1, 3, null, 2)
                    .operations().isEmpty());

        assertTrue(NoMoneyNoPartsObjectivePolicy
                .preserveMoveForceForOrdinaryDeploy(
                    "deploy", false, 1, 1, null, 1)
                .operations().isEmpty());
    }
}
