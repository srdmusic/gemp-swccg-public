package com.gempukku.swccgo.ai.models.common.finalization;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.DecisionRejectionKind;
import com.gempukku.swccgo.ai.SwccgAiController;
import com.gempukku.swccgo.ai.models.chosenone.TheChosenOneAi;
import com.gempukku.swccgo.ai.models.rando.RandoCalAi;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Frozen V44/V67j finalizer-owner fixtures over both production bot entry paths. */
public class RevertApprovalFinalizerPilotTest {

    private static final String PLAYER = "asdf";
    private static final String REVERT_PROMPT = "Opponent requests a revert. Allow revert?";
    private static final List<Supplier<SwccgAiController>> BOT_FACTORIES =
            List.of(RandoCalAi::new, TheChosenOneAi::new);

    private static final class PilotChoiceDecision
            extends EngineDecisionFixtures.RecordingMultipleChoice {
        PilotChoiceDecision(String[] results) {
            super(REVERT_PROMPT, results);
        }
    }

    private static final class MalformedRevertDecision implements AwaitingDecision {
        private final Map<String, String[]> params = new HashMap<>();
        int callbackCount;

        MalformedRevertDecision(boolean resultsPresent) {
            if (resultsPresent) {
                params.put("results", new String[0]);
            }
        }

        @Override
        public int getAwaitingDecisionId() {
            return 44;
        }

        @Override
        public String getText() {
            return REVERT_PROMPT;
        }

        @Override
        public AwaitingDecisionType getDecisionType() {
            return AwaitingDecisionType.MULTIPLE_CHOICE;
        }

        @Override
        public Map<String, String[]> getDecisionParameters() {
            return params;
        }

        @Override
        public void decisionMade(String result) {
            callbackCount++;
        }
    }

    private static final class CountingRandom extends Random {
        int draws;

        @Override
        protected int next(int bits) {
            draws++;
            return 0;
        }
    }

    @Test
    public void rrV44RevertReorderedPreservesExactWireHistoryAndBotParity() throws Exception {
        RejectionHistory history = RejectionHistory.empty().append(
                "0", FinalizedResponse.RejectReason.ENGINE_DECISION_INVALID,
                "prior checked rejection");

        assertChoiceForBothBots("RR_V44_REVERT_REORDERED",
                new String[]{"No", "Allow revert"}, "1", 1, "Allow revert", history);
        assertChoiceForBothBots("RR_V44_REVERT_REORDERED_REVERSE",
                new String[]{"Accept", "No"}, "0", 0, "Accept", history);
    }

    @Test
    public void noPositiveLabelFallsBackToLegacyOrdinalZeroForBothBots() throws Exception {
        assertChoiceForBothBots("RR_V44_REVERT_NO_POSITIVE",
                new String[]{"No", "Decline"}, "0", 0, "No", RejectionHistory.empty());
    }

    @Test
    public void singleCandidatePinsExactLowerBoundForBothBots() throws Exception {
        assertChoiceForBothBots("RR_V44_REVERT_SINGLE",
                new String[]{"OK"}, "0", 0, "OK", RejectionHistory.empty());
    }

    @Test
    public void absentAndEmptyResultsRejectBeforeSubmissionForBothBots() throws Exception {
        assertMalformedForBothBots("RR_V44_REVERT_RESULTS_ABSENT", false);
        assertMalformedForBothBots("RR_V44_REVERT_RESULTS_EMPTY", true);
    }

    private static void assertChoiceForBothBots(String fixtureId, String[] results,
                                                String expectedWire, int expectedIndex,
                                                String expectedResult,
                                                RejectionHistory history) throws Exception {
        List<String> parity = new ArrayList<>();
        for (Supplier<SwccgAiController> factory : BOT_FACTORIES) {
            SwccgAiController directBot = factory.get();
            CountingRandom directRandom = installCountingRandom(directBot);
            PilotChoiceDecision directDecision =
                    new PilotChoiceDecision(Arrays.copyOf(results, results.length));

            String directWire = directBot.decide(PLAYER, directDecision, null);
            assertEquals(fixtureId + " direct wire", expectedWire, directWire);

            SwccgAiController engineBot = factory.get();
            CountingRandom engineRandom = installCountingRandom(engineBot);
            PilotChoiceDecision engineDecision =
                    new PilotChoiceDecision(Arrays.copyOf(results, results.length));
            AiDecisionResult result =
                    engineBot.decideForEngine(PLAYER, engineDecision, null, history);

            assertEquals(fixtureId, AiDecisionResult.Status.WIRE_RESPONSE, result.status());
            assertEquals(fixtureId, expectedWire, result.wireResponse());
            assertEquals(fixtureId, AiDecisionResult.MutationMode.NONE, result.mutationMode());
            assertTrue(fixtureId + " typed-finalizer origin", result.fromTypedFinalizer());
            assertNull(fixtureId + " NONE tracker descriptor", result.trackerMutation());
            assertEquals(fixtureId + " exact decision id",
                    String.valueOf(engineDecision.getAwaitingDecisionId()), result.decisionId());

            engineDecision.decisionMade(result.wireResponse());
            engineBot.onDecisionAccepted(PLAYER, engineDecision, null, result);

            assertEquals(fixtureId + " one engine submission", 1, engineDecision.callbackCount);
            assertEquals(fixtureId + " original ordinal", expectedIndex, engineDecision.chosenIndex);
            assertEquals(fixtureId + " original label", expectedResult, engineDecision.chosenResult);
            assertEquals(fixtureId + " direct path RNG", 0, directRandom.draws);
            assertEquals(fixtureId + " finalizer path RNG", 0, engineRandom.draws);

            parity.add(directWire + "|" + result.status() + "|" + result.wireResponse()
                    + "|" + result.mutationMode() + "|" + result.fromTypedFinalizer()
                    + "|" + (result.trackerMutation() != null) + "|"
                    + engineDecision.chosenIndex + "|" + engineDecision.chosenResult);
        }
        assertEquals(fixtureId + " normalized bot parity", parity.get(0), parity.get(1));
    }

    private static void assertMalformedForBothBots(String fixtureId,
                                                   boolean resultsPresent) throws Exception {
        List<String> parity = new ArrayList<>();
        for (Supplier<SwccgAiController> factory : BOT_FACTORIES) {
            SwccgAiController directBot = factory.get();
            CountingRandom directRandom = installCountingRandom(directBot);
            String directWire =
                    directBot.decide(PLAYER, new MalformedRevertDecision(resultsPresent), null);
            assertEquals(fixtureId + " direct behavior remains legacy", "0", directWire);

            SwccgAiController engineBot = factory.get();
            CountingRandom engineRandom = installCountingRandom(engineBot);
            MalformedRevertDecision engineDecision =
                    new MalformedRevertDecision(resultsPresent);
            AiDecisionResult result = engineBot.decideForEngine(
                    PLAYER, engineDecision, null, RejectionHistory.empty());

            assertEquals(fixtureId, AiDecisionResult.Status.TYPED_REJECTION, result.status());
            assertEquals(fixtureId, FinalizedResponse.RejectReason.ORDINAL_OUT_OF_BOUNDS,
                    result.rejectionCode());
            assertNull(fixtureId + " no wire", result.wireResponse());
            assertNull(fixtureId + " no mutation mode", result.mutationMode());
            assertNull(fixtureId + " no tracker descriptor", result.trackerMutation());
            assertTrue(fixtureId + " typed-finalizer origin", result.fromTypedFinalizer());
            assertEquals(fixtureId + " exact decision id",
                    String.valueOf(engineDecision.getAwaitingDecisionId()), result.decisionId());

            engineBot.onDecisionRejected(PLAYER, engineDecision, null, result,
                    DecisionRejectionKind.TYPED_REJECTION, result.rejectionDetail());

            assertEquals(fixtureId + " no engine submission", 0, engineDecision.callbackCount);
            assertEquals(fixtureId + " direct path RNG", 0, directRandom.draws);
            assertEquals(fixtureId + " no fallback RNG", 0, engineRandom.draws);
            assertFalse(fixtureId + " rejection detail must be nonblank",
                    result.rejectionDetail().isBlank());

            parity.add(directWire + "|" + result.status() + "|" + result.rejectionCode()
                    + "|" + result.rejectionDetail() + "|" + result.mutationMode()
                    + "|" + (result.trackerMutation() != null));
        }
        assertEquals(fixtureId + " normalized bot parity", parity.get(0), parity.get(1));
    }

    private static CountingRandom installCountingRandom(SwccgAiController bot)
            throws ReflectiveOperationException {
        CountingRandom random = new CountingRandom();
        Field field = bot.getClass().getDeclaredField("random");
        field.setAccessible(true);
        field.set(bot, random);
        Field side = bot.getClass().getDeclaredField("mySide");
        side.setAccessible(true);
        side.set(bot, Side.DARK);
        return random;
    }
}
