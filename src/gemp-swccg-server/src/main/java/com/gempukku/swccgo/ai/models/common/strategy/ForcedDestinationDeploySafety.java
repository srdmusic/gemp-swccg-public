package com.gempukku.swccgo.ai.models.common.strategy;

import java.util.Objects;

/** Shared policy for a location action that pulls a character to that location. */
public final class ForcedDestinationDeploySafety {
    public static final float WEAK_SOLO_PENALTY = -800.0f;

    public enum ObjectiveState {
        UNFLIPPED_TARGET_NAMED,
        FLIPPED,
        NOT_APPLICABLE
    }

    public enum Verdict {
        FLIP_PLAN_EXEMPT,
        HARD_BLOCK,
        WEAK_SOLO_NO_PLAN,
        ALLOW,
        UNKNOWN
    }

    public record Assessment(Verdict verdict, String reason, float scoreDelta) {
        public Assessment {
            Objects.requireNonNull(verdict, "verdict");
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("assessment reason must be nonblank");
            }
            if (verdict == Verdict.WEAK_SOLO_NO_PLAN
                    ? Float.compare(scoreDelta, WEAK_SOLO_PENALTY) != 0
                    : Float.compare(scoreDelta, 0f) != 0) {
                throw new IllegalArgumentException("score delta does not match verdict");
            }
        }
    }

    private ForcedDestinationDeploySafety() {
    }

    public static Assessment assess(
            boolean exactPullIdentityResolved,
            ObjectiveState objectiveState,
            FormationSafety.CharacterDeployCheck formation,
            boolean weakSoloNoPlan) {
        Objects.requireNonNull(objectiveState, "objectiveState");
        if (!exactPullIdentityResolved) {
            return new Assessment(
                    Verdict.UNKNOWN, "forced-destination pull identity is unresolved", 0f);
        }
        if (objectiveState == ObjectiveState.UNFLIPPED_TARGET_NAMED) {
            return new Assessment(
                    Verdict.FLIP_PLAN_EXEMPT,
                    "pulled persona is named in the unflipped objective condition", 0f);
        }
        if (formation == null
                || formation.state() == FormationSafety.CharacterDeployState.UNKNOWN) {
            return new Assessment(
                    Verdict.UNKNOWN,
                    formation != null ? formation.reason() : "formation assessment is unavailable",
                    0f);
        }
        if (formation.state() == FormationSafety.CharacterDeployState.VETOED) {
            return new Assessment(Verdict.HARD_BLOCK, formation.reason(), 0f);
        }
        if (weakSoloNoPlan) {
            String reason = objectiveState == ObjectiveState.FLIPPED
                    ? "post-flip forced deploy has no safe buddy plan"
                    : "forced deploy is a weak solo with no buddy plan";
            return new Assessment(Verdict.WEAK_SOLO_NO_PLAN, reason, WEAK_SOLO_PENALTY);
        }
        return new Assessment(Verdict.ALLOW, "forced deploy formation is allowed", 0f);
    }
}
