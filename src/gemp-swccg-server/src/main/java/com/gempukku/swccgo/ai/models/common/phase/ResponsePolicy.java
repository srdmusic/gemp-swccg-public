package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.common.AiPriorityCards;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared AI-only routing and ordered scoring for recognized response windows.
 * The bot adapters retain observations, game-state reads, score application,
 * logging, control flow, and final responses. Unknown shapes remain on the
 * legacy dispatcher.
 */
public final class ResponsePolicy {
    private static final int DAMAGE_CANCEL_SCORE = 100;
    private static final int BARRIER_SCORE = 80;
    private static final int SENSE_SCORE = 70;
    private static final String ACTION_TEXT_PRODUCER =
            "RESPONSE_ACTION_TEXT_POLICY";

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

    public static PolicyResult scoreWhenDeployedFreeTrigger(
            String actionId, String why) {
        return one(actionId, "V184-when-deployed-trigger", 300.0f,
                "V184 WHEN-DEPLOYED TRIGGER: free value (" + why
                        + ") — fire it, don't pass");
    }

    public static PolicyResult scoreSenseRedraw(
            String actionId,
            boolean redrawHand,
            boolean mutualRedraw) {
        List<PolicyOperation> operations = new ArrayList<>(2);
        if (redrawHand) {
            add(operations, actionId, "V29.8-sense-redraw-hand", -600.0f,
                    "V29.8 SENSE REDRAW BLOCKED: NEVER redraw hand — save Sense for canceling opponent interrupts! Costs 3 Force AND helps opponent!");
        }
        if (mutualRedraw) {
            add(operations, actionId, "V29.8-sense-mutual-redraw", -600.0f,
                    "V29.8 SENSE UNCERTAIN BLOCKED: Don't make both players redraw — helps opponent!");
        }
        return result(operations);
    }

    public static PolicyResult scoreSaveJedi(String actionId) {
        return one(actionId, "V53b-save-jedi", 500.0f,
                "V53b SAVE JEDI: Stack Jedi on Fallen Order — lose 1 force to save them!");
    }

    public static PolicyResult scoreReact(String actionId) {
        return one(actionId, "RESPONSE-react", -30.0f,
                "Avoid reacts (bot doesn't understand timing)");
    }

    public static PolicyResult scoreCancelOwn(String actionId) {
        return one(actionId, "RESPONSE-cancel-own", -50.0f,
                "Never cancel own cards");
    }

    public static PolicyResult scoreRemainingBattleDamageCancel(
            String actionId) {
        return one(actionId, "RESPONSE-houjix-ghhhk", 30.0f,
                "Cancel battle damage - valuable survival card");
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

    private static PolicyResult one(
            String actionId, String ruleId, float delta, String reason) {
        List<PolicyOperation> operations = new ArrayList<>(1);
        add(operations, actionId, ruleId, delta, reason);
        return result(operations);
    }

    private static void add(
            List<PolicyOperation> operations,
            String actionId,
            String ruleId,
            float delta,
            String reason) {
        operations.add(PolicyOperation.add(
                actionId,
                TraceRuleId.of(ruleId),
                TraceDomainId.RESPONSE_ROUTING,
                TraceOutputKind.BANDED,
                delta,
                reason));
    }

    private static PolicyResult result(List<PolicyOperation> operations) {
        return new PolicyResult(ACTION_TEXT_PRODUCER, operations);
    }
}
