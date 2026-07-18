package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ControlActionPolicyTest {
    @Test
    public void noEscapeRetrievalKeepsItsExactContribution() {
        assertOperation(ControlActionPolicy.noEscapeRetrieval("A"),
                "V29.14-noescape-retrieval", TraceOutputKind.BANDED, 200.0f,
                "V29.14 NO ESCAPE: Free card from Lost Pile — always take it!");
    }

    @Test
    public void forceDrainModifierKeepsItsExactContribution() {
        assertOperation(ControlActionPolicy.forceDrainModifier("A"),
                "V24.2-drain", TraceOutputKind.BANDED, 80.0f,
                "V24.2 FORCE DRAIN BONUS: +1 to force drain — always use!");
    }

    @Test
    public void selfCancelRemainsAnAdditiveHistoricalVeto() {
        String reason = "V52 NEVER SELF-CANCEL DRAIN: Canceling own force drain is suicide!";
        assertOperation(ControlActionPolicy.selfCancelDrain("A", reason),
                "V52-self-cancel", TraceOutputKind.VETO, -9999.0f, reason);
    }

    private static void assertOperation(PolicyResult result, String ruleId,
                                        TraceOutputKind outputKind, float delta,
                                        String reason) {
        assertEquals("CONTROL_ACTION_POLICY", result.producerId());
        assertEquals(1, result.operations().size());
        PolicyOperation operation = result.operations().get(0);
        assertEquals("A", operation.actionId());
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(TraceDomainId.DRAIN_CONTROL, operation.domainId());
        assertEquals(outputKind, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(Float.floatToRawIntBits(delta),
                Float.floatToRawIntBits(operation.delta()));
        assertEquals(reason, operation.reason());
    }
}
