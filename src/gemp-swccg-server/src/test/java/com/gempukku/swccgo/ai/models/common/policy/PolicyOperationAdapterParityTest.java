package com.gempukku.swccgo.ai.models.common.policy;

import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PolicyOperationAdapterParityTest {
    @Test
    public void mirroredAdaptersApplyTheSameOrderedStream() {
        TraceRuleId addRule = TraceRuleId.of("V202-add");
        TraceRuleId finalAddRule = TraceRuleId.of("V202-final-add");
        TraceRuleId deferRule = TraceRuleId.of("V202-defer");
        TraceRuleId vetoRule = TraceRuleId.of("V202-veto");
        float finalAdd = Math.nextUp(0.1f);
        PolicyResult result = new PolicyResult("foundation-policy", List.of(
                PolicyOperation.add("A", finalAddRule, TraceDomainId.DEPLOY_SITING,
                        TraceOutputKind.BANDED, 0.1f, "first add"),
                PolicyOperation.defer("A", deferRule, TraceDomainId.SOLO_FORMATION,
                        TraceOutputKind.VETO, -3.0f, "unsupported solo"),
                PolicyOperation.hardVeto("A", vetoRule, TraceDomainId.SOLO_FORMATION,
                        TraceOutputKind.VETO, "hard formation failure"),
                PolicyOperation.add("A", addRule, TraceDomainId.DEPLOY_SITING,
                        TraceOutputKind.BANDED, finalAdd, "final add"),
                PolicyOperation.add("B", addRule, TraceDomainId.DEPLOY_SITING,
                        TraceOutputKind.BANDED, 999.0f, "other action")));
        PolicyContributionLedger ledger = new PolicyContributionLedger("decision-1");
        ledger.register(result);

        com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction rando =
                new com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction(
                        "A", com.gempukku.swccgo.ai.models.rando.evaluators.ActionType.UNKNOWN,
                        10.0f, "A");
        com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction chosenOne =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction(
                        "A", com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionType.UNKNOWN,
                        10.0f, "A");

        int randoCount = com.gempukku.swccgo.ai.models.rando.evaluators.PolicyOperationAdapter
                .apply(rando, ledger);
        int chosenCount = com.gempukku.swccgo.ai.models.chosenone.evaluators.PolicyOperationAdapter
                .apply(chosenOne, ledger);

        float expectedScore = 10.0f;
        expectedScore += 0.1f;
        expectedScore += -3.0f;
        expectedScore += finalAdd;

        assertEquals(4, randoCount);
        assertEquals(randoCount, chosenCount);
        assertEquals(Float.floatToRawIntBits(expectedScore), Float.floatToRawIntBits(rando.getScore()));
        assertEquals(Float.floatToRawIntBits(rando.getScore()), Float.floatToRawIntBits(chosenOne.getScore()));
        assertEquals(rando.getReasoning(), chosenOne.getReasoning());
        assertEquals(rando.getVetoReason(), chosenOne.getVetoReason());
        assertEquals(rando.getDeferReason(), chosenOne.getDeferReason());
        assertTrue(rando.isHardVetoed());
        assertTrue(chosenOne.isHardVetoed());
        assertTrue(rando.isDeferred());
        assertTrue(chosenOne.isDeferred());
    }
}
