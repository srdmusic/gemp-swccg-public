package com.gempukku.swccgo.ai.models.common.policy;

import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PolicyContributionLedgerTest {
    private static final TraceRuleId RULE = TraceRuleId.of("V202-foundation");
    private static final TraceRuleId RULE_TWO = TraceRuleId.of("V202-foundation-2");

    @Test
    public void preservesOperationOrderAndRawFloatBits() {
        float first = 0.1f;
        float second = Math.nextUp(0.1f);
        PolicyOperation one = PolicyOperation.add("A", RULE, TraceDomainId.DRAW_COUNT,
                TraceOutputKind.BANDED, first, "first");
        PolicyOperation two = PolicyOperation.add("A", RULE_TWO, TraceDomainId.DRAW_COUNT,
                TraceOutputKind.BANDED, second, "second");

        PolicyContributionLedger ledger = new PolicyContributionLedger("decision-1");
        ledger.register(new PolicyResult("draw-policy", List.of(one, two)));

        assertEquals(List.of(one, two), ledger.orderedOperations());
        assertEquals(Float.floatToRawIntBits(first),
                Float.floatToRawIntBits(ledger.orderedOperations().get(0).delta()));
        assertEquals(Float.floatToRawIntBits(second),
                Float.floatToRawIntBits(ledger.orderedOperations().get(1).delta()));
        assertEquals("draw-policy", ledger.producerFor("A", RULE));
    }

    @Test
    public void rejectsASecondProducerForTheSameActionAndArmAtomically() {
        PolicyOperation operation = PolicyOperation.add("A", RULE, TraceDomainId.DRAW_COUNT,
                TraceOutputKind.BANDED, 3.0f, "draw three");
        PolicyContributionLedger ledger = new PolicyContributionLedger("decision-1");
        ledger.register(new PolicyResult("new-policy", List.of(operation)));

        try {
            ledger.register(new PolicyResult("legacy-evaluator", List.of(operation)));
            fail("expected duplicate producer rejection");
        } catch (IllegalStateException expected) {
            assertEquals(1, ledger.orderedOperations().size());
            assertEquals("new-policy", ledger.producerFor("A", RULE));
        }
    }

    @Test
    public void preservesMultipleOrderedArmsFromTheSameProducer() {
        PolicyOperation one = PolicyOperation.add("A", RULE, TraceDomainId.DRAW_COUNT,
                TraceOutputKind.BANDED, 1.0f, "one");
        PolicyOperation two = PolicyOperation.defer("A", RULE_TWO, TraceDomainId.DRAW_COUNT,
                TraceOutputKind.VETO, -2.0f, "defer");
        PolicyContributionLedger ledger = new PolicyContributionLedger("decision-1");

        ledger.register(new PolicyResult("draw-policy", List.of(one)));
        ledger.register(new PolicyResult("draw-policy", List.of(two)));

        assertEquals(List.of(one, two), ledger.orderedOperations());
    }

    @Test
    public void rejectsRepeatedArmFromTheSameProducer() {
        PolicyOperation operation = PolicyOperation.add("A", RULE, TraceDomainId.DRAW_COUNT,
                TraceOutputKind.BANDED, 1.0f, "one");
        PolicyContributionLedger ledger = new PolicyContributionLedger("decision-1");

        try {
            ledger.register(new PolicyResult("draw-policy", List.of(operation, operation)));
            fail("expected repeated contribution rejection");
        } catch (IllegalStateException expected) {
            assertEquals(List.of(), ledger.orderedOperations());
        }
    }

    @Test
    public void hardVetoCannotCarryAnAdditiveDelta() {
        try {
            new PolicyOperation("A", RULE, TraceDomainId.DRAW_COUNT, TraceOutputKind.VETO,
                    PolicyOperationKind.HARD_VETO, -100.0f, "blocked");
            fail("expected hard-veto delta rejection");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void blankActionIdIsReservedForTheExistingPassCandidate() {
        PolicyOperation pass = PolicyOperation.add("", TraceRuleId.of("V202-pass"),
                TraceDomainId.PASS_CANCEL, TraceOutputKind.ORDERING, 5.0f, "legal Pass");
        assertEquals("", pass.actionId());
    }
}
