package com.gempukku.swccgo.ai.models.common.finalization;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/** Target-specific response-boundary baselines for the future phase owners. */
public class ActivateControlOwnerResponseTest {

    private static final long SEED = 42L;
    private static final String ZERO_CONFIRM_TEXT =
            "You have not activated Force. Do you want to Pass?";

    @Test
    public void explicitActivationAmountAndAllowanceFinalizeWithoutRandom() throws Exception {
        assertFinalizedIntegerAccepted("Choose amount of Force to activate", 0, 5, 5);
        assertFinalizedIntegerAccepted(
                "Choose amount of Force to allow opponent to activate without you performing a top-level action",
                1, 5, 5);
    }

    @Test
    public void explicitYesAndNoFinalizeAcrossBothResultOrdersWithoutRandom() throws Exception {
        assertFinalizedChoiceAccepted(new String[]{"Yes", "No"}, "Yes");
        assertFinalizedChoiceAccepted(new String[]{"Yes", "No"}, "No");
        assertFinalizedChoiceAccepted(new String[]{"No", "Yes"}, "Yes");
        assertFinalizedChoiceAccepted(new String[]{"No", "Yes"}, "No");
    }

    private static void assertFinalizedIntegerAccepted(String text, int min, int max, int value)
            throws Exception {
        EngineDecisionFixtures.RecordingInteger snapshotDecision = integerDecision(text, min, max);
        DecisionSnapshot snapshot = EngineDecisionFixtures.snapshotOf(snapshotDecision);
        ResponseFinalizerContractTest.CountingRandom random =
                new ResponseFinalizerContractTest.CountingRandom(SEED);
        FinalizedResponse finalized = ResponseFinalizer.finalize(
                snapshot, ResponseContract.from(snapshot),
                new ResponseIntent.IntegerValue(value),
                random, RejectionHistory.empty());

        assertEquals(FinalizedResponse.Status.ACCEPTED, finalized.status());
        assertEquals(String.valueOf(value), finalized.wireResponse());
        assertEquals(0, random.intDraws);

        EngineDecisionFixtures.RecordingInteger engine = integerDecision(text, min, max);
        engine.decisionMade(finalized.wireResponse());
        assertEquals(Integer.valueOf(value), engine.chosen);
        assertEquals(1, engine.callbackCount);
    }

    private static void assertFinalizedChoiceAccepted(String[] results, String expectedResult)
            throws Exception {
        int ordinal = Arrays.asList(results).indexOf(expectedResult);
        EngineDecisionFixtures.RecordingMultipleChoice snapshotDecision =
                new EngineDecisionFixtures.RecordingMultipleChoice(ZERO_CONFIRM_TEXT, results);
        DecisionSnapshot snapshot = EngineDecisionFixtures.snapshotOf(snapshotDecision);
        ResponseFinalizerContractTest.CountingRandom random =
                new ResponseFinalizerContractTest.CountingRandom(SEED);
        FinalizedResponse finalized = ResponseFinalizer.finalize(
                snapshot, ResponseContract.from(snapshot),
                new ResponseIntent.CandidateOrdinal(ordinal),
                random, RejectionHistory.empty());

        assertEquals(FinalizedResponse.Status.ACCEPTED, finalized.status());
        assertEquals(String.valueOf(ordinal), finalized.wireResponse());
        assertEquals(0, random.intDraws);

        EngineDecisionFixtures.RecordingMultipleChoice engine =
                new EngineDecisionFixtures.RecordingMultipleChoice(ZERO_CONFIRM_TEXT, results);
        engine.decisionMade(finalized.wireResponse());
        assertEquals(expectedResult, engine.chosenResult);
        assertEquals(1, engine.callbackCount);
    }

    private static EngineDecisionFixtures.RecordingInteger integerDecision(
            String text, int min, int max) {
        return new EngineDecisionFixtures.RecordingInteger(text, min, max, max);
    }
}
