package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Set;

/** Immutable board facts for one physical battle target. */
public record BattleLocationAssessment(
        boolean known,
        float ourPower,
        float opponentPower,
        float ourAbility,
        float opponentAbility,
        float ourWeaponBonus,
        float opponentWeaponBonus,
        int ourDestinyDraws,
        int opponentDestinyDraws,
        boolean destinyEligible,
        String formationVetoReason,
        boolean targetOverpower,
        Set<Integer> friendlyCardIds,
        boolean vaderAtTarget,
        boolean vaderArmed,
        boolean lukeAtTarget,
        boolean jediAtTarget,
        boolean haveIHaveYouNow,
        BattlePredictionAssessment prediction) {

    public BattleLocationAssessment {
        friendlyCardIds = Set.copyOf(friendlyCardIds);
        if (prediction == null) {
            throw new IllegalArgumentException("prediction must be nonnull");
        }
        if (known && (ourDestinyDraws < 1 || opponentDestinyDraws < 1)) {
            throw new IllegalArgumentException("known destiny draw estimates must be positive");
        }
    }

    public static BattleLocationAssessment unknown() {
        return new BattleLocationAssessment(
                false, 0f, 0f, 0f, 0f, 0f, 0f,
                1, 1, false, null, false, Set.of(),
                false, false, false, false, false,
                BattlePredictionAssessment.unknown());
    }
}
