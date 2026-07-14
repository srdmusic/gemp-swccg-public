package com.gempukku.swccgo.common;

/** Stable physical identity plus the current engine wire id for one deployed card. */
public record DeployPhysicalCardRef(int permanentCardId, int currentCardId) {
    public DeployPhysicalCardRef {
        if (permanentCardId < 0) {
            throw new IllegalArgumentException("permanentCardId must be >= 0");
        }
        if (currentCardId < 0) {
            throw new IllegalArgumentException("currentCardId must be >= 0");
        }
    }
}
