package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyContributionLedger;
import com.gempukku.swccgo.ai.models.common.policy.ObjectivePreferencePolicy;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.strategy.FormationSafety;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Pure scalar owner for ranking candidate deploy-phase plans. */
public final class DeployPlanRankingPolicy {

    private static final String PRODUCER = "DEPLOY_PLAN_RANKING_POLICY";
    private static final TraceDomainId DOMAIN = TraceDomainId.DEPLOY_SEQUENCING;
    private static final TraceOutputKind OUTPUT_KIND = TraceOutputKind.ORDERING;

    private DeployPlanRankingPolicy() {
    }

    public static PolicyResult evaluate(List<InstructionFacts> instructions,
                                        List<LocationFacts> locations) {
        Objects.requireNonNull(instructions, "instructions");
        Objects.requireNonNull(locations, "locations");
        List<PolicyOperation> operations = new ArrayList<>();
        Set<String> contributionIds = new HashSet<>();

        for (InstructionFacts instruction : instructions) {
            Objects.requireNonNull(instruction, "instruction");
            requireUnique(contributionIds, instruction.contributionId());
            add(operations, instruction.contributionId(),
                    "deploy-plan-ranking-base-power",
                    instruction.powerContribution() * 2,
                    "Base power value");
        }

        for (LocationFacts location : locations) {
            Objects.requireNonNull(location, "location");
            requireUnique(contributionIds, location.contributionId());
            if (location.objectiveRelevant()) {
                addObjective(operations, location.contributionId(),
                        "V22-plan-ranking-objective-location",
                        location.objectiveBonus(),
                        "V22 objective-relevant location bonus");
            }

            if (location.theirPower() > 0) {
                int legacyOurPower = (int) location.ourPower();
                int advantage = legacyOurPower - (int) location.theirPower();
                int denyDrainBonus = location.ourForceIcons() > 0
                        ? location.ourForceIcons() * 20 : 0;
                int winControlBonus = advantage > 0
                        && location.theirForceIcons() > 0
                        ? location.theirForceIcons() * 15 : 0;

                if (advantage >= 4) {
                    add(operations, location.contributionId(),
                            "deploy-plan-ranking-favorable",
                            50 + advantage * 10
                                    + denyDrainBonus + winControlBonus,
                            "FAVORABLE FIGHT");
                } else if (advantage > 0) {
                    add(operations, location.contributionId(),
                            "deploy-plan-ranking-marginal",
                            25 + advantage * 5
                                    + denyDrainBonus + winControlBonus,
                            "MARGINAL FIGHT");
                } else {
                    add(operations, location.contributionId(),
                            "deploy-plan-ranking-losing",
                            5 + denyDrainBonus,
                            "LOSING");
                }

                if (location.ourAbility() >= 4) {
                    add(operations, location.contributionId(),
                            "deploy-plan-ranking-destiny", 25,
                            "Can draw destiny");
                } else {
                    add(operations, location.contributionId(),
                            "deploy-plan-ranking-vulnerable",
                            -(20 + legacyOurPower * 2),
                            "Vulnerable");
                }
            } else {
                float establishBonus = 40;
                if (location.theirForceIcons() > 0) {
                    establishBonus += location.theirForceIcons() * 15;
                }
                if (location.ourForceIcons() > 0) {
                    establishBonus += location.ourForceIcons() * 15;
                }
                if (location.ourAbility() >= 4) {
                    establishBonus += 25;
                } else if (location.ourPower() < 5) {
                    establishBonus -= 500;  // BLOCKED - easy crush target
                }
                add(operations, location.contributionId(),
                        "deploy-plan-ranking-establish", establishBonus,
                        "EMPTY/ESTABLISH LOCATION");
            }

            if (location.theirCardCount() == 0
                    && location.triggerKnowable()
                    && location.immediateReactExposureProven()
                    && !location.formationPenaltyExempt()
                    && location.strongestImmediateReactEffectivePower() > 0.0f
                    && location.strongestImmediateReactEffectivePower()
                    >= FormationSafety.DOMINANCE_MULTIPLE
                    * location.ourPower()) {
                add(operations, location.contributionId(),
                        "deploy-plan-ranking-isolated-packet", -150.0f,
                        "Proven immediate react dominates empty target packet");
            }
        }

        return new PolicyResult(PRODUCER, operations);
    }

    public static PolicyResult evaluateAdjunct(AdjunctFacts adjunct) {
        Objects.requireNonNull(adjunct, "adjunct");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (adjunct.objectiveCapitalPlan()) {
            addObjective(operations, adjunct.contributionId(),
                    "V22-plan-ranking-objective-capital-bespin", 200.0f,
                    "V22 objective capital ship priority for Bespin");
        }
        return new PolicyResult(PRODUCER, operations);
    }

    /** Objective gate bonus for a complete actor-plus-support formation plan. */
    public static PolicyResult evaluateFlipGateFormation(
            FlipGateFormationFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (facts.completeFormation()) {
            addObjective(operations, facts.contributionId(),
                    "V297-plan-ranking-flip-gate-formation",
                    facts.objectiveBonus(),
                    "V297 objective flip-gate actor and buddy formation");
        }
        return new PolicyResult(PRODUCER, operations);
    }

    /** Adds one Endor objective adjustment through the shared objective ceiling. */
    public static PolicyResult evaluateEndorAdjustment(
            String contributionId, String ruleId, float adjustment,
            String reason) {
        requireNonBlank(contributionId, "contributionId");
        requireNonBlank(ruleId, "ruleId");
        requireNonBlank(reason, "reason");
        if (!Float.isFinite(adjustment)) {
            throw new IllegalArgumentException("adjustment must be finite");
        }
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (adjustment != 0.0f) {
            addObjective(operations, contributionId, ruleId,
                    adjustment, reason);
        }
        return new PolicyResult(PRODUCER, operations);
    }

    /** Applies ranking contributions without emitting action reasoning or logger output. */
    public static float apply(float initialScore, PolicyResult result) {
        return apply(initialScore, new PolicyResult[]{result});
    }

    /** Applies all ranking producers with one shared objective-preference ceiling. */
    public static float apply(float initialScore, PolicyResult... results) {
        Objects.requireNonNull(results, "results");
        PolicyContributionLedger ledger =
                new PolicyContributionLedger("deploy-plan-ranking-internal");
        for (PolicyResult result : results) {
            Objects.requireNonNull(result, "result");
            if (!PRODUCER.equals(result.producerId())) {
                throw new IllegalArgumentException("unexpected ranking producer "
                        + result.producerId());
            }
            ledger.register(result);
        }
        float score = initialScore;
        float objectivePreference = 0.0f;
        for (PolicyOperation operation : ledger.orderedOperations()) {
            if (operation.kind() != PolicyOperationKind.ADD) {
                throw new IllegalStateException(
                        "deploy plan ranking accepts ADD operations only");
            }
            float delta = operation.delta();
            if (operation.domainId() == TraceDomainId.OBJECTIVE_INTENT) {
                delta = ObjectivePreferencePolicy.applyWithinCeiling(
                        objectivePreference, delta);
                objectivePreference += delta;
            }
            score += delta;
        }
        return score;
    }

    public record InstructionFacts(String contributionId,
                                   int powerContribution) {
        public InstructionFacts {
            contributionId = requireNonBlank(
                    contributionId, "contributionId");
        }
    }

    public record LocationFacts(String contributionId,
                                float ourPower,
                                float ourAbility,
                                float theirPower,
                                int ourForceIcons,
                                int theirForceIcons,
                                int theirCardCount,
                                boolean objectiveRelevant,
                                float objectiveBonus,
                                boolean triggerKnowable,
                                boolean immediateReactExposureProven,
                                float strongestImmediateReactEffectivePower,
                                boolean formationPenaltyExempt) {
        public LocationFacts {
            contributionId = requireNonBlank(
                    contributionId, "contributionId");
            if (!Float.isFinite(ourPower)
                    || !Float.isFinite(ourAbility)
                    || !Float.isFinite(theirPower)
                    || !Float.isFinite(strongestImmediateReactEffectivePower)) {
                throw new IllegalArgumentException(
                        "formation and react values must be finite");
            }
            if (theirCardCount < 0) {
                throw new IllegalArgumentException(
                        "theirCardCount must be nonnegative");
            }
            if (!Float.isFinite(objectiveBonus)) {
                throw new IllegalArgumentException("objectiveBonus must be finite");
            }
        }

        public LocationFacts(String contributionId,
                             float ourPower,
                             float ourAbility,
                             float theirPower,
                             int ourForceIcons,
                             int theirForceIcons,
                             boolean objectiveRelevant,
                             float objectiveBonus) {
            this(contributionId, ourPower, ourAbility, theirPower,
                    ourForceIcons, theirForceIcons,
                    theirPower > 0.0f ? 1 : 0,
                    objectiveRelevant, objectiveBonus,
                    false, false, 0.0f, false);
        }
    }

    public record AdjunctFacts(String contributionId,
                               boolean objectiveCapitalPlan) {
        public AdjunctFacts {
            contributionId = requireNonBlank(
                    contributionId, "contributionId");
        }
    }

    public record FlipGateFormationFacts(String contributionId,
                                         boolean completeFormation,
                                         float objectiveBonus) {
        public FlipGateFormationFacts {
            contributionId = requireNonBlank(
                    contributionId, "contributionId");
            if (!Float.isFinite(objectiveBonus) || objectiveBonus < 0.0f) {
                throw new IllegalArgumentException(
                        "objectiveBonus must be finite and nonnegative");
            }
        }
    }

    private static void add(List<PolicyOperation> operations,
                            String contributionId,
                            String ruleId,
                            float delta,
                            String reason) {
        operations.add(PolicyOperation.add(contributionId,
                TraceRuleId.of(ruleId), DOMAIN, OUTPUT_KIND, delta, reason));
    }

    private static void addObjective(
            List<PolicyOperation> operations, String contributionId,
            String ruleId, float delta, String reason) {
        operations.add(PolicyOperation.add(contributionId,
                TraceRuleId.of(ruleId), TraceDomainId.OBJECTIVE_INTENT,
                OUTPUT_KIND, delta, reason));
    }

    private static void requireUnique(Set<String> contributionIds,
                                      String contributionId) {
        if (!contributionIds.add(contributionId)) {
            throw new IllegalArgumentException(
                    "duplicate contributionId " + contributionId);
        }
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must be nonblank");
        }
        return value;
    }
}
