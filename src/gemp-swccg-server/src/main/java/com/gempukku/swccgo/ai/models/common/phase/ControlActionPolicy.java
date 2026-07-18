package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.List;
import java.util.Objects;

/** Shared CONTROL policy arms offered outside the stock force-drain action. */
public final class ControlActionPolicy {
    private ControlActionPolicy() {
    }

    public static PolicyResult noEscapeRetrieval(String actionId) {
        return one(actionId, "V29.14-noescape-retrieval", TraceOutputKind.BANDED,
                200.0f,
                "V29.14 NO ESCAPE: Free card from Lost Pile — always take it!");
    }

    public static PolicyResult forceDrainModifier(String actionId) {
        return one(actionId, "V24.2-drain", TraceOutputKind.BANDED,
                80.0f,
                "V24.2 FORCE DRAIN BONUS: +1 to force drain — always use!");
    }

    public static PolicyResult selfCancelDrain(String actionId, String reason) {
        Objects.requireNonNull(reason, "reason");
        return one(actionId, "V52-self-cancel", TraceOutputKind.VETO,
                -9999.0f, reason);
    }

    private static PolicyResult one(String actionId, String ruleId,
                                    TraceOutputKind outputKind, float delta,
                                    String reason) {
        return new PolicyResult("CONTROL_ACTION_POLICY", List.of(
                PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                        TraceDomainId.DRAIN_CONTROL, outputKind, delta, reason)));
    }
}
