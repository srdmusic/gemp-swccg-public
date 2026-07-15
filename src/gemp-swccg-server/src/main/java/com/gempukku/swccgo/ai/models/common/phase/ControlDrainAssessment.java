package com.gempukku.swccgo.ai.models.common.phase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Ordered, pure CONTROL force-drain policy over lazily collected fact slices. */
public final class ControlDrainAssessment {

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

    public record Operation(String ruleId, String detail, float delta, boolean terminal) {
        public Operation {
            Objects.requireNonNull(ruleId, "ruleId");
            Objects.requireNonNull(detail, "detail");
        }
    }

    public record Result(List<Operation> operations) {
        public Result {
            operations = List.copyOf(operations);
        }
    }

    private ControlDrainAssessment() {
    }

    public static Result assess(Facts facts) {
        Objects.requireNonNull(facts, "facts");
        List<Operation> operations = new ArrayList<>();

        Primary primary = facts.primary();
        if (primary != null) {
            if (primary.drainAmount() <= 0f) {
                return terminal(operations, "V24.15",
                    "V24.15 DRAIN BLOCK: Force drain would be 0 - pointless and opens us to Surprise Assault!",
                    -9999.0f);
            }
            if (primary.initiateCost() > primary.drainAmount()) {
                if (primary.initiateCost() - primary.drainAmount() >= 2.0f) {
                    return terminal(operations, "V189", String.format(
                        "V189 DRAIN NET-VALUE BLOCK: initiate cost %.0f > drain %.0f at %s - net <= -2, never worth it",
                        primary.initiateCost(), primary.drainAmount(), primary.locationTitle()), -2000.0f);
                }
                if (primary.forcePile() - primary.initiateCost()
                        < primary.plannedDeploySpend() + primary.moveAllowance()) {
                    return terminal(operations, "V189", String.format(
                        "V189 DRAIN NET-VALUE BLOCK: net -1 but budget fails - %d Force - %.0f cost < %d planned deploys + %d move allowance at %s",
                        primary.forcePile(), primary.initiateCost(), primary.plannedDeploySpend(),
                        primary.moveAllowance(), primary.locationTitle()), -2000.0f);
                }
            }
        }

        if (facts.simpleTricksBlocks()) {
            return terminal(operations, "V25",
                "V25 SIMPLE TRICKS: Non-battleground drain will be CANCELLED by Simple Tricks And Nonsense!",
                -9999.0f);
        }

        Economy economy = facts.economy();
        boolean suppressTurnLogic = false;
        if (economy.underBattleOrder()) {
            final int battleOrderCost = 3;
            if (economy.forceAvailable() < battleOrderCost) {
                return terminal(operations, "BATTLE_ORDER",
                    "Under Battle Order but can't afford drain (need " + battleOrderCost
                        + ", have " + economy.forceAvailable() + ")", -50.0f);
            }
            if (economy.hasDeployableCard()
                    && economy.cheapestDeployCost() < Integer.MAX_VALUE
                    && economy.forceAvailable() - battleOrderCost
                        < economy.cheapestDeployCost()) {
                return terminal(operations, "BATTLE_ORDER",
                    "Under Battle Order - saving force for deploy (cost "
                        + economy.cheapestDeployCost() + ")", -50.0f);
            }
            if (!economy.hasDeployableCard()) {
                return terminal(operations, "BATTLE_ORDER",
                    "Under Battle Order but NO deployable cards - drain is our only pressure!", 70.0f);
            }
            if (facts.battleOrderCostWaived()) {
                return terminal(operations, "V140",
                    "V140 BATTLE ORDER COST WAIVED: engine initiate-cost is 0 - drain is FREE!", 60.0f);
            }

            DrainValue drainValue = facts.battleOrderDrainValue();
            if (drainValue != null && drainValue.amount() <= 1.0f) {
                operations.add(new Operation("V104", String.format(
                    "V104 BATTLE ORDER + DRAIN <= 1: drain %.0f at %s, pay 3 = net %.0f - hard block",
                    drainValue.amount(), drainValue.locationTitle(), drainValue.amount() - 3.0f),
                    -2000.0f, false));
                suppressTurnLogic = true;
            }
            if (!suppressTurnLogic) {
                if (economy.turnNumber() >= 3) {
                    operations.add(new Operation("V52",
                        "V52 DRAIN ANYWAY: Turn " + economy.turnNumber()
                            + " - any drain is damage, pay the Battle Order cost!",
                        50.0f, false));
                } else {
                    operations.add(new Operation("V48",
                        "V48 BATTLE ORDER EARLY: Turn " + economy.turnNumber()
                            + " - save force for deploys",
                        -50.0f, false));
                }
            }
        } else if (!economy.hasDeployableCard()) {
            operations.add(new Operation("CONTROL_BASE",
                "Force drain (no deployable cards - our only pressure!)", 70.0f, false));
        } else {
            operations.add(new Operation("CONTROL_BASE", "Force drain is good", 50.0f, false));
        }

        MultiDrain multi = facts.multiDrain();
        if (multi != null) {
            if (multi.thisDrainAmount() >= 3.0f) {
                operations.add(new Operation("V52", "V52 MULTI-DRAIN: Drain "
                    + (int) multi.thisDrainAmount() + " - top priority drain site!", 300.0f, false));
            } else if (multi.thisDrainAmount() >= 2.0f) {
                operations.add(new Operation("V52", "V52 MULTI-DRAIN: Drain "
                    + (int) multi.thisDrainAmount() + " - high value drain!", 200.0f, false));
            } else if (multi.drainCapableSites() >= 2) {
                operations.add(new Operation("V52", "V52 MULTI-DRAIN: "
                    + multi.drainCapableSites() + " drain sites - drain everywhere!", 100.0f, false));
            }
        }

        HuntDown huntDown = facts.huntDown();
        if (huntDown.active()) {
            if (huntDown.opponentIcons() >= 2) {
                operations.add(new Operation("V29.9",
                    "V29.9 HUNT DOWN DRAIN: High-value drain location ("
                        + huntDown.opponentIcons() + " opponent icons)!", 40.0f, false));
            }
            operations.add(new Operation("V29.9",
                "V29.9 HUNT DOWN: Force drains are critical - Visage adds +1, keep pressure on!",
                30.0f, false));
        }

        return new Result(operations);
    }

    private static Result terminal(List<Operation> operations,
                                   String ruleId,
                                   String detail,
                                   float delta) {
        operations.add(new Operation(ruleId, detail, delta, true));
        return new Result(operations);
    }
}
