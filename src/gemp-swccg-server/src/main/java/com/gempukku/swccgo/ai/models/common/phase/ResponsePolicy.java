package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.common.AiPriorityCards;

import java.util.Locale;

/**
 * Shared AI-only routing helpers for recognized response windows. The bot
 * adapters retain game-state reads, tracing, logging, and final responses.
 * Unknown shapes remain on the legacy dispatcher.
 */
public final class ResponsePolicy {
    private static final int DAMAGE_CANCEL_SCORE = 100;
    private static final int BARRIER_SCORE = 80;
    private static final int SENSE_SCORE = 70;

    public enum Route {
        OPTIONAL_FORFEIT,
        REVERT_APPROVAL,
        UNDERCOVER_SPY,
        LEGACY
    }

    public record IndexedChoice(int index, String label) {
    }

    public record YesNoIndexes(int yesIndex, int noIndex) {
        public int choose(boolean yes) {
            return yes ? yesIndex : noIndex;
        }
    }

    private ResponsePolicy() {
    }

    public static Route classify(
            String decisionType, String decisionText) {
        String lower = decisionText == null
                ? "" : decisionText.toLowerCase(Locale.ROOT);

        // V45 intentionally has no decision-type guard in the legacy route.
        if (lower.contains("forfeit") && lower.contains("if desired")) {
            return Route.OPTIONAL_FORFEIT;
        }
        if ("MULTIPLE_CHOICE".equals(decisionType)
                && lower.contains("revert")) {
            return Route.REVERT_APPROVAL;
        }
        if ("MULTIPLE_CHOICE".equals(decisionType)
                && lower.contains("undercover spy")) {
            return Route.UNDERCOVER_SPY;
        }
        return Route.LEGACY;
    }

    public static IndexedChoice revertApproval(String[] results) {
        int index = 0;
        String label = "(default index 0)";
        if (results != null && results.length > 0) {
            for (int i = 0; i < results.length; i++) {
                String result = results[i] == null
                        ? "" : results[i].toLowerCase(Locale.ROOT);
                if (result.equals("yes") || result.contains("allow")
                        || result.contains("accept") || result.contains("ok")
                        || result.equals("revert")) {
                    index = i;
                    label = results[i];
                    break;
                }
            }
        }
        return new IndexedChoice(index, label);
    }

    public static YesNoIndexes yesNoIndexes(String[] results) {
        int yesIndex = 0;
        int noIndex = 1;
        if (results != null) {
            for (int i = 0; i < results.length; i++) {
                String result = results[i] == null
                        ? "" : results[i].toLowerCase(Locale.ROOT);
                if (result.equals("yes")) {
                    yesIndex = i;
                } else if (result.equals("no")) {
                    noIndex = i;
                }
            }
        }
        return new YesNoIndexes(yesIndex, noIndex);
    }

    public static boolean shouldDeployUndercover(int opponentDrain) {
        return opponentDrain >= 1;
    }

    /**
     * Preserves the legacy fallback's additive priority-card scoring. This is
     * separate from the deeper ActionText response rules.
     */
    public static int scorePriorityCards(
            String actionText, String decisionText) {
        int score = 0;

        if (actionText.contains("houjix") || actionText.contains("ghhhk")) {
            if (decisionText.contains("battle damage")
                    || decisionText.contains("cancel")) {
                score += DAMAGE_CANCEL_SCORE;
            }
        }

        if (actionText.contains("barrier")) {
            if (decisionText.contains("deploy")
                    || decisionText.contains("character")) {
                score += BARRIER_SCORE;
            }
        }

        if (actionText.contains("sense") && actionText.contains("cancel")) {
            AiPriorityCards.SenseTargetResult senseResult =
                    AiPriorityCards.getSenseTargetValue(decisionText);
            score += senseResult.isHighValue
                    ? senseResult.score : SENSE_SCORE / 2;
        }

        return score;
    }
}
