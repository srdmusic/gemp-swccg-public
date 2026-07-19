package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Set;

/**
 * Shared V160/V169 MOVE blocked-response classification.
 * Adapters retain context reads, power scans, action construction, and control flow.
 */
public final class MoveBlockedResponsePolicy {
    public enum Outcome {
        NOT_BLOCKED,
        ENDANGERED_FALLTHROUGH,
        HARD_BLOCK
    }

    public record Evaluation(
            Outcome outcome, String reason, float delta,
            boolean powerFactsAvailable,
            float ourPower, float opponentPower) {
        private static Evaluation notBlocked() {
            return new Evaluation(
                    Outcome.NOT_BLOCKED, null, 0.0f,
                    false, 0.0f, 0.0f);
        }
    }

    private MoveBlockedResponsePolicy() {
    }

    public static boolean matches(
            Set<String> blockedResponses,
            String actionId, String actionText) {
        return blockedResponses != null
                && !blockedResponses.isEmpty()
                && (blockedResponses.contains(actionId)
                        || blockedResponses.contains(actionText));
    }

    public static Evaluation classify(
            boolean blockedMatch, boolean powerFactsAvailable,
            float ourPower, float opponentPower) {
        if (!blockedMatch) {
            return Evaluation.notBlocked();
        }
        if (powerFactsAvailable && opponentPower > ourPower) {
            return new Evaluation(
                    Outcome.ENDANGERED_FALLTHROUGH,
                    null, 0.0f, true, ourPower, opponentPower);
        }
        return new Evaluation(
                Outcome.HARD_BLOCK,
                "CANCEL-LOOP BLOCK: this move led to repeated Done-cancels — try something else (LADDER VETO)",
                -100000.0f,
                powerFactsAvailable, ourPower, opponentPower);
    }
}
