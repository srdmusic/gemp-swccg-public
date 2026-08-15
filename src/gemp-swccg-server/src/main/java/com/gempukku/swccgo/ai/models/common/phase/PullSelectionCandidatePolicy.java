package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import com.gempukku.swccgo.common.CardCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure route-specific child-selection scoring for pull-adjacent decisions. */
public final class PullSelectionCandidatePolicy {

    public static final float AMSD_BLOCK_SCORE = -9999.0f;
    private static final String PRODUCER = "PULL_SELECTION_CANDIDATE_POLICY";

    public enum AdapterStep {
        FALL_THROUGH,
        CONTINUE_CANDIDATE
    }

    public record Evaluation(PolicyResult result, AdapterStep adapterStep,
                             boolean resetToAmsdBlockScore) {
        public Evaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(adapterStep, "adapterStep");
        }
    }

    private PullSelectionCandidatePolicy() {
    }

    public static PolicyResult scoreIwtmLocation(
            PullSelectionCandidateFacts.IwtmLocation facts) {
        Objects.requireNonNull(facts, "facts");
        return facts.starkillerSystem()
                ? one(facts.actionId(), "V186-iwtm-system",
                TraceDomainId.OBJECTIVE_INTENT, TraceOutputKind.ORDERING, 300.0f,
                "V186 STARKILLER BASE SYSTEM - download engine for the 2-battleground flip")
                : empty();
    }

    public static PolicyResult scoreCountedObjectiveProgress(
            String actionId, boolean requiredActor,
            boolean requiredCompanion,
            boolean requiredLocation) {
        Objects.requireNonNull(actionId, "actionId");
        if ((requiredActor ? 1 : 0)
                + (requiredCompanion ? 1 : 0)
                + (requiredLocation ? 1 : 0) > 1) {
            throw new IllegalArgumentException(
                    "A pull candidate cannot fill multiple counted roles");
        }
        if (requiredActor) {
            return one(actionId, "PULL.OBJECTIVE.COUNTED_REQUIRED_ACTOR",
                    TraceDomainId.OBJECTIVE_INTENT, TraceOutputKind.BANDED,
                    300.0f,
                    "Pull the typed actor required by the counted objective");
        }
        if (requiredCompanion) {
            return one(actionId,
                    "PULL.OBJECTIVE.COUNTED_REQUIRED_COMPANION",
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.BANDED, 300.0f,
                    "Pull the control companion required by the counted objective");
        }
        if (requiredLocation) {
            return one(actionId, "PULL.OBJECTIVE.COUNTED_REQUIRED_LOCATION",
                    TraceDomainId.OBJECTIVE_INTENT, TraceOutputKind.BANDED,
                    300.0f,
                    "Pull a missing location required by the counted objective");
        }
        return empty();
    }

    public static PolicyResult scoreCountedObjectiveHoldLocation(
            String actionId, boolean expandsHoldRoute) {
        Objects.requireNonNull(actionId, "actionId");
        return expandsHoldRoute
                ? one(actionId,
                    "PULL.OBJECTIVE.COUNTED_HOLD_LOCATION",
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.BANDED, 300.0f,
                    "Pull a third selected-planet site to buffer the two-site back hold")
                : empty();
    }

    public static PolicyResult scoreMassassiWarRoom(
            String actionId, boolean preferredWarRoom) {
        Objects.requireNonNull(actionId, "actionId");
        return preferredWarRoom
                ? one(actionId,
                    "PULL.OBJECTIVE.MASSASSI_WAR_ROOM",
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.BANDED, 300.0f,
                    "MASSASSI: pull the War Room so Rebel Tech adds to Attack Run while tutoring the Trench")
                : empty();
    }

    public static PolicyResult scoreImperialEntanglementsBackSite(
            String actionId, boolean expandsAtRiskBack) {
        Objects.requireNonNull(actionId, "actionId");
        return expandsAtRiskBack
                ? one(actionId,
                    "PULL.OBJECTIVE.IMPERIAL_ENTANGLEMENTS_BACK_SITE",
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.BANDED, 300.0f,
                    "IMPERIAL ENTANGLEMENTS BACK: expand the Tatooine control route while the relative count is at risk")
                : empty();
    }

    public static PolicyResult scoreTheyHaveNoIdeaNativeRoute(
            String actionId, int routePriority) {
        Objects.requireNonNull(actionId, "actionId");
        if (routePriority <= 0) return empty();
        String stage = routePriority >= 4
                ? "LANDING_PAD_STAGE" : "ROGUE_ONE_READY";
        String reason = routePriority >= 4
                ? "THNI: stage Landing Pad Nine before Rogue One so the flipped objective retains its site exception"
                : "THNI: Landing Pad Nine is staged, secure Rogue One for the Scarif system and back-side exception";
        return one(actionId,
                "PULL.OBJECTIVE.THEY_HAVE_NO_IDEA." + stage,
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED, 300.0f,
                reason);
    }

    public static PolicyResult scoreMassassiAttackRunPackage(
            String actionId, int prerequisitePriority) {
        Objects.requireNonNull(actionId, "actionId");
        if (prerequisitePriority <= 0) return empty();
        return one(actionId,
                "PULL.OBJECTIVE.MASSASSI_ATTACK_RUN_PACKAGE",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                300.0f,
                "MASSASSI: select the next missing executable Attack Run prerequisite");
    }

    public static PolicyResult scoreRequiredOnTableCard(
            String actionId, boolean requiredOnTableCard) {
        return scoreRequiredOnTableCard(
                actionId, requiredOnTableCard, true);
    }

    public static PolicyResult scoreRequiredOnTableCard(
            String actionId, boolean requiredOnTableCard,
            boolean routeReady) {
        Objects.requireNonNull(actionId, "actionId");
        if (!requiredOnTableCard) return empty();
        return routeReady
                ? one(actionId,
                    "PULL.OBJECTIVE.REQUIRED_ON_TABLE_CARD",
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.BANDED,
                    300.0f,
                    "Pull the missing card whose active table presence is required to flip")
                : one(actionId,
                    "PULL.OBJECTIVE.REQUIRED_ON_TABLE_CARD_ROUTE_BLOCKED",
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.BANDED,
                    150.0f,
                    "Keep the required card available, but rank it below a printing whose deploy route is ready");
    }

    public static PolicyResult scoreRequiredCardDeployEnabler(
            String actionId, boolean requiredActor,
            boolean requiredLocation) {
        Objects.requireNonNull(actionId, "actionId");
        if (requiredActor && requiredLocation) {
            throw new IllegalArgumentException(
                    "A deploy-enabler candidate cannot be both actor and location");
        }
        if (requiredActor) {
            return one(actionId,
                    "PULL.OBJECTIVE.REQUIRED_CARD_ENABLER_ACTOR",
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.BANDED,
                    300.0f,
                    "Pull an actor required to make a missing objective card deployable");
        }
        if (requiredLocation) {
            return one(actionId,
                    "PULL.OBJECTIVE.REQUIRED_CARD_ENABLER_LOCATION",
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.BANDED,
                    250.0f,
                    "Pull a location required to make a missing objective card deployable");
        }
        return empty();
    }

    public static PolicyResult scoreUnknownPull(
            PullSelectionCandidateFacts.UnknownPull facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();

        if (facts.gainDecision()) {
            if (facts.category() == CardCategory.CHARACTER) {
                operations.add(add(facts.actionId(), "pull-unknown-character",
                        TraceDomainId.PULL_SEARCH, TraceOutputKind.ORDERING,
                        10.0f, "Character - valuable"));
            } else if (facts.category() == CardCategory.STARSHIP) {
                operations.add(add(facts.actionId(), "pull-unknown-starship",
                        TraceDomainId.PULL_SEARCH, TraceOutputKind.ORDERING,
                        8.0f, "Starship - valuable"));
            } else if (facts.category() == CardCategory.LOCATION) {
                operations.add(add(facts.actionId(), "pull-unknown-location",
                        TraceDomainId.PULL_SEARCH, TraceOutputKind.ORDERING,
                        10.0f, "Location - valuable"));
            }
        }
        if (facts.gainDecision() && facts.huntDownLightsaber()) {
            operations.add(add(facts.actionId(), "V25-hunt-down-lightsaber",
                    TraceDomainId.OBJECTIVE_INTENT, TraceOutputKind.BANDED, 200.0f,
                    "V25 HUNT DOWN: LIGHTSABER — critical for deck engine!"));
        }
        if (facts.gainDecision() && facts.activeObjectiveFlipGate()) {
            operations.add(add(facts.actionId(), "PULL.OBJECTIVE.FLIP_GATE_SITE",
                    TraceDomainId.OBJECTIVE_INTENT, TraceOutputKind.BANDED, 300.0f,
                    "Pull the exact pre-flip objective control site"));
        }

        addCloudCityUnknown(operations, facts);

        if (facts.priorityProtectionScore() != null) {
            operations.add(add(facts.actionId(), "pull-unknown-priority",
                    facts.gainDecision() ? TraceDomainId.PULL_SEARCH
                            : TraceDomainId.FORCE_LOSS_PAYMENT,
                    TraceOutputKind.BANDED,
                    facts.priorityProtectionScore() * 0.3f, "Priority card"));
        }
        if (facts.amsdState() == PullSelectionCandidateFacts.UnknownAmsdState.PIETT) {
            operations.add(add(facts.actionId(), "V24.10-amsd-safety-piett",
                    TraceDomainId.DECK_PLAYBOOK, TraceOutputKind.VETO, 500.0f,
                    "V24.10 AMSD SAFETY NET: PIETT — approved for AMSD!"));
        } else if (facts.amsdState()
                == PullSelectionCandidateFacts.UnknownAmsdState.NON_PIETT) {
            operations.add(add(facts.actionId(), "V24.10-amsd-safety-block",
                    TraceDomainId.DECK_PLAYBOOK, TraceOutputKind.VETO, -9999.0f,
                    "V24.10 AMSD SAFETY NET: " + facts.title()
                            + " is NOT Piett — AMSD requires Piett only!"));
        }
        return result(operations);
    }

    public static PolicyResult scoreBlueprintPull(
            PullSelectionCandidateFacts.BlueprintPull facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        addCloudCityBlueprint(operations, facts);
        if (facts.planState() == PullSelectionCandidateFacts.PlanState.IN_PLAN) {
            operations.add(add(facts.actionId(), "pull-plan-match",
                    TraceDomainId.DEPLOY_SEQUENCING, TraceOutputKind.ORDERING, 100.0f,
                    "IN DEPLOYMENT PLAN: " + facts.planStrategy()));
        } else if (facts.planState()
                == PullSelectionCandidateFacts.PlanState.HOLD_BACK) {
            operations.add(add(facts.actionId(), "pull-plan-hold",
                    TraceDomainId.DEPLOY_SEQUENCING, TraceOutputKind.ORDERING, -50.0f,
                    "HOLD BACK: save for later"));
        }
        return result(operations);
    }

    public static Evaluation evaluateAmsdPilot(
            PullSelectionCandidateFacts.AmsdPilot facts) {
        Objects.requireNonNull(facts, "facts");
        return switch (facts.state()) {
            case NOT_AMSD -> evaluation(empty(), AdapterStep.FALL_THROUGH, false);
            case NON_PIETT -> evaluation(one(facts.actionId(),
                    "V24.10-amsd-pilot-block", TraceDomainId.DECK_PLAYBOOK,
                    TraceOutputKind.VETO, -9999.0f,
                    "V24.10 AMSD BLOCKED: Only Piett may use AMSD — "
                            + facts.title() + " is not allowed!"),
                    AdapterStep.CONTINUE_CANDIDATE, true);
            case PIETT_EXECUTOR_MISSING -> evaluation(one(facts.actionId(),
                    "V24.10-amsd-executor-missing", TraceDomainId.DECK_PLAYBOOK,
                    TraceOutputKind.VETO, -9999.0f,
                    "V24.10 AMSD: Piett selected but Executor NOT in reserve!"),
                    AdapterStep.CONTINUE_CANDIDATE, true);
            case PIETT_EXECUTOR_PRESENT -> evaluation(one(facts.actionId(),
                    "V24.10-amsd-approved", TraceDomainId.DECK_PLAYBOOK,
                    TraceOutputKind.VETO, 300.0f,
                    "V24.10 AMSD: Piett + Executor in reserve — APPROVED!"),
                    AdapterStep.FALL_THROUGH, false);
            case PIETT_ORACLE_UNAVAILABLE -> evaluation(one(facts.actionId(),
                    "V24.10-amsd-oracle-unavailable", TraceDomainId.DECK_PLAYBOOK,
                    TraceOutputKind.VETO, 200.0f,
                    "V24.10 AMSD: Piett selected (oracle unavailable — allowing)"),
                    AdapterStep.FALL_THROUGH, false);
        };
    }

    private static void addCloudCityUnknown(
            List<PolicyOperation> operations,
            PullSelectionCandidateFacts.UnknownPull facts) {
        if (facts.cloudCityMode() == PullSelectionCandidateFacts.CloudCityMode.IM_SORRY) {
            switch (facts.cloudCitySite()) {
                case DINING_ROOM -> operations.add(add(facts.actionId(),
                        "V24.10-im-sorry-dining", TraceDomainId.DECK_PLAYBOOK,
                        TraceOutputKind.BANDED, -50.0f,
                        "V24.10 I'M SORRY: Dining Room likely already on table"));
                case SECURITY_TOWER -> operations.add(add(facts.actionId(),
                        "V24.13-im-sorry-security", TraceDomainId.DECK_PLAYBOOK,
                        TraceOutputKind.BANDED, -30.0f,
                        "V24.13 I'M SORRY: Security Tower — force-gen only, deploy LAST!"));
                case CARBONITE_CHAMBER -> operations.add(add(facts.actionId(),
                        "V24.13-im-sorry-carbonite", TraceDomainId.DECK_PLAYBOOK,
                        TraceOutputKind.BANDED, 150.0f,
                        "V24.13 I'M SORRY: Carbonite Chamber — priority battleground!"));
                default -> operations.add(add(facts.actionId(),
                        "V24.10-im-sorry-other", TraceDomainId.DECK_PLAYBOOK,
                        TraceOutputKind.BANDED, 100.0f,
                        "V24.10 I'M SORRY: Pull interior CC site — expand drain sites!"));
            }
        } else if (facts.cloudCityMode()
                == PullSelectionCandidateFacts.CloudCityMode.SLIP_SLIDING) {
            addSlipSliding(operations, facts.actionId(), facts.cloudCitySite());
        }
    }

    private static void addCloudCityBlueprint(
            List<PolicyOperation> operations,
            PullSelectionCandidateFacts.BlueprintPull facts) {
        if (facts.cloudCityMode() == PullSelectionCandidateFacts.CloudCityMode.OBJECTIVE) {
            switch (facts.cloudCitySite()) {
                case UPPER_WALKWAY -> operations.add(add(facts.actionId(),
                        "V26-objective-exterior", TraceDomainId.OBJECTIVE_INTENT,
                        TraceOutputKind.ORDERING, 300.0f,
                        "V26 OBJECTIVE: Upper Walkway is EXTERIOR — only way to get it out! I'm Sorry can't pull this!"));
                case DINING_ROOM -> operations.add(add(facts.actionId(),
                        "V26-objective-dining", TraceDomainId.OBJECTIVE_INTENT,
                        TraceOutputKind.ORDERING, -300.0f,
                        "V26 OBJECTIVE: Dining Room is INTERIOR — save for Slip Sliding Away!"));
                default -> operations.add(add(facts.actionId(),
                        "V26-objective-interior", TraceDomainId.OBJECTIVE_INTENT,
                        TraceOutputKind.ORDERING, -200.0f,
                        "V26 OBJECTIVE: Interior CC site — save for I'm Sorry, deploy Exterior first!"));
            }
        } else if (facts.cloudCityMode()
                == PullSelectionCandidateFacts.CloudCityMode.IM_SORRY) {
            switch (facts.cloudCitySite()) {
                case DINING_ROOM -> operations.add(add(facts.actionId(),
                        "V24.10-im-sorry-dining", TraceDomainId.DECK_PLAYBOOK,
                        TraceOutputKind.BANDED, -50.0f,
                        "V24.10 I'M SORRY: Dining Room likely already on table from Slip Sliding"));
                case SECURITY_TOWER -> operations.add(add(facts.actionId(),
                        "V24.13-im-sorry-security", TraceDomainId.DECK_PLAYBOOK,
                        TraceOutputKind.BANDED, -30.0f,
                        "V24.13 I'M SORRY: Security Tower is force-gen only — deploy LAST!"));
                case CARBONITE_CHAMBER -> operations.add(add(facts.actionId(),
                        "V24.13-im-sorry-carbonite", TraceDomainId.DECK_PLAYBOOK,
                        TraceOutputKind.BANDED, 150.0f,
                        "V24.13 I'M SORRY: Carbonite Chamber — key battleground, pull FIRST!"));
                default -> operations.add(add(facts.actionId(),
                        "V24.10-im-sorry-other", TraceDomainId.DECK_PLAYBOOK,
                        TraceOutputKind.BANDED, 100.0f,
                        "V24.10 I'M SORRY: Pull interior CC site — expand drain sites!"));
            }
        } else if (facts.cloudCityMode()
                == PullSelectionCandidateFacts.CloudCityMode.SLIP_SLIDING) {
            addSlipSliding(operations, facts.actionId(), facts.cloudCitySite());
        }
    }

    private static void addSlipSliding(List<PolicyOperation> operations,
                                       String actionId,
                                       PullSelectionCandidateFacts.CloudCitySite site) {
        if (site == PullSelectionCandidateFacts.CloudCitySite.DINING_ROOM) {
            operations.add(add(actionId, "V24.10-slip-dining",
                    TraceDomainId.DECK_PLAYBOOK, TraceOutputKind.BANDED, 300.0f,
                    "V24.10 SLIP SLIDING: Dining Room — guarantees best starting CC site pair!"));
        } else {
            operations.add(add(actionId, "V24.10-slip-other",
                    TraceDomainId.DECK_PLAYBOOK, TraceOutputKind.BANDED, -50.0f,
                    "V24.10 SLIP SLIDING: Other CC site — Dining Room is better"));
        }
    }

    private static Evaluation evaluation(PolicyResult result, AdapterStep step,
                                         boolean resetScore) {
        return new Evaluation(result, step, resetScore);
    }

    private static PolicyResult empty() {
        return result(List.of());
    }

    private static PolicyResult result(List<PolicyOperation> operations) {
        return new PolicyResult(PRODUCER, operations);
    }

    private static PolicyResult one(String actionId, String rule,
                                    TraceDomainId domainId,
                                    TraceOutputKind outputKind,
                                    float delta, String reason) {
        return result(List.of(add(actionId, rule, domainId, outputKind,
                delta, reason)));
    }

    private static PolicyOperation add(String actionId, String rule,
                                       TraceDomainId domainId,
                                       TraceOutputKind outputKind,
                                       float delta, String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(rule),
                domainId, outputKind,
                delta, reason);
    }
}
