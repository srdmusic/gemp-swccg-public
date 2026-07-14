package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.finalization.ResponseContract;
import com.gempukku.swccgo.ai.models.common.finalization.ResponseFinalizer;
import com.gempukku.swccgo.ai.models.common.finalization.ResponseIntent;

import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** One typed finalization owner for canonical top-level DRAW decisions. */
public final class DrawPhaseOwner {

    private static final RandomGenerator NO_RANDOM = new RandomGenerator() {
        @Override
        public long nextLong() {
            throw new IllegalStateException("owned DRAW finalization must not consume RNG");
        }
    };

    private DrawPhaseOwner() {
    }

    @FunctionalInterface
    public interface EvaluatorLane {
        Evaluation evaluate();
    }

    /** The single CombinedEvaluator result translated without exposing mirrored evaluator types. */
    public record Evaluation(boolean pass, String actionId) {
        public Evaluation {
            if (pass && actionId != null && !actionId.isEmpty()) {
                throw new IllegalArgumentException("a pass evaluation cannot carry a nonempty action id");
            }
            if (!pass && (actionId == null || actionId.isBlank())) {
                throw new IllegalArgumentException("a candidate evaluation requires a nonblank action id");
            }
        }

        public static Evaluation passResult() {
            return new Evaluation(true, "");
        }

        public static Evaluation candidate(String actionId) {
            return new Evaluation(false, actionId);
        }
    }

    public static AiDecisionResult decide(DecisionSnapshot snapshot,
                                          RejectionHistory history,
                                          EvaluatorLane evaluatorLane) {
        Objects.requireNonNull(history, "history");
        Objects.requireNonNull(evaluatorLane, "evaluatorLane");
        if (snapshot == null) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "owned DRAW route has no immutable decision snapshot", "unknown");
        }

        String decisionId = snapshot.decisionFacts().decisionId();
        Evaluation evaluation = evaluatorLane.evaluate();
        if (evaluation == null) {
            return reject(FinalizedResponse.RejectReason.NO_LEGAL_FALLBACK,
                    "owned DRAW evaluator lane produced no result", decisionId);
        }

        ResponseContract contract = ResponseContract.from(snapshot);
        ResponseIntent intent;
        String expectedWire;
        if (evaluation.pass()) {
            if (!contract.policyPassAllowed() || !contract.emptyWireAccepted()) {
                return reject(FinalizedResponse.RejectReason.NO_LEGAL_FALLBACK,
                        "owned DRAW evaluator selected Pass but the frozen contract denies it; "
                                + "the DRAW owner does not invent a forced fallback",
                        decisionId);
            }
            intent = new ResponseIntent.Pass();
            expectedWire = "";
        } else {
            int ordinal = uniqueOrdinal(contract.candidateWireIds(), evaluation.actionId());
            if (ordinal < 0) {
                return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                        "owned DRAW winner action id is missing or ambiguous in frozen candidate order: "
                                + evaluation.actionId(), decisionId);
            }
            intent = new ResponseIntent.CandidateOrdinal(ordinal);
            expectedWire = evaluation.actionId();
        }

        FinalizedResponse finalized = ResponseFinalizer.finalize(
                snapshot, contract, intent, NO_RANDOM, history);
        if (finalized.status() == FinalizedResponse.Status.REJECTED) {
            return reject(finalized.rejection().reason(), finalized.rejection().detail(), decisionId);
        }
        if (finalized.status() != FinalizedResponse.Status.ACCEPTED) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "owned DRAW finalizer changed the evaluator intent: " + finalized.status(), decisionId);
        }
        if (finalized.randomDraw() != null) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "owned DRAW finalizer consumed RNG", decisionId);
        }
        if (!expectedWire.equals(finalized.wireResponse())) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "owned DRAW finalizer wire mismatch: expected '" + expectedWire
                            + "' but got '" + finalized.wireResponse() + "'", decisionId);
        }
        return AiDecisionResult.finalizerWire(finalized.wireResponse(), decisionId,
                finalized.trackerMutation());
    }

    private static int uniqueOrdinal(List<String> candidateWireIds, String actionId) {
        int ordinal = -1;
        for (int i = 0; i < candidateWireIds.size(); i++) {
            if (Objects.equals(actionId, candidateWireIds.get(i))) {
                if (ordinal >= 0) {
                    return -1;
                }
                ordinal = i;
            }
        }
        return ordinal;
    }

    private static AiDecisionResult reject(FinalizedResponse.RejectReason reason,
                                           String detail,
                                           String decisionId) {
        return AiDecisionResult.typedRejection(reason, detail, decisionId);
    }
}
