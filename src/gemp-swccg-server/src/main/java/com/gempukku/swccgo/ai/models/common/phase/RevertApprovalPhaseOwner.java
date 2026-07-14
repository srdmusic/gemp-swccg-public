package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.FinalizedResponseAdapter;
import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.finalization.ResponseContract;
import com.gempukku.swccgo.ai.models.common.finalization.ResponseFinalizer;
import com.gempukku.swccgo.ai.models.common.finalization.ResponseIntent;

import java.util.Locale;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** One typed finalization owner for V44/V67j opponent-revert approval. */
public final class RevertApprovalPhaseOwner {

    private static final RandomGenerator NO_RANDOM = new RandomGenerator() {
        @Override
        public long nextLong() {
            throw new IllegalStateException("V44/V67j finalization must not consume RNG");
        }
    };

    private RevertApprovalPhaseOwner() {
    }

    /** Exact legacy label scan, including the fallback ordinal and log label. */
    public record LegacySelection(int ordinal, String resultText) {
        public LegacySelection {
            if (ordinal < 0) {
                throw new IllegalArgumentException("legacy revert ordinal must be nonnegative");
            }
            Objects.requireNonNull(resultText, "resultText");
        }
    }

    @FunctionalInterface
    interface FinalizerLane {
        FinalizedResponse finalize(DecisionSnapshot snapshot,
                                   ResponseContract contract,
                                   ResponseIntent intent,
                                   RandomGenerator random,
                                   RejectionHistory history);
    }

    public static LegacySelection legacySelection(String[] results) {
        if (results != null) {
            for (int ordinal = 0; ordinal < results.length; ordinal++) {
                String normalized = results[ordinal] != null
                        ? results[ordinal].toLowerCase(Locale.ROOT) : "";
                if (normalized.equals("yes") || normalized.contains("allow")
                        || normalized.contains("accept") || normalized.contains("ok")
                        || normalized.equals("revert")) {
                    return new LegacySelection(ordinal, results[ordinal]);
                }
            }
        }
        return new LegacySelection(0, "(default index 0)");
    }

    public static AiDecisionResult decide(DecisionSnapshot snapshot,
                                          RejectionHistory history,
                                          LegacySelection selection) {
        return decide(snapshot, history, selection, ResponseFinalizer::finalize);
    }

    static AiDecisionResult decide(DecisionSnapshot snapshot,
                                   RejectionHistory history,
                                   LegacySelection selection,
                                   FinalizerLane finalizerLane) {
        Objects.requireNonNull(history, "history");
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(finalizerLane, "finalizerLane");
        if (snapshot == null) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "owned V44/V67j route has no immutable decision snapshot", "unknown");
        }

        String decisionId = snapshot.decisionFacts().decisionId();
        ResponseContract contract;
        try {
            contract = ResponseContract.from(snapshot);
        } catch (RuntimeException e) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "V44/V67j response contract is malformed: " + detail(e), decisionId);
        }

        FinalizedResponse finalized;
        try {
            finalized = finalizerLane.finalize(snapshot, contract,
                    new ResponseIntent.CandidateOrdinal(selection.ordinal()),
                    NO_RANDOM, history);
        } catch (RuntimeException e) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "V44/V67j finalizer violated the no-RNG single lane: " + detail(e), decisionId);
        }
        if (finalized == null) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "V44/V67j finalizer returned no result", decisionId);
        }
        if (finalized.status() == FinalizedResponse.Status.REJECTED) {
            return FinalizedResponseAdapter.toDecisionResult(
                    finalized, decisionId, AiDecisionResult.MutationMode.NONE);
        }

        String expectedWire = String.valueOf(selection.ordinal());
        if (finalized.status() != FinalizedResponse.Status.ACCEPTED
                || finalized.randomDraw() != null
                || !expectedWire.equals(finalized.wireResponse())) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "V44/V67j finalizer changed or replaced the exact legacy wire", decisionId);
        }
        return FinalizedResponseAdapter.toDecisionResult(
                finalized, decisionId, AiDecisionResult.MutationMode.NONE);
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
