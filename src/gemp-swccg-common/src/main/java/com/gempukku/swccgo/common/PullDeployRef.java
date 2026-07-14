package com.gempukku.swccgo.common;

import java.util.List;
import java.util.Objects;

/** Immutable engine handoff from a deploy-from-pile child to deploy targeting. */
public record PullDeployRef(
        long transactionId,
        Integer parentDecisionId,
        Integer parentActionOrdinal,
        String playerId,
        PullPhysicalCardRef sourceCard,
        GameTextActionId gameTextActionId,
        Zone sourceZone,
        String sourceZoneOwner,
        PullPhysicalCardRef selectedCard,
        List<PullPhysicalCardRef> orderedDestinationCards,
        PullPhysicalCardRef forcedDestinationCard) {

    public PullDeployRef {
        if (transactionId <= 0) {
            throw new IllegalArgumentException("transactionId must be > 0");
        }
        if ((parentDecisionId == null) != (parentActionOrdinal == null)) {
            throw new IllegalArgumentException("parent decision id and ordinal must be known together");
        }
        if (parentDecisionId != null && parentDecisionId < 0) {
            throw new IllegalArgumentException("parentDecisionId must be >= 0");
        }
        if (parentActionOrdinal != null && parentActionOrdinal < 0) {
            throw new IllegalArgumentException("parentActionOrdinal must be >= 0");
        }
        Objects.requireNonNull(playerId, "playerId");
        if (playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must be nonblank");
        }
        Objects.requireNonNull(sourceZone, "sourceZone");
        Objects.requireNonNull(sourceZoneOwner, "sourceZoneOwner");
        if (sourceZoneOwner.isBlank()) {
            throw new IllegalArgumentException("sourceZoneOwner must be nonblank");
        }
        Objects.requireNonNull(selectedCard, "selectedCard");
        orderedDestinationCards = List.copyOf(orderedDestinationCards);
        if (forcedDestinationCard != null
                && (orderedDestinationCards.size() != 1
                    || !orderedDestinationCards.get(0).equals(forcedDestinationCard))) {
            throw new IllegalArgumentException(
                    "forced destination requires one matching ordered destination");
        }
    }

    public PullDeployRef withDestinations(List<PullPhysicalCardRef> destinationCards,
                                          boolean autoSelected) {
        List<PullPhysicalCardRef> ordered = List.copyOf(destinationCards);
        if (autoSelected && ordered.size() != 1) {
            throw new IllegalArgumentException(
                    "an auto-selected deploy destination requires exactly one card");
        }
        PullPhysicalCardRef forced = autoSelected ? ordered.get(0) : null;
        return new PullDeployRef(transactionId, parentDecisionId, parentActionOrdinal, playerId,
                sourceCard, gameTextActionId, sourceZone, sourceZoneOwner,
                selectedCard, ordered, forced);
    }
}
