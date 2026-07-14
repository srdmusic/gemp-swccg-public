package com.gempukku.swccgo.ai.models.common.phase;

/** Immutable identity of one offered initiation target. */
public record BattleInitiationAssessment(
        int candidateOrdinal,
        int targetCardId,
        BattleDeployIntent deployIntent,
        BattleLocationAssessment location) {

    public BattleInitiationAssessment {
        if (candidateOrdinal < 0) {
            throw new IllegalArgumentException("candidateOrdinal must be >= 0");
        }
        if (targetCardId < 0) {
            throw new IllegalArgumentException("targetCardId must be >= 0");
        }
        if (deployIntent == null) {
            throw new IllegalArgumentException("deployIntent must be nonnull");
        }
        deployIntent = deployIntent.forTarget(targetCardId);
        if (location == null) {
            throw new IllegalArgumentException("location must be nonnull");
        }
    }

    public BattleInitiationAssessment(int candidateOrdinal, int targetCardId) {
        this(candidateOrdinal, targetCardId, BattleDeployIntent.none(),
                BattleLocationAssessment.unknown());
    }

    public BattleInitiationAssessment(int candidateOrdinal, int targetCardId,
                                      BattleDeployIntent deployIntent) {
        this(candidateOrdinal, targetCardId, deployIntent,
                BattleLocationAssessment.unknown());
    }
}
