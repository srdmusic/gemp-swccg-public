package com.gempukku.swccgo.ai.models.common.objective;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Pure adapter for typed objective starting-process references. */
public final class ObjectiveSetupAdapter {

    private ObjectiveSetupAdapter() {
    }

    public enum Kind {
        LOCATION,
        EFFECT,
        INTERRUPT
    }

    public record StartingIntent(
            Kind kind,
            Set<String> blueprintIds,
            Set<String> titleCompatibilityFragments) {

        public StartingIntent {
            Objects.requireNonNull(kind, "kind");
            blueprintIds = Set.copyOf(blueprintIds);
            titleCompatibilityFragments = Set.copyOf(titleCompatibilityFragments);
            if (blueprintIds.isEmpty() && titleCompatibilityFragments.isEmpty()) {
                throw new IllegalArgumentException("starting intent requires at least one typed reference");
            }
        }
    }

    public record Result(DecisionSnapshot snapshot, List<StartingIntent> intents) {
        public Result {
            Objects.requireNonNull(snapshot, "snapshot");
            intents = List.copyOf(intents);
        }
    }

    public static Result adapt(DecisionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        ObjectiveFacts facts = snapshot.objectiveFacts();
        if (facts.strategy().isUnknown()) {
            return new Result(snapshot, List.of());
        }

        ObjectiveFacts.StartingRefs refs = facts.strategy().value().startingRefs();
        List<StartingIntent> intents = new ArrayList<>();
        addIfPresent(intents, Kind.LOCATION,
                refs.locationBlueprintIds(), refs.locationTitleFragments());
        addIfPresent(intents, Kind.EFFECT,
                refs.effectBlueprintIds(), refs.effectTitleFragments());
        addIfPresent(intents, Kind.INTERRUPT,
                refs.interruptBlueprintIds(), refs.interruptTitleFragments());
        return new Result(snapshot, intents);
    }

    private static void addIfPresent(List<StartingIntent> intents,
                                     Kind kind,
                                     Set<String> blueprintIds,
                                     Set<String> titleFragments) {
        if (!blueprintIds.isEmpty() || !titleFragments.isEmpty()) {
            intents.add(new StartingIntent(kind, blueprintIds, titleFragments));
        }
    }
}
