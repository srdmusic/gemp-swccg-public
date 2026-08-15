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
            AdapterStep adapterStep,
            BespinFirstOutcome outcome,
            String releaseReason) {

        public BespinFirstEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(adapterStep, "adapterStep");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    private DeployObjectiveSequencingPolicy() {
    }

    public static boolean isEarlyLocationCandidate(
            DeployObjectiveSequencingFacts.EarlyLocationCandidate facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.cardResolved()) {
            return facts.locationByCategory();
        }

        String subject = unresolvedDeploySubject(facts.actionText());
        return subject.matches(".*\\b(location|site|system)\\b.*");
    }

    private static String unresolvedDeploySubject(String actionText) {
        String text = actionText.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();
        int deploy = text.indexOf("deploy ");
        if (deploy < 0) {
            return "";
        }
        String subject = text.substring(deploy + "deploy ".length());
        for (String boundary : new String[] {
                " to ", " at ", " aboard ", " on ", " from ", " with "}) {
            int index = subject.indexOf(boundary);
            if (index >= 0) {
                subject = subject.substring(0, index);
            }
        }
        for (String boundary : new String[] {
                "to ", "at ", "aboard ", "on ", "from ", "with "}) {
            if (subject.startsWith(boundary)) {
                return "";
            }
        }
        return subject;
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
            bespinBoost = 300.0f;
            operations.add(addObjective(facts.actionId(), "V24.15-bespin-priority",
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
        if (facts.cardResolved()) {
            if (facts.locationByCategory() || facts.shipByCategory()) {
                return BespinFirstRoute.EXEMPT;
            }
            return facts.characterByCategory()
                    ? BespinFirstRoute.CANDIDATE
                    : BespinFirstRoute.EXEMPT;
        }

        String subject = unresolvedDeploySubject(text);
        boolean locationDeploy = subject.matches(".*\\b(location|site|system)\\b.*");
        boolean amsdAction = text.contains("alert my star destroyer")
                || text.contains("amsd");
        boolean executorDeploy = subject.contains("executor");
        boolean shipDeploy = subject.contains("starship") || subject.contains("capital")
                || subject.contains("star destroyer");
        boolean bespinDeploy = subject.matches(".*\\bbespin\\b.*");
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
                    AdapterStep.FALL_THROUGH,
                    BespinFirstOutcome.RELEASED,
                    "objective game text forbids deploying Executor");
        }
        if (facts.oracleAnalyzed() && !facts.capitalAccessible()) {
            return new BespinFirstEvaluation(
                    new PolicyResult(BESPIN_FIRST_PRODUCER, List.of()),
                    AdapterStep.FALL_THROUGH,
                    BespinFirstOutcome.RELEASED,
                    "no capital starship in hand/reserve/force/used \u2014 no live path to occupy Bespin space");
        }
        PolicyOperation penalty = addObjective(facts.actionId(), "V29-bespin-first",
                TraceOutputKind.BANDED, -300.0f,
                "V29 BESPIN-FIRST: prefer the Bespin -> Executor/AMSD sequence before characters "
                        + "(-300 objective preference).");
        return new BespinFirstEvaluation(
                new PolicyResult(BESPIN_FIRST_PRODUCER, List.of(penalty)),
                AdapterStep.FALL_THROUGH,
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

    private static PolicyOperation addObjective(
            String actionId,
            String ruleId,
            TraceOutputKind outputKind,
            float delta,
            String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                TraceDomainId.OBJECTIVE_INTENT, outputKind, delta, reason);
    }
}
