package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure owner of legacy early-location and Bespin-first DEPLOY sequencing. */
public final class DeployObjectiveSequencingPolicy {

    private static final String EARLY_PRODUCER =
            "DEPLOY_EARLY_LOCATION_POLICY";
    private static final String BESPIN_FIRST_PRODUCER =
            "DEPLOY_BESPIN_FIRST_POLICY";

    public enum AdapterStep {
        FALL_THROUGH,
        CONTINUE_ACTION
    }

    public enum BespinFirstRoute {
        EXEMPT,
        CANDIDATE
    }

    public enum BespinFirstOutcome {
        RELEASED,
        PENALIZED
    }

    public record EarlyLocationEvaluation(
            PolicyResult result,
            AdapterStep adapterStep,
            boolean piettPriorityApplied,
            float bespinBoost) {

        public EarlyLocationEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(adapterStep, "adapterStep");
        }
    }

    public record BespinFirstEvaluation(
            PolicyResult result,
            BespinFirstOutcome outcome,
            String releaseReason) {

        public BespinFirstEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    private DeployObjectiveSequencingPolicy() {
    }

    public static EarlyLocationEvaluation evaluateEarlyLocation(
            DeployObjectiveSequencingFacts.EarlyLocation facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        operations.add(add(facts.actionId(), "deploy-location-text-priority",
                TraceOutputKind.ORDERING, 200.0f,
                "LOCATION - deploy first!"));

        boolean piettPriority = facts.piettOracleAnalyzed()
                && !facts.piettAccessible()
                && !facts.piettLost()
                && facts.piettTurnNumber() <= 4;
        if (piettPriority) {
            operations.add(add(facts.actionId(), "V24.10-piett-location-priority",
                    TraceOutputKind.ORDERING, 150.0f,
                    "V24.10 PIETT MISSING: Deploy locations to generate force \u2014 need to draw for Piett!"));
        }

        float bespinBoost = 0.0f;
        if (facts.objectiveAnalyzed()
                && facts.needsBespinSystem()
                && facts.objectiveTurnNumber() <= 3
                && facts.bespinDeploy()
                && !facts.bespinOnTable()) {
            bespinBoost = facts.objectiveTurnNumber() <= 1
                    ? 800.0f : 400.0f;
            operations.add(add(facts.actionId(), "V24.15-bespin-priority",
                    TraceOutputKind.ORDERING, bespinBoost,
                    "V24.15 BESPIN PRIORITY: Deploy Bespin system FIRST \u2014 objective foundation!"));
        }

        return new EarlyLocationEvaluation(
                new PolicyResult(EARLY_PRODUCER, operations),
                AdapterStep.CONTINUE_ACTION, piettPriority, bespinBoost);
    }

    public static BespinFirstRoute classifyBespinFirst(
            DeployObjectiveSequencingFacts.BespinFirstCandidate facts) {
        Objects.requireNonNull(facts, "facts");
        String text = facts.guardCheckText();
        boolean locationDeploy = facts.locationByCategory()
                || text.contains("location") || text.contains("site")
                || text.contains("system");
        boolean amsdAction = text.contains("alert my star destroyer")
                || text.contains("amsd");
        boolean executorDeploy = text.contains("executor");
        boolean shipDeploy = facts.shipByCategory()
                || text.contains("starship") || text.contains("capital")
                || text.contains("star destroyer");
        boolean bespinDeploy = text.contains("bespin");
        return locationDeploy || amsdAction || executorDeploy
                || shipDeploy || bespinDeploy
                ? BespinFirstRoute.EXEMPT : BespinFirstRoute.CANDIDATE;
    }

    public static BespinFirstEvaluation evaluateBespinFirst(
            DeployObjectiveSequencingFacts.BespinFirstDecision facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.objectiveForbidsExecutor()) {
            return new BespinFirstEvaluation(
                    new PolicyResult(BESPIN_FIRST_PRODUCER, List.of()),
                    BespinFirstOutcome.RELEASED,
                    "objective game text forbids deploying Executor");
        }
        if (facts.oracleAnalyzed() && !facts.capitalAccessible()) {
            return new BespinFirstEvaluation(
                    new PolicyResult(BESPIN_FIRST_PRODUCER, List.of()),
                    BespinFirstOutcome.RELEASED,
                    "no capital starship in hand/reserve/force/used \u2014 no live path to occupy Bespin space");
        }
        PolicyOperation penalty = add(facts.actionId(), "V29-bespin-first",
                TraceOutputKind.VETO, -500.0f,
                "V29 BESPIN-FIRST: Executor MUST deploy before characters! "
                        + "Get Bespin \u2192 Executor/AMSD \u2192 THEN characters.");
        return new BespinFirstEvaluation(
                new PolicyResult(BESPIN_FIRST_PRODUCER, List.of(penalty)),
                BespinFirstOutcome.PENALIZED, null);
    }

    private static PolicyOperation add(
            String actionId,
            String ruleId,
            TraceOutputKind outputKind,
            float delta,
            String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                TraceDomainId.DEPLOY_SEQUENCING, outputKind, delta, reason);
    }
}
