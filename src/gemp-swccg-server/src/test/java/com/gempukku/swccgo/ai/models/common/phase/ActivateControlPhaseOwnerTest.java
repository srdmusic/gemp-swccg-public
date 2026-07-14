package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.finalization.ResponseFinalizer;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ActivateControlPhaseOwnerTest {

    @Test
    public void everyLegalRouteFinalizesExactlyOnceWithoutChangingWire() {
        assertAccepted(ActivateControlRoute.ACTIVATE_TOP_LEVEL,
                topLevel(DecisionOrigin.PHASE_ACTION), "A1");
        assertAccepted(ActivateControlRoute.CONTROL_TOP_LEVEL,
                topLevel(DecisionOrigin.PHASE_ACTION), "A1");
        assertAccepted(ActivateControlRoute.ACTIVATE_AMOUNT,
                integer(DecisionOrigin.ACTIVATE_AMOUNT, 0, 7), "5");
        assertAccepted(ActivateControlRoute.ACTIVATE_ALLOWANCE,
                integer(DecisionOrigin.ACTIVATE_ALLOWANCE, 1, 7), "7");
        assertAccepted(ActivateControlRoute.ACTIVATE_ZERO_CONFIRM,
                multiple(DecisionOrigin.ACTIVATE_ZERO_CONFIRM, "Yes", "No"), "1");
        assertAccepted(ActivateControlRoute.ACTIVATE_ACK,
                multiple(DecisionOrigin.ACTIVATE_INTERRUPTION_ACK, "OK"), "0");
    }

    @Test
    public void passTopLevelFinalizesAsExactEmptyWire() {
        assertAccepted(ActivateControlRoute.ACTIVATE_TOP_LEVEL,
                topLevel(DecisionOrigin.PHASE_ACTION), "");
    }

    @Test
    public void zeroConfirmationUsesExactLabelsInEitherOrder() {
        assertEquals("1", ActivateControlPhaseOwner.zeroConfirmation(
                List.of("Yes", "No"), false).wire());
        assertEquals("0", ActivateControlPhaseOwner.zeroConfirmation(
                List.of("No", "Yes"), false).wire());
        assertEquals("0", ActivateControlPhaseOwner.zeroConfirmation(
                List.of("Yes", "No"), true).wire());
        assertEquals("1", ActivateControlPhaseOwner.zeroConfirmation(
                List.of("No", "Yes"), true).wire());
    }

    @Test
    public void malformedZeroAndAckLabelsRejectBeforeFinalizer() {
        assertRejectedBeforeFinalizer(
                multiple(DecisionOrigin.ACTIVATE_ZERO_CONFIRM, "Yes", "Maybe"),
                ActivateControlRoute.ACTIVATE_ZERO_CONFIRM,
                () -> ActivateControlPhaseOwner.zeroConfirmation(
                        List.of("Yes", "Maybe"), false));
        assertRejectedBeforeFinalizer(
                multiple(DecisionOrigin.ACTIVATE_ZERO_CONFIRM, "Yes", "Yes", "No"),
                ActivateControlRoute.ACTIVATE_ZERO_CONFIRM,
                () -> ActivateControlPhaseOwner.zeroConfirmation(
                        List.of("Yes", "Yes", "No"), false));
        assertRejectedBeforeFinalizer(
                multiple(DecisionOrigin.ACTIVATE_INTERRUPTION_ACK, "Continue"),
                ActivateControlRoute.ACTIVATE_ACK,
                () -> ActivateControlPhaseOwner.interruptionAcknowledgement(
                        List.of("Continue")));
        assertRejectedBeforeFinalizer(
                multiple(DecisionOrigin.ACTIVATE_INTERRUPTION_ACK, "OK", "OK"),
                ActivateControlRoute.ACTIVATE_ACK,
                () -> ActivateControlPhaseOwner.interruptionAcknowledgement(
                        List.of("OK", "OK")));
        assertRejectedBeforeFinalizer(
                multiple(DecisionOrigin.ACTIVATE_INTERRUPTION_ACK, "OK", "Continue"),
                ActivateControlRoute.ACTIVATE_ACK,
                () -> ActivateControlPhaseOwner.interruptionAcknowledgement(
                        List.of("OK", "Continue")));
    }

    @Test
    public void absentIntegerBoundsRejectBeforeSelectionAndFinalizer() {
        DecisionSnapshot snapshot = integerWithoutMaximum(DecisionOrigin.ACTIVATE_AMOUNT);
        AtomicInteger selections = new AtomicInteger();
        AtomicInteger finalizers = new AtomicInteger();

        AiDecisionResult result = ActivateControlPhaseOwner.decide(
                snapshot, RejectionHistory.empty(), ActivateControlRoute.ACTIVATE_AMOUNT,
                () -> {
                    selections.incrementAndGet();
                    return ActivateControlPhaseOwner.Selection.wire("3");
                },
                (s, c, i, r, h) -> {
                    finalizers.incrementAndGet();
                    return ResponseFinalizer.finalize(s, c, i, r, h);
                });

        assertTypedRejection(result);
        assertEquals(0, selections.get());
        assertEquals(0, finalizers.get());
    }

    @Test
    public void missingSnapshotAndContractMismatchRejectBeforeFinalizer() {
        AtomicInteger finalizers = new AtomicInteger();
        AiDecisionResult missing = ActivateControlPhaseOwner.decide(
                null, RejectionHistory.empty(), ActivateControlRoute.ACTIVATE_AMOUNT,
                () -> ActivateControlPhaseOwner.Selection.wire("1"),
                (s, c, i, r, h) -> {
                    finalizers.incrementAndGet();
                    return ResponseFinalizer.finalize(s, c, i, r, h);
                });
        assertTypedRejection(missing);

        AiDecisionResult mismatch = ActivateControlPhaseOwner.decide(
                topLevel(DecisionOrigin.PHASE_ACTION), RejectionHistory.empty(),
                ActivateControlRoute.ACTIVATE_AMOUNT,
                () -> ActivateControlPhaseOwner.Selection.wire("1"),
                (s, c, i, r, h) -> {
                    finalizers.incrementAndGet();
                    return ResponseFinalizer.finalize(s, c, i, r, h);
                });
        assertTypedRejection(mismatch);
        assertEquals(0, finalizers.get());
    }

    @Test
    public void exactImmutableRejectionHistoryReachesFinalizer() {
        RejectionHistory history = RejectionHistory.empty().append(
                "2", FinalizedResponse.RejectReason.ENGINE_DECISION_INVALID,
                "engine rejected first wire");
        AtomicInteger calls = new AtomicInteger();
        DecisionSnapshot snapshot = integer(DecisionOrigin.ACTIVATE_AMOUNT, 0, 7);

        AiDecisionResult result = ActivateControlPhaseOwner.decide(
                snapshot, history, ActivateControlRoute.ACTIVATE_AMOUNT,
                () -> ActivateControlPhaseOwner.Selection.wire("5"),
                (s, c, i, r, receivedHistory) -> {
                    calls.incrementAndGet();
                    assertSame(history, receivedHistory);
                    return ResponseFinalizer.finalize(s, c, i, r, receivedHistory);
                });

        assertWire(result, "5");
        assertEquals(1, calls.get());
        assertEquals(1, history.size());
    }

    private static void assertAccepted(ActivateControlRoute route,
                                       DecisionSnapshot snapshot, String wire) {
        AtomicInteger calls = new AtomicInteger();
        AiDecisionResult result = ActivateControlPhaseOwner.decide(
                snapshot, RejectionHistory.empty(), route,
                () -> ActivateControlPhaseOwner.Selection.wire(wire),
                (s, c, i, r, h) -> {
                    calls.incrementAndGet();
                    return ResponseFinalizer.finalize(s, c, i, r, h);
                });
        assertWire(result, wire);
        assertEquals(1, calls.get());
    }

    private static void assertRejectedBeforeFinalizer(
            DecisionSnapshot snapshot, ActivateControlRoute route,
            ActivateControlPhaseOwner.SelectionLane selection) {
        AtomicInteger finalizers = new AtomicInteger();
        AiDecisionResult result = ActivateControlPhaseOwner.decide(
                snapshot, RejectionHistory.empty(), route, selection,
                (s, c, i, r, h) -> {
                    finalizers.incrementAndGet();
                    return ResponseFinalizer.finalize(s, c, i, r, h);
                });
        assertTypedRejection(result);
        assertEquals(0, finalizers.get());
    }

    private static void assertWire(AiDecisionResult result, String wire) {
        assertEquals(AiDecisionResult.Status.WIRE_RESPONSE, result.status());
        assertEquals(wire, result.wireResponse());
        assertEquals(AiDecisionResult.MutationMode.OUTER_COMMON, result.mutationMode());
        assertTrue(result.fromTypedFinalizer());
        assertNotNull(result.trackerMutation());
        assertEquals(wire, result.trackerMutation().wireResponse());
    }

    private static void assertTypedRejection(AiDecisionResult result) {
        assertEquals(AiDecisionResult.Status.TYPED_REJECTION, result.status());
        assertNotNull(result.rejectionCode());
        assertNotNull(result.rejectionDetail());
        assertNull(result.wireResponse());
        assertNull(result.trackerMutation());
    }

    private static DecisionSnapshot topLevel(DecisionOrigin origin) {
        Map<String, String[]> params = base(origin);
        params.put("actionId", new String[]{"A1"});
        params.put("actionText", new String[]{"Activate Force"});
        params.put("min", new String[]{"0"});
        params.put("noPass", new String[]{"false"});
        return PullTestFixtures.snapshot(AwaitingDecisionType.CARD_ACTION_CHOICE,
                "Choose action or Pass", params);
    }

    private static DecisionSnapshot integer(DecisionOrigin origin, int min, int max) {
        Map<String, String[]> params = base(origin);
        params.put("min", new String[]{String.valueOf(min)});
        params.put("max", new String[]{String.valueOf(max)});
        params.put("defaultValue", new String[]{String.valueOf(max)});
        return PullTestFixtures.snapshot(AwaitingDecisionType.INTEGER,
                "Choose amount", params);
    }

    private static DecisionSnapshot integerWithoutMaximum(DecisionOrigin origin) {
        Map<String, String[]> params = base(origin);
        params.put("min", new String[]{"0"});
        return PullTestFixtures.snapshot(AwaitingDecisionType.INTEGER,
                "Choose amount", params);
    }

    private static DecisionSnapshot multiple(DecisionOrigin origin, String... results) {
        Map<String, String[]> params = base(origin);
        params.put("results", results);
        return PullTestFixtures.snapshot(AwaitingDecisionType.MULTIPLE_CHOICE,
                "Choose result", params);
    }

    private static Map<String, String[]> base(DecisionOrigin origin) {
        Map<String, String[]> params = new LinkedHashMap<>();
        params.put(DecisionOrigin.WIRE_PARAMETER, new String[]{origin.name()});
        return params;
    }
}
