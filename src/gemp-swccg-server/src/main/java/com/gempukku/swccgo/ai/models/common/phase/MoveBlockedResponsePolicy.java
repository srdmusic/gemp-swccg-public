package com.gempukku.swccgo.ai.models.common.phase;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Shared V160/V169 MOVE blocked-response classification and retry budget.
 * Adapters retain context reads, power scans, action construction, and control flow.
 */
public final class MoveBlockedResponsePolicy {
    public static final int ENDANGERED_SOFT_RETRY_BUDGET = 3;

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

    public record RetryEvaluation(
            boolean hardBlock, String reason, float delta,
            int attempt, int retryBudget) {
    }

    /**
     * Per-AI-instance V169 retry state. The adapter owns this object; the shared
     * policy owns its turn reset, key accounting, and exact score boundary.
     */
    public static final class RetryBudget {
        private final Map<String, Integer> attemptsByAction = new HashMap<>();
        private int turn = -1;

        public RetryEvaluation evaluate(int currentTurn, String actionKey) {
            if (turn != currentTurn) {
                attemptsByAction.clear();
                turn = currentTurn;
            }
            int attempt = attemptsByAction.merge(
                    actionKey, 1, Integer::sum);
            if (attempt <= ENDANGERED_SOFT_RETRY_BUDGET) {
                return new RetryEvaluation(
                        false,
                        "BLOCKED (loop prevention) — soft (V169: endangered mover, retreat must stay possible)",
                        -250.0f,
                        attempt,
                        ENDANGERED_SOFT_RETRY_BUDGET);
            }
            return new RetryEvaluation(
                    true,
                    "BLOCKED (loop prevention) — hard veto (V169 retry budget exhausted: no safe destination materialized)",
                    -100000.0f,
                    attempt,
                    ENDANGERED_SOFT_RETRY_BUDGET);
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
        if (isEndangered(powerFactsAvailable, ourPower, opponentPower)) {
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

    public static boolean isEndangered(
            boolean powerFactsAvailable,
            float ourPower, float opponentPower) {
        return powerFactsAvailable && opponentPower > ourPower;
    }
}
