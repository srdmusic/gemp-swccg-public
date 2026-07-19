package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.List;
import java.util.Objects;

/** Shared owner of MOVE Force-reserve and maintenance-conservation scores. */
public final class MoveForceEconomyPolicy {
    public enum Mode {
        NONE,
        HARD_RESERVE,
        LOW_RESERVE,
        MAINTENANCE_CONSERVE
    }

    public record Evaluation(PolicyResult result, Mode mode, int reserveNeeded) {
    }

    private MoveForceEconomyPolicy() {
    }

    public static Evaluation reserve(String actionId, int forcePile,
                                     boolean dtfActive,
                                     boolean grabberNeedsForce,
                                     boolean hasCriticalInterrupt) {
        Objects.requireNonNull(actionId, "actionId");
        int reserveNeeded = (dtfActive ? 1 : 0) + (grabberNeedsForce ? 1 : 0);
        if (reserveNeeded > 0 && forcePile <= reserveNeeded) {
            float penalty = hasCriticalInterrupt ? -150.0f : -100.0f;
            return one("MOVE_FORCE_RESERVE_POLICY", actionId,
                    "V29-move-reserve", TraceDomainId.MOVE, penalty,
                    String.format(
                            "V29 FORCE RESERVE: Only %d Force, need %d (DTF=%s, grabber=%s) — save Force!",
                            forcePile, reserveNeeded, dtfActive, grabberNeedsForce),
                    Mode.HARD_RESERVE, reserveNeeded);
        }
        if (reserveNeeded > 0 && forcePile <= reserveNeeded + 1) {
            return one("MOVE_FORCE_RESERVE_POLICY", actionId,
                    "V29-move-reserve", TraceDomainId.MOVE, -60.0f,
                    "V29 FORCE RESERVE: Low Force — move cautiously",
                    Mode.LOW_RESERVE, reserveNeeded);
        }
        return none("MOVE_FORCE_RESERVE_POLICY", reserveNeeded);
    }

    public static Evaluation maintenance(String actionId, int maintenanceCost,
                                         int forcePile) {
        Objects.requireNonNull(actionId, "actionId");
        if (maintenanceCost > 0 && forcePile <= maintenanceCost + 1) {
            return one("MOVE_MAINTENANCE_POLICY", actionId,
                    "V27-maintenance-move", TraceDomainId.FORCE_BUDGET, -80.0f,
                    String.format(
                            "V27 MAINTENANCE: Need %d Force for upkeep, only %d left — DON'T waste Force moving!",
                            maintenanceCost, forcePile),
                    Mode.MAINTENANCE_CONSERVE, 0);
        }
        return none("MOVE_MAINTENANCE_POLICY", 0);
    }

    private static Evaluation one(String producerId, String actionId,
                                  String ruleId, TraceDomainId domainId,
                                  float delta, String reason, Mode mode,
                                  int reserveNeeded) {
        PolicyOperation operation = PolicyOperation.add(actionId,
                TraceRuleId.of(ruleId), domainId,
                TraceOutputKind.BANDED, delta, reason);
        return new Evaluation(new PolicyResult(producerId, List.of(operation)),
                mode, reserveNeeded);
    }

    private static Evaluation none(String producerId, int reserveNeeded) {
        return new Evaluation(new PolicyResult(producerId, List.of()),
                Mode.NONE, reserveNeeded);
    }
}
