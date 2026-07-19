package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Objects;

/** Immutable safety facts for an actual reserve-deploy child candidate. */
public record PullDeployCandidateFacts(
        String actionId,
        String displayTitle,
        String weaponDeviceBlockReason) {
    public PullDeployCandidateFacts {
        Objects.requireNonNull(actionId, "actionId");
        displayTitle = displayTitle == null ? "" : displayTitle;
        weaponDeviceBlockReason = weaponDeviceBlockReason == null
                ? "" : weaponDeviceBlockReason;
    }
}
