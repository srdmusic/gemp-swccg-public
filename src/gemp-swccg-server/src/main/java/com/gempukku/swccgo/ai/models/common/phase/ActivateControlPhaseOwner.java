package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.FinalizedResponseAdapter;
import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.finalization.ResponseContract;
import com.gempukku.swccgo.ai.models.common.finalization.ResponseFinalizer;
import com.gempukku.swccgo.ai.models.common.finalization.ResponseIntent;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Single typed finalization owner for the six stamped ACTIVATE and CONTROL routes. */
public final class ActivateControlPhaseOwner {

    private static final RandomGenerator NO_RANDOM = new RandomGenerator() {
        @Override
        public long nextLong() {
            throw new IllegalStateException("owned ACTIVATE/CONTROL finalization must not consume RNG");
        }
    };

    private ActivateControlPhaseOwner() {
    }

    @FunctionalInterface
    public interface SelectionLane {
        Selection select();
    }

    @FunctionalInterface
    interface FinalizerLane {
        FinalizedResponse finalize(DecisionSnapshot snapshot,
                                   ResponseContract contract,
                                   ResponseIntent intent,
                                   RandomGenerator random,
                                   RejectionHistory history);
    }

    /** Pre-finalizer result. Rejection is distinct from an empty Pass wire. */
    public record Selection(String wire,
                            FinalizedResponse.RejectReason rejectionReason,
                            String rejectionDetail) {
        public Selection {
            boolean selected = wire != null;
            boolean rejected = rejectionReason != null || rejectionDetail != null;
            if (selected == rejected) {
                throw new IllegalArgumentException("selection must carry exactly one wire or rejection");
            }
            if (rejected && (rejectionReason == null
                    || rejectionDetail == null || rejectionDetail.isBlank())) {
                throw new IllegalArgumentException("rejected selection requires typed nonblank detail");
            }
        }

        public static Selection wire(String wire) {
            return new Selection(Objects.requireNonNull(wire, "wire"), null, null);
        }

        public static Selection rejected(FinalizedResponse.RejectReason reason, String detail) {
            return new Selection(null, Objects.requireNonNull(reason, "reason"), detail);
        }

        public boolean isRejected() {
            return wire == null;
        }
    }

    public static Selection zeroConfirmation(List<String> results, boolean keepThreeForBattle) {
        int yes = uniqueExact(results, "Yes");
        int no = uniqueExact(results, "No");
        if (results == null || results.size() != 2 || yes < 0 || no < 0) {
            return Selection.rejected(FinalizedResponse.RejectReason.ORDINAL_OUT_OF_BOUNDS,
                    "ACTIVATE zero confirmation requires exactly one Yes and one No result");
        }
        return Selection.wire(String.valueOf(keepThreeForBattle ? yes : no));
    }

    public static Selection interruptionAcknowledgement(List<String> results) {
        int ok = uniqueExact(results, "OK");
        if (results == null || results.size() != 1 || ok < 0) {
            return Selection.rejected(FinalizedResponse.RejectReason.ORDINAL_OUT_OF_BOUNDS,
                    "ACTIVATE interruption acknowledgement requires exactly one OK result");
        }
        return Selection.wire(String.valueOf(ok));
    }

    public static AiDecisionResult decide(DecisionSnapshot snapshot,
                                          RejectionHistory history,
                                          ActivateControlRoute route,
                                          SelectionLane selectionLane) {
        return decide(snapshot, history, route, selectionLane, ResponseFinalizer::finalize);
    }

    static AiDecisionResult decide(DecisionSnapshot snapshot,
                                   RejectionHistory history,
                                   ActivateControlRoute route,
                                   SelectionLane selectionLane,
                                   FinalizerLane finalizerLane) {
        Objects.requireNonNull(history, "history");
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(selectionLane, "selectionLane");
        Objects.requireNonNull(finalizerLane, "finalizerLane");
        if (snapshot == null) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "owned ACTIVATE/CONTROL route has no immutable decision snapshot", "unknown");
        }

        String decisionId = snapshot.decisionFacts().decisionId();
        if (route == ActivateControlRoute.LEGACY_UNOWNED) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "unowned ACTIVATE/CONTROL route reached the owner", decisionId);
        }

        ResponseContract contract;
        try {
            contract = ResponseContract.from(snapshot);
        } catch (RuntimeException e) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "ACTIVATE/CONTROL response contract is malformed: " + detail(e), decisionId);
        }
        if (!routeMatchesContract(route, contract.decisionType())) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "ACTIVATE/CONTROL route does not match response contract "
                            + contract.decisionType(), decisionId);
        }
        if ((route == ActivateControlRoute.ACTIVATE_AMOUNT
                || route == ActivateControlRoute.ACTIVATE_ALLOWANCE)
                && (contract.minimum() == null || contract.maximum() == null
                    || contract.minimum() < 0 || contract.maximum() <= 0
                    || contract.maximum() < contract.minimum())) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "owned ACTIVATE integer bounds are absent or malformed", decisionId);
        }

        Selection selection = selectionLane.select();
        if (selection == null) {
            return reject(FinalizedResponse.RejectReason.NO_LEGAL_FALLBACK,
                    "owned ACTIVATE/CONTROL selection lane produced no result", decisionId);
        }
        if (selection.isRejected()) {
            return reject(selection.rejectionReason(), selection.rejectionDetail(), decisionId);
        }

        ResponseIntent intent;
        try {
            intent = translate(route, contract, selection.wire());
        } catch (IllegalArgumentException e) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    e.getMessage(), decisionId);
        }

        FinalizedResponse finalized;
        try {
            finalized = finalizerLane.finalize(snapshot, contract, intent, NO_RANDOM, history);
        } catch (RuntimeException e) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "ACTIVATE/CONTROL finalizer violated the no-RNG lane: " + detail(e), decisionId);
        }
        if (finalized == null) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "ACTIVATE/CONTROL finalizer returned no result", decisionId);
        }
        if (finalized.status() == FinalizedResponse.Status.REJECTED) {
            return reject(finalized.rejection().reason(), finalized.rejection().detail(), decisionId);
        }
        if (finalized.status() != FinalizedResponse.Status.ACCEPTED
                || finalized.randomDraw() != null
                || !selection.wire().equals(finalized.wireResponse())) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "ACTIVATE/CONTROL finalizer changed the owned intent", decisionId);
        }
        return FinalizedResponseAdapter.toDecisionResult(finalized, decisionId,
                AiDecisionResult.MutationMode.OUTER_COMMON);
    }

    private static ResponseIntent translate(ActivateControlRoute route,
                                            ResponseContract contract,
                                            String wire) {
        return switch (route) {
            case ACTIVATE_TOP_LEVEL, CONTROL_TOP_LEVEL -> topLevelIntent(contract, wire);
            case ACTIVATE_AMOUNT, ACTIVATE_ALLOWANCE -> integerIntent(wire);
            case ACTIVATE_ZERO_CONFIRM, ACTIVATE_ACK -> ordinalIntent(contract, wire);
            default -> throw new IllegalArgumentException("unowned ACTIVATE/CONTROL route");
        };
    }

    private static ResponseIntent topLevelIntent(ResponseContract contract, String wire) {
        if (wire.isEmpty()) {
            if (!contract.policyPassAllowed() || !contract.emptyWireAccepted()) {
                throw new IllegalArgumentException("owned top-level Pass is not legal");
            }
            return new ResponseIntent.Pass();
        }
        int ordinal = uniqueExact(contract.candidateWireIds(), wire);
        if (ordinal < 0) {
            throw new IllegalArgumentException(
                    "owned top-level action id is missing or ambiguous: " + wire);
        }
        return new ResponseIntent.CandidateOrdinal(ordinal);
    }

    private static ResponseIntent integerIntent(String wire) {
        try {
            return new ResponseIntent.IntegerValue(Integer.parseInt(wire));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("owned ACTIVATE amount is not an integer: " + wire);
        }
    }

    private static ResponseIntent ordinalIntent(ResponseContract contract, String wire) {
        final int ordinal;
        try {
            ordinal = Integer.parseInt(wire);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("owned ACTIVATE result is not an ordinal: " + wire);
        }
        if (!contract.inCandidateBounds(ordinal)) {
            throw new IllegalArgumentException("owned ACTIVATE ordinal is out of bounds: " + wire);
        }
        return new ResponseIntent.CandidateOrdinal(ordinal);
    }

    private static boolean routeMatchesContract(ActivateControlRoute route,
                                                AwaitingDecisionType type) {
        return switch (route) {
            case ACTIVATE_TOP_LEVEL, CONTROL_TOP_LEVEL ->
                    type == AwaitingDecisionType.CARD_ACTION_CHOICE;
            case ACTIVATE_AMOUNT, ACTIVATE_ALLOWANCE -> type == AwaitingDecisionType.INTEGER;
            case ACTIVATE_ZERO_CONFIRM, ACTIVATE_ACK ->
                    type == AwaitingDecisionType.MULTIPLE_CHOICE;
            default -> false;
        };
    }

    private static int uniqueExact(List<String> values, String expected) {
        if (values == null) {
            return -1;
        }
        int found = -1;
        for (int i = 0; i < values.size(); i++) {
            if (Objects.equals(expected, values.get(i))) {
                if (found >= 0) {
                    return -1;
                }
                found = i;
            }
        }
        return found;
    }

    private static String detail(RuntimeException e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    private static AiDecisionResult reject(FinalizedResponse.RejectReason reason,
                                           String detail,
                                           String decisionId) {
        return AiDecisionResult.typedRejection(reason, detail, decisionId);
    }
}
