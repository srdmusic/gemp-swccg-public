package com.gempukku.swccgo.ai.models.common.finalization;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * FINALIZER RUNTIME (2026-07-13, Handoffs/CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md
 * §2): the immutable rejection-history append — returns a NEW history, never mutates the
 * existing list, rejects blank detail, and carries the exact wire + typed engine reason.
 */
public class RejectionHistoryAppendTest {

    @Test
    public void appendReturnsNewImmutableHistoryWithoutMutatingOriginal() {
        RejectionHistory empty = RejectionHistory.empty();
        RejectionHistory afterOne = empty.append("0",
                FinalizedResponse.RejectReason.ENGINE_DECISION_INVALID, "engine rejected");

        assertEquals("original stays empty (count 0)", 0, empty.size());
        assertNotSame("append returns a new instance", empty, afterOne);
        assertEquals("new history has count 1", 1, afterOne.size());
        assertEquals("exact wire preserved", "0", afterOne.attempts().get(0).wireResponse());
        assertEquals("typed engine reason preserved",
                FinalizedResponse.RejectReason.ENGINE_DECISION_INVALID,
                afterOne.attempts().get(0).reason());
        assertEquals("detail preserved", "engine rejected", afterOne.attempts().get(0).detail());
        assertTrue("containsWire finds the appended wire", afterOne.containsWire("0"));
    }

    @Test
    public void appendChainProducesCountsZeroThenOne() {
        RejectionHistory h0 = RejectionHistory.empty();
        assertEquals(0, h0.size());
        RejectionHistory h1 = h0.append("5",
                FinalizedResponse.RejectReason.ENGINE_DECISION_INVALID, "invalid");
        assertEquals(1, h1.size());
    }

    @Test
    public void appendRejectsBlankDetail() {
        RejectionHistory empty = RejectionHistory.empty();
        try {
            empty.append("0", FinalizedResponse.RejectReason.ENGINE_DECISION_INVALID, "   ");
            fail("blank detail must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("nonblank"));
        }
    }

    @Test
    public void attemptsListIsImmutable() {
        RejectionHistory afterOne = RejectionHistory.empty().append("0",
                FinalizedResponse.RejectReason.ENGINE_DECISION_INVALID, "invalid");
        try {
            afterOne.attempts().add(new RejectionHistory.Attempt("9",
                    FinalizedResponse.RejectReason.ENGINE_DECISION_INVALID, "x"));
            fail("attempts list must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }
}
