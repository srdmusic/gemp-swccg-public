package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Objects;

/** Immutable facts for early location and Bespin-first DEPLOY sequencing. */
public final class DeployObjectiveSequencingFacts {

    private DeployObjectiveSequencingFacts() {
    }

    public record EarlyLocationCandidate(
            String actionText,
            boolean cardResolved,
            boolean locationByCategory) {

        public EarlyLocationCandidate {
            actionText = actionText == null ? "" : actionText;
        }
    }

    public record EarlyLocation(
            String actionId,
            boolean piettOracleAnalyzed,
            boolean piettAccessible,
            boolean piettLost,
            int piettTurnNumber,
            boolean objectiveAnalyzed,
            boolean needsBespinSystem,
            int objectiveTurnNumber,
            boolean bespinDeploy,
            boolean bespinOnTable) {

        public EarlyLocation {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record BespinFirstCandidate(
            String guardCheckText,
            boolean cardResolved,
            boolean characterByCategory,
            boolean locationByCategory,
            boolean shipByCategory) {

        public BespinFirstCandidate {
            guardCheckText = guardCheckText == null ? "" : guardCheckText;
        }
    }

    public record BespinFirstDecision(
            String actionId,
            boolean objectiveForbidsExecutor,
            boolean oracleAnalyzed,
            boolean capitalAccessible) {

        public BespinFirstDecision {
            Objects.requireNonNull(actionId, "actionId");
        }
    }
}
