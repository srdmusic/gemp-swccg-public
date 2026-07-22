package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Shared DEPLOY-1 owner for applying the current phase plan to one deploy candidate. */
public final class DeployPlanPolicy {
    public enum AdapterStep {
        FALL_THROUGH,
        CONTINUE_ACTION
    }

    public record Evaluation(PolicyResult result, AdapterStep adapterStep) {
    }

    private DeployPlanPolicy() {
    }

    public static Evaluation evaluate(Facts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        AdapterStep step = AdapterStep.FALL_THROUGH;
        if (!facts.hasPlan()) {
            return evaluation(operations, step);
        }

        if (facts.hasPendingInstructions()) {
            if (facts.plannedCard()) {
                operations.add(add(facts.actionId(), "deploy-plan-membership", 100.0f,
                        "IN DEPLOYMENT PLAN: " + facts.strategyValue()));
                if (facts.instructionPriority() <= 1) {
                    operations.add(add(facts.actionId(), "deploy-plan-priority", 50.0f,
                            "Highest priority deployment"));
                } else if (facts.instructionPriority() <= 3) {
                    operations.add(add(facts.actionId(), "deploy-plan-priority", 25.0f,
                            "High priority deployment"));
                }
                if (facts.objectiveFormationPlan()) {
                    operations.add(add(facts.actionId(),
                            "DEPLOY.FORMATION.OBJECTIVE_TIE_BREAK", 25.0f,
                            "Objective formation plan wins deploy ties within 25 points"));
                }
            } else if (!facts.forceAllowExtras()) {
                if (facts.locationStrategy()) {
                    if (facts.locationCard()) {
                        operations.add(add(facts.actionId(), "V24.10", 100.0f,
                                "V24.10: Location not in plan but DEPLOY_LOCATIONS allows all locations!"));
                    } else if (facts.turnNumber() >= 2) {
                        operations.add(add(facts.actionId(), "V40-plan-turn-release", 0.0f,
                                "V40: DEPLOY_LOCATIONS incomplete but turn "
                                        + facts.turnNumber() + " \u2014 deploy freely!"));
                    } else if (facts.tdigwatt()) {
                        operations.add(add(facts.actionId(), "V40-plan-location-first", -1000.0f,
                                "BLOCKED: Plan is DEPLOY_LOCATIONS ONLY (turn 1, TDIGWATT) - deploy locations first!"));
                        step = AdapterStep.CONTINUE_ACTION;
                    } else {
                        operations.add(add(facts.actionId(), "V40-plan-non-tdigwatt", 0.0f,
                                "V40: Not TDIGWATT \u2014 deploy freely on turn 1!"));
                    }
                } else if (facts.waitingForPlannedCards()) {
                    if (facts.availableForce() < 8) {
                        operations.add(add(facts.actionId(), "V40-plan-save-force", 0.0f,
                                "V40: Saving force for planned cards (neutral)"));
                        step = AdapterStep.CONTINUE_ACTION;
                    } else {
                        operations.add(add(facts.actionId(), "V40-plan-force-surplus", 0.0f,
                                "V40: Plenty of Force \u2014 deploy off-plan!"));
                    }
                } else {
                    operations.add(add(facts.actionId(), "V40-plan-off-plan", 0.0f,
                            "V40: Not in deployment plan (neutral)"));
                }
            } else if (facts.locationStrategy()) {
                operations.add(add(facts.actionId(), "V29.7-plan-stale", 10.0f,
                        "V29.7: Stale plan \u2014 deploy characters now!"));
            } else {
                operations.add(add(facts.actionId(), "deploy-plan-extra-stale", 0.0f,
                        "Extra deploy (plan stale)"));
            }
        } else if (facts.planComplete()) {
            if (facts.locationStrategy()) {
                operations.add(add(facts.actionId(), "deploy-plan-locations-complete", 25.0f,
                        "DEPLOY_LOCATIONS complete - extra deploy allowed"));
            }
            if (facts.extraForceBudget() > 0) {
                operations.add(add(facts.actionId(), "deploy-plan-extra-budget", 25.0f,
                        "Plan COMPLETE - extra deploy allowed"));
            } else {
                operations.add(add(facts.actionId(), "V40-plan-complete", 0.0f,
                        "V40: Plan complete \u2014 deploy freely!"));
            }
        }

        if (step == AdapterStep.CONTINUE_ACTION) {
            return evaluation(operations, step);
        }
        if (facts.holdBackCard()) {
            operations.add(add(facts.actionId(), "V40-plan-hold-back", 0.0f,
                    "V40: Hold-back card (neutral)"));
        }
        return evaluation(operations, step);
    }

    public static PolicyResult evaluateDestinationTarget(
            DestinationTargetFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(2);
        if (facts.plannedTarget()) {
            operations.add(add(facts.actionId(), "deploy-plan-target-match",
                    200.0f,
                    "PLANNED TARGET: " + facts.plannedTargetName()));
        } else {
            operations.add(add(facts.actionId(), "deploy-plan-target-other",
                    -100.0f,
                    "Not planned target (want "
                            + facts.plannedTargetName() + ")"));
            if (facts.plannedTargetOffered()) {
                operations.add(PolicyOperation.defer(
                        facts.actionId(), TraceRuleId.of("deploy-plan-target-defer"),
                        TraceDomainId.DEPLOY_SEQUENCING, TraceOutputKind.VETO,
                        0.0f,
                        "Exact planned target is offered: "
                                + facts.plannedTargetName()));
            }
        }
        return new PolicyResult("DEPLOY_PLAN_DESTINATION_POLICY", operations);
    }

    public record Facts(String actionId, boolean hasPlan,
                        boolean hasPendingInstructions, boolean plannedCard,
                        boolean objectiveFormationPlan,
                        int instructionPriority, boolean forceAllowExtras,
                        boolean waitingForPlannedCards, boolean locationStrategy,
                        boolean locationCard, boolean tdigwatt,
                        int turnNumber, int availableForce,
                        boolean planComplete, int extraForceBudget,
                        boolean holdBackCard, String strategyValue) {
        public Facts {
            Objects.requireNonNull(actionId, "actionId");
            strategyValue = strategyValue == null ? "" : strategyValue;
        }
    }

    public record DestinationTargetFacts(
            String actionId, boolean plannedTarget,
            boolean plannedTargetOffered, String plannedTargetName) {
        public DestinationTargetFacts {
            Objects.requireNonNull(actionId, "actionId");
            plannedTargetName = plannedTargetName == null
                    ? "null" : plannedTargetName;
        }
    }

    private static PolicyOperation add(String actionId, String rule,
                                       float delta, String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(rule),
                TraceDomainId.DEPLOY_SEQUENCING,
                TraceOutputKind.ORDERING, delta, reason);
    }

    private static Evaluation evaluation(List<PolicyOperation> operations,
                                         AdapterStep step) {
        return new Evaluation(new PolicyResult("DEPLOY_PLAN_POLICY", operations), step);
    }
}
