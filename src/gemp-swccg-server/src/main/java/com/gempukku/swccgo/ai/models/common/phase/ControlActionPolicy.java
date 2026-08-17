package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.List;
import java.util.Objects;

/** Shared CONTROL policy arms offered outside the stock force-drain action. */
public final class ControlActionPolicy {
    private ControlActionPolicy() {
    }

    public static PolicyResult noEscapeRetrieval(String actionId) {
        return one(actionId, "V29.14-noescape-retrieval", TraceOutputKind.BANDED,
                200.0f,
                "V29.14 NO ESCAPE: Free card from Lost Pile — always take it!");
    }

    public static PolicyResult forceDrainModifier(String actionId) {
        return one(actionId, "V24.2-drain", TraceOutputKind.BANDED,
                80.0f,
                "V24.2 FORCE DRAIN BONUS: +1 to force drain — always use!");
    }

    public static PolicyResult selfCancelDrain(String actionId, String reason) {
        Objects.requireNonNull(reason, "reason");
        return one(actionId, "V52-self-cancel", TraceOutputKind.VETO,
                -9999.0f, reason);
    }

    public static PolicyResult revealOpponentHand(String actionId,
                                                  int opponentHandSize) {
        return opponentHandSize > 6
                ? one(actionId, "CONTROL-reveal-opponent-hand",
                TraceOutputKind.ORDERING, 50.0f,
                "Opponent has many cards - reveal worth it")
                : one(actionId, "CONTROL-reveal-opponent-hand",
                TraceOutputKind.ORDERING, -50.0f,
                "Opponent has few cards - save reveal");
    }

    public static PolicyResult makeOpponentLose(String actionId) {
        return one(actionId, "CONTROL-make-opponent-lose",
                TraceOutputKind.ORDERING, 30.0f,
                "Making opponent lose force");
    }

    public static PolicyResult retrieve(String actionId, int lostPileSize) {
        return retrieve(actionId, lostPileSize, false);
    }

    public static PolicyResult retrieve(String actionId, int lostPileSize,
                                        boolean exactIsbAgentRetrieval,
                                        boolean exactEndorBikerRetrieval) {
        return retrieve(actionId, lostPileSize, exactIsbAgentRetrieval,
                exactEndorBikerRetrieval, false, true);
    }

    public static PolicyResult retrieve(String actionId, int lostPileSize,
                                        boolean exactIsbAgentRetrieval,
                                        boolean exactEndorBikerRetrieval,
                                        boolean exactWoklingSacrifice,
                                        boolean allOriginalDeckLocationsInPlay) {
        if (exactWoklingSacrifice && !allOriginalDeckLocationsInPlay) {
            return new PolicyResult("CONTROL_ACTION_POLICY", List.of(
                    PolicyOperation.hardVeto(
                            actionId,
                            TraceRuleId.of("V53d-wokling-location-ramp"),
                            TraceDomainId.FORCE_BUDGET,
                            TraceOutputKind.VETO,
                            "V53d WOKLING HOLD: preserve +1 Force generation until every original deck location is deployed")));
        }
        // 2026-08-07 (m01675): Endor back-side free biker-scout retrieval joins the ISB
        // carve-out — same BANDED magnitude, reused not rebalanced.
        if (exactEndorBikerRetrieval) {
            return objectiveOne(actionId, "OBJECTIVE.ENDOR.RETRIEVE_BIKER",
                    TraceOutputKind.BANDED, 300.0f,
                    "ENDOR OBJECTIVE: Free biker scout retrieval feeds drain protection");
        }
        return retrieve(actionId, lostPileSize, exactIsbAgentRetrieval);
    }

    public static PolicyResult retrieve(String actionId, int lostPileSize,
                                        boolean exactIsbAgentRetrieval) {
        if (exactIsbAgentRetrieval) {
            return objectiveOne(actionId, "OBJECTIVE.ISB.RETRIEVE_AGENT",
                    TraceOutputKind.BANDED, 300.0f,
                    "ISB OBJECTIVE: Free native agent retrieval before drawing");
        }
        return lostPileSize > 15
                ? one(actionId, "CONTROL-retrieve",
                TraceOutputKind.ORDERING, 30.0f,
                "High lost pile - retrieve worth it")
                : one(actionId, "CONTROL-retrieve",
                TraceOutputKind.ORDERING, -30.0f,
                "Low lost pile - save retrieve");
    }

    public static PolicyResult peekAtTop(String actionId) {
        return one(actionId, "CONTROL-peek-at-top",
                TraceOutputKind.ORDERING, 30.0f,
                "Peek for card advantage");
    }

    public static PolicyResult steal(String actionId) {
        return one(actionId, "CONTROL-steal",
                TraceOutputKind.ORDERING, 30.0f,
                "Stealing is good");
    }

    public static PolicyResult dangerousCard(String actionId) {
        return one(actionId, "CONTROL-dangerous-card",
                TraceOutputKind.ORDERING, -50.0f,
                "Known dangerous card");
    }

    private static PolicyResult one(String actionId, String ruleId,
                                    TraceOutputKind outputKind, float delta,
                                    String reason) {
        return new PolicyResult("CONTROL_ACTION_POLICY", List.of(
                PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                        TraceDomainId.DRAIN_CONTROL, outputKind, delta, reason)));
    }

    private static PolicyResult objectiveOne(
            String actionId, String ruleId, TraceOutputKind outputKind,
            float delta, String reason) {
        return new PolicyResult("CONTROL_ACTION_POLICY", List.of(
                PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                        TraceDomainId.OBJECTIVE_INTENT,
                        outputKind, delta, reason)));
    }
}
