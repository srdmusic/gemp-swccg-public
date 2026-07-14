package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.finalization.ResponseContract;
import com.gempukku.swccgo.ai.models.common.finalization.ResponseFinalizer;
import com.gempukku.swccgo.ai.models.common.finalization.ResponseIntent;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;

/** Single typed finalization owner for an already-routed PULL transaction stage. */
public final class PullPhaseOwner {

    private static final RandomGenerator NO_RANDOM = new RandomGenerator() {
        @Override
        public long nextLong() {
            throw new IllegalStateException("owned PULL finalization must not consume RNG");
        }
    };

    private PullPhaseOwner() {
    }

    @FunctionalInterface
    public interface CompatibilityLane {
        /** Returns the exact wire chosen by the current legacy evaluator path. */
        String exactLegacyWire(PullRoute route, PullFacts facts, PullAssessment assessment);
    }

    @FunctionalInterface
    interface FinalizerLane {
        FinalizedResponse finalize(DecisionSnapshot snapshot,
                                   ResponseContract contract,
                                   ResponseIntent intent,
                                   RandomGenerator random,
                                   RejectionHistory history);
    }

    public static AiDecisionResult decide(DecisionSnapshot snapshot,
                                          RejectionHistory history,
                                          PullRoute route,
                                          PullFacts facts,
                                          PullAssessment assessment,
                                          CompatibilityLane compatibilityLane) {
        return decide(snapshot, history, route, facts, assessment, compatibilityLane,
                ResponseFinalizer::finalize);
    }

    static AiDecisionResult decide(DecisionSnapshot snapshot,
                                   RejectionHistory history,
                                   PullRoute route,
                                   PullFacts facts,
                                   PullAssessment assessment,
                                   CompatibilityLane compatibilityLane,
                                   FinalizerLane finalizerLane) {
        Objects.requireNonNull(history, "history");
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(facts, "facts");
        Objects.requireNonNull(assessment, "assessment");
        Objects.requireNonNull(compatibilityLane, "compatibilityLane");
        Objects.requireNonNull(finalizerLane, "finalizerLane");
        if (snapshot == null) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "owned PULL route has no immutable decision snapshot", "unknown");
        }

        String decisionId = snapshot.decisionFacts().decisionId();
        if (route == PullRoute.LEGACY_UNOWNED || facts.route() != route
                || assessment.route() != route) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "PULL route, facts, and assessment do not identify one owned stage", decisionId);
        }
        if (!decisionId.equals(facts.decisionId())
                || snapshot.decisionFacts().turn() != facts.turn()
                || !snapshot.decisionFacts().currentPlayer().equals(facts.playerId())
                || !Objects.equals(snapshot.decisionFacts().phase(), facts.phase())) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "PULL facts do not match the immutable decision snapshot", decisionId);
        }

        ResponseContract contract;
        try {
            contract = ResponseContract.from(snapshot);
        } catch (RuntimeException e) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "PULL response contract is malformed: " + detail(e), decisionId);
        }
        if (!routeMatchesContract(route, contract.decisionType())) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "PULL route does not match response contract " + contract.decisionType(), decisionId);
        }

        String expectedWire;
        ResponseIntent intent;
        if (route == PullRoute.PULL_FAILED_VERIFY) {
            expectedWire = "";
            intent = new ResponseIntent.CardOrdinals(List.of());
        } else {
            expectedWire = compatibilityLane.exactLegacyWire(route, facts, assessment);
            if (expectedWire == null) {
                return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                        "PULL compatibility lane returned an unknown wire", decisionId);
            }
            Translation translation = translate(route, contract, expectedWire);
            if (translation.failure() != null) {
                return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                        translation.failure(), decisionId);
            }
            intent = translation.intent();
        }

        FinalizedResponse finalized;
        try {
            finalized = finalizerLane.finalize(snapshot, contract, intent, NO_RANDOM, history);
        } catch (RuntimeException e) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "PULL finalizer violated the no-RNG single lane: " + detail(e), decisionId);
        }
        if (finalized == null) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "PULL finalizer returned no result", decisionId);
        }
        if (finalized.status() != FinalizedResponse.Status.ACCEPTED
                || finalized.randomDraw() != null
                || !expectedWire.equals(finalized.wireResponse())) {
            return reject(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                    "PULL finalizer changed or replaced the exact compatibility wire", decisionId);
        }
        return AiDecisionResult.finalizerWire(finalized.wireResponse(), decisionId,
                finalized.trackerMutation());
    }

    private static Translation translate(PullRoute route,
                                         ResponseContract contract,
                                         String wire) {
        if (route == PullRoute.PULL_PARENT) {
            if (wire.isEmpty()) {
                if (!contract.policyPassAllowed() || !contract.emptyWireAccepted()) {
                    return Translation.failure("exact PULL Pass is not legal for this frozen contract");
                }
                return Translation.success(new ResponseIntent.Pass());
            }
            int ordinal = uniqueOrdinal(contract.candidateWireIds(), wire);
            return ordinal >= 0
                    ? Translation.success(new ResponseIntent.CandidateOrdinal(ordinal))
                    : Translation.failure("PULL parent wire is missing or ambiguous: " + wire);
        }

        if (wire.isEmpty()) {
            return Translation.success(new ResponseIntent.CardOrdinals(List.of()));
        }
        String[] ids = wire.split(",", -1);
        List<Integer> ordinals = new ArrayList<>(ids.length);
        Set<Integer> seen = new HashSet<>();
        for (String id : ids) {
            if (id.isEmpty()) {
                return Translation.failure("PULL card wire contains an empty selection id");
            }
            int ordinal = uniqueOrdinal(contract.candidateWireIds(), id);
            if (ordinal < 0 || !seen.add(ordinal)) {
                return Translation.failure("PULL card wire is missing, duplicate, or ambiguous: " + id);
            }
            ordinals.add(ordinal);
        }
        return Translation.success(new ResponseIntent.CardOrdinals(ordinals));
    }

    private static int uniqueOrdinal(List<String> candidateWireIds, String wireId) {
        int ordinal = -1;
        for (int i = 0; i < candidateWireIds.size(); i++) {
            if (Objects.equals(wireId, candidateWireIds.get(i))) {
                if (ordinal >= 0) {
                    return -1;
                }
                ordinal = i;
            }
        }
        return ordinal;
    }

    private static boolean routeMatchesContract(PullRoute route, AwaitingDecisionType type) {
        return switch (route) {
            case PULL_PARENT -> type == AwaitingDecisionType.CARD_ACTION_CHOICE;
            case PULL_DEPLOY_CHILD, PULL_TAKE_CHILD, PULL_FAILED_VERIFY ->
                    type == AwaitingDecisionType.ARBITRARY_CARDS;
            case PULL_DESTINATION -> type == AwaitingDecisionType.CARD_SELECTION;
            default -> false;
        };
    }

    private record Translation(ResponseIntent intent, String failure) {
        private static Translation success(ResponseIntent intent) {
            return new Translation(Objects.requireNonNull(intent, "intent"), null);
        }

        private static Translation failure(String failure) {
            return new Translation(null, failure);
        }
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
