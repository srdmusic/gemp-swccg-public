package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.List;

/** Shared owner of ACTIVATE action-choice and zero-activation confirmation scores. */
public final class ActivateActionPolicy {
    public enum Mode {
        TOP_LEVEL_ACTIVATE,
        // V297.1 RETIRED (battlePlausible bypass removed):
        // TOP_LEVEL_ACTIVATE_WITHOUT_BATTLE,
        TOP_LEVEL_KEEP_BUFFER,
        CONFIRM_KEEP_BUFFER,
        REJECT_BUFFER_REACTIVATION,
        CONFIRM_REACTIVATION,
        // V297.1 RETIRED (battlePlausible bypass removed):
        // CONFIRM_REACTIVATION_WITHOUT_BATTLE,
        REJECT_SKIP,
        ALWAYS_ACTIVATE,
        NONE
    }

    public record Evaluation(PolicyResult result, Mode mode) {
    }

    private ActivateActionPolicy() {
    }

    public static Evaluation topLevel(String actionId, int reserveDeckSize) {
        if (reserveDeckSize <= 4) {
            return one(actionId, "V61c-activate-choice", TraceOutputKind.VETO,
                    -6000.0f,
                    "V61c DESTINY BUFFER: reserve <= 4; pass activation, keep 4 for destiny",
                    Mode.TOP_LEVEL_KEEP_BUFFER);
        }
        Mode mode = Mode.TOP_LEVEL_ACTIVATE;
        return one(actionId, "V168-activate-choice", TraceOutputKind.ORDERING,
                5000.0f,
                "V168 ALWAYS ACTIVATE: never pass Force activation while Force can be activated",
                mode);
    }

    public static Evaluation zeroConfirmation(String actionId, String actionTextLower,
                                              int reserveDeckSize) {
        // See topLevel: a later deploy or move can create the battle.
        if (reserveDeckSize <= 4) {
            if ("yes".equals(actionTextLower)) {
                return one(actionId, "V61c-confirm-pass", TraceOutputKind.VETO,
                        9999.0f,
                        "V61c DESTINY BUFFER: reserve <= 4; confirm pass, keep 4 for destiny",
                        Mode.CONFIRM_KEEP_BUFFER);
            }
            if ("no".equals(actionTextLower)) {
                return one(actionId, "V61c-reject-reactivation", TraceOutputKind.VETO,
                        -9999.0f,
                        "V61c DESTINY BUFFER: reserve <= 4; do not go back and activate",
                        Mode.REJECT_BUFFER_REACTIVATION);
            }
            return none();
        }

        if ("no".equals(actionTextLower)) {
            Mode mode = Mode.CONFIRM_REACTIVATION;
            return one(actionId, "V38.3-confirm-reactivation", TraceOutputKind.VETO,
                    9999.0f,
                    "V38.3 MUST ACTIVATE: Go back and activate Force!",
                    mode);
        }
        if ("yes".equals(actionTextLower)) {
            return one(actionId, "V38.3-reject-skip", TraceOutputKind.VETO,
                    -9999.0f,
                    "V38.3 NEVER SKIP ACTIVATION: Do not pass without activating!",
                    Mode.REJECT_SKIP);
        }
        return none();
    }

    public static Evaluation alwaysActivate(String actionId) {
        return one(actionId, "V38.3-activate-base", TraceOutputKind.ORDERING,
                500.0f,
                "V38.3 ALWAYS ACTIVATE: Force is currency — activate it!",
                Mode.ALWAYS_ACTIVATE);
    }

    private static Evaluation one(String actionId, String ruleId,
                                  TraceOutputKind outputKind, float delta,
                                  String reason, Mode mode) {
        PolicyOperation operation = PolicyOperation.add(actionId,
                TraceRuleId.of(ruleId), TraceDomainId.ACTIVATION_AMOUNT,
                outputKind, delta, reason);
        return new Evaluation(new PolicyResult("ACTIVATE_ACTION_POLICY", List.of(operation)), mode);
    }

    private static Evaluation none() {
        return new Evaluation(new PolicyResult("ACTIVATE_ACTION_POLICY", List.of()), Mode.NONE);
    }
}
