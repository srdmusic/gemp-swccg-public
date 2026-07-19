package com.gempukku.swccgo.ai.models.common.phase;

/** Immutable board facts consumed by the DEPLOY-1 sequencing policy. */
public final class DeploySequencingFacts {
    private DeploySequencingFacts() {
    }

    public record PowerGap(String locationTitle, float ourPower, float theirPower) {
    }
}
