package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyContributionLedger;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
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
                add(operations, location.contributionId(),
                        "V22-plan-ranking-objective-location",
                        location.objectiveBonus(),
                        "V22 objective-relevant location bonus");
            }

            if (location.theirPower() > 0) {
                int advantage = location.ourPower() - (int) location.theirPower();
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
                            -(20 + location.ourPower() * 2),
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
        }

        return new PolicyResult(PRODUCER, operations);
    }

    public static PolicyResult evaluateAdjunct(AdjunctFacts adjunct) {
        Objects.requireNonNull(adjunct, "adjunct");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (adjunct.objectiveCapitalPlan()) {
            add(operations, adjunct.contributionId(),
                    "V22-plan-ranking-objective-capital-bespin", 200.0f,
                    "V22 objective capital ship priority for Bespin");
        }
        return new PolicyResult(PRODUCER, operations);
    }

    /** Applies ranking contributions without emitting action reasoning or logger output. */
    public static float apply(float initialScore, PolicyResult result) {
        Objects.requireNonNull(result, "result");
        if (!PRODUCER.equals(result.producerId())) {
            throw new IllegalArgumentException("unexpected ranking producer "
                    + result.producerId());
        }

        PolicyContributionLedger ledger =
                new PolicyContributionLedger("deploy-plan-ranking-internal");
        ledger.register(result);
        float score = initialScore;
        for (PolicyOperation operation : ledger.orderedOperations()) {
            if (operation.kind() != PolicyOperationKind.ADD) {
                throw new IllegalStateException(
                        "deploy plan ranking accepts ADD operations only");
            }
            score += operation.delta();
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
                                int ourPower,
                                int ourAbility,
                                float theirPower,
                                int ourForceIcons,
                                int theirForceIcons,
                                boolean objectiveRelevant,
                                float objectiveBonus) {
        public LocationFacts {
            contributionId = requireNonBlank(
                    contributionId, "contributionId");
            if (!Float.isFinite(theirPower)) {
                throw new IllegalArgumentException("theirPower must be finite");
            }
            if (!Float.isFinite(objectiveBonus)) {
                throw new IllegalArgumentException("objectiveBonus must be finite");
            }
        }
    }

    public record AdjunctFacts(String contributionId,
                               boolean objectiveCapitalPlan) {
        public AdjunctFacts {
            contributionId = requireNonBlank(
                    contributionId, "contributionId");
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
