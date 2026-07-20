package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.List;
import java.util.Objects;

/** Pure scoring and adapter-control policy for DEPLOY action-text decisions. */
public final class DeployActionTextPolicy {

    private static final String PRODUCER = "DEPLOY_ACTION_TEXT_POLICY";
    private static final int AMSD_MINIMUM_FORCE = 7;

    public enum AdapterStep {
        FALL_THROUGH,
        CONTINUE_ACTION
    }

    public record Evaluation(
            PolicyResult result,
            AdapterStep adapterStep,
            boolean recordFailedTurn) {

        public Evaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(adapterStep, "adapterStep");
        }
    }

    private DeployActionTextPolicy() {
    }

    public static Evaluation evaluateAmsd(DeployActionTextFacts.AmsdFacts facts) {
        Objects.requireNonNull(facts, "facts");

        if (!facts.bespinSystemOnTable()) {
            return evaluation(operation(facts.actionId(), "V24-amsd-no-bespin",
                    TraceOutputKind.VETO, -9999.0f,
                    "V24 AMSD BLOCKED: No Bespin system on table — Star Destroyer has nowhere to deploy!"),
                    AdapterStep.CONTINUE_ACTION, false);
        }
        if (facts.alreadyFailedThisTurn()) {
            return evaluation(operation(facts.actionId(), "V24.10-amsd-retry-block",
                    TraceOutputKind.VETO, -9999.0f,
                    "V24.10 AMSD BLOCKED: Already failed this turn — save for next turn after recirculation!"),
                    AdapterStep.CONTINUE_ACTION, false);
        }
        if (facts.actionKind() == DeployActionTextFacts.AmsdActionKind.OTHER_SPECIFIC) {
            return evaluation(operation(facts.actionId(), "V24.10-amsd-non-piett-specific",
                    TraceOutputKind.VETO, -9999.0f,
                    "V24.10 AMSD BLOCKED: Only Piett may use AMSD — this action targets a different pilot!"),
                    AdapterStep.CONTINUE_ACTION, true);
        }
        if (!facts.oracleAnalyzed()) {
            return evaluation(null, AdapterStep.FALL_THROUGH, false);
        }

        boolean generic = facts.actionKind()
                == DeployActionTextFacts.AmsdActionKind.GENERIC_REVEAL;
        if (!facts.piettInHand()) {
            return evaluation(operation(facts.actionId(),
                            generic ? "V24.10-amsd-missing-piett-generic"
                                    : "V24.10-amsd-missing-piett-specific",
                            TraceOutputKind.VETO, -9999.0f,
                            generic
                                    ? "V24.10 AMSD BLOCKED: Piett NOT in hand — can't use AMSD!"
                                    : "V24.10 AMSD BLOCKED: Piett is NOT in hand — can't use AMSD!"),
                    AdapterStep.CONTINUE_ACTION, true);
        }

        boolean executorAvailable = facts.executorInHand() || facts.executorInReserve();
        if (!executorAvailable) {
            return evaluation(operation(facts.actionId(),
                            generic ? "V29.4-amsd-missing-executor-generic"
                                    : "V29.4-amsd-missing-executor-specific",
                            TraceOutputKind.VETO, -9999.0f,
                            generic
                                    ? "V29.4 AMSD BLOCKED: Piett in hand but Executor NOT in hand or reserve (may be in force/used pile)!"
                                    : "V29.4 AMSD BLOCKED: Piett in hand but Executor NOT in hand or reserve!"),
                    AdapterStep.CONTINUE_ACTION, true);
        }
        if (facts.forceAvailable() < AMSD_MINIMUM_FORCE) {
            return evaluation(operation(facts.actionId(), "V45-amsd-unaffordable",
                    TraceOutputKind.VETO, -9999.0f,
                    String.format(
                            "V45 AMSD UNAFFORDABLE: Need %d force for Piett+Executor but only %d available!",
                            AMSD_MINIMUM_FORCE, facts.forceAvailable())),
                    AdapterStep.CONTINUE_ACTION, false);
        }

        String source = facts.executorInHand() ? "hand" : "reserve";
        boolean early = facts.currentTurn() <= 2;
        float delta = early ? 1500.0f : 500.0f;
        String ruleId;
        String reason;
        if (generic && early) {
            ruleId = "V24.15-amsd-approved-early-generic";
            reason = "V24.15 AMSD MEGA PRIORITY: Turn " + facts.currentTurn()
                    + " — Executor (from " + source
                    + ") MUST deploy NOW to control Bespin!";
        } else if (generic) {
            ruleId = "V24.10-amsd-approved-generic";
            reason = "V24.10 AMSD APPROVED: Piett + Executor (from " + source
                    + ") ready — fire AMSD!";
        } else if (early) {
            ruleId = "V24.15-amsd-approved-early-specific";
            reason = "V24.15 AMSD MEGA PRIORITY: Turn " + facts.currentTurn()
                    + " — Executor (from " + source + ") MUST deploy NOW!";
        } else {
            ruleId = "V24.10-amsd-approved-specific";
            reason = "V24.10 AMSD APPROVED: Piett + Executor (from " + source
                    + ") ready!";
        }
        return evaluation(operation(facts.actionId(), ruleId,
                        TraceOutputKind.ORDERING, delta, reason),
                AdapterStep.FALL_THROUGH, false);
    }

    public static PolicyResult scoreDockingBay(
            DeployActionTextFacts.DockingBayFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.emptyFriendlyBays() > 0) {
            return result(operation(facts.actionId(), "V29.7-docking-bay-empty",
                    TraceOutputKind.VETO, -200.0f,
                    "V29.7 DOCKING BAY: Already have " + facts.emptyFriendlyBays()
                            + " empty bay(s) — deploy characters there first, don't give opponent more locations!"));
        }
        if (facts.totalFriendlyBays() >= 2) {
            return result(operation(facts.actionId(), "V29.7-docking-bay-enough",
                    TraceOutputKind.VETO, -50.0f,
                    "V29.7 DOCKING BAY: Already have " + facts.totalFriendlyBays()
                            + " bays — enough for transit"));
        }
        if (facts.totalFriendlyBays() == 0) {
            return result(operation(facts.actionId(), "V29.7-docking-bay-first",
                    TraceOutputKind.BANDED, 200.0f,
                    "V29.7 FIRST DOCKING BAY: Deploy FIRST to create battleground for characters!"));
        }
        return result(operation(facts.actionId(), "V29.7-docking-bay-second",
                TraceOutputKind.BANDED, 30.0f,
                "V29.7 DOCKING BAY: Deploy second bay for transit network"));
    }

    public static PolicyResult scoreVaderCastle(
            DeployActionTextFacts.VaderCastleFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.objectiveAnalyzed() || !facts.huntDownVActive()) {
            return result(operation(facts.actionId(), "V25-vader-castle-generic",
                    TraceOutputKind.BANDED, 50.0f,
                    "Deploy Vader from reserve"));
        }
        if (facts.vaderOnTable()) {
            return result(operation(facts.actionId(), "V25-vader-castle-vader-present",
                    TraceOutputKind.BANDED, 0.0f,
                    "Vader already on table — Castle deploy not urgent"));
        }
        if (facts.forceAvailable() < 6) {
            return result(operation(facts.actionId(), "V25-vader-castle-unaffordable",
                    TraceOutputKind.VETO, -500.0f,
                    "V25 HUNT DOWN: NOT ENOUGH FORCE for Vader! Need 6, have "
                            + facts.forceAvailable() + ". SAVE Castle action!"));
        }
        return result(operation(facts.actionId(), "V25-vader-castle-priority",
                TraceOutputKind.ORDERING, 550.0f,
                "V25 HUNT DOWN: DEPLOY VADER NOW! Have " + facts.forceAvailable()
                        + " Force, deck cannot function without him!"));
    }

    public static PolicyResult scoreDiningRoomLando(
            DeployActionTextFacts.DiningRoomLandoFacts facts) {
        Objects.requireNonNull(facts, "facts");
        boolean friendliesPresent = facts.friendlyCountAtDiningRoom() > 0;
        boolean objectiveNeedsPresence = facts.objectiveAnalyzed()
                && facts.needsBespinSystemPresence();
        if (objectiveNeedsPresence && friendliesPresent) {
            return result(operation(facts.actionId(), "V29.6-dining-room-objective-safe",
                    TraceOutputKind.BANDED, 150.0f,
                    "V29.6 DINING ROOM: Deploy Lando with "
                            + facts.friendlyCountAtDiningRoom() + " friendlies — safe!"));
        }
        if (objectiveNeedsPresence) {
            return result(operation(facts.actionId(), "V29.6-dining-room-objective-alone",
                    TraceOutputKind.VETO, -30.0f,
                    "V29.6 DINING ROOM: Lando would be ALONE — deploy a buddy first!"));
        }
        if (friendliesPresent) {
            return result(operation(facts.actionId(), "V29.6-dining-room-generic-safe",
                    TraceOutputKind.BANDED, 30.0f,
                    "Dining Room: Deploy Lando from reserve (friendlies present)"));
        }
        return result(operation(facts.actionId(), "V29.6-dining-room-generic-alone",
                TraceOutputKind.VETO, -20.0f,
                "V29.6 Dining Room: Lando alone — risky!"));
    }

    public static PolicyResult scoreBespinShip(
            DeployActionTextFacts.BespinShipFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.friendlyPowerAtBespin()) {
            return result(operation(facts.actionId(), "V22.5-bespin-ship-critical",
                    TraceOutputKind.ORDERING, 300.0f,
                    "V22.5 CRITICAL: Deploy ship to Bespin! Enables Dark Deal + CC Occupation!"));
        }
        return result(operation(facts.actionId(), "V22.5-bespin-ship-present",
                TraceOutputKind.BANDED, 100.0f,
                "V22.5: Deploy ship (Bespin already occupied)"));
    }

    public static PolicyResult scoreSimultaneousDeploy(
            DeployActionTextFacts.ActionFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return result(operation(facts.actionId(), "V22.5-simultaneous-deploy",
                TraceOutputKind.BANDED, 120.0f,
                "V22.5: Deploy pilot+ship combo - efficient!"));
    }

    public static PolicyResult scoreMainGenerator(
            DeployActionTextFacts.MainGeneratorFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return result(operation(facts.actionId(), "V160-main-generator",
                TraceOutputKind.ORDERING, 800.0f,
                "V160 PUSH TARGET THE MAIN GENERATOR: deck's flip engine — deploy/fire to enable AT-AT vs Main Power Generators"));
    }

    public static PolicyResult scoreGenericDeploy(
            DeployActionTextFacts.GenericDeployFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return switch (facts.kind()) {
            case PROJECTION_ON_SIDE -> result(operation(facts.actionId(),
                    "generic-deploy-projection", TraceOutputKind.VETO,
                    -50.0f, "Never put projection on side of table"));
            case DEPLOY_ON -> result(operation(facts.actionId(),
                    "generic-deploy-on", TraceOutputKind.BANDED,
                    30.0f, "Deploy on location/table"));
            case DEPLOY_UNIQUE -> result(operation(facts.actionId(),
                    "generic-deploy-unique", TraceOutputKind.BANDED,
                    30.0f, "Special battleground deploy"));
        };
    }

    public static PolicyResult scoreGenericPlayCard(
            DeployActionTextFacts.PlayCardFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.forceAvailable() == 0) {
            return result(operation(facts.actionId(),
                    "generic-play-card-no-force", TraceOutputKind.VETO,
                    -50.0f, "No Force available - can't play cards!"));
        }
        if (facts.forceAvailable() <= 1) {
            return result(operation(facts.actionId(),
                    "generic-play-card-low-force", TraceOutputKind.VETO,
                    -30.0f, "Very low Force (" + facts.forceAvailable()
                            + ") - unlikely to afford cards"));
        }
        return result(operation(facts.actionId(),
                "generic-play-card", TraceOutputKind.BANDED,
                5.0f, "Generic play card — moderate priority"));
    }

    /** Pure arithmetic for the top-level legacy DEPLOY fallback. */
    public static int scoreLegacyFallbackDeployLocation(
            int deployLocationScore) {
        return deployLocationScore;
    }

    /** Pure arithmetic for a matched losing-location fallback candidate. */
    public static int scoreLegacyFallbackReinforce(
            int reinforceScore,
            float powerAdvantage,
            boolean battleground) {
        int score = reinforceScore;
        if (powerAdvantage < -5) {
            score += 15;
        }
        if (battleground) {
            score += 10;
        }
        return score;
    }

    /** Pure arithmetic for a matched opponent-only fallback candidate. */
    public static int scoreLegacyFallbackGainGround(
            int gainGroundScore,
            boolean hasOpponentForceIcons,
            boolean battleground,
            float opponentPower) {
        if (!hasOpponentForceIcons) {
            return 0;
        }
        int score = gainGroundScore;
        if (battleground) {
            score += 15;
        }
        if (opponentPower > 8) {
            score -= 10;
        }
        return score;
    }

    /** Pure arithmetic for a matched analyzed-location fallback candidate. */
    public static int scoreLegacyFallbackDomainMatch(
            boolean matchingDomain,
            boolean emptyWithoutFriendlyIcons) {
        int score = matchingDomain ? 5 : 0;
        if (emptyWithoutFriendlyIcons) {
            score -= 20;
        }
        return score;
    }

    /** Pure arithmetic for the matching-pilot fallback. */
    public static int scoreLegacyFallbackMatchingPilot(
            int matchingPilotScore) {
        return matchingPilotScore;
    }

    private static PolicyOperation operation(
            String actionId,
            String ruleId,
            TraceOutputKind outputKind,
            float delta,
            String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                TraceDomainId.DEPLOY_SEQUENCING, outputKind, delta, reason);
    }

    private static PolicyResult result(PolicyOperation operation) {
        return new PolicyResult(PRODUCER, List.of(operation));
    }

    private static Evaluation evaluation(
            PolicyOperation operation,
            AdapterStep step,
            boolean recordFailedTurn) {
        return new Evaluation(
                new PolicyResult(PRODUCER,
                        operation == null ? List.of() : List.of(operation)),
                step,
                recordFailedTurn);
    }
}
