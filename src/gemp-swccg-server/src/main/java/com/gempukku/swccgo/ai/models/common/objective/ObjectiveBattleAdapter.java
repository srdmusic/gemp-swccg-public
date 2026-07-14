package com.gempukku.swccgo.ai.models.common.objective;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;

/** Pure objective intent adapter for BATTLE. */
public final class ObjectiveBattleAdapter {

    private ObjectiveBattleAdapter() {
    }

    public sealed interface Intent permits HuntIntent {
    }

    public record HuntIntent(
            Set<Integer> inquisitorCardIds,
            Set<Integer> inquisitorWithHatredCardIds) implements Intent {

        public HuntIntent {
            inquisitorCardIds = Set.copyOf(inquisitorCardIds);
            inquisitorWithHatredCardIds = Set.copyOf(inquisitorWithHatredCardIds);
            if (!inquisitorCardIds.containsAll(inquisitorWithHatredCardIds)) {
                throw new IllegalArgumentException("stacked Hatred facts must belong to typed Inquisitors");
            }
        }
    }

    public record Result(DecisionSnapshot snapshot, List<Intent> intents) {
        public Result {
            Objects.requireNonNull(snapshot, "snapshot");
            intents = List.copyOf(intents);
        }
    }

    /** One objective-owned view of the three migrated Hunt Down operations. */
    public record InitiationAssessment(
            float adjustedBarrierRisk,
            boolean vaderExpendabilityApplied,
            List<ObjectiveContribution> contributions) {

        public InitiationAssessment {
            if (!Float.isFinite(adjustedBarrierRisk)) {
                throw new IllegalArgumentException("adjustedBarrierRisk must be finite");
            }
            contributions = List.copyOf(contributions);
        }

        public float contribution(ObjectiveContribution.Rule rule) {
            float value = 0f;
            for (ObjectiveContribution contribution : contributions) {
                if (contribution.rule() == rule) {
                    value += contribution.value();
                }
            }
            return value;
        }
    }

    public static Result adapt(DecisionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        ObjectiveFacts facts = snapshot.objectiveFacts();
        if (facts.strategy().isUnknown()) {
            return new Result(snapshot, List.of());
        }

        ObjectiveFacts.StrategyFacts strategy = facts.strategy().value();
        if (!strategy.kind().huntDownVirtual()) {
            return new Result(snapshot, List.of());
        }

        Set<Integer> inquisitors = Set.of();
        Set<Integer> hatred = Set.of();
        if (facts.board().isKnown()) {
            ObjectiveFacts.TypedBoardFacts board = facts.board().value();
            inquisitors = board.inquisitorCardIds();
            hatred = board.inquisitorWithHatredCardIds();
        }

        return new Result(snapshot, List.of(
                new HuntIntent(inquisitors, hatred)));
    }

    /**
     * Emit each migrated Hunt Down initiation contribution at most once for one
     * candidate. Generic barrier risk remains the caller's operation; this adapter
     * owns only its V35 factor and the two positive objective bonuses.
     */
    public static InitiationAssessment assessInitiation(
            DecisionSnapshot snapshot,
            int candidateOrdinal,
            Set<Integer> friendlyCardIdsAtTarget,
            boolean vaderAtTarget,
            boolean armedVader,
            boolean lukeAtTarget,
            boolean jediAtTarget,
            float genericBarrierRisk,
            float vaderExpendabilityFactor) {
        Objects.requireNonNull(snapshot, "snapshot");
        friendlyCardIdsAtTarget = Set.copyOf(friendlyCardIdsAtTarget);
        Result result = adapt(snapshot);
        HuntIntent hunt = null;
        for (Intent intent : result.intents()) {
            if (intent instanceof HuntIntent candidate) {
                hunt = candidate;
                break;
            }
        }
        if (hunt == null) {
            return new InitiationAssessment(genericBarrierRisk, false, List.of());
        }

        ArrayList<ObjectiveContribution> contributions = new ArrayList<>();
        float adjustedBarrier = genericBarrierRisk;
        boolean expendabilityApplied = vaderAtTarget && genericBarrierRisk != 0f;
        if (expendabilityApplied) {
            adjustedBarrier = genericBarrierRisk * vaderExpendabilityFactor;
            contributions.add(new ObjectiveContribution(
                    ObjectiveContribution.Rule.V35_VADER_EXPENDABLE,
                    ObjectiveContribution.Channel.BATTLE_INITIATE,
                    candidateOrdinal,
                    adjustedBarrier - genericBarrierRisk));
        }

        if (armedVader) {
            contributions.add(new ObjectiveContribution(
                    ObjectiveContribution.Rule.V29_9_HUNT_DOWN,
                    ObjectiveContribution.Channel.BATTLE_INITIATE,
                    candidateOrdinal,
                    lukeAtTarget ? 200.0f : 80.0f));
        }

        boolean inquisitorAtTarget = intersects(
                friendlyCardIdsAtTarget, hunt.inquisitorCardIds());
        if (inquisitorAtTarget) {
            boolean hatredAtTarget = intersects(
                    friendlyCardIdsAtTarget,
                    hunt.inquisitorWithHatredCardIds());
            float destinyBonus = hatredAtTarget ? 250.0f : 120.0f;
            if (jediAtTarget) {
                destinyBonus += 100.0f;
            }
            contributions.add(new ObjectiveContribution(
                    ObjectiveContribution.Rule.V35_HUNT_DESTINY,
                    ObjectiveContribution.Channel.BATTLE_INITIATE,
                    candidateOrdinal,
                    destinyBonus));
        }
        return new InitiationAssessment(adjustedBarrier,
                expendabilityApplied, contributions);
    }

    private static boolean intersects(Set<Integer> left, Set<Integer> right) {
        for (Integer value : left) {
            if (right.contains(value)) {
                return true;
            }
        }
        return false;
    }
}
