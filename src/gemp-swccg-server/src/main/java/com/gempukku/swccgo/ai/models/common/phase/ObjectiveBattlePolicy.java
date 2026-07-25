package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.List;
import java.util.Objects;

/** Pure objective-aware battle initiation scoring. */
public final class ObjectiveBattlePolicy {
    public static final String REQUIRED_LOCATION_CONTEST_RULE_ID =
            "BATTLE.OBJECTIVE.REQUIRED_LOCATION_CONTEST";
    public static final float REQUIRED_LOCATION_CONTEST_BONUS = 80.0f;

    private static final int MINIMUM_RESERVE = 3;
    private static final float MINIMUM_EFFECTIVE_DIFF = -2.0f;
    private static final float OVERPOWER_MARGIN = 8.0f;
    private static final String PRODUCER = "OBJECTIVE_BATTLE_POLICY";

    public record Facts(
            String actionId,
            boolean exactStructuredPreFlipTarget,
            boolean missingSelfControl,
            boolean bothSidesPresent,
            boolean formationSafetyVeto,
            boolean predictorSafe,
            float effectiveDiff,
            int reserveDeckSize,
            float ourPower,
            float theirPower) {
        public Facts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    private ObjectiveBattlePolicy() {
    }

    public static PolicyResult evaluate(Facts facts) {
        Objects.requireNonNull(facts, "facts");

        boolean v25Suicide = facts.theirPower() > facts.ourPower() * 2.0f
                && facts.theirPower() > 6.0f;
        float rawOverpowerMargin = facts.ourPower() - facts.theirPower();
        boolean reserveReady = facts.reserveDeckSize() >= MINIMUM_RESERVE
                || rawOverpowerMargin >= OVERPOWER_MARGIN;

        if (!facts.exactStructuredPreFlipTarget()
                || !facts.missingSelfControl()
                || !facts.bothSidesPresent()
                || facts.formationSafetyVeto()
                || !facts.predictorSafe()
                || facts.effectiveDiff() < MINIMUM_EFFECTIVE_DIFF
                || !reserveReady
                || v25Suicide) {
            return new PolicyResult(PRODUCER, List.of());
        }

        return new PolicyResult(
                PRODUCER,
                List.of(PolicyOperation.add(
                        facts.actionId(),
                        TraceRuleId.of(REQUIRED_LOCATION_CONTEST_RULE_ID),
                        TraceDomainId.BATTLE_INITIATION,
                        TraceOutputKind.BANDED,
                        REQUIRED_LOCATION_CONTEST_BONUS,
                        "Contest the exact unmet pre-flip objective control location")));
    }
}
