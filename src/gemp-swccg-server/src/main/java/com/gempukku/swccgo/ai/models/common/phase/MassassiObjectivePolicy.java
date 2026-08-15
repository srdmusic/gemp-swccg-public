package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.List;
import java.util.Objects;

/** Exact action scoring for the Massassi Base Operations Attack Run payoff. */
public final class MassassiObjectivePolicy {
    public static final String ATTACK_RUN_RULE_ID =
            "OBJECTIVE.MASSASSI.ATTACK_RUN";

    private static final String PRODUCER =
            "MASSASSI_OBJECTIVE_POLICY";

    private MassassiObjectivePolicy() {
    }

    public static PolicyResult scoreAttackRun(String actionId) {
        Objects.requireNonNull(actionId, "actionId");
        return new PolicyResult(
                PRODUCER,
                List.of(PolicyOperation.add(
                    actionId,
                    TraceRuleId.of(ATTACK_RUN_RULE_ID),
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.BANDED,
                    300.0f,
                    "MASSASSI OBJECTIVE PAYOFF: prefer beginning the exact Attack Run with the armed carrier ready at Death Star (+300 bounded preference)")));
    }
}
