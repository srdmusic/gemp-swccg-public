package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.List;
import java.util.Objects;

/** Pure V298 ground-first allocation policy for deploy routes into space. */
public final class SpaceDeploymentAllocationPolicy {
    public static final float SYSTEM_ABILITY_TARGET = 4.0f;
    public static final float PRESSURE_BOLSTER_LIMIT = 7.0f;
    private static final float MANDATORY_FALLBACK_DELTA = -800.0f;
    private static final String PRODUCER = "SPACE_DEPLOYMENT_ALLOCATION_POLICY";

    public enum Outcome {
        NO_SPACE_ROUTE,
        BUDDY_PROGRESS,
        BUDDY_COMPLETE,
        GROUND_FIRST_AFTER_FOUR,
        PRESSURE_EXCEPTION,
        FAVORABLE_BATTLE_EXCEPTION,
        OBJECTIVE_EXCEPTION,
        REPILOT_EXCEPTION,
        TERMINAL_EXCEPTION
    }

    public record Evaluation(PolicyResult result, Outcome outcome) {
        public Evaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public record Facts(
            String actionId,
            boolean spaceRoute,
            float currentSystemAbility,
            float projectedSystemAbility,
            boolean opponentSpacePressure,
            boolean favorableSpaceBattle,
            boolean typedSpaceObjectiveNeed,
            boolean orphanRepilot,
            boolean terminalDefense) {
        public Facts {
            Objects.requireNonNull(actionId, "actionId");
            if (!Float.isFinite(currentSystemAbility)
                    || !Float.isFinite(projectedSystemAbility)) {
                throw new IllegalArgumentException(
                        "space ability facts must be finite");
            }
        }
    }

    private SpaceDeploymentAllocationPolicy() {
    }

    public static Evaluation evaluate(Facts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.spaceRoute()) {
            return evaluation(Outcome.NO_SPACE_ROUTE, List.of());
        }

        if (facts.orphanRepilot()) {
            return evaluation(Outcome.REPILOT_EXCEPTION, List.of());
        }
        if (facts.typedSpaceObjectiveNeed()) {
            return evaluation(Outcome.OBJECTIVE_EXCEPTION, List.of());
        }
        if (facts.terminalDefense()) {
            return evaluation(Outcome.TERMINAL_EXCEPTION, List.of());
        }
        if (facts.favorableSpaceBattle()) {
            return evaluation(Outcome.FAVORABLE_BATTLE_EXCEPTION,
                    List.of());
        }

        if (facts.currentSystemAbility() < SYSTEM_ABILITY_TARGET) {
            if (facts.projectedSystemAbility() >= SYSTEM_ABILITY_TARGET) {
                return evaluation(Outcome.BUDDY_COMPLETE,
                        List.of(add(facts.actionId(),
                                "V298-space-buddy-complete", 300.0f,
                                String.format(
                                        "V298 SPACE BUDDY: actual system ability %.2f to %.2f reaches 4",
                                        facts.currentSystemAbility(),
                                        facts.projectedSystemAbility()))));
            }
            return evaluation(Outcome.BUDDY_PROGRESS,
                    facts.projectedSystemAbility()
                            > facts.currentSystemAbility()
                            ? List.of(add(facts.actionId(),
                                    "V298-space-buddy-progress", 100.0f,
                                    String.format(
                                            "V298 SPACE BUDDY: actual system ability %.2f to %.2f builds toward 4",
                                            facts.currentSystemAbility(),
                                            facts.projectedSystemAbility())))
                            : List.of());
        }

        if (facts.opponentSpacePressure()
                && facts.currentSystemAbility() < PRESSURE_BOLSTER_LIMIT
                && facts.projectedSystemAbility()
                    <= PRESSURE_BOLSTER_LIMIT) {
            return evaluation(Outcome.PRESSURE_EXCEPTION, List.of());
        }

        return evaluation(Outcome.GROUND_FIRST_AFTER_FOUR,
                List.of(PolicyOperation.defer(
                        facts.actionId(),
                        TraceRuleId.of("V298-space-ground-first"),
                        TraceDomainId.DEPLOY_SITING,
                        TraceOutputKind.VETO,
                        MANDATORY_FALLBACK_DELTA,
                        String.format(
                                "V298 GROUND FIRST: quiet system already has actual ability %.2f, preserve bodies and Force for sites",
                                facts.currentSystemAbility()))));
    }

    public static boolean isDeferred(Evaluation evaluation) {
        return evaluation != null
                && evaluation.outcome() == Outcome.GROUND_FIRST_AFTER_FOUR;
    }

    /**
     * Legacy action text does not identify the candidate card, so projected
     * ability is unknown. It may build toward four, but pressure cannot bypass
     * the post-four ground-first rule without a verified projection.
     */
    public static Evaluation evaluateLegacyUnknownProjection(
            String actionId, float currentSystemAbility) {
        return evaluate(new Facts(
                actionId, true, currentSystemAbility,
                currentSystemAbility, false, false,
                false, false, false));
    }

    public static int scoreLegacyFallback(Evaluation evaluation) {
        return isDeferred(evaluation) ? -1000 : 0;
    }

    private static Evaluation evaluation(Outcome outcome,
                                         List<PolicyOperation> operations) {
        return new Evaluation(new PolicyResult(PRODUCER, operations), outcome);
    }

    private static PolicyOperation add(String actionId, String ruleId,
                                       float delta, String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                TraceDomainId.DEPLOY_SITING, TraceOutputKind.BANDED,
                delta, reason);
    }
}
