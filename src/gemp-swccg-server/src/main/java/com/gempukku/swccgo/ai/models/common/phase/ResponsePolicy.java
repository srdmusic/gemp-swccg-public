package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.common.AiPriorityCards;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import com.gempukku.swccgo.common.Side;

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

    public record CancelEvaluation(
            PolicyResult result, boolean delegatesSelfCancelDrain) {
    }

    public record BarrierEvaluation(
            PolicyResult result, boolean terminal, boolean rememberTarget) {
    }

    public enum GrabOutcome {
        CONFIRMED_OWN,
        CONFIRMED_OPPONENT,
        NAME_OPPONENT,
        NAME_OWN_DARK,
        NAME_OWN_LIGHT,
        UNKNOWN_OPPONENT_TURN,
        UNKNOWN_OWN_TURN
    }

    public record GrabEvaluation(
            PolicyResult result, boolean setScoreBeforeAdd,
            boolean terminal, GrabOutcome outcome) {
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

    public static PolicyResult scoreSenseSelfCancel(String actionId) {
        return one(actionId, "V37.3-sense-self-cancel", -9999.0f,
                "V37.3 SENSE SELF-CANCEL: NEVER cancel our OWN interrupt!");
    }

    public static CancelEvaluation scoreSenseCancel(
            String actionId,
            boolean destinyBased,
            boolean highValue,
            int targetScore,
            String matchedPattern,
            boolean forceDrain,
            boolean myTurn) {
        if (destinyBased) {
            if (highValue && targetScore >= 80) {
                return scored(one(actionId,
                        "RESPONSE-sense-destiny-critical", 10.0f,
                        "Destiny cancel critical target: " + matchedPattern));
            }
            return scored(one(actionId,
                    "RESPONSE-sense-destiny-skip", -10.0f,
                    "Destiny-based cancel (unreliable, skip)"));
        }
        if (highValue && targetScore >= 80) {
            return scored(one(actionId, "RESPONSE-sense-critical", 70.0f,
                    "Cancel CRITICAL target: " + matchedPattern + "!"));
        }
        if (highValue && targetScore >= 60) {
            return scored(one(actionId, "RESPONSE-sense-high-value", 50.0f,
                    "Cancel high-value target: " + matchedPattern));
        }
        if (highValue) {
            return scored(one(actionId, "RESPONSE-sense-valuable", 45.0f,
                    "Cancel valuable target: " + matchedPattern));
        }
        if (forceDrain) {
            if (myTurn) {
                return delegateSelfCancelDrain();
            }
            return scored(one(actionId,
                    "RESPONSE-sense-force-drain-opponent", 35.0f,
                    "Cancel opponent's force drain"));
        }
        if (!myTurn) {
            return scored(one(actionId, "RESPONSE-sense-opponent-turn", 30.0f,
                    "Cancel opponent interrupt (their turn)"));
        }
        return scored(one(actionId, "RESPONSE-sense-own-turn", 15.0f,
                "Cancel opponent interrupt (our turn)"));
    }

    public static CancelEvaluation scoreLateForceDrainCancel(
            String actionId, boolean myTurn) {
        if (myTurn) {
            return delegateSelfCancelDrain();
        }
        return scored(one(actionId,
                "RESPONSE-late-force-drain-opponent", 30.0f,
                "Cancel opponent's force drain"));
    }

    public static PolicyResult scoreRemainingBattleDamageCancel(
            String actionId) {
        return one(actionId, "RESPONSE-houjix-ghhhk", 30.0f,
                "Cancel battle damage - valuable survival card");
    }

    public static BarrierEvaluation scoreBarrier(
            String actionId,
            String targetCardName,
            boolean alreadyBarriered,
            boolean ownTarget,
            boolean weHavePresence,
            boolean locationContested,
            float targetPower,
            float ourPower,
            float theirPower) {
        if (alreadyBarriered) {
            return barrier(one(actionId, "RESPONSE-barrier-already", -50.0f,
                    "Already barriered " + targetCardName + " this turn - wasteful!"),
                    true, false);
        }
        if (ownTarget) {
            return barrier(one(actionId, "RESPONSE-barrier-own", -9999.0f,
                    String.format("V35.1 SELF-BARRIER BLOCK: %s is OUR character — NEVER prevent our own from battling!",
                            targetCardName)), true, false);
        }
        if (!weHavePresence) {
            return barrier(one(actionId, "RESPONSE-barrier-no-presence", -9999.0f,
                    "V48 BARRIER USELESS: No friendly presence at location — serves no purpose!"),
                    false, false);
        }
        if (!locationContested) {
            return barrier(one(actionId, "RESPONSE-barrier-not-contested", -30.0f,
                    "Save barrier - location not contested"), false, false);
        }
        if (ourPower >= theirPower + 8) {
            return barrier(one(actionId, "RESPONSE-barrier-dominating", -30.0f,
                    "Save barrier - already dominating (" + (int) ourPower + " vs "
                            + (int) theirPower + ")"), false, false);
        }
        if (targetPower >= 5) {
            return barrier(one(actionId, "RESPONSE-barrier-high-target", 50.0f,
                    "Barrier on HIGH POWER target (" + (int) targetPower + ")!"),
                    false, true);
        }
        if (theirPower >= ourPower) {
            return barrier(one(actionId, "RESPONSE-barrier-protect", 40.0f,
                    "Barrier to protect (losing " + (int) ourPower + " vs "
                            + (int) theirPower + ")"), false, true);
        }
        return barrier(one(actionId, "RESPONSE-barrier-contested", 30.0f,
                "Barrier at contested location"), false, true);
    }

    public static GrabEvaluation scoreGrab(
            String actionId,
            boolean confirmedOwnCard,
            boolean confirmedOpponentCard,
            Side mySide,
            boolean looksLightSide,
            boolean looksDarkSide,
            boolean myTurn) {
        if (confirmedOwnCard && !confirmedOpponentCard) {
            return grab(one(actionId, "RESPONSE-grab-confirmed-own", -9999.0f,
                    "V53 NEVER GRAB OWN: Grabbing own interrupt is suicide!"),
                    true, true, GrabOutcome.CONFIRMED_OWN);
        }
        if (confirmedOpponentCard) {
            return grab(one(actionId, "RESPONSE-grab-confirmed-opponent", 30.0f,
                    "V53 GRAB OPPONENT: Confirmed opponent's interrupt — grab it!"),
                    false, true, GrabOutcome.CONFIRMED_OPPONENT);
        }
        if ((mySide == Side.DARK && looksLightSide)
                || (mySide == Side.LIGHT && looksDarkSide)) {
            String reason = mySide == Side.DARK
                    ? "Grab Light side card (we are Dark)"
                    : "Grab Dark side card (we are Light)";
            return grab(one(actionId, "RESPONSE-grab-name-opponent", 30.0f, reason),
                    false, false, GrabOutcome.NAME_OPPONENT);
        }
        if ((mySide == Side.DARK && looksDarkSide)
                || (mySide == Side.LIGHT && looksLightSide)) {
            String reason = mySide == Side.DARK
                    ? "V53 NEVER GRAB OWN: Grabbing own Dark card!"
                    : "V53 NEVER GRAB OWN: Grabbing own Light card!";
            return grab(one(actionId, "RESPONSE-grab-name-own", -9999.0f, reason),
                    true, false, mySide == Side.DARK
                            ? GrabOutcome.NAME_OWN_DARK : GrabOutcome.NAME_OWN_LIGHT);
        }
        if (!myTurn) {
            return grab(one(actionId, "RESPONSE-grab-unknown-opponent-turn", 30.0f,
                    "Grab unknown card (opponent's turn — likely theirs)"),
                    false, false, GrabOutcome.UNKNOWN_OPPONENT_TURN);
        }
        return grab(one(actionId, "RESPONSE-grab-unknown-own-turn", -200.0f,
                "V53 GRAB CAUTION: Unknown owner on our turn — avoid!"),
                false, false, GrabOutcome.UNKNOWN_OWN_TURN);
    }

    public static PolicyResult scoreCancelSelection(
            String actionId, boolean resolved, boolean opponentCard) {
        if (!resolved) {
            return result(new ArrayList<>());
        }
        if (opponentCard) {
            return one(actionId, "RESPONSE-cancel-selection-opponent", 100.0f,
                    "Opponent's card - cancel!");
        }
        return one(actionId, "RESPONSE-cancel-selection-own", -200.0f,
                "Our card - don't cancel!");
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

    private static CancelEvaluation scored(PolicyResult result) {
        return new CancelEvaluation(result, false);
    }

    private static BarrierEvaluation barrier(
            PolicyResult result, boolean terminal, boolean rememberTarget) {
        return new BarrierEvaluation(result, terminal, rememberTarget);
    }

    private static GrabEvaluation grab(
            PolicyResult result, boolean setScoreBeforeAdd,
            boolean terminal, GrabOutcome outcome) {
        return new GrabEvaluation(result, setScoreBeforeAdd, terminal, outcome);
    }

    private static CancelEvaluation delegateSelfCancelDrain() {
        return new CancelEvaluation(result(new ArrayList<>()), true);
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
