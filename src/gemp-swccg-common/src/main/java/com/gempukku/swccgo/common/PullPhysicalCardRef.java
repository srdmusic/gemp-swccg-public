package com.gempukku.swccgo.common;

/** Stable physical identity plus the current engine wire id for one card. */
public record PullPhysicalCardRef(int permanentCardId, int currentCardId) {
    public PullPhysicalCardRef {
        if (permanentCardId < 0) {
            throw new IllegalArgumentException("permanentCardId must be >= 0");
        }
        if (currentCardId < 0) {
            throw new IllegalArgumentException("currentCardId must be >= 0");
        }
    }
}
