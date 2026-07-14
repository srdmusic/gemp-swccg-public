package com.gempukku.swccgo.common;

import java.util.List;
import java.util.Objects;

/** Immutable physical identity and legality snapshot carried by one deploy action. */
public record DeployActionMetadata(
        DeployPhysicalCardRef sourceCard,
        Zone sourceZone,
        boolean destinationLegalityKnown,
        boolean forcedDestination,
        List<DeployDestinationRef> orderedLegalDestinations,
        List<DeployPhysicalCardRef> orderedLegalBuddies,
        DeployPhysicalCardRef selectedBuddy) {

    public DeployActionMetadata {
        Objects.requireNonNull(sourceCard, "sourceCard");
        orderedLegalDestinations = List.copyOf(orderedLegalDestinations);
        orderedLegalBuddies = List.copyOf(orderedLegalBuddies);
        if (!destinationLegalityKnown && !orderedLegalDestinations.isEmpty()) {
            throw new IllegalArgumentException(
                    "unknown destination legality cannot carry invented destinations");
        }
        if (forcedDestination
                && (!destinationLegalityKnown || orderedLegalDestinations.size() != 1)) {
            throw new IllegalArgumentException(
                    "forced destination requires exactly one known destination");
        }
        if (selectedBuddy != null && !orderedLegalBuddies.isEmpty()
                && !orderedLegalBuddies.contains(selectedBuddy)) {
            throw new IllegalArgumentException(
                    "selected buddy must be one of the ordered legal buddies");
        }
    }

    public static DeployActionMetadata unknownDestinations(DeployPhysicalCardRef sourceCard,
                                                            Zone sourceZone) {
        return new DeployActionMetadata(
                sourceCard, sourceZone, false, false, List.of(), List.of(), null);
    }

    public DeployActionMetadata withDestinations(List<DeployDestinationRef> destinations) {
        return new DeployActionMetadata(sourceCard, sourceZone, true, false, destinations,
                orderedLegalBuddies, selectedBuddy);
    }

    public DeployActionMetadata withForcedDestination(boolean forced) {
        return new DeployActionMetadata(sourceCard, sourceZone,
                destinationLegalityKnown, forced, orderedLegalDestinations,
                orderedLegalBuddies, selectedBuddy);
    }

    public DeployActionMetadata withBuddyCandidates(List<DeployPhysicalCardRef> buddies) {
        return new DeployActionMetadata(sourceCard, sourceZone,
                destinationLegalityKnown, forcedDestination,
                orderedLegalDestinations, buddies, null);
    }

    public DeployActionMetadata withSelectedBuddy(DeployPhysicalCardRef buddy) {
        return new DeployActionMetadata(sourceCard, sourceZone,
                destinationLegalityKnown, forcedDestination, orderedLegalDestinations,
                orderedLegalBuddies, Objects.requireNonNull(buddy, "buddy"));
    }
}
