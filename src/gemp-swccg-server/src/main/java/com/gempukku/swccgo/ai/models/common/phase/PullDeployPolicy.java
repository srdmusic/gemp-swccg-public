package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure DeployEvaluator-side PULL guard policy shared by both bots. */
public final class PullDeployPolicy {

    public enum AdapterStep {
        FALL_THROUGH,
        CONTINUE_ACTION
    }

    public record Evaluation(PolicyResult result, AdapterStep adapterStep) {
    }

    private PullDeployPolicy() {
    }

    public static Evaluation evaluate(PullDeployFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();

        if (facts.failedTwice()) {
            return stop(operations, facts.actionId(), "V60-fail-stop", -9999.0f,
                    "V60 RESERVE FAIL-STOP: '" + facts.actionText()
                            + "' failed 2x \u2014 stop trying!");
        }
        if (!facts.objectiveRoutePullVetoBypass()
                && facts.reserveSize() >= 0
                && facts.reserveSize() <= 2) {
            return stop(operations, facts.actionId(), "V60-reserve-risk", -9999.0f,
                    "V60 RESERVE RISK: " + facts.reserveSize()
                            + " cards in Reserve \u2014 reveal almost the whole deck!");
        }
        if (!facts.namedMissingTarget().isEmpty()) {
            return stop(operations, facts.actionId(), "V60-named-miss", -9999.0f,
                    "V60 RESERVE MISS: '" + facts.namedMissingTarget()
                            + "' not in Reserve \u2014 pull fails + reveals deck!");
        }
        if (!facts.genericTypedMiss().isEmpty()) {
            return stop(operations, facts.actionId(), "V67bg", -9999.0f,
                    "V67bg RESERVE MISS (typed '" + facts.genericTypedMiss()
                            + "'): no card matching Filter in Reserve \u2014 pull will fail!");
        }
        if (!facts.genericUntypedMiss().isEmpty()) {
            return stop(operations, facts.actionId(), "V60-generic-miss", -9999.0f,
                    "V60 RESERVE MISS (generic, untyped): no '"
                            + facts.genericUntypedMiss()
                            + "' in Reserve \u2014 pull fails + reveals deck!");
        }

        if (facts.memoryValidation().outcome() == PullOracleView.Outcome.WILL_FAIL) {
            return stop(operations, facts.actionId(), "V66", -9999.0f,
                    "V66 MEMORY: " + facts.memoryValidation().reason());
        }
        if (facts.memoryValidation().outcome() == PullOracleView.Outcome.WASTEFUL) {
            operations.add(add(facts.actionId(), "V66-wasteful", -800.0f,
                    "V66 MEMORY: " + facts.memoryValidation().reason()));
        }

        if (facts.sourceValidation().outcome() == PullOracleView.Outcome.WILL_FAIL) {
            return stop(operations, facts.actionId(), "V67h", -9999.0f,
                    "V67h MEMORY (game-text): " + facts.sourceValidation().reason());
        }
        if (facts.sourceValidation().outcome() == PullOracleView.Outcome.WILL_SUCCEED
                && facts.allReserveTargetsUnattachableWeapons()) {
            return stop(operations, facts.actionId(), "V185", -2000.0f,
                    "V185 WEAPON, NO LEGAL HOLDER: every Reserve-Deck target left for '"
                            + facts.actionText()
                            + "' is a weapon Rando has no in-play character to hold (per the weapon's own deploy filter) \u2014 deploy a valid character first");
        }
        if (facts.sourceValidation().outcome() == PullOracleView.Outcome.WILL_SUCCEED
                && facts.starshipOnlyWithoutSpaceLocation()) {
            return stop(operations, facts.actionId(), "V190", -12000.0f,
                    "V190 STARSHIP PULL, NO SPACE LOCATION ON TABLE: every Reserve target left for '"
                            + facts.actionText()
                            + "' is a starship and there is no system to deploy it to \u2014 it would park at a docking bay at 0 power; deploy a system first");
        }

        return evaluation(operations, AdapterStep.FALL_THROUGH);
    }

    private static Evaluation stop(List<PolicyOperation> operations,
                                   String actionId, String rule,
                                   float delta, String reason) {
        operations.add(add(actionId, rule, delta, reason));
        return evaluation(operations, AdapterStep.CONTINUE_ACTION);
    }

    private static PolicyOperation add(String actionId, String rule,
                                       float delta, String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(rule),
                TraceDomainId.PULL_SEARCH, TraceOutputKind.VETO, delta, reason);
    }

    private static Evaluation evaluation(List<PolicyOperation> operations,
                                         AdapterStep step) {
        return new Evaluation(new PolicyResult("PULL_DEPLOY_POLICY", operations), step);
    }
}
