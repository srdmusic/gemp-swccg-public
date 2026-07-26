package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Objects;

/**
 * Pure terminal choice for each child of the TIGIH virtual-Hut move.
 */
public final class CaptureVirtualHutChoicePolicy {
    public enum Choice {
        PREFER,
        HARD_VETO,
        NEUTRAL
    }

    public record Facts(
            boolean candidateAdmissible,
            boolean admissibleSelectableRouteExists) {
    }

    private CaptureVirtualHutChoicePolicy() {
    }

    public static Choice choose(Facts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.candidateAdmissible()) {
            return Choice.PREFER;
        }
        if (facts.admissibleSelectableRouteExists()) {
            return Choice.HARD_VETO;
        }
        return Choice.NEUTRAL;
    }
}
