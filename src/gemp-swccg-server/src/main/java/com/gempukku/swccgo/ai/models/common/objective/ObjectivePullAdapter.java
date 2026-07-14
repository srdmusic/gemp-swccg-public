package com.gempukku.swccgo.ai.models.common.objective;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.phase.PullAssessment;
import com.gempukku.swccgo.ai.models.common.phase.PullFacts;
import com.gempukku.swccgo.ai.models.common.phase.PullRoute;
import com.gempukku.swccgo.common.PullPhysicalCardRef;

import java.util.List;
import java.util.Objects;

/** Pure bridge from typed PULL facts to objective-owned outputs. */
public final class ObjectivePullAdapter {

    private static final float PARENT_CONTRIBUTION = 1500.0f;
    private static final float CHILD_LOCATION_RANK = 500.0f;

    private ObjectivePullAdapter() {
    }

    public enum ChildKind {
        LOCATION,
        OTHER
    }

    public record ChildCandidate(PullPhysicalCardRef card, ChildKind kind) {
        public ChildCandidate {
            Objects.requireNonNull(card, "card");
            Objects.requireNonNull(kind, "kind");
        }
    }

    public record ChildRank(PullPhysicalCardRef card, float rank) {
        public ChildRank {
            Objects.requireNonNull(card, "card");
            if (Float.floatToIntBits(rank) != Float.floatToIntBits(CHILD_LOCATION_RANK)) {
                throw new IllegalArgumentException("objective child location rank must be +500");
            }
        }
    }

    public record FailedVerifyIntent(PullPhysicalCardRef sourceCard, long transactionId) {
        public FailedVerifyIntent {
            Objects.requireNonNull(sourceCard, "sourceCard");
            if (transactionId <= 0) {
                throw new IllegalArgumentException("transactionId must be > 0");
            }
        }
    }

    public record Result(
            DecisionSnapshot snapshot,
            PullFacts pullFacts,
            List<ObjectiveContribution> parentContributions,
            List<ChildRank> childRanks,
            List<FailedVerifyIntent> failedVerifyIntents) {

        public Result {
            Objects.requireNonNull(snapshot, "snapshot");
            Objects.requireNonNull(pullFacts, "pullFacts");
            parentContributions = List.copyOf(parentContributions);
            childRanks = List.copyOf(childRanks);
            failedVerifyIntents = List.copyOf(failedVerifyIntents);
            int populatedStages = (parentContributions.isEmpty() ? 0 : 1)
                    + (childRanks.isEmpty() ? 0 : 1)
                    + (failedVerifyIntents.isEmpty() ? 0 : 1);
            if (populatedStages > 1) {
                throw new IllegalArgumentException("PULL parent, child, and failure outputs are separate stages");
            }
        }
    }

    public static Result adaptParent(DecisionSnapshot snapshot,
                                     PullFacts pullFacts,
                                     PullAssessment pullAssessment,
                                     int candidateOrdinal) {
        requireInputs(snapshot, pullFacts, pullAssessment);
        if (candidateOrdinal < 0) {
            throw new IllegalArgumentException("candidateOrdinal must be >= 0");
        }
        if (!eligibleNonFailure(pullFacts, pullAssessment)
                || pullFacts.route() != PullRoute.PULL_PARENT) {
            return empty(snapshot, pullFacts);
        }

        ObjectiveFacts.Identity identity = knownIdentity(snapshot);
        if (identity == null) {
            return empty(snapshot, pullFacts);
        }
        for (PullFacts.ParentCandidate candidate : pullFacts.parentCandidates()) {
            if (candidate.ordinal() == candidateOrdinal
                    && isObjectiveSource(identity, candidate.sourceCard())) {
                ObjectiveContribution contribution = new ObjectiveContribution(
                        ObjectiveContribution.Rule.V192_PULL_PARENT,
                        ObjectiveContribution.Channel.PULL_PARENT,
                        candidateOrdinal,
                        PARENT_CONTRIBUTION);
                return new Result(snapshot, pullFacts, List.of(contribution), List.of(), List.of());
            }
        }
        return empty(snapshot, pullFacts);
    }

    /** Preserve the legacy parent score when physical type proof exists but immutable identity cannot match it. */
    public static Result adaptParent(DecisionSnapshot snapshot,
                                     PullFacts pullFacts,
                                     PullAssessment pullAssessment,
                                     int candidateOrdinal,
                                     boolean physicalObjectiveSource) {
        Result result = adaptParent(snapshot, pullFacts, pullAssessment, candidateOrdinal);
        if (!result.parentContributions().isEmpty()
                || !physicalObjectiveSource
                || !eligibleNonFailure(pullFacts, pullAssessment)
                || pullFacts.route() != PullRoute.PULL_PARENT
                || pullFacts.parentCandidates().stream()
                .noneMatch(candidate -> candidate.ordinal() == candidateOrdinal)) {
            return result;
        }
        return new Result(snapshot, pullFacts, List.of(new ObjectiveContribution(
                ObjectiveContribution.Rule.V192_PULL_PARENT,
                ObjectiveContribution.Channel.PULL_PARENT,
                candidateOrdinal,
                PARENT_CONTRIBUTION)), List.of(), List.of());
    }

    public static Result adaptChild(DecisionSnapshot snapshot,
                                    PullFacts pullFacts,
                                    PullAssessment pullAssessment,
                                    ChildCandidate candidate) {
        requireInputs(snapshot, pullFacts, pullAssessment);
        Objects.requireNonNull(candidate, "candidate");
        if (!eligibleNonFailure(pullFacts, pullAssessment)
                || (pullFacts.route() != PullRoute.PULL_DEPLOY_CHILD
                    && pullFacts.route() != PullRoute.PULL_TAKE_CHILD)
                || candidate.kind() != ChildKind.LOCATION
                || !pullFacts.candidateCards().contains(candidate.card())) {
            return empty(snapshot, pullFacts);
        }

        ObjectiveFacts.Identity identity = knownIdentity(snapshot);
        if (identity == null || !knownChildSourceMatches(identity, pullFacts)) {
            return empty(snapshot, pullFacts);
        }
        ChildRank rank = new ChildRank(candidate.card(), CHILD_LOCATION_RANK);
        return new Result(snapshot, pullFacts, List.of(), List.of(rank), List.of());
    }

    /** Resolve an ARBITRARY_CARDS wire ordinal through the typed physical-card order. */
    public static Result adaptChildAtOrdinal(DecisionSnapshot snapshot,
                                             PullFacts pullFacts,
                                             PullAssessment pullAssessment,
                                             int candidateOrdinal,
                                             ChildKind kind) {
        requireInputs(snapshot, pullFacts, pullAssessment);
        Objects.requireNonNull(kind, "kind");
        if (candidateOrdinal < 0 || candidateOrdinal >= pullFacts.candidateCards().size()) {
            return empty(snapshot, pullFacts);
        }
        return adaptChild(snapshot, pullFacts, pullAssessment,
                new ChildCandidate(pullFacts.candidateCards().get(candidateOrdinal), kind));
    }

    /** Preserve the predecessor child rank on its legacy route when immutable identity cannot match physical type. */
    public static Result adaptChildAtOrdinal(DecisionSnapshot snapshot,
                                             PullFacts pullFacts,
                                             PullAssessment pullAssessment,
                                             int candidateOrdinal,
                                             ChildKind kind,
                                             boolean physicalObjectiveSource,
                                             boolean legacyFallbackAllowed) {
        Result result = adaptChildAtOrdinal(
                snapshot, pullFacts, pullAssessment, candidateOrdinal, kind);
        if (!result.childRanks().isEmpty()
                || !legacyFallbackAllowed
                || !physicalObjectiveSource
                || !eligibleNonFailure(pullFacts, pullAssessment)
                || (pullFacts.route() != PullRoute.PULL_DEPLOY_CHILD
                    && pullFacts.route() != PullRoute.PULL_TAKE_CHILD)
                || kind != ChildKind.LOCATION
                || candidateOrdinal < 0
                || candidateOrdinal >= pullFacts.candidateCards().size()) {
            return result;
        }
        ChildRank rank = new ChildRank(
                pullFacts.candidateCards().get(candidateOrdinal), CHILD_LOCATION_RANK);
        return new Result(snapshot, pullFacts, List.of(), List.of(rank), List.of());
    }

    public static Result adaptFailedVerify(DecisionSnapshot snapshot,
                                           PullFacts pullFacts,
                                           PullAssessment pullAssessment) {
        requireInputs(snapshot, pullFacts, pullAssessment);
        if (!canonicalFailedVerify(pullFacts, pullAssessment)) {
            return empty(snapshot, pullFacts);
        }

        ObjectiveFacts.Identity identity = knownIdentity(snapshot);
        if (identity == null || !knownChildSourceMatches(identity, pullFacts)) {
            return empty(snapshot, pullFacts);
        }
        FailedVerifyIntent intent = new FailedVerifyIntent(
                pullFacts.sourceCard().value(), pullFacts.transactionId());
        return new Result(snapshot, pullFacts, List.of(), List.of(), List.of(intent));
    }

    private static void requireInputs(DecisionSnapshot snapshot,
                                      PullFacts pullFacts,
                                      PullAssessment pullAssessment) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(pullFacts, "pullFacts");
        Objects.requireNonNull(pullAssessment, "pullAssessment");
    }

    private static Result empty(DecisionSnapshot snapshot, PullFacts pullFacts) {
        return new Result(snapshot, pullFacts, List.of(), List.of(), List.of());
    }

    private static ObjectiveFacts.Identity knownIdentity(DecisionSnapshot snapshot) {
        return snapshot.objectiveFacts().identity().isKnown()
                ? snapshot.objectiveFacts().identity().value()
                : null;
    }

    private static boolean knownChildSourceMatches(ObjectiveFacts.Identity identity,
                                                   PullFacts pullFacts) {
        return pullFacts.sourceCard().isKnown()
                && isObjectiveSource(identity, pullFacts.sourceCard().value());
    }

    private static boolean eligibleNonFailure(PullFacts pullFacts,
                                              PullAssessment pullAssessment) {
        return pullAssessment.route() == pullFacts.route()
                && pullAssessment.verdict() != PullAssessment.Verdict.BLOCK
                && pullFacts.route() != PullRoute.PULL_FAILED_VERIFY
                && !pullAssessment.evidence().contains(
                        PullAssessment.Evidence.FAILED_VERIFY_EMPTY_SELECTION);
    }

    private static boolean canonicalFailedVerify(PullFacts pullFacts,
                                                 PullAssessment pullAssessment) {
        return pullFacts.route() == PullRoute.PULL_FAILED_VERIFY
                && pullAssessment.route() == pullFacts.route()
                && pullAssessment.verdict() == PullAssessment.Verdict.ALLOW
                && pullAssessment.evidence().contains(
                        PullAssessment.Evidence.FAILED_VERIFY_EMPTY_SELECTION);
    }

    private static boolean isObjectiveSource(ObjectiveFacts.Identity identity,
                                             PullPhysicalCardRef sourceCard) {
        return sourceCard.permanentCardId() == identity.objectivePermanentCardId()
                && sourceCard.currentCardId() == identity.objectiveCurrentCardId();
    }
}
