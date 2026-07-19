package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import com.gempukku.swccgo.common.Phase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Shared additive Pass scoring for Rando and Chosen One. */
public final class PassPolicy {

    public static final float BASE_SCORE = 5.0f;
    public static final String BASE_REASON = "Default pass option";
    public static final TraceRuleId BASE_RULE_ID = TraceRuleId.of("PASS-baseline");
    public static final TraceDomainId BASE_DOMAIN_ID = TraceDomainId.PASS_CANCEL;
    public static final TraceOutputKind BASE_OUTPUT_KIND = TraceOutputKind.BANDED;

    private static final String PRODUCER = "PASS_POLICY";

    public record Facts(
            String actionId,
            int turnNumber,
            Phase phase,
            boolean activateDecision,
            boolean drawDecision,
            boolean controlDecision,
            boolean initiateBattleDecision,
            boolean battlePhaseAction,
            boolean followthroughDecision,
            boolean hasGameState,
            int forcePileSize,
            int reserveDeckSize,
            int handSize,
            boolean drawTheirFireActive,
            int maintenanceObligation) {
        public Facts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    private PassPolicy() {
    }

    public static PolicyResult evaluate(Facts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();

        float earlyGameMultiplier = facts.turnNumber() <= 3 ? 0.5f : 1.0f;
        if (facts.turnNumber() <= 3) {
            add(operations, facts.actionId(), "PASS-early-game", -3.0f,
                    "Early game - reduced pass preference");
        }

        if (facts.initiateBattleDecision() || facts.battlePhaseAction()) {
            add(operations, facts.actionId(), "PASS-battle-action", -10.0f,
                    "Battle phase - should fight, not pass");
            return result(operations);
        }

        if (facts.followthroughDecision()) {
            add(operations, facts.actionId(), "PASS-follow-through", -15.0f,
                    "Already committed to action - follow through");
            return result(operations);
        }

        if (!facts.hasGameState()) {
            return result(operations);
        }

        if (facts.forcePileSize() < 3
                && !facts.activateDecision()
                && !facts.drawDecision()
                && !facts.controlDecision()) {
            add(operations, facts.actionId(), "PASS-low-force",
                    2.0f * earlyGameMultiplier,
                    "Low on Force - prefer to pass");
        }

        if (facts.reserveDeckSize() < 10 && !facts.controlDecision()) {
            add(operations, facts.actionId(), "PASS-low-reserve",
                    3.0f * earlyGameMultiplier,
                    "Reserve deck low - conserve cards");
        }

        if (!facts.activateDecision() && !facts.drawDecision()) {
            if (facts.handSize() < 5) {
                add(operations, facts.actionId(), "PASS-small-hand",
                        8.0f * earlyGameMultiplier,
                        "Small hand (" + facts.handSize() + ") - save force for drawing");
            } else if (facts.handSize() < 7) {
                add(operations, facts.actionId(), "PASS-below-target",
                        4.0f * earlyGameMultiplier,
                        "Hand below target (" + facts.handSize() + "/7) - conserve force");
            } else if (facts.handSize() >= 10 && facts.phase() == Phase.DEPLOY) {
                float bloatPenalty = -50.0f - (facts.handSize() - 10) * 20.0f;
                if (facts.forcePileSize() >= 8) {
                    bloatPenalty -= 100.0f;
                }
                add(operations, facts.actionId(), "V37.4-pass",
                        TraceDomainId.PASS_CANCEL, bloatPenalty,
                        String.format(
                                "V37.4 HAND BLOAT: %d cards in hand, %d Force — DEPLOY SOMETHING!",
                                facts.handSize(), facts.forcePileSize()));
            }
        }

        if (facts.phase() == Phase.MOVE
                && facts.forcePileSize() <= 4
                && facts.handSize() < 7) {
            add(operations, facts.actionId(), "PASS-move-draw",
                    10.0f * earlyGameMultiplier,
                    "Move phase + low force + small hand - pass to draw");
        }

        if (!facts.activateDecision()
                && !facts.initiateBattleDecision()
                && facts.drawTheirFireActive()) {
            int reserveNeeded = 3;
            if (facts.forcePileSize() <= reserveNeeded) {
                float bonus = 20.0f;
                if (facts.forcePileSize() <= 1) {
                    bonus = 40.0f;
                }
                if (facts.forcePileSize() == 0) {
                    bonus = 60.0f;
                }
                add(operations, facts.actionId(), "V27.1-pass-DTF",
                        TraceDomainId.FORCE_BUDGET, bonus,
                        String.format(
                                "V27.1 DTF RESERVE: Draw Their Fire on table! Need %d Force for battle interrupts, only %d left — CONSERVE!",
                                reserveNeeded, facts.forcePileSize()));
            }
        }

        if (!facts.activateDecision()
                && facts.maintenanceObligation() > 0
                && facts.forcePileSize() <= facts.maintenanceObligation() + 1) {
            float bonus = facts.forcePileSize() < facts.maintenanceObligation()
                    ? 50.0f : 25.0f;
            add(operations, facts.actionId(), "V27-maintenance-pass",
                    TraceDomainId.FORCE_BUDGET, bonus,
                    String.format(
                            "V27 MAINTENANCE RESERVE: Need %d Force for maintenance, only %d in pile — CONSERVE!",
                            facts.maintenanceObligation(), facts.forcePileSize()));
        }

        return result(operations);
    }

    private static PolicyResult result(List<PolicyOperation> operations) {
        return new PolicyResult(PRODUCER, operations);
    }

    private static void add(List<PolicyOperation> operations, String actionId,
                            String ruleId, float delta, String reason) {
        add(operations, actionId, ruleId, TraceDomainId.PASS_CANCEL,
                delta, reason);
    }

    private static void add(List<PolicyOperation> operations, String actionId,
                            String ruleId, TraceDomainId domainId,
                            float delta, String reason) {
        operations.add(PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                domainId, TraceOutputKind.BANDED, delta, reason));
    }
}
