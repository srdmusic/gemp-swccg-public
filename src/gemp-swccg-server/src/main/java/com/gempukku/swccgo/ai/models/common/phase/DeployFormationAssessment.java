package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.DeployDestinationRef;
import com.gempukku.swccgo.common.DeployPhysicalCardRef;

import java.util.List;
import java.util.Objects;

/** One immutable formation verdict reused through a physical DEPLOY attempt. */
public record DeployFormationAssessment(
        Verdict verdict,
        DeployPhysicalCardRef sourceCard,
        List<DeployDestinationRef> orderedDestinations,
        List<DeployDestinationRef> allowedDestinations,
        List<DeployDestinationRef> weakSoloNoPlanDestinations,
        DeployPhysicalCardRef exactFirstCard,
        DeployPhysicalCardRef exactBuddyCard,
        String reason) {

    public enum Verdict {
        SAFE_SOLO,
        SAFE_SEQUENCE,
        ALL_DESTINATIONS_BLOCKED,
        TARGETED_RESCUE,
        OVERPOWER_OPPORTUNITY,
        UNKNOWN
    }

    public DeployFormationAssessment {
        Objects.requireNonNull(verdict, "verdict");
        Objects.requireNonNull(sourceCard, "sourceCard");
        orderedDestinations = List.copyOf(orderedDestinations);
        allowedDestinations = List.copyOf(allowedDestinations);
        weakSoloNoPlanDestinations = List.copyOf(weakSoloNoPlanDestinations);
        if (!orderedDestinations.containsAll(allowedDestinations)) {
            throw new IllegalArgumentException(
                    "allowed destinations must be offered destinations");
        }
        if (!allowedDestinations.containsAll(weakSoloNoPlanDestinations)) {
            throw new IllegalArgumentException(
                    "weak-solo destinations must be allowed destinations");
        }
        if ((verdict == Verdict.ALL_DESTINATIONS_BLOCKED || verdict == Verdict.UNKNOWN)
                && (!allowedDestinations.isEmpty()
                    || !weakSoloNoPlanDestinations.isEmpty())) {
            throw new IllegalArgumentException(
                    verdict + " cannot expose an allowed destination");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("formation reason must be nonblank");
        }
        if (verdict == Verdict.SAFE_SEQUENCE
                && (exactFirstCard == null || exactBuddyCard == null)) {
            throw new IllegalArgumentException("SAFE_SEQUENCE requires exact first and buddy cards");
        }
    }

    /** Compatibility constructor for assessments where every offered destination is allowed. */
    public DeployFormationAssessment(
            Verdict verdict,
            DeployPhysicalCardRef sourceCard,
            List<DeployDestinationRef> orderedDestinations,
            DeployPhysicalCardRef exactFirstCard,
            DeployPhysicalCardRef exactBuddyCard,
            String reason) {
        this(verdict, sourceCard, orderedDestinations,
                verdict == Verdict.ALL_DESTINATIONS_BLOCKED || verdict == Verdict.UNKNOWN
                        ? List.of() : orderedDestinations,
                List.of(), exactFirstCard, exactBuddyCard, reason);
    }

    public boolean allows(DeployDestinationRef destination) {
        return allowedDestinations.contains(destination);
    }

    public boolean isWeakSoloNoPlan(DeployDestinationRef destination) {
        return weakSoloNoPlanDestinations.contains(destination);
    }

    public static DeployFormationAssessment unknown(DeployPhysicalCardRef sourceCard,
                                                     List<DeployDestinationRef> destinations,
                                                     String reason) {
        return new DeployFormationAssessment(
                Verdict.UNKNOWN, sourceCard, destinations, null, null, reason);
    }
}
