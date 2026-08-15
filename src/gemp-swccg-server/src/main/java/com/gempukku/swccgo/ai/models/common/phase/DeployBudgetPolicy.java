package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Shared owner of DEPLOY-1 Force budget and future-obligation scores. */
public final class DeployBudgetPolicy {
    public enum AdapterStep {
        FALL_THROUGH,
        CONTINUE_ACTION
    }

    public record Evaluation(PolicyResult result, AdapterStep adapterStep) {
    }

    private DeployBudgetPolicy() {
    }

    public static Evaluation newMaintenanceCard(NewMaintenanceFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        if (!facts.maintenanceCard()) {
            return evaluation("DEPLOY_NEW_MAINTENANCE_POLICY", operations,
                    AdapterStep.FALL_THROUGH);
        }

        int forceAfterDeploy = facts.totalForce() - facts.deployCost();
        int forceAfterAllDeploys = forceAfterDeploy - facts.pendingDeployCost()
                - facts.battleReserve();
        int safeBuffer = facts.maintenanceCost() + facts.drainBuffer();
        if (forceAfterDeploy < facts.maintenanceCost()) {
            operations.add(add(facts.actionId(), "V59", -2000.0f,
                    "V59 MAINTENANCE HARD: " + facts.cardTitle()
                            + " needs " + facts.maintenanceCost()
                            + "F upkeep but only " + forceAfterDeploy
                            + "F left \u2014 WILL die at end of turn!"));
        } else if (forceAfterAllDeploys < facts.maintenanceCost()) {
            operations.add(add(facts.actionId(), "V59", -1500.0f,
                    "V59 MAINTENANCE HOLISTIC: " + facts.cardTitle()
                            + " needs " + facts.maintenanceCost()
                            + "F but only " + forceAfterAllDeploys
                            + "F after all planned deploys + battle reserve \u2014 WILL be sacrificed!"));
        } else if (forceAfterAllDeploys < safeBuffer) {
            operations.add(add(facts.actionId(), "V64", -400.0f,
                    "V64 MAINTENANCE TIGHT: " + facts.cardTitle()
                            + " \u2014 " + forceAfterAllDeploys
                            + "F post-deploys, need " + facts.maintenanceCost()
                            + "+" + facts.drainBuffer()
                            + " drain buffer \u2014 likely sacrifice!"));
        }
        return evaluation("DEPLOY_NEW_MAINTENANCE_POLICY", operations,
                AdapterStep.FALL_THROUGH);
    }

    public static Evaluation existingMaintenance(String actionId, int totalForce,
                                                 int deployCost,
                                                 int existingMaintenanceCost) {
        List<PolicyOperation> operations = new ArrayList<>();
        if (deployCost > 0 && existingMaintenanceCost > 0) {
            int forceAfterDeploy = totalForce - deployCost;
            if (forceAfterDeploy < existingMaintenanceCost) {
                operations.add(add(actionId, "V24.5", -50.0f,
                        "V40 MAINTENANCE RESERVE: Deploying this leaves only "
                                + forceAfterDeploy + " Force but need "
                                + existingMaintenanceCost
                                + " for existing maintenance cards (mild caution)"));
            } else if (forceAfterDeploy < existingMaintenanceCost + 2) {
                operations.add(add(actionId, "V24.5", -50.0f,
                        "V40 MAINTENANCE RESERVE: Tight on Force for existing maintenance ("
                                + forceAfterDeploy + " left, need "
                                + existingMaintenanceCost + ") (mild caution)"));
            }
        }
        return evaluation("DEPLOY_EXISTING_MAINTENANCE_POLICY", operations,
                AdapterStep.FALL_THROUGH);
    }

    public static Evaluation affordability(String actionId, int deployCost,
                                           int availableForce) {
        List<PolicyOperation> operations = new ArrayList<>();
        if (deployCost > availableForce) {
            operations.add(add(actionId, "deploy-affordability", -1000.0f,
                    String.format("Can't afford! Need %d, have %d",
                            deployCost, availableForce)));
            return evaluation("DEPLOY_AFFORDABILITY_POLICY", operations,
                    AdapterStep.CONTINUE_ACTION);
        }
        return evaluation("DEPLOY_AFFORDABILITY_POLICY", operations,
                AdapterStep.FALL_THROUGH);
    }

    public static Evaluation futureObligations(FutureObligationFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        if (facts.deployCost() <= 0) {
            return evaluation("DEPLOY_FUTURE_OBLIGATION_POLICY", operations,
                    AdapterStep.FALL_THROUGH);
        }
        int forceAfterDeploy = facts.availableForce() - facts.deployCost();
        if (facts.objectiveFormationReserve() > 0
                && forceAfterDeploy < facts.objectiveFormationReserve()) {
            operations.add(addObjective(facts.actionId(),
                    "DEPLOY.BUDGET.OBJECTIVE_FORMATION_RESERVE", -300.0f,
                    String.format("Off-plan deploy leaves %d Force; offered objective formation needs %d",
                            forceAfterDeploy, facts.objectiveFormationReserve())));
        }
        if (facts.objectiveRequiredCardReserve() > 0
                && forceAfterDeploy
                    < facts.objectiveRequiredCardReserve()) {
            operations.add(addObjective(facts.actionId(),
                    "DEPLOY.BUDGET.OBJECTIVE_REQUIRED_CARD_RESERVE",
                    -300.0f,
                    String.format("Off-plan deploy leaves %d Force; a missing required objective card needs %d",
                            forceAfterDeploy,
                            facts.objectiveRequiredCardReserve())));
        }
        if (facts.captureMoveForceReserve() > 0
                && forceAfterDeploy
                    < facts.captureMoveForceReserve()) {
            operations.add(addObjective(
                    facts.actionId(),
                    "DEPLOY.BUDGET.CAPTURE_MOVE_RESERVE",
                    -300.0f,
                    String.format(
                        "Off-plan deploy leaves %d Force; the exact capture move needs %d",
                        forceAfterDeploy,
                        facts.captureMoveForceReserve())));
        }
        if (facts.vaderMoveReserve() > 0
                && forceAfterDeploy < facts.vaderMoveReserve()) {
            operations.add(addObjective(facts.actionId(), "V48", -300.0f,
                    String.format("V48 VADER MOVE RESERVE: Deploy costs %d, leaves %d \u2014 need %d for Vader to move!",
                            facts.deployCost(), forceAfterDeploy, facts.vaderMoveReserve())));
        }
        if (facts.hiddenPathTransitReserve() > 0
                && forceAfterDeploy < facts.hiddenPathTransitReserve()) {
            operations.add(addObjective(facts.actionId(), "V67z", -300.0f,
                    String.format("V67z TRANSIT RESERVE: Deploy costs %d, leaves %d \u2014 need %d to transit Jedi off Mapuzo (flip the objective)!",
                            facts.deployCost(), forceAfterDeploy,
                            facts.hiddenPathTransitReserve())));
        }
        if (facts.vergeMoveReserve() > 0
                && forceAfterDeploy < facts.vergeMoveReserve()) {
            operations.add(addObjective(facts.actionId(), "V79", -300.0f,
                    String.format("V79 VERGE MOVE RESERVE: Deploy costs %d, leaves %d \u2014 need %d for Death Star to move toward Scarif!",
                            facts.deployCost(), forceAfterDeploy, facts.vergeMoveReserve())));
        }
        if (facts.maintenanceCard()
                && forceAfterDeploy < facts.maintenanceCost()) {
            float penalty = forceAfterDeploy <= 0 ? -500.0f : -50.0f;
            operations.add(add(facts.actionId(), "V29.13-maintenance", penalty,
                    String.format("V29.13 MAINT AWARENESS: This card costs %d maint at end of turn, only %d Force left after deploy \u2014 plan to activate more next turn",
                            facts.maintenanceCost(), forceAfterDeploy)));
        }
        if ((facts.dtfActive() || facts.grabberUnused()) && forceAfterDeploy <= 0) {
            String tools = facts.dtfActive() ? "DTF active" : "";
            if (facts.grabberUnused()) {
                tools += facts.dtfActive() ? " + grabber ready" : "Grabber ready";
            }
            operations.add(add(facts.actionId(), "V29.13-interrupt", -30.0f,
                    "V29.13 INTERRUPT RESERVE: " + tools
                            + " but 0 Force left for them after deploy"));
        }
        return evaluation("DEPLOY_FUTURE_OBLIGATION_POLICY", operations,
                AdapterStep.FALL_THROUGH);
    }

    public record NewMaintenanceFacts(String actionId, String cardTitle,
                                      boolean maintenanceCard,
                                      int totalForce, int deployCost,
                                      int maintenanceCost,
                                      int pendingDeployCost,
                                      int battleReserve, int drainBuffer) {
        public NewMaintenanceFacts {
            Objects.requireNonNull(actionId, "actionId");
            cardTitle = cardTitle == null ? "" : cardTitle;
        }
    }

    public record FutureObligationFacts(String actionId, int availableForce,
                                        int deployCost, int vaderMoveReserve,
                                        int hiddenPathTransitReserve,
                                        int vergeMoveReserve,
                                        boolean maintenanceCard,
                                        int maintenanceCost,
                                        boolean dtfActive,
                                        boolean grabberUnused,
                                        int objectiveFormationReserve,
                                        int objectiveRequiredCardReserve,
                                        int captureMoveForceReserve) {
        public FutureObligationFacts {
            Objects.requireNonNull(actionId, "actionId");
        }

        public FutureObligationFacts(
                String actionId, int availableForce,
                int deployCost, int vaderMoveReserve,
                int hiddenPathTransitReserve,
                int vergeMoveReserve,
                boolean maintenanceCard,
                int maintenanceCost,
                boolean dtfActive,
                boolean grabberUnused,
                int objectiveFormationReserve,
                int objectiveRequiredCardReserve) {
            this(actionId, availableForce, deployCost,
                    vaderMoveReserve, hiddenPathTransitReserve,
                    vergeMoveReserve, maintenanceCard,
                    maintenanceCost, dtfActive, grabberUnused,
                    objectiveFormationReserve,
                    objectiveRequiredCardReserve, 0);
        }

        public FutureObligationFacts(
                String actionId, int availableForce,
                int deployCost, int vaderMoveReserve,
                int hiddenPathTransitReserve,
                int vergeMoveReserve,
                boolean maintenanceCard,
                int maintenanceCost,
                boolean dtfActive,
                boolean grabberUnused,
                int objectiveFormationReserve) {
            this(actionId, availableForce, deployCost,
                    vaderMoveReserve, hiddenPathTransitReserve,
                    vergeMoveReserve, maintenanceCard,
                    maintenanceCost, dtfActive, grabberUnused,
                    objectiveFormationReserve, 0, 0);
        }
    }

    private static PolicyOperation add(String actionId, String rule,
                                       float delta, String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(rule),
                TraceDomainId.DEPLOY_SEQUENCING,
                TraceOutputKind.VETO, delta, reason);
    }

    private static PolicyOperation addObjective(
            String actionId, String rule, float delta, String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(rule),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED, delta, reason);
    }

    private static Evaluation evaluation(String producer,
                                         List<PolicyOperation> operations,
                                         AdapterStep step) {
        return new Evaluation(new PolicyResult(producer, operations), step);
    }
}
