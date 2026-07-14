package com.gempukku.swccgo.ai.models.common.strategy;

import java.util.Objects;

/** Immutable obligation inputs shared by DEPLOY planning and adjacent reserve consumers. */
public record ForceObligationVector(
        int battleReserve,
        int maintenanceObligation,
        int maintenanceCardCount,
        boolean drawTheirFireActive,
        boolean firstStrikeActive,
        boolean imperialArrestOrderActive,
        boolean grabberUnused,
        int undercoverSpyCount,
        boolean deathStarMovementNeeded) {

    public ForceObligationVector {
        if (battleReserve < 0 || maintenanceObligation < 0
                || maintenanceCardCount < 0 || undercoverSpyCount < 0) {
            throw new IllegalArgumentException("Force obligation counts must be non-negative");
        }
    }

    public static ForceObligationVector from(ForceReserveService.Facts facts,
                                             int battleReserve) {
        Objects.requireNonNull(facts, "facts");
        return new ForceObligationVector(
                battleReserve,
                facts.maintenanceObligation,
                facts.maintenanceCardCount,
                facts.dtfActive,
                facts.firstStrikeActive,
                facts.iaoActive,
                facts.grabberUnused,
                facts.undercoverSpyCount,
                facts.vergeNeedsDeathStarMove);
    }

    /** Exact reserve currently used by DeployPhasePlanner budget math. */
    public int plannerReserve() {
        return battleReserve + maintenanceObligation;
    }
}
