package com.gempukku.swccgo.ai.models.common.objective;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;

import java.util.List;
import java.util.Objects;
import java.util.Set;

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
            if (inquisitorCardIds.isEmpty()) {
                throw new IllegalArgumentException("Hunt intent requires a typed Inquisitor");
            }
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

    public static Result adapt(DecisionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        ObjectiveFacts facts = snapshot.objectiveFacts();
        if (facts.strategy().isUnknown() || facts.board().isUnknown()) {
            return new Result(snapshot, List.of());
        }

        ObjectiveFacts.StrategyFacts strategy = facts.strategy().value();
        ObjectiveFacts.TypedBoardFacts board = facts.board().value();
        if (!strategy.kind().huntDownVirtual() || board.inquisitorCardIds().isEmpty()) {
            return new Result(snapshot, List.of());
        }

        return new Result(snapshot, List.of(
                new HuntIntent(board.inquisitorCardIds(), board.inquisitorWithHatredCardIds())));
    }
}
