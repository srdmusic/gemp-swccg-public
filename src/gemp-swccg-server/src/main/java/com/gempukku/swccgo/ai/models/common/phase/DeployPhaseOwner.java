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

/** Single typed finalization owner for an already-routed DEPLOY transaction stage. */
public final class DeployPhaseOwner {
    private enum AllBlockedPolicy {
        NOT_APPLICABLE,
        OPTIONAL_PASS,
        MANDATORY_EXACT_COMPATIBILITY
    }

    private static final RandomGenerator NO_RANDOM = new RandomGenerator() {
        @Override
        public long nextLong() {
            throw new IllegalStateException("owned DEPLOY finalization must not consume RNG");
        }
    };

    private DeployPhaseOwner() {
    }

    @FunctionalInterface
    public interface CompatibilityLane {
        String exactLegacyWire(DeployRoute route, DeployFacts facts,
                               DeployAssessment assessment);
    }

    public static AiDecisionResult decide(DecisionSnapshot snapshot,
                                          RejectionHistory history,
                                          DeployFacts facts,
                                          DeployAssessment assessment,
                                          CompatibilityLane compatibilityLane) {
        Objects.requireNonNull(history, "history");
        Objects.requireNonNull(facts, "facts");
        Objects.requireNonNull(assessment, "assessment");
        Objects.requireNonNull(compatibilityLane, "compatibilityLane");
        if (snapshot == null || facts.route() != assessment.route()) {
            return reject("DEPLOY facts and assessment do not identify one owned route",
                    facts.decisionId());
        }

        ResponseContract contract;
        try {
            contract = ResponseContract.from(snapshot);
        } catch (RuntimeException e) {
            return reject("DEPLOY response contract is malformed: " + detail(e),
                    facts.decisionId());
        }
        if (!routeMatchesContract(facts.route(), contract.decisionType())) {
            return reject("DEPLOY route does not match response contract "
                    + contract.decisionType(), facts.decisionId());
        }

        AllBlockedPolicy blockedPolicy = allBlockedPolicy(
                facts, assessment, contract);
        String expectedWire;
        ResponseIntent intent;
        if (blockedPolicy == AllBlockedPolicy.OPTIONAL_PASS) {
            expectedWire = "";
            intent = new ResponseIntent.Pass();
        } else if (facts.route() == DeployRoute.DEPLOY_V170_UNDERCOVER) {
            Integer ordinal = assessment.ownedChoiceOrdinal();
            if (ordinal == null) {
                return reject("V170 has no source-proven choice ordinal", facts.decisionId());
            }
            expectedWire = String.valueOf(ordinal);
            intent = new ResponseIntent.CandidateOrdinal(ordinal);
        } else {
            expectedWire = compatibilityLane.exactLegacyWire(
                    facts.route(), facts, assessment);
            if (expectedWire == null) {
                return reject("DEPLOY compatibility lane returned an unknown wire",
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
            return reject("DEPLOY finalizer violated the no-RNG lane: " + detail(e),
                    facts.decisionId());
        }
        if (finalized == null || finalized.status() != FinalizedResponse.Status.ACCEPTED
                || finalized.randomDraw() != null
                || !expectedWire.equals(finalized.wireResponse())) {
            return reject("DEPLOY finalizer changed the exact compatibility wire",
                    facts.decisionId());
        }
        return AiDecisionResult.finalizerWire(
                finalized.wireResponse(), facts.decisionId(), finalized.trackerMutation());
    }

    private static AllBlockedPolicy allBlockedPolicy(
            DeployFacts facts,
            DeployAssessment assessment,
            ResponseContract contract) {
        if (facts.route() != DeployRoute.DEPLOY_PARENT
                || assessment.formation().verdict()
                    != DeployFormationAssessment.Verdict.ALL_DESTINATIONS_BLOCKED) {
            return AllBlockedPolicy.NOT_APPLICABLE;
        }
        return contract.policyPassAllowed() && contract.emptyWireAccepted()
                ? AllBlockedPolicy.OPTIONAL_PASS
                : AllBlockedPolicy.MANDATORY_EXACT_COMPATIBILITY;
    }

    private static Translation translate(ResponseContract contract, String wire) {
        AwaitingDecisionType type = contract.decisionType();
        if (type == AwaitingDecisionType.CARD_ACTION_CHOICE) {
            if (wire.isEmpty()) {
                if (!contract.policyPassAllowed() || !contract.emptyWireAccepted()) {
                    return Translation.failure("exact DEPLOY Pass is not legal");
                }
                return Translation.success(new ResponseIntent.Pass());
            }
            int ordinal = uniqueOrdinal(contract.candidateWireIds(), wire);
            return ordinal >= 0
                    ? Translation.success(new ResponseIntent.CandidateOrdinal(ordinal))
                    : Translation.failure("DEPLOY parent wire is absent or ambiguous: " + wire);
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
                            "DEPLOY destination wire is absent, duplicate, or ambiguous: " + id);
                }
                ordinals.add(ordinal);
            }
            return Translation.success(new ResponseIntent.CardOrdinals(ordinals));
        }
        if (type == AwaitingDecisionType.MULTIPLE_CHOICE) {
            try {
                return Translation.success(new ResponseIntent.CandidateOrdinal(
                        Integer.parseInt(wire)));
            } catch (NumberFormatException e) {
                return Translation.failure("DEPLOY choice wire is not an ordinal: " + wire);
            }
        }
        return Translation.failure("unsupported DEPLOY response type " + type);
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

    private static boolean routeMatchesContract(DeployRoute route,
                                                AwaitingDecisionType type) {
        return switch (route) {
            case DEPLOY_PARENT -> type == AwaitingDecisionType.CARD_ACTION_CHOICE;
            case DEPLOY_DESTINATION -> type == AwaitingDecisionType.CARD_SELECTION;
            case DEPLOY_BUDDY -> type == AwaitingDecisionType.CARD_SELECTION
                    || type == AwaitingDecisionType.ARBITRARY_CARDS;
            case DEPLOY_V170_UNDERCOVER, DEPLOY_CAPACITY, DEPLOY_CONFIRMATION ->
                    type == AwaitingDecisionType.MULTIPLE_CHOICE;
            default -> false;
        };
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
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
