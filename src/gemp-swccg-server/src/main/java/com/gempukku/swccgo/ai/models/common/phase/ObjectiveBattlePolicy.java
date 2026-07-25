package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure objective-aware battle initiation scoring. */
public final class ObjectiveBattlePolicy {
    public static final String REQUIRED_LOCATION_CONTEST_RULE_ID =
            "BATTLE.OBJECTIVE.REQUIRED_LOCATION_CONTEST";
    public static final float REQUIRED_LOCATION_CONTEST_BONUS = 80.0f;
    public static final String GLOBAL_BLOCKER_REMOVAL_RULE_ID =
            "BATTLE.OBJECTIVE.GLOBAL_BLOCKER_REMOVAL";
    public static final float GLOBAL_BLOCKER_REMOVAL_BONUS = 250.0f;
    public static final String REQUIRED_CARD_CONTROL_ENABLER_RULE_ID =
            "BATTLE.OBJECTIVE.REQUIRED_CARD_CONTROL_ENABLER";
    public static final float REQUIRED_CARD_CONTROL_ENABLER_BONUS = 80.0f;
    public static final String REQUIRED_CARD_RETENTION_RULE_ID =
            "BATTLE.OBJECTIVE.REQUIRED_CARD_RETENTION";
    public static final float REQUIRED_CARD_RETENTION_BONUS = 80.0f;
    public static final String HARD_LOSS_LOCATION_RULE_ID =
            "BATTLE.OBJECTIVE.HARD_LOSS_LOCATION_DEFENSE";
    public static final float HARD_LOSS_LOCATION_BONUS = 250.0f;

    private static final int MINIMUM_RESERVE = 3;
    private static final float MINIMUM_EFFECTIVE_DIFF = -2.0f;
    private static final float OVERPOWER_MARGIN = 8.0f;
    private static final String PRODUCER = "OBJECTIVE_BATTLE_POLICY";

    public record Facts(
            String actionId,
            boolean exactStructuredPreFlipTarget,
            boolean missingSelfControl,
            boolean requiredCardControlEnabler,
            boolean requiredCardRetention,
            boolean hardLossLocation,
            boolean globalBlocker,
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

        public Facts(
                String actionId,
                boolean exactStructuredPreFlipTarget,
                boolean missingSelfControl,
                boolean requiredCardControlEnabler,
                boolean hardLossLocation,
                boolean globalBlocker,
                boolean bothSidesPresent,
                boolean formationSafetyVeto,
                boolean predictorSafe,
                float effectiveDiff,
                int reserveDeckSize,
                float ourPower,
                float theirPower) {
            this(actionId, exactStructuredPreFlipTarget,
                    missingSelfControl, requiredCardControlEnabler,
                    false, hardLossLocation, globalBlocker,
                    bothSidesPresent, formationSafetyVeto,
                    predictorSafe, effectiveDiff, reserveDeckSize,
                    ourPower, theirPower);
        }

        public Facts(
                String actionId,
                boolean exactStructuredPreFlipTarget,
                boolean missingSelfControl,
                boolean requiredCardControlEnabler,
                boolean globalBlocker,
                boolean bothSidesPresent,
                boolean formationSafetyVeto,
                boolean predictorSafe,
                float effectiveDiff,
                int reserveDeckSize,
                float ourPower,
                float theirPower) {
            this(actionId, exactStructuredPreFlipTarget,
                    missingSelfControl, requiredCardControlEnabler,
                    false, false, globalBlocker, bothSidesPresent,
                    formationSafetyVeto, predictorSafe,
                    effectiveDiff, reserveDeckSize, ourPower,
                    theirPower);
        }

        public Facts(
                String actionId,
                boolean exactStructuredPreFlipTarget,
                boolean missingSelfControl,
                boolean globalBlocker,
                boolean bothSidesPresent,
                boolean formationSafetyVeto,
                boolean predictorSafe,
                float effectiveDiff,
                int reserveDeckSize,
                float ourPower,
                float theirPower) {
            this(actionId, exactStructuredPreFlipTarget,
                    missingSelfControl, false, false, false,
                    globalBlocker,
                    bothSidesPresent, formationSafetyVeto,
                    predictorSafe, effectiveDiff, reserveDeckSize,
                    ourPower, theirPower);
        }

        public Facts(
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
            this(actionId, exactStructuredPreFlipTarget,
                    missingSelfControl, false, false, false,
                    false,
                    bothSidesPresent,
                    formationSafetyVeto, predictorSafe,
                    effectiveDiff, reserveDeckSize, ourPower,
                    theirPower);
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

        if (!facts.bothSidesPresent()
                || facts.formationSafetyVeto()
                || !facts.predictorSafe()
                || facts.effectiveDiff() < MINIMUM_EFFECTIVE_DIFF
                || !reserveReady
                || v25Suicide) {
            return new PolicyResult(PRODUCER, List.of());
        }

        List<PolicyOperation> operations = new ArrayList<>();
        if (facts.exactStructuredPreFlipTarget()
                && facts.missingSelfControl()) {
            operations.add(PolicyOperation.add(
                    facts.actionId(),
                    TraceRuleId.of(REQUIRED_LOCATION_CONTEST_RULE_ID),
                    TraceDomainId.BATTLE_INITIATION,
                    TraceOutputKind.BANDED,
                    REQUIRED_LOCATION_CONTEST_BONUS,
                    "Contest the exact unmet pre-flip objective control location"));
        }
        if (facts.requiredCardControlEnabler()
                && facts.missingSelfControl()) {
            operations.add(PolicyOperation.add(
                    facts.actionId(),
                    TraceRuleId.of(
                            REQUIRED_CARD_CONTROL_ENABLER_RULE_ID),
                    TraceDomainId.BATTLE_INITIATION,
                    TraceOutputKind.BANDED,
                    REQUIRED_CARD_CONTROL_ENABLER_BONUS,
                    "Contest the exact location that enables deployment of a missing required objective card"));
        }
        if (facts.requiredCardRetention()
                && facts.missingSelfControl()) {
            operations.add(PolicyOperation.add(
                    facts.actionId(),
                    TraceRuleId.of(
                            REQUIRED_CARD_RETENTION_RULE_ID),
                    TraceDomainId.BATTLE_INITIATION,
                    TraceOutputKind.BANDED,
                    REQUIRED_CARD_RETENTION_BONUS,
                    "Contest the exact location that keeps an active required objective card on table"));
        }
        if (facts.hardLossLocation()
                && facts.missingSelfControl()) {
            operations.add(PolicyOperation.add(
                    facts.actionId(),
                    TraceRuleId.of(HARD_LOSS_LOCATION_RULE_ID),
                    TraceDomainId.BATTLE_INITIATION,
                    TraceOutputKind.BANDED,
                    HARD_LOSS_LOCATION_BONUS,
                    "Contest an objective location whose destruction would place the objective out of play"));
        }
        if (facts.globalBlocker()) {
            operations.add(PolicyOperation.add(
                    facts.actionId(),
                    TraceRuleId.of(GLOBAL_BLOCKER_REMOVAL_RULE_ID),
                    TraceDomainId.BATTLE_INITIATION,
                    TraceOutputKind.BANDED,
                    GLOBAL_BLOCKER_REMOVAL_BONUS,
                    "Remove an opponent actor blocking the objective at this battleground"));
        }
        return new PolicyResult(PRODUCER, operations);
    }
}
