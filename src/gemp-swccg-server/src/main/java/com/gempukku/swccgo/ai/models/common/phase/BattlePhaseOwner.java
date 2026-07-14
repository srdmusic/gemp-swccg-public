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

/** Single typed finalization owner for an already-routed BATTLE decision. */
public final class BattlePhaseOwner {
    private static final RandomGenerator NO_RANDOM = new RandomGenerator() {
        @Override
        public long nextLong() {
            throw new IllegalStateException(
                    "owned BATTLE finalization must not consume RNG");
        }
    };

    private BattlePhaseOwner() {
    }

    @FunctionalInterface
    public interface CompatibilityLane {
        String exactLegacyWire(BattleFacts facts, BattleAssessment assessment);
    }

    public static AiDecisionResult decide(DecisionSnapshot snapshot,
                                          RejectionHistory history,
                                          BattleFacts facts,
                                          BattleAssessment assessment,
                                          CompatibilityLane compatibilityLane) {
        Objects.requireNonNull(history, "history");
        Objects.requireNonNull(facts, "facts");
        Objects.requireNonNull(assessment, "assessment");
        Objects.requireNonNull(compatibilityLane, "compatibilityLane");
        if (snapshot == null || facts.route() != assessment.route()) {
            return reject("BATTLE facts and assessment do not identify one route",
                    facts.decisionId());
        }

        ResponseContract contract;
        try {
            contract = ResponseContract.from(snapshot);
        } catch (RuntimeException e) {
            return reject("BATTLE response contract is malformed: " + detail(e),
                    facts.decisionId());
        }
        if (contract.decisionType() != facts.decisionType()) {
            return reject("BATTLE facts do not match response contract type",
                    facts.decisionId());
        }

        String expectedWire;
        ResponseIntent intent;
        if (assessment.optionalImmuneForfeit()) {
            expectedWire = "";
            intent = new ResponseIntent.Pass();
        } else {
            expectedWire = compatibilityLane.exactLegacyWire(facts, assessment);
            if (expectedWire == null) {
                return reject("BATTLE compatibility lane returned an unknown wire",
                        facts.decisionId());
            }
            Translation translated = translate(contract, expectedWire);
            if (translated.failure() != null) {
                return reject(translated.failure(), facts.decisionId());
            }
            intent = translated.intent();
        }

        FinalizedResponse finalized;
        try {
            finalized = ResponseFinalizer.finalize(
                    snapshot, contract, intent, NO_RANDOM, history);
        } catch (RuntimeException e) {
            return reject("BATTLE finalizer violated the no-RNG lane: " + detail(e),
                    facts.decisionId());
        }
        if (finalized == null
                || finalized.status() != FinalizedResponse.Status.ACCEPTED
                || finalized.randomDraw() != null
                || !expectedWire.equals(finalized.wireResponse())) {
            return reject("BATTLE finalizer changed the exact compatibility wire",
                    facts.decisionId());
        }
        return AiDecisionResult.finalizerWire(
                finalized.wireResponse(), facts.decisionId(),
                finalized.trackerMutation());
    }

    private static Translation translate(ResponseContract contract, String wire) {
        AwaitingDecisionType type = contract.decisionType();
        if (type == AwaitingDecisionType.EMPTY) {
            return wire.isEmpty()
                    ? Translation.success(new ResponseIntent.Acknowledge())
                    : Translation.failure("EMPTY BATTLE response must be empty");
        }
        if (type == AwaitingDecisionType.INTEGER) {
            try {
                return Translation.success(new ResponseIntent.IntegerValue(
                        Integer.parseInt(wire)));
            } catch (NumberFormatException e) {
                return Translation.failure("BATTLE integer wire is not an integer: " + wire);
            }
        }
        if (type == AwaitingDecisionType.ACTION_CHOICE
                || type == AwaitingDecisionType.CARD_ACTION_CHOICE
                || type == AwaitingDecisionType.MULTIPLE_CHOICE) {
            if (wire.isEmpty()) {
                if (type == AwaitingDecisionType.CARD_ACTION_CHOICE
                        && !contract.policyPassAllowed()) {
                    return Translation.success(new ResponseIntent.Acknowledge());
                }
                return Translation.success(new ResponseIntent.Pass());
            }
            int ordinal = uniqueOrdinal(contract.candidateWireIds(), wire);
            return ordinal >= 0
                    ? Translation.success(new ResponseIntent.CandidateOrdinal(ordinal))
                    : Translation.failure(
                            "BATTLE action wire is absent or ambiguous: " + wire);
        }
        if (type == AwaitingDecisionType.CARD_SELECTION
                || type == AwaitingDecisionType.ARBITRARY_CARDS) {
            if (wire.isEmpty()) {
                return Translation.success(new ResponseIntent.CardOrdinals(List.of()));
            }
            String[] ids = wire.split(",", -1);
            List<Integer> ordinals = new ArrayList<>(ids.length);
            Set<Integer> seen = new HashSet<>();
            for (String id : ids) {
                int ordinal = uniqueOrdinal(contract.candidateWireIds(), id);
                if (ordinal < 0 || !seen.add(ordinal)) {
                    return Translation.failure(
                            "BATTLE card wire is absent, duplicate, or ambiguous: " + id);
                }
                ordinals.add(ordinal);
            }
            return Translation.success(new ResponseIntent.CardOrdinals(ordinals));
        }
        return Translation.failure("unsupported BATTLE response type " + type);
    }

    private static int uniqueOrdinal(List<String> ids, String wire) {
        int found = -1;
        for (int i = 0; i < ids.size(); i++) {
            if (Objects.equals(ids.get(i), wire)) {
                if (found >= 0) {
                    return -1;
                }
                found = i;
            }
        }
        return found;
    }

    private record Translation(ResponseIntent intent, String failure) {
        static Translation success(ResponseIntent intent) {
            return new Translation(Objects.requireNonNull(intent, "intent"), null);
        }

        static Translation failure(String reason) {
            return new Translation(null, reason);
        }
    }

    private static AiDecisionResult reject(String detail, String decisionId) {
        return AiDecisionResult.typedRejection(
                FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                detail, decisionId);
    }

    private static String detail(RuntimeException e) {
        return e.getMessage() != null ? e.getMessage()
                : e.getClass().getSimpleName();
    }
}
