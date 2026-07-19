package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure ordered scoring policy for BATTLE action-text decisions. */
public final class BattleActionTextPolicy {

    private static final float ABILITY_POWER_EQUIVALENT = 2.5f;
    private static final int MINIMUM_BATTLE_RESERVE = 3;

    private BattleActionTextPolicy() {
    }

    public static float effectivePowerDifference(
            float ourPower,
            float theirPower,
            float ourAbility,
            float theirAbility) {
        return ourPower - theirPower
                + ((ourAbility - theirAbility) * ABILITY_POWER_EQUIVALENT);
    }

    public static PolicyResult scoreInitiation(
            BattleActionTextFacts.InitiationFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();

        if (!facts.locationResolved()) {
            add(operations, facts.actionId(), "V25-battle-no-location",
                    TraceOutputKind.BANDED, 30.0f,
                    "V25 BATTLE: Initiate battle (no location data)");
        } else {
            addResolvedInitiation(operations, facts);
        }

        if (facts.reserveDeckSize() < MINIMUM_BATTLE_RESERVE) {
            add(operations, facts.actionId(), "V25-battle-low-reserve",
                    TraceOutputKind.VETO, -50.0f,
                    "V25 BATTLE: Low reserve (" + facts.reserveDeckSize()
                            + ") — bad destiny draws!");
        }

        return new PolicyResult("BATTLE_ACTION_TEXT_INITIATION_POLICY", operations);
    }

    private static void addResolvedInitiation(
            List<PolicyOperation> operations,
            BattleActionTextFacts.InitiationFacts facts) {
        float effectiveDiff = effectivePowerDifference(
                facts.ourPower(), facts.theirPower(),
                facts.ourAbility(), facts.theirAbility());
        String location = facts.locationTitle();

        if (facts.theirPower() <= 0.0f) {
            add(operations, facts.actionId(), "V25-battle-no-opponent",
                    TraceOutputKind.VETO, -100.0f,
                    "V25 BATTLE: No opponent at " + location);
        } else if (facts.theirPower() > facts.ourPower() * 2.0f
                && facts.theirPower() > 6.0f) {
            add(operations, facts.actionId(), "V25-battle-suicide",
                    TraceOutputKind.VETO, -500.0f,
                    String.format(
                            "V25 BATTLE SUICIDE: %.0f vs %.0f at %s — NEVER!",
                            facts.ourPower(), facts.theirPower(), location));
        } else if (effectiveDiff >= 8.0f) {
            add(operations, facts.actionId(), "V25-battle-crush",
                    TraceOutputKind.BANDED, 200.0f,
                    String.format(
                            "V25 BATTLE CRUSH at %s: %.0f vs %.0f — ATTACK!",
                            location, facts.ourPower(), facts.theirPower()));
        } else if (effectiveDiff >= 5.0f) {
            add(operations, facts.actionId(), "V25-battle-favorable",
                    TraceOutputKind.BANDED, 120.0f,
                    String.format(
                            "V25 BATTLE FAVORABLE at %s: %.0f vs %.0f",
                            location, facts.ourPower(), facts.theirPower()));
        } else if (effectiveDiff >= 2.0f) {
            add(operations, facts.actionId(), "V25-battle-marginal",
                    TraceOutputKind.BANDED, 60.0f,
                    String.format(
                            "V25 BATTLE MARGINAL at %s: %.0f vs %.0f",
                            location, facts.ourPower(), facts.theirPower()));
        } else if (effectiveDiff >= -2.0f) {
            add(operations, facts.actionId(), "V25-battle-even",
                    TraceOutputKind.BANDED, 20.0f,
                    String.format(
                            "V25 BATTLE EVEN at %s: %.0f vs %.0f — risky but worth trying",
                            location, facts.ourPower(), facts.theirPower()));
        } else {
            float penalty = -60.0f;
            if (effectiveDiff < -8.0f) {
                penalty = -120.0f;
            }
            if (effectiveDiff < -15.0f) {
                penalty = -250.0f;
            }
            add(operations, facts.actionId(), "V25-battle-unfavorable",
                    TraceOutputKind.VETO, penalty,
                    String.format(
                            "V25 BATTLE UNFAVORABLE at %s: %.0f vs %.0f — avoid!",
                            location, facts.ourPower(), facts.theirPower()));
        }
    }

    private static void add(
            List<PolicyOperation> operations,
            String actionId,
            String ruleId,
            TraceOutputKind outputKind,
            float delta,
            String reason) {
        operations.add(PolicyOperation.add(
                actionId,
                TraceRuleId.of(ruleId),
                TraceDomainId.BATTLE_INITIATION,
                outputKind,
                delta,
                reason));
    }
}
