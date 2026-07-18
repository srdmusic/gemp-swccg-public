package com.gempukku.swccgo.ai.models.common.policy;

import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Per-decision guard that prevents two policy producers from contributing the
 * same rule arm to the same action.
 */
public final class PolicyContributionLedger {
    private final String decisionId;
    private final Map<ContributionKey, String> producerByContribution = new LinkedHashMap<>();
    private final List<PolicyOperation> orderedOperations = new ArrayList<>();

    public PolicyContributionLedger(String decisionId) {
        this.decisionId = requireNonBlank(decisionId, "decisionId");
    }

    public void register(PolicyResult result) {
        Objects.requireNonNull(result, "result");

        Map<ContributionKey, String> prospectiveOwners =
                new LinkedHashMap<>(producerByContribution);
        for (PolicyOperation operation : result.operations()) {
            ContributionKey key = new ContributionKey(operation.actionId(), operation.ruleArmId());
            String existingProducer = prospectiveOwners.putIfAbsent(key, result.producerId());
            if (existingProducer != null) {
                throw new IllegalStateException("decision " + decisionId + " action "
                        + operation.actionId() + " rule " + operation.ruleArmId().id()
                        + " already contributes from " + existingProducer
                        + "; repeated contribution from " + result.producerId());
            }
        }

        for (PolicyOperation operation : result.operations()) {
            ContributionKey key = new ContributionKey(operation.actionId(), operation.ruleArmId());
            producerByContribution.put(key, result.producerId());
            orderedOperations.add(operation);
        }
    }

    public List<PolicyOperation> orderedOperations() {
        return List.copyOf(orderedOperations);
    }

    public List<PolicyOperation> operationsFor(String actionId) {
        Objects.requireNonNull(actionId, "actionId");
        return orderedOperations.stream()
                .filter(operation -> operation.actionId().equals(actionId))
                .toList();
    }

    public String producerFor(String actionId, TraceRuleId ruleArmId) {
        return producerByContribution.get(new ContributionKey(actionId, ruleArmId));
    }

    public record ContributionKey(String actionId, TraceRuleId ruleArmId) {
        public ContributionKey {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(ruleArmId, "ruleArmId");
        }
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must be nonblank");
        }
        return value;
    }
}
