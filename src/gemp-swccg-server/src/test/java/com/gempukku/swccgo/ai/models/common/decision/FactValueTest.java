package com.gempukku.swccgo.ai.models.common.decision;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Pure construction tests for {@link FactValue} — Batch-2 typed-facts increment 1.
 * Contract: Handoffs/CODEX_RANDO_FACTS_ASSESSMENTS_CONTRACT_2026-07-13.md
 * ("Unknown handling": unknown is data; known false / known zero are representable
 * and distinct from UNKNOWN; no fail-open).
 */
public class FactValueTest {

    @Test
    public void knownValueCarriesValueProducerAndProvenance() {
        FactValue<Integer> fact = FactValue.known(7, "test-producer", "gameState.forcePile");
        assertTrue(fact.isKnown());
        assertFalse(fact.isUnknown());
        assertEquals(FactValue.State.KNOWN, fact.state());
        assertEquals(Integer.valueOf(7), fact.value());
        assertEquals("test-producer", fact.producerId());
        assertEquals("gameState.forcePile", fact.provenance());
    }

    @Test
    public void unknownPreservesProducerProvenanceAndReason() {
        FactValue<Float> fact = FactValue.unknown("deploy-resolver", "blueprint#501_042", "blueprint lookup returned null");
        assertTrue(fact.isUnknown());
        assertFalse(fact.isKnown());
        assertEquals(FactValue.State.UNKNOWN, fact.state());
        assertEquals("deploy-resolver", fact.producerId());
        assertEquals("blueprint#501_042", fact.provenance());
        assertEquals("blueprint lookup returned null", fact.unknownReason());
    }

    @Test
    public void knownFalseIsRepresentableAndDistinctFromUnknown() {
        FactValue<Boolean> knownFalse = FactValue.known(false, "p", "k");
        FactValue<Boolean> unknown = FactValue.unknown("p", "k", "not observed");
        assertTrue(knownFalse.isKnown());
        assertFalse(knownFalse.value());
        assertNotEquals(knownFalse, unknown);
        assertNotEquals(knownFalse.state(), unknown.state());
    }

    @Test
    public void knownZeroIsRepresentableAndDistinctFromUnknown() {
        FactValue<Integer> knownZero = FactValue.known(0, "p", "k");
        FactValue<Integer> unknown = FactValue.unknown("p", "k", "not observed");
        assertTrue(knownZero.isKnown());
        assertEquals(Integer.valueOf(0), knownZero.value());
        assertNotEquals(knownZero, unknown);
    }

    @Test
    public void valueOfUnknownThrowsInsteadOfFailingOpen() {
        FactValue<Integer> unknown = FactValue.unknown("p", "k", "engine threw");
        try {
            unknown.value();
            fail("value() on UNKNOWN must throw — unknown is data, not a default");
        } catch (IllegalStateException expected) {
            // Message must carry the diagnostics a consumer needs.
            assertTrue(expected.getMessage().contains("p"));
            assertTrue(expected.getMessage().contains("engine threw"));
        }
    }

    @Test
    public void unknownReasonOfKnownThrows() {
        FactValue<Integer> known = FactValue.known(3, "p", "k");
        try {
            known.unknownReason();
            fail("unknownReason() on KNOWN must throw");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    @Test
    public void knownRejectsNullValue() {
        try {
            FactValue.known(null, "p", "k");
            fail("known(null) must be rejected — use unknown(...) with a reason");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void unknownRequiresAReason() {
        try {
            FactValue.unknown("p", "k", null);
            fail("unknown without a reason must be rejected");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void equalityIsByValue() {
        assertEquals(FactValue.known(5, "p", "k"), FactValue.known(5, "p", "k"));
        assertEquals(FactValue.unknown("p", "k", "r"), FactValue.unknown("p", "k", "r"));
        assertNotEquals(FactValue.known(5, "p", "k"), FactValue.known(6, "p", "k"));
        assertNotEquals(FactValue.unknown("p", "k", "r1"), FactValue.unknown("p", "k", "r2"));
    }
}
