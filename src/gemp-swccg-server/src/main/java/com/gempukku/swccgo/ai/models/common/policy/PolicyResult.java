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
        Map<String, Float> objectiveByAction = new HashMap<>();
        List<PolicyOperation> bounded = new ArrayList<>(operations.size());
        for (PolicyOperation operation : operations) {
            if (operation.kind() == PolicyOperationKind.ADD
                    && ObjectivePreferencePolicy.isObjective(
                    operation.domainId())) {
                float applied = objectiveByAction.getOrDefault(
                        operation.actionId(), 0.0f);
                float accepted = ObjectivePreferencePolicy.applyWithinCeiling(
                        applied, operation.delta());
                objectiveByAction.put(operation.actionId(), applied + accepted);
                bounded.add(new PolicyOperation(
                        operation.actionId(), operation.ruleArmId(),
                        operation.domainId(), operation.outputKind(),
                        operation.kind(), accepted, operation.reason()));
            } else {
                bounded.add(operation);
            }
        }
        operations = List.copyOf(bounded);
    }

}
