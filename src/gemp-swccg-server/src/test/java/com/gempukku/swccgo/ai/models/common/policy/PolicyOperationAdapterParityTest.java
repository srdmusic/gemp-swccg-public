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

    @Test
    public void objectivePreferenceNormalizesAndSaturatesAcrossMerges() {
        TraceRuleId objectiveOne = TraceRuleId.of("OBJECTIVE.TEST.ONE");
        TraceRuleId objectiveTwo = TraceRuleId.of("OBJECTIVE.TEST.TWO");
        TraceRuleId tactical = TraceRuleId.of("TACTICAL.TEST.NEGATIVE");

        PolicyOperation normalized = PolicyOperation.add(
                "A", objectiveOne, TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED, 20000.0f, "first objective match");
        assertEquals(300.0f, normalized.delta(), 0.0f);

        PolicyContributionLedger firstLedger = new PolicyContributionLedger("objective-first");
        firstLedger.register(new PolicyResult("objective-first", List.of(
                normalized,
                PolicyOperation.add(
                        "A", objectiveTwo, TraceDomainId.OBJECTIVE_INTENT,
                        TraceOutputKind.BANDED, 800.0f, "second objective match"),
                PolicyOperation.add(
                        "A", tactical, TraceDomainId.DEPLOY_SITING,
                        TraceOutputKind.BANDED, -350.0f, "unsafe tactical state"))));

        PolicyContributionLedger secondLedger = new PolicyContributionLedger("objective-merge");
        secondLedger.register(new PolicyResult(
                "objective-merge", List.of(PolicyOperation.add(
                        "A", objectiveTwo, TraceDomainId.OBJECTIVE_INTENT,
                        TraceOutputKind.BANDED, 1200.0f, "third objective match"))));

        com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction rando =
                new com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction(
                        "A", com.gempukku.swccgo.ai.models.rando.evaluators.ActionType.UNKNOWN,
                        0.0f, "A");
        com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction randoMerge =
                new com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction(
                        "A", com.gempukku.swccgo.ai.models.rando.evaluators.ActionType.UNKNOWN,
                        0.0f, "A");
        com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction chosen =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction(
                        "A", com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionType.UNKNOWN,
                        0.0f, "A");
        com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction chosenMerge =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction(
                        "A", com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionType.UNKNOWN,
                        0.0f, "A");

        com.gempukku.swccgo.ai.models.rando.evaluators.PolicyOperationAdapter.apply(rando, firstLedger);
        com.gempukku.swccgo.ai.models.rando.evaluators.PolicyOperationAdapter.apply(randoMerge, secondLedger);
        rando.mergeFrom(randoMerge);
        com.gempukku.swccgo.ai.models.chosenone.evaluators.PolicyOperationAdapter.apply(chosen, firstLedger);
        com.gempukku.swccgo.ai.models.chosenone.evaluators.PolicyOperationAdapter.apply(chosenMerge, secondLedger);
        chosen.mergeFrom(chosenMerge);

        assertEquals(-50.0f, rando.getScore(), 0.0f);
        assertEquals(rando.getScore(), chosen.getScore(), 0.0f);
        assertEquals(5, rando.getReasoning().size());
        assertEquals(
                "OBJECTIVE MERGE CAP: requested +300.0, applied +0.0, suppressed +300.0",
                rando.getReasoning().get(3));
        assertEquals(rando.getReasoning(), chosen.getReasoning());
    }
}
