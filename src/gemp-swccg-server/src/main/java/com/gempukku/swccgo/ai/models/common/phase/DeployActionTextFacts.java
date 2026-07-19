package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Objects;

/** Immutable adapter facts for shared DEPLOY action-text decisions. */
public final class DeployActionTextFacts {

    public enum AmsdActionKind {
        GENERIC_REVEAL,
        PIETT_SPECIFIC,
        OTHER_SPECIFIC
    }

    private DeployActionTextFacts() {
    }

    public record AmsdFacts(
            String actionId,
            boolean bespinSystemOnTable,
            boolean alreadyFailedThisTurn,
            AmsdActionKind actionKind,
            boolean oracleAnalyzed,
            boolean piettInHand,
            boolean executorInHand,
            boolean executorInReserve,
            int currentTurn,
            int forceAvailable) {

        public AmsdFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(actionKind, "actionKind");
        }
    }

    public record DockingBayFacts(
            String actionId,
            int emptyFriendlyBays,
            int totalFriendlyBays) {

        public DockingBayFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record VaderCastleFacts(
            String actionId,
            boolean objectiveAnalyzed,
            boolean huntDownVActive,
            boolean vaderOnTable,
            int forceAvailable) {

        public VaderCastleFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record DiningRoomLandoFacts(
            String actionId,
            boolean objectiveAnalyzed,
            boolean needsBespinSystemPresence,
            int friendlyCountAtDiningRoom) {

        public DiningRoomLandoFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record BespinShipFacts(
            String actionId,
            boolean friendlyPowerAtBespin) {

        public BespinShipFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record ActionFacts(String actionId) {
        public ActionFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }
}
