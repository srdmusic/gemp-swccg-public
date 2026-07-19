package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.List;
import java.util.Objects;

/** Pure V70 safety policy for stock reserve-deploy child candidates. */
public final class PullDeployCandidatePolicy {

    public enum AdapterStep {
        FALL_THROUGH,
        CONTINUE_CANDIDATE
    }

    public record Evaluation(PolicyResult result, AdapterStep adapterStep) {
        public Evaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(adapterStep, "adapterStep");
        }
    }

    private PullDeployCandidatePolicy() {
    }

    public static Evaluation evaluate(PullDeployCandidateFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.weaponDeviceBlockReason().isEmpty()) {
            return new Evaluation(
                    new PolicyResult("PULL_DEPLOY_CANDIDATE_POLICY", List.of()),
                    AdapterStep.FALL_THROUGH);
        }
        PolicyOperation operation = PolicyOperation.add(
                facts.actionId(),
                TraceRuleId.of("V70-reserve-candidate"),
                TraceDomainId.PULL_SEARCH,
                TraceOutputKind.VETO,
                -9999.0f,
                "V70 NO 2ND WEAPON: " + facts.weaponDeviceBlockReason()
                        + " \u2014 '" + facts.displayTitle() + "'");
        return new Evaluation(
                new PolicyResult("PULL_DEPLOY_CANDIDATE_POLICY", List.of(operation)),
                AdapterStep.CONTINUE_CANDIDATE);
    }
}
