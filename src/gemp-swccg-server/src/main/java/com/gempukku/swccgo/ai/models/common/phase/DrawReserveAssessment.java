package com.gempukku.swccgo.ai.models.common.phase;

/** V194: pure AI owner of the DRAW phase's Force-reserve arithmetic. */
public record DrawReserveAssessment(
        int forceToReserve,
        int vergeForceToReserve,
        int hiddenPathForceToReserve) {

    public DrawReserveAssessment(int forceToReserve) {
        this(forceToReserve, 0, 0);
    }

    public int generalForceToReserve() {
        return forceToReserve
                - vergeForceToReserve
                - hiddenPathForceToReserve;
    }

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
        reserve += maintenanceObligation;
        int generalReserve = Math.min(10, reserve);
        int totalReserve = Math.min(10,
                reserve + (vergeNeedsDeathStarMove ? 1 : 0));
        int vergeReserve = totalReserve - generalReserve;
        return new DrawReserveAssessment(
                totalReserve, vergeReserve, 0);
    }

    /** Hidden Path transit Force is mandatory and added after the base cap. */
    public DrawReserveAssessment plusHiddenPathTransitReserve(
            int transitReserve) {
        return new DrawReserveAssessment(
                forceToReserve + transitReserve,
                vergeForceToReserve,
                hiddenPathForceToReserve + transitReserve);
    }
}
