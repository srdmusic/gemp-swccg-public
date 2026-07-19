package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Shared owner of MOVE Force-reserve, maintenance-conservation, and
 * transport-action feasibility scores.
 */
public final class MoveForceEconomyPolicy {
    public enum Mode {
        NONE,
        HARD_RESERVE,
        LOW_RESERVE,
        MAINTENANCE_CONSERVE
    }

    public record Evaluation(PolicyResult result, Mode mode, int reserveNeeded) {
    }

    public record ActionGate(boolean applies, String reason, float delta) {
        private static ActionGate none() {
            return new ActionGate(false, null, 0.0f);
        }
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

    public static boolean isOdinNesloorAction(
            String sourceTitle, String actionLower) {
        if (sourceTitle == null || actionLower == null) {
            return false;
        }

        String titleLower = sourceTitle.toLowerCase(Locale.ROOT);
        boolean actionMatches = actionLower.contains("odin nesloor")
                || actionLower.contains("transport")
                || actionLower.contains("relocate");
        return titleLower.contains("odin nesloor") && actionMatches;
    }

    public static ActionGate odinNesloorFloor(int forcePile) {
        if (forcePile < 5) {
            return new ActionGate(
                    true,
                    "V134 ODIN NESLOOR FLOOR: only " + forcePile
                            + " force in pile (need 5+) — hold the interrupt (LADDER VETO)",
                    -100000.0f);
        }
        return ActionGate.none();
    }

    public static boolean isNamedTransportInterrupt(String sourceTitle) {
        if (sourceTitle == null) {
            return false;
        }
        String titleLower = sourceTitle.toLowerCase(Locale.ROOT);
        return titleLower.contains("elis helrot")
                || titleLower.contains("nabrun leids");
    }

    public static ActionGate transportInterruptFloor(
            int forcePile, int reserveDeckSize) {
        if (forcePile >= 4 && reserveDeckSize >= 1) {
            return ActionGate.none();
        }

        String why = forcePile < 4
                ? "only " + forcePile
                        + " force in pile (need 4+ to cover destiny draw)"
                : "reserve deck empty — cannot draw destiny";
        return new ActionGate(
                true,
                "V141 TRANSPORT INTERRUPT BLOCK: " + why
                        + " — hold the interrupt",
                -2000.0f);
    }

    public static boolean isTransportInterruptAction(
            String sourceTitle, String sourceGameText, String actionLower) {
        if (sourceTitle == null || actionLower == null) {
            return false;
        }

        String titleLower = sourceTitle.toLowerCase(Locale.ROOT);
        boolean transportInterrupt = isNamedTransportInterrupt(sourceTitle);
        if (!transportInterrupt && sourceGameText != null) {
            String gameTextLower = sourceGameText.toLowerCase(Locale.ROOT);
            transportInterrupt = gameTextLower.contains("'transport'")
                    && gameTextLower.contains("draw destiny")
                    && gameTextLower.contains("place interrupt in lost pile");
        }

        return transportInterrupt
                && (actionLower.contains(titleLower)
                    || actionLower.contains("transport")
                    || actionLower.contains("relocate"));
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
