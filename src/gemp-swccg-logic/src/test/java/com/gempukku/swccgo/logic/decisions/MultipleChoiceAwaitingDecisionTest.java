package com.gempukku.swccgo.logic.decisions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * F1 FOCUSED ENGINE TEST (2026-07-13).
 *
 * Packet: Handoffs/CODEX_FINALIZER_FIXTURE_RETRY_PACKET_2026-07-13.md, F1.
 * {@link MultipleChoiceAwaitingDecision#decisionMade} must validate the parsed
 * ordinal range BEFORE indexing {@code _possibleResults}: negative, exactly-size,
 * and non-numeric input are all CHECKED rejections
 * ({@link DecisionResultInvalidException}, "Unknown response number"), never an
 * unchecked ArrayIndexOutOfBoundsException that would bypass the mediator's
 * checked catch (audit CODEX_FINALIZER_RESPONSE_AUDIT_2026-07-13.md P0 #1).
 *
 * Required cases per packet: valid first and last ordinal, negative, exactly
 * {@code size}, non-numeric, and label mapping from a deliberately permuted
 * result array.
 */
public class MultipleChoiceAwaitingDecisionTest {

    /** Concrete recording subclass over the REAL abstract engine class, so
     *  {@code decisionMade} runs the REAL validator. */
    private static class RecordingMultipleChoice extends MultipleChoiceAwaitingDecision {
        int chosenIndex = -1;
        String chosenResult;
        int callbackCount;

        RecordingMultipleChoice(String[] possibleResults) {
            super("Test choice", possibleResults);
        }

        @Override
        protected void validDecisionMade(int index, String result) {
            chosenIndex = index;
            chosenResult = result;
            callbackCount++;
        }
    }

    private static RecordingMultipleChoice fresh() {
        return new RecordingMultipleChoice(new String[]{"Alpha", "Beta", "Gamma"});
    }

    @Test
    public void validFirstOrdinalMapsToFirstResult() throws Exception {
        RecordingMultipleChoice decision = fresh();
        decision.decisionMade("0");
        assertEquals(0, decision.chosenIndex);
        assertEquals("Alpha", decision.chosenResult);
        assertEquals(1, decision.callbackCount);
    }

    @Test
    public void validLastOrdinalMapsToLastResult() throws Exception {
        RecordingMultipleChoice decision = fresh();
        decision.decisionMade("2");
        assertEquals(2, decision.chosenIndex);
        assertEquals("Gamma", decision.chosenResult);
        assertEquals(1, decision.callbackCount);
    }

    @Test
    public void negativeOrdinalIsCheckedRejected() {
        RecordingMultipleChoice decision = fresh();
        // assertThrows on the CHECKED type also proves no unchecked
        // ArrayIndexOutOfBoundsException escapes (it would fail the assertion).
        DecisionResultInvalidException e = assertThrows(DecisionResultInvalidException.class,
                () -> decision.decisionMade("-1"));
        assertEquals("Unknown response number", e.getWarningMessage());
        assertEquals(0, decision.callbackCount);
    }

    @Test
    public void ordinalExactlySizeIsCheckedRejected() {
        RecordingMultipleChoice decision = fresh();
        DecisionResultInvalidException e = assertThrows(DecisionResultInvalidException.class,
                () -> decision.decisionMade("3")); // size of the 3-element result array
        assertEquals("Unknown response number", e.getWarningMessage());
        assertEquals(0, decision.callbackCount);
    }

    @Test
    public void nonNumericIsCheckedRejected() {
        RecordingMultipleChoice decision = fresh();
        DecisionResultInvalidException e = assertThrows(DecisionResultInvalidException.class,
                () -> decision.decisionMade("Beta")); // a label is NOT a wire ordinal
        assertEquals("Unknown response number", e.getWarningMessage());
        assertEquals(0, decision.callbackCount);
    }

    @Test
    public void labelMappingFollowsDeliberatelyPermutedResultArray() throws Exception {
        // Same labels, permuted order: the ordinal must map to the result AT that
        // ordinal in THIS array, not to any assumed natural position.
        RecordingMultipleChoice permuted =
                new RecordingMultipleChoice(new String[]{"Gamma", "Alpha", "Beta"});
        permuted.decisionMade("1");
        assertEquals(1, permuted.chosenIndex);
        assertEquals("Alpha", permuted.chosenResult);

        RecordingMultipleChoice permutedAgain =
                new RecordingMultipleChoice(new String[]{"Gamma", "Alpha", "Beta"});
        permutedAgain.decisionMade("0");
        assertEquals(0, permutedAgain.chosenIndex);
        assertEquals("Gamma", permutedAgain.chosenResult);
    }
}
