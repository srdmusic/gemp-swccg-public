package com.gempukku.swccgo.ai.models.common.policy;

import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.common.Phase;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Short-lived AI-only continuity between one parent choice and its next stock
 * child decision. It stores exact identities only and is never attached to a
 * GEMP decision, action, card, or response.
 */
public record PendingAiIntent(
        ExpiryKey expiryKey,
        String parentDecisionId,
        String parentActionId,
        Integer sourcePhysicalCardId,
        TraceDomainId owningDomain,
        ChildShape expectedChildShape,
        Set<String> expectedActionIds,
        Set<Integer> expectedPhysicalCardIds) {

    public PendingAiIntent {
        Objects.requireNonNull(expiryKey, "expiryKey");
        parentDecisionId = requireNonBlank(parentDecisionId, "parentDecisionId");
        parentActionId = requireNonBlank(parentActionId, "parentActionId");
        Objects.requireNonNull(owningDomain, "owningDomain");
        Objects.requireNonNull(expectedChildShape, "expectedChildShape");
        Objects.requireNonNull(expectedActionIds, "expectedActionIds");
        Objects.requireNonNull(expectedPhysicalCardIds, "expectedPhysicalCardIds");
        if (sourcePhysicalCardId != null && sourcePhysicalCardId <= 0) {
            throw new IllegalArgumentException("sourcePhysicalCardId must be positive when present");
        }
        if (owningDomain == TraceDomainId.LEGACY_UNTAGGED
                || owningDomain == TraceDomainId.COMBINED_EVALUATOR) {
            throw new IllegalArgumentException("pending intent requires a real owning domain");
        }
        for (String actionId : expectedActionIds) {
            requireNonBlank(actionId, "expected action id");
        }
        for (Integer cardId : expectedPhysicalCardIds) {
            if (cardId == null || cardId <= 0) {
                throw new IllegalArgumentException("expected physical card ids must be positive");
            }
        }
        expectedActionIds = Set.copyOf(expectedActionIds);
        expectedPhysicalCardIds = Set.copyOf(expectedPhysicalCardIds);
        if (expectedActionIds.isEmpty() && expectedPhysicalCardIds.isEmpty()) {
            throw new IllegalArgumentException("pending intent requires an exact action or physical-card constraint");
        }
    }

    public record ExpiryKey(String gameId, int turn, Phase phase) {
        public ExpiryKey {
            gameId = requireNonBlank(gameId, "gameId");
            if (turn < 0) {
                throw new IllegalArgumentException("turn must be >= 0, was " + turn);
            }
            Objects.requireNonNull(phase, "phase");
        }
    }

    public enum ChildShape {
        ACTION,
        CARD,
        DESTINATION,
        INTEGER,
        YES_NO
    }

    public record ChildCandidate(String actionId, Integer physicalCardId) {
        public ChildCandidate {
            if (actionId != null && actionId.isBlank()) {
                throw new IllegalArgumentException("actionId must be nonblank when present");
            }
            if (physicalCardId != null && physicalCardId <= 0) {
                throw new IllegalArgumentException("physicalCardId must be positive when present");
            }
            if (actionId == null && physicalCardId == null) {
                throw new IllegalArgumentException("child candidate requires an action or physical-card id");
            }
        }
    }

    public record ChildDecision(ExpiryKey expiryKey, ChildShape shape,
                                List<ChildCandidate> candidates) {
        public ChildDecision {
            Objects.requireNonNull(expiryKey, "expiryKey");
            Objects.requireNonNull(shape, "shape");
            Objects.requireNonNull(candidates, "candidates");
            candidates = List.copyOf(candidates);
        }
    }

    boolean matches(ChildCandidate candidate) {
        if (!expectedActionIds.isEmpty()
                && (candidate.actionId() == null || !expectedActionIds.contains(candidate.actionId()))) {
            return false;
        }
        return expectedPhysicalCardIds.isEmpty()
                || (candidate.physicalCardId() != null
                && expectedPhysicalCardIds.contains(candidate.physicalCardId()));
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must be nonblank");
        }
        return value;
    }
}
