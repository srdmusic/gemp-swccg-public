package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.List;

/** Exact objective policy for 12_180 and 12_180_BACK. */
public final class NoMoneyNoPartsObjectivePolicy {

    private NoMoneyNoPartsObjectivePolicy() {
    }

    public static PolicyResult scoreBackGambitParent(
            String actionId, boolean exactAction,
            boolean safeCandidateAvailable) {
        if (!exactAction) return empty();
        if (!safeCandidateAvailable) {
            return result(PolicyOperation.hardVeto(
                    actionId,
                    TraceRuleId.of("OBJECTIVE.NO_MONEY.GAMBIT_NO_SAFE_CARD"),
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.VETO,
                    "NO MONEY: do not start the back-side gambit without a safe hand card"));
        }
        return result(add(
                actionId,
                "OBJECTIVE.NO_MONEY.GAMBIT_PARENT",
                TraceOutputKind.BANDED,
                3000.0f,
                "NO MONEY: use the back-side two-Force gambit with a safe hand card"));
    }

    public static PolicyResult scoreBackGambitCandidate(
            String actionId, boolean exactSelection,
            boolean safeCandidate, boolean safeAlternativeAvailable) {
        if (!exactSelection) return empty();
        if (!safeCandidate && safeAlternativeAvailable) {
            return result(PolicyOperation.hardVeto(
                    actionId,
                    TraceRuleId.of("OBJECTIVE.NO_MONEY.GAMBIT_CARD_UNSAFE"),
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.VETO,
                    "NO MONEY: keep this undeployable bluff card while a safe gambit card is available"));
        }
        return safeCandidate
                ? result(add(
                    actionId,
                    "OBJECTIVE.NO_MONEY.GAMBIT_CARD_SAFE",
                    TraceOutputKind.BANDED,
                    2000.0f,
                    "NO MONEY: this card can survive either opponent response"))
                : empty();
    }

    public static PolicyResult scoreOpponentWattoRemoval(
            String actionId, boolean exactAction) {
        return exactAction
                ? result(add(
                    actionId,
                    "OBJECTIVE.NO_MONEY.OPPONENT_REMOVE_WATTO",
                    TraceOutputKind.BANDED,
                    900.0f,
                    "NO MONEY counterplay: remove Watto to break the opponent's flip formation"))
                : empty();
    }

    public static PolicyResult preserveMoveForceForOrdinaryDeploy(
            String actionId, boolean ordinaryDeploy,
            int moveForceReserve, int forceAvailable,
            Integer exactPayment, int fallbackPayment) {
        if (!ordinaryDeploy || moveForceReserve <= 0) {
            return empty();
        }
        int fallback = Math.max(0, fallbackPayment);
        if (exactPayment == null) {
            return fallback > 0
                    && forceAvailable - fallback < moveForceReserve
                    ? result(PolicyOperation.hardVeto(
                        actionId,
                        TraceRuleId.of(
                            "OBJECTIVE.NO_MONEY.MOVE_PAYMENT_UNKNOWN"),
                        TraceDomainId.OBJECTIVE_INTENT,
                        TraceOutputKind.VETO,
                        "NO MONEY: cannot prove this deploy preserves the exact Mos Espa move payment"))
                    : empty();
        }
        int payment = Math.max(0, exactPayment);
        if (payment == 0
                || forceAvailable - payment >= moveForceReserve) {
            return empty();
        }
        return result(PolicyOperation.hardVeto(
                actionId,
                TraceRuleId.of(
                    "OBJECTIVE.NO_MONEY.MOVE_FORCE_RESERVE"),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.VETO,
                "NO MONEY: preserve the exact landspeed payment that occupies Mos Espa"));
    }

    public static boolean isExactOpponentWattoRemovalAction(
            com.gempukku.swccgo.game.SwccgGame game,
            String playerId,
            com.gempukku.swccgo.game.PhysicalCard source,
            String actionText) {
        if (game == null || game.getGameState() == null
                || playerId == null || source == null || actionText == null
                || source.getZone() == null
                || !source.getZone().isInPlay()
                || playerId.equals(source.getOwner())
                || !"Place Watto in Used Pile".equals(actionText)) {
            return false;
        }
        return "12_180_BACK".equals(
                source.getBlueprintId(
                    game.getGameState(), false));
    }

    private static PolicyOperation add(
            String actionId, String rule,
            TraceOutputKind kind, float delta, String reason) {
        return PolicyOperation.add(
                actionId,
                TraceRuleId.of(rule),
                TraceDomainId.OBJECTIVE_INTENT,
                kind, delta, reason);
    }

    private static PolicyResult result(PolicyOperation operation) {
        return new PolicyResult(
                "NO_MONEY_NO_PARTS_OBJECTIVE_POLICY",
                List.of(operation));
    }

    private static PolicyResult empty() {
        return new PolicyResult(
                "NO_MONEY_NO_PARTS_OBJECTIVE_POLICY",
                List.of());
    }
}
