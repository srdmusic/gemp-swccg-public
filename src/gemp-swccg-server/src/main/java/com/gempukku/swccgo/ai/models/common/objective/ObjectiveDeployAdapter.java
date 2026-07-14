package com.gempukku.swccgo.ai.models.common.objective;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure objective contribution adapter for one DEPLOY candidate. */
public final class ObjectiveDeployAdapter {

    private static final float V83 = -2000.0f;
    private static final float V88 = 1500.0f;
    private static final float V108 = 500.0f;
    private static final float V110 = -2000.0f;
    private static final float OBJECTIVE_SITE = 200.0f;
    private static final float V193_PARENT = 400.0f;
    private static final float V193_CHILD = 2000.0f;

    private ObjectiveDeployAdapter() {
    }

    public enum Stage {
        PARENT_ACTION,
        CHILD_DESTINATION
    }

    /** Candidate-local typed facts supplied by the DEPLOY owner. */
    public record CandidateFacts(
            int candidateOrdinal,
            Stage stage,
            int deployingCardId,
            boolean character,
            boolean filtersSenator,
            boolean keywordOrLoreSenator,
            Integer destinationLocationId,
            boolean targetsFlipCriticalControlSite,
            boolean flipCriticalControlCardAvailable,
            boolean objectiveSiteScoringEligible,
            boolean objectiveRelevantLocation,
            boolean v193ParentLegacyBranchEmits,
            boolean v193ChildLegacyBranchEmits,
            Float ability,
            Float deployCost) {

        public CandidateFacts {
            if (candidateOrdinal < 0) {
                throw new IllegalArgumentException("candidateOrdinal must be >= 0");
            }
            Objects.requireNonNull(stage, "stage");
            if (deployingCardId < 0) {
                throw new IllegalArgumentException("deployingCardId must be >= 0");
            }
            if (destinationLocationId != null && destinationLocationId < 0) {
                throw new IllegalArgumentException("destinationLocationId must be >= 0");
            }
            requireNonNegativeFinite(ability, "ability");
            requireNonNegativeFinite(deployCost, "deployCost");
        }
    }

    public record Result(
            DecisionSnapshot snapshot,
            List<ObjectiveContribution> contributions) {

        public Result {
            Objects.requireNonNull(snapshot, "snapshot");
            contributions = List.copyOf(contributions);
        }
    }

    public static Result adapt(DecisionSnapshot snapshot, CandidateFacts candidate) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(candidate, "candidate");

        ObjectiveFacts facts = snapshot.objectiveFacts();
        if (facts.strategy().isUnknown() || facts.board().isUnknown()) {
            return new Result(snapshot, List.of());
        }

        ObjectiveFacts.StrategyFacts strategy = facts.strategy().value();
        ObjectiveFacts.TypedBoardFacts board = facts.board().value();
        boolean myLord = strategy.kind().myLord();
        boolean destinationSenator = candidate.stage() == Stage.PARENT_ACTION
                ? candidate.filtersSenator()
                : candidate.keywordOrLoreSenator();
        boolean senateDestination = candidate.destinationLocationId() != null
                && board.galacticSenateLocationIds().contains(candidate.destinationLocationId());
        ObjectiveContribution.Channel channel = candidate.stage() == Stage.PARENT_ACTION
                ? ObjectiveContribution.Channel.DEPLOY_PARENT
                : ObjectiveContribution.Channel.DEPLOY_CHILD;

        List<ObjectiveContribution> contributions = new ArrayList<>();
        if (myLord && destinationSenator
                && candidate.destinationLocationId() != null && !senateDestination) {
            add(contributions, ObjectiveContribution.Rule.MY_LORD_V83,
                    channel, candidate.candidateOrdinal(), V83);
        }
        if (candidate.stage() == Stage.PARENT_ACTION && myLord && candidate.character()
                && !candidate.keywordOrLoreSenator() && !board.nonSenateSiteOnTable()) {
            add(contributions, ObjectiveContribution.Rule.MY_LORD_V110,
                    channel, candidate.candidateOrdinal(), V110);
        }
        if (candidate.stage() == Stage.PARENT_ACTION && myLord && candidate.character()
                && candidate.keywordOrLoreSenator()) {
            add(contributions, ObjectiveContribution.Rule.MY_LORD_V108,
                    channel, candidate.candidateOrdinal(), V108);
        }
        if (myLord && destinationSenator && senateDestination) {
            add(contributions, ObjectiveContribution.Rule.MY_LORD_V88,
                    channel, candidate.candidateOrdinal(), V88);
        }
        if (candidate.character() && candidate.objectiveSiteScoringEligible()
                && candidate.objectiveRelevantLocation()) {
            add(contributions, ObjectiveContribution.Rule.OBJECTIVE_SITE,
                    channel, candidate.candidateOrdinal(), OBJECTIVE_SITE);
        }

        if (candidate.stage() == Stage.PARENT_ACTION
                && candidate.v193ParentLegacyBranchEmits()) {
            add(contributions, ObjectiveContribution.Rule.V193_PARENT,
                    channel, candidate.candidateOrdinal(), V193_PARENT);
        } else if (candidate.stage() == Stage.CHILD_DESTINATION
                && candidate.v193ChildLegacyBranchEmits()) {
            add(contributions, ObjectiveContribution.Rule.V193_CHILD,
                    channel, candidate.candidateOrdinal(), V193_CHILD);
        }

        return new Result(snapshot, contributions);
    }

    private static void add(List<ObjectiveContribution> contributions,
                            ObjectiveContribution.Rule rule,
                            ObjectiveContribution.Channel channel,
                            int candidateOrdinal,
                            float value) {
        contributions.add(new ObjectiveContribution(rule, channel, candidateOrdinal, value));
    }

    private static void requireNonNegativeFinite(Float value, String name) {
        if (value != null && (!Float.isFinite(value) || value < 0.0f)) {
            throw new IllegalArgumentException(name + " must be finite and >= 0 when present");
        }
    }
}
