package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.DeployDestinationRef;

/** Exact accepted DEPLOY opportunity carried into the same turn's BATTLE window. */
public record BattleDeployIntent(
        Kind kind,
        Integer targetCardId,
        int turn,
        String attemptId) {

    public enum Kind {
        NONE,
        OVERPOWER,
        TARGETED_RESCUE
    }

    public BattleDeployIntent {
        if (kind == null) {
            throw new IllegalArgumentException("kind must be nonnull");
        }
        if (kind == Kind.NONE) {
            if (targetCardId != null || turn != -1 || attemptId != null) {
                throw new IllegalArgumentException("NONE cannot retain DEPLOY identity");
            }
        } else if (targetCardId == null || targetCardId < 0 || turn < 0
                || attemptId == null || attemptId.isBlank()) {
            throw new IllegalArgumentException(
                    "accepted DEPLOY intent requires target, turn, and attempt identity");
        }
    }

    public static BattleDeployIntent none() {
        return new BattleDeployIntent(Kind.NONE, null, -1, null);
    }

    public static BattleDeployIntent from(DeployTransaction transaction,
                                          int currentTurn,
                                          String playerId) {
        if (transaction == null
                || transaction.stage() != DeployTransaction.Stage.COMPLETED
                || transaction.terminalReason() != null
                || currentTurn < 0
                || playerId == null
                || !playerId.equals(transaction.key().playerId())
                || currentTurn != transaction.key().turn()
                || !(transaction.selectedDestination()
                    instanceof DeployDestinationRef.Card cardDestination)) {
            return none();
        }

        Kind kind = switch (transaction.formation().verdict()) {
            case OVERPOWER_OPPORTUNITY -> Kind.OVERPOWER;
            case TARGETED_RESCUE -> Kind.TARGETED_RESCUE;
            default -> Kind.NONE;
        };
        if (kind == Kind.NONE
                || !transaction.formation().allows(transaction.selectedDestination())) {
            return none();
        }
        return new BattleDeployIntent(
                kind,
                cardDestination.card().currentCardId(),
                currentTurn,
                transaction.key().attemptId());
    }

    public BattleDeployIntent forTarget(int candidateTargetCardId) {
        return targetCardId != null && targetCardId == candidateTargetCardId
                ? this : none();
    }
}
