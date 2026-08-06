package com.gempukku.swccgo.ai.models.common.phase;

/** V194: pure AI owner of the DRAW phase's Force-reserve arithmetic. */
public record DrawReserveAssessment(int forceToReserve) {

    public static DrawReserveAssessment base(boolean drawTheirFire,
                                             boolean firstStrike,
                                             boolean contestedAny,
                                             boolean turnFourOrLater,
                                             boolean imperialArrestOrder,
                                             boolean vergeNeedsDeathStarMove,
                                             int maintenanceObligation) {
        int reserve = 0;
        if (drawTheirFire) {
            reserve += 1;
        }
        if (firstStrike) {
            reserve += 1;
        }
        if (contestedAny) {
            reserve += 1;
        }
        if (turnFourOrLater) {
            reserve += 1;
        }
        if (imperialArrestOrder) {
            reserve += 2;
        }
        if (vergeNeedsDeathStarMove) {
            reserve += 1;
        }
        reserve += maintenanceObligation;
        return new DrawReserveAssessment(Math.min(10, reserve));
    }

    /** Hidden Path transit Force is mandatory and added after the base cap. */
    public DrawReserveAssessment plusHiddenPathTransitReserve(
            int transitReserve) {
        return new DrawReserveAssessment(forceToReserve + transitReserve);
    }
}
