package com.gempukku.swccgo.ai.models.common.policy;

import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectivePreferencePolicyTest {
    private static final TraceOutputKind OUTPUT = TraceOutputKind.BANDED;

    @Test
    public void everyPositiveObjectiveSignalNormalizesToThreeHundred() {
        assertEquals(300.0f, add("A", "objective-large",
                TraceDomainId.OBJECTIVE_INTENT, 6000.0f).delta(), 0.0f);
        assertEquals(300.0f, add("A", "objective-small",
                TraceDomainId.OBJECTIVE_INTENT, 200.0f).delta(), 0.0f);
        assertEquals(450.0f, add("A", "tactical-large",
                TraceDomainId.DEPLOY_SITING, 450.0f).delta(), 0.0f);
    }

    @Test
    public void objectiveSignalsShareOnePerActionCeiling() {
        PolicyResult result = new PolicyResult("objective-test", List.of(
                add("A", "objective-one", TraceDomainId.OBJECTIVE_INTENT,
                        200.0f),
                add("A", "objective-two", TraceDomainId.OBJECTIVE_INTENT,
                        250.0f),
                add("A", "objective-three", TraceDomainId.OBJECTIVE_INTENT,
                        50.0f),
                add("B", "objective-other-action",
                        TraceDomainId.OBJECTIVE_INTENT, 300.0f)));

        assertEquals(300.0f, result.operations().get(0).delta(), 0.0f);
        assertEquals(0.0f, result.operations().get(1).delta(), 0.0f);
        assertEquals(0.0f, result.operations().get(2).delta(), 0.0f);
        assertEquals(300.0f, result.operations().get(3).delta(), 0.0f);
        assertEquals("third objective match remains visible",
                result.operations().get(2).reason());
    }

    @Test
    public void negativeObjectiveIsBoundedButHardVetoRemainsCategorical() {
        PolicyOperation negative = add("A", "objective-negative",
                TraceDomainId.OBJECTIVE_INTENT, -6000.0f);
        PolicyOperation veto = PolicyOperation.hardVeto(
                "A", TraceRuleId.of("objective-terminal-veto"),
                TraceDomainId.OBJECTIVE_INTENT, TraceOutputKind.VETO,
                "terminal objective loss");

        PolicyResult result = new PolicyResult(
                "objective-safety-test", List.of(negative, veto));

        assertEquals(-300.0f, result.operations().get(0).delta(), 0.0f);
        assertTrue(result.operations().get(1).kind()
                == PolicyOperationKind.HARD_VETO);
        assertEquals(0.0f, result.operations().get(1).delta(), 0.0f);
    }

    @Test
    public void negativeObjectiveSignalsShareOnePerActionFloor() {
        PolicyResult result = new PolicyResult("objective-negative-test", List.of(
                add("A", "negative-one", TraceDomainId.OBJECTIVE_INTENT,
                        -200.0f),
                add("A", "negative-two", TraceDomainId.OBJECTIVE_INTENT,
                        -250.0f),
                add("A", "positive-offset", TraceDomainId.OBJECTIVE_INTENT,
                        300.0f),
                add("B", "negative-other-action",
                        TraceDomainId.OBJECTIVE_INTENT, -500.0f)));

        assertEquals(-200.0f, result.operations().get(0).delta(), 0.0f);
        assertEquals(-100.0f, result.operations().get(1).delta(), 0.0f);
        assertEquals(300.0f, result.operations().get(2).delta(), 0.0f);
        assertEquals(-300.0f, result.operations().get(3).delta(), 0.0f);
    }

    @Test
    public void laterPositiveSignalIsSuppressedInsteadOfReinflated() {
        PolicyResult result = new PolicyResult("objective-partial-test", List.of(
                add("A", "negative-first", TraceDomainId.OBJECTIVE_INTENT,
                        -200.0f),
                add("A", "positive-first", TraceDomainId.OBJECTIVE_INTENT,
                        300.0f),
                add("A", "positive-later", TraceDomainId.OBJECTIVE_INTENT,
                        300.0f)));

        assertEquals(-200.0f, result.operations().get(0).delta(), 0.0f);
        assertEquals(300.0f, result.operations().get(1).delta(), 0.0f);
        assertEquals(0.0f, result.operations().get(2).delta(), 0.0f);
    }

    private static PolicyOperation add(String actionId, String ruleId,
                                       TraceDomainId domain, float delta) {
        return PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                domain, OUTPUT, delta,
                ruleId.equals("objective-three")
                        ? "third objective match remains visible"
                        : ruleId);
    }
}
