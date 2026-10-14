package com.gempukku.swccgo.ai.models.common.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable ordered operations emitted by one policy producer. */
public record PolicyResult(String producerId, List<PolicyOperation> operations) {

    public PolicyResult {
        Objects.requireNonNull(producerId, "producerId");
        Objects.requireNonNull(operations, "operations");
        if (producerId.isBlank()) {
            throw new IllegalArgumentException("producerId must be nonblank");
        }
        Map<String, ObjectivePreferencePolicy.State> objectiveByAction =
                new HashMap<>();
        List<PolicyOperation> bounded = new ArrayList<>(operations.size());
        for (PolicyOperation operation : operations) {
            if (operation.kind() == PolicyOperationKind.ADD
                    && ObjectivePreferencePolicy.isObjective(
                    operation.domainId())) {
                ObjectivePreferencePolicy.State state =
                        objectiveByAction.getOrDefault(
                                operation.actionId(),
                                ObjectivePreferencePolicy.State.empty());
                ObjectivePreferencePolicy.Application accepted =
                        ObjectivePreferencePolicy.apply(
                                state, operation.objectiveStrength(),
                                operation.delta());
                objectiveByAction.put(
                        operation.actionId(), accepted.state());
                bounded.add(operation.withAppliedDelta(accepted.delta()));
            } else {
                bounded.add(operation);
            }
        }
        operations = List.copyOf(bounded);
    }

}
