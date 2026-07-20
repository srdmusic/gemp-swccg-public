package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Ordered, pure CONTROL force-drain policy over lazily collected fact slices. */
public final class ControlDrainAssessment {

    private static final int LEGACY_CONTROLLED_BATTLEGROUND_BONUS = 20;

    public interface Facts {
        Primary primary();
        boolean simpleTricksBlocks();
        Economy economy();
        boolean battleOrderCostWaived();
        DrainValue battleOrderDrainValue();
        MultiDrain multiDrain();
        HuntDown huntDown();
    }

    public record Primary(float drainAmount,
                          float initiateCost,
                          String locationTitle,
                          int forcePile,
                          int plannedDeploySpend,
                          int moveAllowance) {
    }

    public record Economy(boolean underBattleOrder,
                          int forceAvailable,
                          boolean hasDeployableCard,
                          int cheapestDeployCost,
                          int turnNumber) {
    }

    public record DrainValue(float amount, String locationTitle) {
    }

    public record MultiDrain(float thisDrainAmount,
                             int drainCapableSites,
                             String locationTitle) {
    }

    public record HuntDown(boolean active, int opponentIcons) {
    }

    private ControlDrainAssessment() {
    }

    /** Pure arithmetic for the top-level legacy CONTROL fallback. */
    public static int scoreLegacyFallback(
            int forceDrainScore, int controlledBattlegrounds) {
        int score = 0;
        score += forceDrainScore;
        if (controlledBattlegrounds > 0) {
            score += LEGACY_CONTROLLED_BATTLEGROUND_BONUS
                    * controlledBattlegrounds;
        }
        return score;
    }

    public static PolicyResult assess(String actionId, Facts facts) {
        Objects.requireNonNull(actionId, "actionId");
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();

        Primary primary = facts.primary();
        if (primary != null) {
            if (primary.drainAmount() <= 0f) {
                return terminal(operations, actionId, "V24.15-drain", TraceOutputKind.VETO,
                    "V24.15 DRAIN BLOCK: Force drain would be 0 - pointless and opens us to Surprise Assault!",
                    -9999.0f);
            }
            if (primary.initiateCost() > primary.drainAmount()) {
                if (primary.initiateCost() - primary.drainAmount() >= 2.0f) {
                    return terminal(operations, actionId, "V189", TraceOutputKind.VETO, String.format(
                        "V189 DRAIN NET-VALUE BLOCK: initiate cost %.0f > drain %.0f at %s - net <= -2, never worth it",
                        primary.initiateCost(), primary.drainAmount(), primary.locationTitle()), -2000.0f);
                }
                if (primary.forcePile() - primary.initiateCost()
                        < primary.plannedDeploySpend() + primary.moveAllowance()) {
                    return terminal(operations, actionId, "V189", TraceOutputKind.VETO, String.format(
                        "V189 DRAIN NET-VALUE BLOCK: net -1 but budget fails - %d Force - %.0f cost < %d planned deploys + %d move allowance at %s",
                        primary.forcePile(), primary.initiateCost(), primary.plannedDeploySpend(),
                        primary.moveAllowance(), primary.locationTitle()), -2000.0f);
                }
            }
        }

        if (facts.simpleTricksBlocks()) {
            return terminal(operations, actionId, "V25-SimpleTricks", TraceOutputKind.VETO,
                "V25 SIMPLE TRICKS: Non-battleground drain will be CANCELLED by Simple Tricks And Nonsense!",
                -9999.0f);
        }

        Economy economy = facts.economy();
        boolean suppressTurnLogic = false;
        if (economy.underBattleOrder()) {
            final int battleOrderCost = 3;
            if (economy.forceAvailable() < battleOrderCost) {
                return terminal(operations, actionId, "CONTROL-battle-order-afford",
                    TraceOutputKind.BANDED,
                    "Under Battle Order but can't afford drain (need " + battleOrderCost
                        + ", have " + economy.forceAvailable() + ")", -50.0f);
            }
            if (economy.hasDeployableCard()
                    && economy.cheapestDeployCost() < Integer.MAX_VALUE
                    && economy.forceAvailable() - battleOrderCost
                        < economy.cheapestDeployCost()) {
                return terminal(operations, actionId, "CONTROL-battle-order-save-deploy",
                    TraceOutputKind.BANDED,
                    "Under Battle Order - saving force for deploy (cost "
                        + economy.cheapestDeployCost() + ")", -50.0f);
            }
            if (!economy.hasDeployableCard()) {
                return terminal(operations, actionId, "CONTROL-battle-order-only-pressure",
                    TraceOutputKind.BANDED,
                    "Under Battle Order but NO deployable cards - drain is our only pressure!", 70.0f);
            }
            if (facts.battleOrderCostWaived()) {
                return terminal(operations, actionId, "V140", TraceOutputKind.ORDERING,
                    "V140 BATTLE ORDER COST WAIVED: engine initiate-cost is 0 - drain is FREE!", 60.0f);
            }

            DrainValue drainValue = facts.battleOrderDrainValue();
            if (drainValue != null && drainValue.amount() <= 1.0f) {
                add(operations, actionId, "V104", TraceOutputKind.VETO, String.format(
                    "V104 BATTLE ORDER + DRAIN <= 1: drain %.0f at %s, pay 3 = net %.0f - hard block",
                    drainValue.amount(), drainValue.locationTitle(), drainValue.amount() - 3.0f),
                    -2000.0f);
                suppressTurnLogic = true;
            }
            if (!suppressTurnLogic) {
                if (economy.turnNumber() >= 3) {
                    add(operations, actionId, "V52-drain-anyway", TraceOutputKind.BANDED,
                        "V52 DRAIN ANYWAY: Turn " + economy.turnNumber()
                            + " - any drain is damage, pay the Battle Order cost!",
                        50.0f);
                } else {
                    add(operations, actionId, "V48-drain", TraceOutputKind.BANDED,
                        "V48 BATTLE ORDER EARLY: Turn " + economy.turnNumber()
                            + " - save force for deploys",
                        -50.0f);
                }
            }
        } else if (!economy.hasDeployableCard()) {
            add(operations, actionId, "CONTROL-base-no-deploy", TraceOutputKind.BANDED,
                "Force drain (no deployable cards - our only pressure!)", 70.0f);
        } else {
            add(operations, actionId, "CONTROL-base-drain", TraceOutputKind.BANDED,
                "Force drain is good", 50.0f);
        }

        MultiDrain multi = facts.multiDrain();
        if (multi != null) {
            if (multi.thisDrainAmount() >= 3.0f) {
                add(operations, actionId, "V52-multi-drain", TraceOutputKind.BANDED,
                    "V52 MULTI-DRAIN: Drain " + (int) multi.thisDrainAmount()
                        + " - top priority drain site!", 300.0f);
            } else if (multi.thisDrainAmount() >= 2.0f) {
                add(operations, actionId, "V52-multi-drain", TraceOutputKind.BANDED,
                    "V52 MULTI-DRAIN: Drain " + (int) multi.thisDrainAmount()
                        + " - high value drain!", 200.0f);
            } else if (multi.drainCapableSites() >= 2) {
                add(operations, actionId, "V52-multi-drain", TraceOutputKind.BANDED,
                    "V52 MULTI-DRAIN: " + multi.drainCapableSites()
                        + " drain sites - drain everywhere!", 100.0f);
            }
        }

        HuntDown huntDown = facts.huntDown();
        if (huntDown.active()) {
            if (huntDown.opponentIcons() >= 2) {
                add(operations, actionId, "V29.9-HuntDown-high", TraceOutputKind.BANDED,
                    "V29.9 HUNT DOWN DRAIN: High-value drain location ("
                        + huntDown.opponentIcons() + " opponent icons)!", 40.0f);
            }
            add(operations, actionId, "V29.9-HuntDown-drain", TraceOutputKind.BANDED,
                "V29.9 HUNT DOWN: Force drains are critical - Visage adds +1, keep pressure on!",
                30.0f);
        }

        return result(operations);
    }

    private static void add(List<PolicyOperation> operations, String actionId,
                            String ruleId, TraceOutputKind outputKind,
                            String detail, float delta) {
        operations.add(PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                TraceDomainId.DRAIN_CONTROL, outputKind, delta, detail));
    }

    private static PolicyResult terminal(List<PolicyOperation> operations,
                                         String actionId,
                                         String ruleId,
                                         TraceOutputKind outputKind,
                                         String detail,
                                         float delta) {
        add(operations, actionId, ruleId, outputKind, detail, delta);
        return result(operations);
    }

    private static PolicyResult result(List<PolicyOperation> operations) {
        return new PolicyResult("CONTROL_DRAIN_POLICY", operations);
    }
}
