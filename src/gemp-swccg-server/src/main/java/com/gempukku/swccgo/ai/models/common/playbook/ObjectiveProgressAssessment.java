package com.gempukku.swccgo.ai.models.common.playbook;

import java.util.Objects;
import java.util.Set;

/** Factual effect of one candidate on the active objective plan. */
public record ObjectiveProgressAssessment(
        String objectiveBlueprintId,
        boolean flipped,
        Outcome outcome,
        Set<String> satisfiedRequirements,
        Set<String> missingRequirements,
        Set<String> advancedRequirements,
        String evidence) {

    public ObjectiveProgressAssessment {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(satisfiedRequirements, "satisfiedRequirements");
        Objects.requireNonNull(missingRequirements, "missingRequirements");
        Objects.requireNonNull(advancedRequirements, "advancedRequirements");
        Objects.requireNonNull(evidence, "evidence");
        if (objectiveBlueprintId != null && objectiveBlueprintId.isBlank()) {
            throw new IllegalArgumentException("objectiveBlueprintId must be nonblank when present");
        }
        if (evidence.isBlank()) {
            throw new IllegalArgumentException("evidence must be nonblank");
        }

        satisfiedRequirements = copyRequirements(satisfiedRequirements, "satisfiedRequirements");
        missingRequirements = copyRequirements(missingRequirements, "missingRequirements");
        advancedRequirements = copyRequirements(advancedRequirements, "advancedRequirements");

        if (!missingRequirements.containsAll(advancedRequirements)) {
            throw new IllegalArgumentException("advancedRequirements must be missing before the candidate is applied");
        }
        if (!java.util.Collections.disjoint(satisfiedRequirements, missingRequirements)) {
            throw new IllegalArgumentException("a requirement cannot be both satisfied and missing");
        }
        if (outcome == Outcome.NO_OBJECTIVE) {
            if (objectiveBlueprintId != null || flipped
                    || !satisfiedRequirements.isEmpty()
                    || !missingRequirements.isEmpty()
                    || !advancedRequirements.isEmpty()) {
                throw new IllegalArgumentException("NO_OBJECTIVE must not fabricate objective facts");
            }
        } else if (outcome != Outcome.UNPROVEN && objectiveBlueprintId == null) {
            throw new IllegalArgumentException(outcome + " requires an objective blueprint id");
        }
        if ((outcome == Outcome.COMPLETES_FLIP_NOW
                || outcome == Outcome.ADVANCES_MISSING_REQUIREMENT)
                && advancedRequirements.isEmpty()) {
            throw new IllegalArgumentException(outcome + " requires at least one advanced requirement");
        }
        if ((outcome == Outcome.COMPLETES_FLIP_NOW
                || outcome == Outcome.ADVANCES_MISSING_REQUIREMENT) && flipped) {
            throw new IllegalArgumentException(outcome + " requires the objective front side");
        }
        if (outcome == Outcome.PROTECTS_FLIP_BACK && !flipped) {
            throw new IllegalArgumentException("flip-back protection requires an already-flipped objective");
        }
    }

    public static ObjectiveProgressAssessment noObjective() {
        return new ObjectiveProgressAssessment(null, false, Outcome.NO_OBJECTIVE,
                Set.of(), Set.of(), Set.of(), "No active objective");
    }

    public static ObjectiveProgressAssessment unproven(String objectiveBlueprintId, boolean flipped,
                                                        String evidence) {
        return new ObjectiveProgressAssessment(objectiveBlueprintId, flipped, Outcome.UNPROVEN,
                Set.of(), Set.of(), Set.of(), evidence);
    }

    public enum Outcome {
        NO_OBJECTIVE,
        COMPLETES_FLIP_NOW,
        ADVANCES_MISSING_REQUIREMENT,
        PROTECTS_FLIP_BACK,
        NEUTRAL,
        CONFLICTS_WITH_PLAN,
        UNPROVEN
    }

    private static Set<String> copyRequirements(Set<String> requirements, String name) {
        for (String requirement : requirements) {
            if (requirement == null || requirement.isBlank()) {
                throw new IllegalArgumentException(name + " must contain nonblank requirement ids");
            }
        }
        return Set.copyOf(requirements);
    }
}
