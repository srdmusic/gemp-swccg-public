package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Objects;

/** Immutable DeployEvaluator-side PULL guard facts. */
public record PullDeployFacts(
        String actionId,
        String actionText,
        boolean failedTwice,
        int reserveSize,
        String namedMissingTarget,
        String genericTypedMiss,
        String genericUntypedMiss,
        PullOracleView.Validation memoryValidation,
        PullOracleView.Validation sourceValidation,
        String sourceTitle,
        boolean allReserveTargetsUnattachableWeapons,
        boolean starshipOnlyWithoutSpaceLocation) {
    public PullDeployFacts {
        Objects.requireNonNull(actionId, "actionId");
        actionText = actionText == null ? "" : actionText;
        namedMissingTarget = namedMissingTarget == null ? "" : namedMissingTarget;
        genericTypedMiss = genericTypedMiss == null ? "" : genericTypedMiss;
        genericUntypedMiss = genericUntypedMiss == null ? "" : genericUntypedMiss;
        memoryValidation = memoryValidation == null
                ? new PullOracleView.Validation(PullOracleView.Outcome.UNKNOWN, "")
                : memoryValidation;
        sourceValidation = sourceValidation == null
                ? new PullOracleView.Validation(PullOracleView.Outcome.UNKNOWN, "")
                : sourceValidation;
        sourceTitle = sourceTitle == null ? "?" : sourceTitle;
    }
}
