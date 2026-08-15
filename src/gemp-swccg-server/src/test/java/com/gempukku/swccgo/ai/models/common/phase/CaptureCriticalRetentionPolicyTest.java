package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CaptureCriticalRetentionPolicyTest {

    @Test
    public void bothObjectivesPreferToRetainCaptureAndPayoffRoles()
            throws Exception {
        for (CaptureObjectivePolicy.ObjectiveKind objective
                : CaptureObjectivePolicy.ObjectiveKind.values()) {
            for (CaptureObjectivePolicy.CriticalRole role
                    : CaptureObjectivePolicy.CriticalRole.values()) {
                PolicyOperation operation = only(
                        CaptureObjectivePolicy
                            .scoreCriticalRetention(
                                new CaptureObjectivePolicy
                                    .RetentionFacts(
                                        objective + "-" + role,
                                        objective,
                                        role,
                                        true)));

                assertEquals(PolicyOperationKind.ADD,
                        operation.kind());
                assertEquals(
                        "FORCE_LOSS.OBJECTIVE.CAPTURE_CRITICAL",
                        operation.ruleArmId().id());
                assertEquals(
                        TraceDomainId.OBJECTIVE_INTENT,
                        operation.domainId());
                assertEquals(TraceOutputKind.BANDED,
                        operation.outputKind());
                assertRawFloat(-300.0f,
                        operation.delta());
            }
        }
    }

    @Test
    public void unpreferredDuplicateRemainsUnprotected()
            throws Exception {
        PolicyResult result =
                CaptureObjectivePolicy.scoreCriticalRetention(
                    new CaptureObjectivePolicy.RetentionFacts(
                        "duplicate",
                        CaptureObjectivePolicy.ObjectiveKind
                            .TIGIH,
                        CaptureObjectivePolicy.CriticalRole
                            .PAYOFF_CARD,
                        false));

        assertEquals("CAPTURE_OBJECTIVE_POLICY",
                result.producerId());
        assertTrue(result.operations().isEmpty());
    }

    private static PolicyOperation only(
            PolicyResult result) {
        assertEquals("CAPTURE_OBJECTIVE_POLICY",
                result.producerId());
        assertEquals(1, result.operations().size());
        return result.operations().get(0);
    }

    private static void assertRawFloat(
            float expected,
            float actual) {
        assertEquals(
                Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(actual));
    }
}
