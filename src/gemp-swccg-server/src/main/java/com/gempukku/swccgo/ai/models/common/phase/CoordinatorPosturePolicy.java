package com.gempukku.swccgo.ai.models.common.phase;

/** Pure arithmetic for the top-level legacy posture fallback. */
public final class CoordinatorPosturePolicy {

    private CoordinatorPosturePolicy() {
    }

    public static int score(
            boolean behindOnLifeForce,
            boolean aheadOnBoard,
            boolean behindOnBoard,
            boolean forceDrain,
            boolean initiateBattle,
            boolean deployOrDraw,
            boolean matchesHandTitle) {
        int score = 0;
        if (behindOnLifeForce && (forceDrain || initiateBattle)) {
            score += 40;
        }
        if (aheadOnBoard && initiateBattle) {
            score += 30;
        }
        if (behindOnBoard && initiateBattle) {
            score -= 30;
        }
        if (behindOnBoard && deployOrDraw) {
            score += 20;
        }
        if (matchesHandTitle) {
            score += 60;
        }
        return score;
    }
}
