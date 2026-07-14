package com.gempukku.swccgo.ai.models.common.objective;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Pure objective intent adapter for MOVE. */
public final class ObjectiveMoveAdapter {

    private static final int HIDDEN_PATH_REQUIRED_SITES = 2;
    private static final int UNDERGROUND_CORRIDOR_MOVE_COST = 1;

    private ObjectiveMoveAdapter() {
    }

    public sealed interface Intent permits HiddenPathIntent, UndergroundCorridorIntent {
    }

    public record HiddenPathIntent(
            boolean objectiveFlipped,
            int nonMapuzoJediSiteCount,
            int requiredSiteCount,
            boolean flipConditionMet) implements Intent {

        public HiddenPathIntent {
            if (nonMapuzoJediSiteCount < 0) {
                throw new IllegalArgumentException("nonMapuzoJediSiteCount must be >= 0");
            }
            if (requiredSiteCount != HIDDEN_PATH_REQUIRED_SITES) {
                throw new IllegalArgumentException("Hidden Path requires exactly two non-Mapuzo Jedi sites");
            }
            if (flipConditionMet != (nonMapuzoJediSiteCount >= requiredSiteCount)) {
                throw new IllegalArgumentException("flipConditionMet must match the typed site count");
            }
        }
    }

    public record UndergroundCorridorIntent(
            Set<Integer> jediSurvivorCardIds,
            int moveCost) implements Intent {

        public UndergroundCorridorIntent {
            jediSurvivorCardIds = Set.copyOf(jediSurvivorCardIds);
            if (jediSurvivorCardIds.isEmpty()) {
                throw new IllegalArgumentException("Underground Corridor intent requires a Jedi Survivor");
            }
            if (moveCost != UNDERGROUND_CORRIDOR_MOVE_COST) {
                throw new IllegalArgumentException("Underground Corridor uses the default move cost of 1");
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
        if (facts.identity().isUnknown() || facts.strategy().isUnknown() || facts.board().isUnknown()) {
            return new Result(snapshot, List.of());
        }

        ObjectiveFacts.StrategyFacts strategy = facts.strategy().value();
        if (!strategy.kind().hiddenPath()) {
            return new Result(snapshot, List.of());
        }

        ObjectiveFacts.TypedBoardFacts board = facts.board().value();
        List<Intent> intents = new ArrayList<>();
        intents.add(new HiddenPathIntent(
                facts.identity().value().flipped(),
                board.hiddenPathNonMapuzoJediSiteCount(),
                HIDDEN_PATH_REQUIRED_SITES,
                board.hiddenPathFlipConditionMet()));
        if (!board.jediSurvivorCardIds().isEmpty()) {
            intents.add(new UndergroundCorridorIntent(
                    board.jediSurvivorCardIds(), UNDERGROUND_CORRIDOR_MOVE_COST));
        }
        return new Result(snapshot, intents);
    }
}
