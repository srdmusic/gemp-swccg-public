package com.gempukku.swccgo.ai.models.common.phase;

/** Pure owner of the DRAW phase's Force-reserve arithmetic. */
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

    /** Corridor transit Force is mandatory and therefore added after the base cap. */
    public DrawReserveAssessment plusCorridorCharacters(int corridorCharacters) {
        return new DrawReserveAssessment(forceToReserve + corridorCharacters);
    }
}
