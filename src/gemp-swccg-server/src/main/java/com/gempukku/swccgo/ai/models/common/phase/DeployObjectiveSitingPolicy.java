package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.playbook.ObjectiveProgressAssessment;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Shared pure owner of objective-specific DEPLOY-2 destination scores. */
public final class DeployObjectiveSitingPolicy {
    private DeployObjectiveSitingPolicy() {
    }

    public static PolicyResult evaluate(Facts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();

        if (!facts.undercoverSpy() && facts.objectiveAnalyzed()
                && facts.objectiveRelevant()) {
            operations.add(add(facts.actionId(), "V22-objective-location",
                    TraceOutputKind.BANDED, facts.objectiveLocationBonus(),
                    "OBJECTIVE LOCATION - deploy here helps flip!"));
        }

        if (facts.objectiveAnalyzed() && facts.myLord() && facts.senator()) {
            if (facts.galacticSenateDestination()) {
                operations.add(add(facts.actionId(), "V88-CS",
                        TraceOutputKind.BANDED, 1500.0f,
                        "V88 MY LORD: senator → Galactic Senate (flip target + weapon destiny -6 protection)"));
            } else {
                operations.add(add(facts.actionId(), "V88-CS",
                        TraceOutputKind.VETO, -2000.0f,
                        "V88 MY LORD: senator not at Galactic Senate — wrong site!"));
            }
        }

        if (facts.textNamesDestination()) {
            if (facts.textRejectsDestination()) {
                operations.add(add(facts.actionId(), "V88-text-named",
                        TraceOutputKind.BANDED, -500.0f,
                        "V88 TEXT-NAMED SITE: character text says NOT at '"
                                + facts.bareSiteTitle() + "' — wrong site"));
            } else if (!facts.textNamedDestinationDoomed()) {
                operations.add(add(facts.actionId(), "V88-text-named",
                        TraceOutputKind.BANDED, 500.0f,
                        "V88 TEXT-NAMED SITE: character text mentions '"
                                + facts.bareSiteTitle() + "' — home-site bonus"));
            }
        }

        if (facts.galacticSenateDestination() && facts.character()
                && !facts.senator()
                && facts.opponentPower() <= facts.friendlySenatorPower()) {
            operations.add(add(facts.actionId(), "V99-CS",
                    TraceOutputKind.VETO, -1500.0f,
                    String.format("V99 SENATE GUARD: non-senator → Galactic Senate (opp %.0f <= my senator %.0f) — wasted, deploy elsewhere",
                            facts.opponentPower(), facts.friendlySenatorPower())));
        }

        return new PolicyResult("DEPLOY_OBJECTIVE_SITING_POLICY", operations);
    }

    public static PolicyResult scoreCloudCityArmy(CloudCityArmyFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return single("DEPLOY_CLOUD_CITY_ARMY_POLICY",
                add(facts.actionId(), "V51-CC-ARMY", TraceOutputKind.BANDED,
                        500.0f, String.format(
                                "V51 CC ARMY: Deploy to %s pre-flip — build Cloud City army!",
                                facts.locationTitle())));
    }

    public static PolicyResult scoreObjectiveFirst(ObjectiveFirstFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return single("DEPLOY_OBJECTIVE_FIRST_POLICY",
                add(facts.actionId(), "V51-OBJ-FIRST", TraceOutputKind.BANDED,
                        300.0f, String.format(
                                "V51 OBJ FIRST: Deploy to %s — objective-relevant location pre-flip!",
                                facts.locationTitle())));
    }

    public static PolicyResult scoreCountedObjectiveProgress(
            String actionId, boolean advancesRequiredLocation) {
        Objects.requireNonNull(actionId, "actionId");
        if (!advancesRequiredLocation) {
            return new PolicyResult(
                    "DEPLOY_COUNTED_OBJECTIVE_PROGRESS_POLICY", List.of());
        }
        return single("DEPLOY_COUNTED_OBJECTIVE_PROGRESS_POLICY",
                add(actionId, "DEPLOY.OBJECTIVE.COUNTED_REQUIRED_LOCATION",
                        TraceOutputKind.BANDED, 600.0f,
                        "Deploy here to advance a missing counted-objective location"));
    }

    public static PolicyResult scoreTheyHaveNoIdeaDataVaultRoute(
            String actionId, boolean routeDestination) {
        Objects.requireNonNull(actionId, "actionId");
        if (!routeDestination) {
            return new PolicyResult(
                    "DEPLOY_THNI_DATA_VAULT_ROUTE_POLICY", List.of());
        }
        return single("DEPLOY_THNI_DATA_VAULT_ROUTE_POLICY",
                add(actionId, "OBJECTIVE.THNI.DATA_VAULT_CONTROLLER",
                        TraceOutputKind.BANDED, 1600.0f,
                        "Keep the Rebel trooper at Data Vault to complete the two-location flip route"));
    }

    public static PolicyResult scoreTheyHaveNoIdeaRogueOneScarifRoute(
            String actionId, boolean routeDestination) {
        Objects.requireNonNull(actionId, "actionId");
        if (!routeDestination) {
            return new PolicyResult(
                    "DEPLOY_THNI_ROGUE_ONE_SCARIF_ROUTE_POLICY", List.of());
        }
        return single("DEPLOY_THNI_ROGUE_ONE_SCARIF_ROUTE_POLICY",
                add(actionId, "OBJECTIVE.THNI.ROGUE_ONE_SCARIF",
                        TraceOutputKind.BANDED, 1600.0f,
                        "Deploy Rogue One to Scarif so it supplies the system-control leg of the flip route"));
    }

    public static PolicyResult scoreThreeSiteObjectiveCompletion(
            String actionId, boolean completesThreeSiteObjective) {
        Objects.requireNonNull(actionId, "actionId");
        if (!completesThreeSiteObjective) {
            return new PolicyResult(
                    "DEPLOY_THREE_SITE_OBJECTIVE_COMPLETION_POLICY",
                    List.of());
        }
        return single(
                "DEPLOY_THREE_SITE_OBJECTIVE_COMPLETION_POLICY",
                add(actionId,
                        "DEPLOY.OBJECTIVE.THREE_SITE_COMPLETION",
                        TraceOutputKind.BANDED, 8000.0f,
                        "Complete the final controlled site for the three-site objective before reinforcing elsewhere"));
    }

    public static PolicyResult scoreActorRuntimeLocation(
            String actionId, boolean advancesActorLocation) {
        Objects.requireNonNull(actionId, "actionId");
        if (!advancesActorLocation) {
            return new PolicyResult(
                    "DEPLOY_ACTOR_RUNTIME_LOCATION_POLICY", List.of());
        }
        return single("DEPLOY_ACTOR_RUNTIME_LOCATION_POLICY",
                add(actionId, "DEPLOY.OBJECTIVE.ACTOR_RUNTIME_LOCATION",
                        TraceOutputKind.BANDED, 1000.0f,
                        "Deploy the required actor to a live qualifying objective location"));
    }

    public static PolicyResult scoreIsbRouteCompletion(
            String actionId, boolean completesIsbRoute) {
        Objects.requireNonNull(actionId, "actionId");
        if (!completesIsbRoute) {
            return new PolicyResult(
                    "DEPLOY_ISB_ROUTE_COMPLETION_POLICY", List.of());
        }
        return single("DEPLOY_ISB_ROUTE_COMPLETION_POLICY",
                add(actionId, "OBJECTIVE.ISB.ROUTE_COMPLETION",
                        TraceOutputKind.BANDED, 8000.0f,
                        "Deploy the ISB agent here to complete the current flip route now"));
    }

    public static PolicyResult scoreRequiredOnTableCard(
            String actionId,
            ObjectiveProgressAssessment.Outcome outcome) {
        Objects.requireNonNull(actionId, "actionId");
        if (outcome == null
                || (outcome
                        != ObjectiveProgressAssessment.Outcome
                            .ADVANCES_MISSING_REQUIREMENT
                    && outcome
                        != ObjectiveProgressAssessment.Outcome
                            .COMPLETES_FLIP_NOW)) {
            return new PolicyResult(
                    "DEPLOY_REQUIRED_ON_TABLE_CARD_POLICY", List.of());
        }
        boolean completes = outcome
                == ObjectiveProgressAssessment.Outcome
                    .COMPLETES_FLIP_NOW;
        return single("DEPLOY_REQUIRED_ON_TABLE_CARD_POLICY",
                add(actionId,
                        "DEPLOY.OBJECTIVE.REQUIRED_ON_TABLE_CARD",
                        TraceOutputKind.BANDED,
                        completes ? 1200.0f : 800.0f,
                        completes
                            ? "Deploy the final active-table card and complete the modeled flip condition"
                            : "Deploy a missing active-table card required by the objective"));
    }

    public static PolicyResult scoreRequiredCardDeployEnabler(
            String actionId, boolean advancesDeployEnabler) {
        Objects.requireNonNull(actionId, "actionId");
        if (!advancesDeployEnabler) {
            return new PolicyResult(
                    "DEPLOY_REQUIRED_CARD_ENABLER_POLICY",
                    List.of());
        }
        return single("DEPLOY_REQUIRED_CARD_ENABLER_POLICY",
                add(actionId,
                        "DEPLOY.OBJECTIVE.REQUIRED_CARD_ENABLER",
                        TraceOutputKind.BANDED,
                        900.0f,
                        "Deploy here to satisfy a source-verified prerequisite for a missing objective card"));
    }

    public static PolicyResult scoreRequiredCardRetentionDefense(
            String actionId, boolean advancesRetentionDefense) {
        Objects.requireNonNull(actionId, "actionId");
        if (!advancesRetentionDefense) {
            return new PolicyResult(
                    "DEPLOY_REQUIRED_CARD_RETENTION_POLICY",
                    List.of());
        }
        return single("DEPLOY_REQUIRED_CARD_RETENTION_POLICY",
                add(actionId,
                        "DEPLOY.OBJECTIVE.REQUIRED_CARD_RETENTION",
                        TraceOutputKind.BANDED,
                        900.0f,
                            "Deploy here to keep an active required objective card from removing itself"));
    }

    public static PolicyResult scoreObjectiveHardLossDefense(
            String actionId, boolean advancesHardLossDefense) {
        Objects.requireNonNull(actionId, "actionId");
        if (!advancesHardLossDefense) {
            return new PolicyResult(
                    "DEPLOY_OBJECTIVE_HARD_LOSS_DEFENSE_POLICY",
                    List.of());
        }
        return single("DEPLOY_OBJECTIVE_HARD_LOSS_DEFENSE_POLICY",
                add(actionId,
                        "DEPLOY.OBJECTIVE.HARD_LOSS_DEFENSE",
                        TraceOutputKind.BANDED,
                        1200.0f,
                        "Deploy here to defend a live threat to the objective's terminal-loss location"));
    }

    public static PolicyResult scoreObjectiveLostPileDeploy(
            String actionId, boolean exactObjectiveAction,
            boolean hasLegalCandidate) {
        Objects.requireNonNull(actionId, "actionId");
        if (!exactObjectiveAction) {
            return new PolicyResult(
                    "DEPLOY_OBJECTIVE_LOST_PILE_POLICY",
                    List.of());
        }
        if (!hasLegalCandidate) {
            return single(
                    "DEPLOY_OBJECTIVE_LOST_PILE_POLICY",
                    PolicyOperation.hardVeto(
                        actionId,
                        TraceRuleId.of(
                            "DEPLOY.OBJECTIVE.LOST_PILE_NO_LEGAL_CARD"),
                        TraceDomainId.DEPLOY_SITING,
                        TraceOutputKind.VETO,
                        "Do not spend the once-per-turn objective action when Lost Pile has no legal source-defined deploy"));
        }
        return single(
                "DEPLOY_OBJECTIVE_LOST_PILE_POLICY",
                add(actionId,
                        "DEPLOY.OBJECTIVE.LOST_PILE_ROUTE",
                        TraceOutputKind.BANDED,
                        300.0f,
                        "Use the objective's legal once-per-turn Lost Pile deploy"));
    }

    public static PolicyResult scorePostFlipObjectivePayoff(
            String actionId,
            ObjectiveAnalyzer.ObjectivePostFlipPayoffRole role) {
        Objects.requireNonNull(actionId, "actionId");
        if (role == null
                || role
                    == ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.NONE) {
            return new PolicyResult(
                    "DEPLOY_POST_FLIP_OBJECTIVE_PAYOFF_POLICY",
                    List.of());
        }
        boolean primary = role
                == ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.PRIMARY;
        return single(
                "DEPLOY_POST_FLIP_OBJECTIVE_PAYOFF_POLICY",
                add(actionId,
                        primary
                            ? "DEPLOY.OBJECTIVE.POST_FLIP_PRIMARY_PAYOFF"
                            : "DEPLOY.OBJECTIVE.POST_FLIP_SECONDARY_PAYOFF",
                        TraceOutputKind.BANDED,
                        primary ? 1200.0f : 700.0f,
                        primary
                            ? "Deploy the named actor to activate the objective's primary back-side payoff"
                            : "Deploy the named actor to activate the objective's secondary back-side payoff"));
    }

    public static PolicyResult scoreFirstOrderReignsDrainPair(
            String actionId, boolean completesPair) {
        Objects.requireNonNull(actionId, "actionId");
        if (!completesPair) {
            return new PolicyResult(
                    "DEPLOY_FIRST_ORDER_DRAIN_PAIR_POLICY",
                    List.of());
        }
        return single(
                "DEPLOY_FIRST_ORDER_DRAIN_PAIR_POLICY",
                add(actionId,
                        "DEPLOY.OBJECTIVE.FIRST_ORDER_DRAIN_PAIR",
                        TraceOutputKind.BANDED,
                        800.0f,
                        "Deploy the second First Order character to activate the objective's +1 battleground drain"));
    }

    public static PolicyResult scoreFirstOrderReignsRouteCrew(
            String actionId, boolean boardsChaseShip) {
        Objects.requireNonNull(actionId, "actionId");
        if (!boardsChaseShip) {
            return new PolicyResult(
                    "DEPLOY_FIRST_ORDER_ROUTE_CREW_POLICY",
                    List.of());
        }
        return single(
                "DEPLOY_FIRST_ORDER_ROUTE_CREW_POLICY",
                add(actionId,
                    "DEPLOY.OBJECTIVE.FIRST_ORDER_ROUTE_CREW",
                    TraceOutputKind.BANDED,
                    1200.0f,
                    "Board a cheap First Order character on the Tracked Fleet chase ship before committing ground forces"));
    }

    public static PolicyResult scoreMassassiRebelTechPackage(
            String actionId, boolean deploysToPreferredTutorSite) {
        Objects.requireNonNull(actionId, "actionId");
        if (!deploysToPreferredTutorSite) {
            return new PolicyResult(
                    "DEPLOY_MASSASSI_ATTACK_RUN_PACKAGE_POLICY",
                    List.of());
        }
        return single(
                "DEPLOY_MASSASSI_ATTACK_RUN_PACKAGE_POLICY",
                add(actionId,
                    "DEPLOY.OBJECTIVE.MASSASSI_REBEL_TECH",
                    TraceOutputKind.BANDED, 1200.0f,
                    "Deploy Rebel Tech where its Trench tutor is executable, preferring Massassi War Room for the Attack Run bonus"));
    }

    public static PolicyResult scoreMassassiAttackRunCarrier(
            String actionId, boolean deploysWithinAttackRunRange) {
        Objects.requireNonNull(actionId, "actionId");
        if (!deploysWithinAttackRunRange) {
            return new PolicyResult(
                    "DEPLOY_MASSASSI_ATTACK_RUN_CARRIER_POLICY",
                    List.of());
        }
        return single(
                "DEPLOY_MASSASSI_ATTACK_RUN_CARRIER_POLICY",
                add(actionId,
                    "DEPLOY.OBJECTIVE.MASSASSI_ATTACK_RUN_CARRIER",
                    TraceOutputKind.BANDED, 1200.0f,
                    "Deploy the compatible Attack Run starfighter within one move of Death Star"));
    }

    public static PolicyResult scoreMassassiAttackRunTorpedoesTarget(
            String actionId, boolean routeReadyCarrier) {
        Objects.requireNonNull(actionId, "actionId");
        if (!routeReadyCarrier) {
            return new PolicyResult(
                    "DEPLOY_MASSASSI_ATTACK_RUN_TORPEDOES_POLICY",
                    List.of());
        }
        return single(
                "DEPLOY_MASSASSI_ATTACK_RUN_TORPEDOES_POLICY",
                add(actionId,
                    "DEPLOY.OBJECTIVE.MASSASSI_ATTACK_RUN_TORPEDOES",
                    TraceOutputKind.BANDED, 1200.0f,
                    "Attach Proton Torpedoes to the piloted Attack Run starfighter on the Death Star route"));
    }

    public static PolicyResult scoreFirstOrderReignsNavyRouteAction(
            String actionId, boolean executableRoute) {
        Objects.requireNonNull(actionId, "actionId");
        if (!executableRoute) {
            return new PolicyResult(
                    "DEPLOY_FIRST_ORDER_NAVY_ROUTE_ACTION_POLICY",
                    List.of());
        }
        return single(
                "DEPLOY_FIRST_ORDER_NAVY_ROUTE_ACTION_POLICY",
                add(actionId,
                        "DEPLOY.OBJECTIVE.FIRST_ORDER_NAVY_ROUTE",
                        TraceOutputKind.BANDED,
                        1500.0f,
                        "Use Navy Of The First Order to deploy the affordable ship-and-pilot chase package"));
    }

    public static PolicyResult scoreFirstOrderReignsNavyRouteCandidate(
            String actionId, boolean advancesRoute) {
        Objects.requireNonNull(actionId, "actionId");
        if (!advancesRoute) {
            return new PolicyResult(
                    "DEPLOY_FIRST_ORDER_NAVY_ROUTE_CANDIDATE_POLICY",
                    List.of());
        }
        return single(
                "DEPLOY_FIRST_ORDER_NAVY_ROUTE_CANDIDATE_POLICY",
                add(actionId,
                        "DEPLOY.OBJECTIVE.FIRST_ORDER_NAVY_ROUTE_CANDIDATE",
                        TraceOutputKind.BANDED,
                        1500.0f,
                        "Choose the affordable Navy ship-and-pilot package that can reach Tracked Fleet"));
    }

    public static PolicyResult scoreFirstOrderReignsNavyRouteDestination(
            String actionId, boolean advancesRoute) {
        Objects.requireNonNull(actionId, "actionId");
        if (!advancesRoute) {
            return new PolicyResult(
                    "DEPLOY_FIRST_ORDER_NAVY_ROUTE_DESTINATION_POLICY",
                    List.of());
        }
        return single(
                "DEPLOY_FIRST_ORDER_NAVY_ROUTE_DESTINATION_POLICY",
                add(actionId,
                        "DEPLOY.OBJECTIVE.FIRST_ORDER_NAVY_ROUTE_DESTINATION",
                        TraceOutputKind.BANDED,
                        1500.0f,
                        "Deploy the Navy ship-and-pilot package to the system that reaches Tracked Fleet"));
    }

    public static PolicyResult blockFirstOrderReignsPreFlipGround(
            String actionId, boolean prematureGround) {
        Objects.requireNonNull(actionId, "actionId");
        if (!prematureGround) {
            return new PolicyResult(
                    "DEPLOY_FIRST_ORDER_PRE_FLIP_GROUND_POLICY",
                    List.of());
        }
        return single(
                "DEPLOY_FIRST_ORDER_PRE_FLIP_GROUND_POLICY",
                PolicyOperation.hardVeto(
                        actionId,
                        TraceRuleId.of(
                                "DEPLOY.OBJECTIVE.FIRST_ORDER_PRE_FLIP_GROUND_HOLD"),
                        TraceDomainId.DEPLOY_SITING,
                        TraceOutputKind.VETO,
                        "Do not spend the Tracked Fleet chase budget on ground forces before the objective flips"));
    }

    public static PolicyResult blockFirstOrderReignsPreFlipCraitGround(
            String actionId, boolean prematureCraitGround) {
        return blockFirstOrderReignsPreFlipGround(
                actionId, prematureCraitGround);
    }

    public static PolicyResult blockTerminalObjectiveExposure(
            String actionId, boolean terminalExposure) {
        Objects.requireNonNull(actionId, "actionId");
        if (!terminalExposure) {
            return new PolicyResult(
                    "DEPLOY_TERMINAL_OBJECTIVE_EXPOSURE_POLICY",
                    List.of());
        }
        return single(
                "DEPLOY_TERMINAL_OBJECTIVE_EXPOSURE_POLICY",
                PolicyOperation.hardVeto(
                        actionId,
                        TraceRuleId.of(
                                "DEPLOY.OBJECTIVE.TERMINAL_ACTOR_EXPOSURE"),
                        TraceDomainId.DEPLOY_SITING,
                        TraceOutputKind.VETO,
                        "Do not deploy the objective's terminal actor into the exact conjunction that can place the objective out of play"));
    }

    public static PolicyResult blockRequiredCardInactivation(
            String actionId, boolean inactivatesRequiredCard) {
        Objects.requireNonNull(actionId, "actionId");
        if (!inactivatesRequiredCard) {
            return new PolicyResult(
                    "DEPLOY_REQUIRED_CARD_INACTIVATION_POLICY",
                    List.of());
        }
        return single(
                "DEPLOY_REQUIRED_CARD_INACTIVATION_POLICY",
                PolicyOperation.hardVeto(
                        actionId,
                        TraceRuleId.of(
                                "DEPLOY.OBJECTIVE.REQUIRED_CARD_INACTIVATION"),
                        TraceDomainId.DEPLOY_SITING,
                        TraceOutputKind.VETO,
                        "Do not deploy a card that suspends an active card required by the objective"));
    }

    public static PolicyResult scoreActorRouteStaging(
            String actionId,
            boolean stagesActorRoute,
            String actorTitle,
            String locationTitle) {
        Objects.requireNonNull(actionId, "actionId");
        if (!stagesActorRoute) {
            return new PolicyResult(
                    "DEPLOY_ACTOR_ROUTE_STAGING_POLICY", List.of());
        }
        return single("DEPLOY_ACTOR_ROUTE_STAGING_POLICY",
                add(actionId,
                        "DEPLOY.OBJECTIVE.ACTOR_ROUTE_STAGING",
                        TraceOutputKind.BANDED, 1000.0f,
                        "Deploy "
                                + (actorTitle != null
                                    ? actorTitle : "typed actor")
                                + " to "
                                + (locationTitle != null
                                    ? locationTitle : "this site")
                                + " to stage the exact flip-gate route"));
    }

    public static PolicyResult scoreKeyCharacter(KeyCharacterFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return single("DEPLOY_KEY_CHARACTER_POLICY",
                add(facts.actionId(), "V67ak", TraceOutputKind.BANDED,
                        800.0f, String.format(
                                "V67ak KEY CHARACTER: %s is named in objective/epic-event text — deploy first to enable flip!",
                                facts.cardTitle())));
    }

    public static CloudCityEngineEvaluation evaluateCloudCityEngine(
            CloudCityEngineFacts facts) {
        Objects.requireNonNull(facts, "facts");
        PolicyOperation operation;
        CloudCityEngineOutcome outcome;
        if (!facts.occupyBespin()) {
            operation = add(facts.actionId(), "V22.7", TraceOutputKind.VETO,
                    -800.0f, "V22.7 BLOCKED: " + facts.cardTitle()
                            + " will SELF-CANCEL — we don't occupy Bespin system!");
            outcome = CloudCityEngineOutcome.BLOCKED;
        } else if (!facts.effectAlreadyOnTable()) {
            operation = add(facts.actionId(), "V24", TraceOutputKind.BANDED,
                    300.0f, "V24 TDIGWATT ENGINE: Deploy " + facts.cardTitle()
                            + " NOW — enables objective damage engine!");
            outcome = CloudCityEngineOutcome.ENGINE_PRIORITY;
        } else {
            operation = add(facts.actionId(), "V22.7", TraceOutputKind.BANDED,
                    50.0f, "V22.7: We occupy Bespin — safe to deploy "
                            + facts.cardTitle());
            outcome = CloudCityEngineOutcome.SAFE;
        }
        return new CloudCityEngineEvaluation(
                single("DEPLOY_CLOUD_CITY_ENGINE_POLICY", operation), outcome);
    }

    public static PolicyResult scoreGherant(GherantFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return single("DEPLOY_GHERANT_POLICY",
                add(facts.actionId(), "V24.1", TraceOutputKind.BANDED,
                        150.0f,
                        "V24.1 GHERANT: Deploys an Executor site — free location + force generation!"));
    }

    public static MustContestEvaluation evaluateMustContest(
            MustContestFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.opponentPower() > 0.0f
                && facts.ourPower() < facts.opponentPower()) {
            PolicyOperation operation = add(facts.actionId(),
                    "V22.7-MUST-CONTEST", TraceOutputKind.BANDED, 300.0f,
                    "V22.7 MUST CONTEST: Opponent controls objective-critical "
                            + facts.locationTitle()
                            + "! Deploy ship to contest!");
            return new MustContestEvaluation(
                    single("DEPLOY_MUST_CONTEST_POLICY", operation),
                    MustContestOutcome.MUST_CONTEST);
        }
        return new MustContestEvaluation(
                new PolicyResult("DEPLOY_MUST_CONTEST_POLICY", List.of()),
                MustContestOutcome.NONE);
    }

    public static IsbAgentEvaluation evaluateIsbAgent(IsbAgentFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.isbAgent()) {
            return new IsbAgentEvaluation(
                    new PolicyResult("DEPLOY_ISB_AGENT_POLICY", List.of()),
                    IsbAgentOutcome.NON_ISB, 0, 0);
        }

        boolean needMoreAgents = facts.preFlip()
                && facts.agentsOnTable() < facts.agentsNeeded();
        int agentsStillNeeded = facts.agentsNeeded() - facts.agentsOnTable();
        float score;
        if (needMoreAgents) {
            score = 200.0f + (4 - agentsStillNeeded) * 30.0f;
        } else if (facts.preFlip()) {
            score = 100.0f;
        } else {
            score = 80.0f;
        }
        if (facts.battleground()) {
            score += 60.0f;
        }
        if (facts.ability() >= 5.0f) {
            score += 40.0f;
        } else if (facts.ability() >= 3.0f) {
            score += 15.0f;
        }

        String reason = "V29.7 ISB AGENT: Deploy ISB agent (ability "
                + String.format("%.0f", facts.ability()) + ", "
                + facts.agentsOnTable() + "/" + facts.agentsNeeded()
                + " on table)"
                + (facts.battleground() ? " to BATTLEGROUND" : "")
                + (needMoreAgents
                        ? " — NEED " + agentsStillNeeded + " MORE FOR FLIP!"
                        : "");
        PolicyOperation operation = add(facts.actionId(), "V29.7-ISB",
                TraceOutputKind.BANDED, score, reason);
        return new IsbAgentEvaluation(
                single("DEPLOY_ISB_AGENT_POLICY", operation),
                IsbAgentOutcome.ISB_AGENT, score, agentsStillNeeded);
    }

    public static HuntDownEvaluation evaluateHuntDownCharacter(
            HuntDownFacts facts) {
        Objects.requireNonNull(facts, "facts");
        PolicyOperation operation = null;
        HuntDownOutcome outcome = HuntDownOutcome.NONE;

        if (facts.vader()) {
            float score = facts.battleground() ? 400.0f : 300.0f;
            operation = add(facts.actionId(), "V25-HUNT-DOWN-VADER",
                    TraceOutputKind.BANDED, score,
                    "V25 HUNT DOWN: DEPLOY VADER! Critical for flip!"
                            + (facts.battleground()
                                    ? " LIVE BATTLEGROUND = ADVANCES FLIP!"
                                    : ""));
            outcome = HuntDownOutcome.VADER;
        } else if (!facts.vaderOnTable() && facts.preFlip()) {
            if (facts.inquisitor()) {
                operation = add(facts.actionId(), "V25-HUNT-DOWN-INQUISITOR",
                        TraceOutputKind.BANDED, -80.0f,
                        "V25 HUNT DOWN: Inquisitor OK but save Force for Vader first!");
                outcome = HuntDownOutcome.INQUISITOR;
            } else {
                operation = add(facts.actionId(), "V25-HUNT-DOWN-SAVE",
                        TraceOutputKind.BANDED, -200.0f,
                        "V25 HUNT DOWN: SAVE FORCE FOR VADER! He must be deployed first!");
                outcome = HuntDownOutcome.SAVE_FOR_VADER;
            }
        }

        PolicyResult result = operation == null
                ? new PolicyResult("DEPLOY_HUNT_DOWN_POLICY", List.of())
                : single("DEPLOY_HUNT_DOWN_POLICY", operation);
        return new HuntDownEvaluation(result, outcome);
    }

    public static CloudCitySpreadEvaluation evaluateCloudCitySpread(
            CloudCitySpreadFacts facts) {
        Objects.requireNonNull(facts, "facts");
        PolicyOperation operation;
        CloudCitySpreadOutcome outcome;

        if (facts.landoAlone()) {
            operation = add(facts.actionId(), "V24.13-LANDO-SUPPORT",
                    TraceOutputKind.BANDED, 250.0f,
                    "V24.13 LANDO SUPPORT: Lando is ALONE here — MUST reinforce!");
            outcome = CloudCitySpreadOutcome.LANDO_SUPPORT;
        } else if (facts.abilityHere() > 0.0f
                && facts.abilityHere() < 6.0f) {
            float score = 100.0f + (6.0f - facts.abilityHere()) * 15.0f;
            operation = add(facts.actionId(), "V25-CC-REINFORCE",
                    TraceOutputKind.BANDED, score,
                    "V25 REINFORCE: Site has ability "
                            + String.format("%.0f", facts.abilityHere())
                            + " — need 6 to hold!");
            outcome = CloudCitySpreadOutcome.REINFORCE;
        } else if (facts.abilityHere() <= 0.0f) {
            if (facts.insecureLocations() > 0) {
                operation = add(facts.actionId(), "V25-CC-SPREAD-DEFER",
                        TraceOutputKind.BANDED, 40.0f,
                        "V25 SPREAD: New CC location but "
                                + facts.insecureLocations()
                                + " site(s) need more ability first");
                outcome = CloudCitySpreadOutcome.SPREAD_DEFER;
            } else {
                operation = add(facts.actionId(), "V25-CC-SPREAD",
                        TraceOutputKind.BANDED, 120.0f,
                        "V25 SPREAD: All held sites have 6+ ability — spread for more occupation damage!");
                outcome = CloudCitySpreadOutcome.SPREAD;
            }
        } else if (facts.insecureLocations() > 0
                || facts.emptyLocations() > 0) {
            operation = add(facts.actionId(), "V25-CC-SECURE-REDIRECT",
                    TraceOutputKind.BANDED, -40.0f,
                    "V25 SECURE: Site already has "
                            + String.format("%.0f", facts.abilityHere())
                            + " ability — other sites need help");
            outcome = CloudCitySpreadOutcome.SECURE_REDIRECT;
        } else {
            operation = add(facts.actionId(), "V25-CC-SECURE",
                    TraceOutputKind.BANDED, 20.0f,
                    "V25 SECURE: All CC sites have 6+ ability — extra defense OK");
            outcome = CloudCitySpreadOutcome.SECURE;
        }
        return new CloudCitySpreadEvaluation(
                single("DEPLOY_CLOUD_CITY_SPREAD_POLICY", operation), outcome);
    }

    public static LandoSafetyEvaluation evaluateLandoSafety(
            LandoSafetyFacts facts) {
        Objects.requireNonNull(facts, "facts");
        PolicyOperation operation = null;
        LandoSafetyOutcome outcome = LandoSafetyOutcome.NONE;

        if (facts.lando()) {
            if (facts.opponentCharactersHere() > 0
                    && facts.friendlyCharactersHere() == 0) {
                operation = add(facts.actionId(), "V41-LANDO-INTO-ENEMY",
                        TraceOutputKind.VETO, -9999.0f,
                        "V41 LANDO INTO ENEMY: " + facts.opponentCharactersHere()
                                + " opponents at " + facts.locationTitle()
                                + " — Lando dies instantly! BLOCKED!");
                outcome = LandoSafetyOutcome.BLOCKED_ENEMY;
            } else if (facts.friendlyCharactersHere() > 0) {
                outcome = LandoSafetyOutcome.SAFE_FRIENDLY;
            } else if (facts.charactersInHand() < 1) {
                operation = add(facts.actionId(), "V47-LANDO-ALONE",
                        TraceOutputKind.VETO, -9999.0f,
                        "V47 LANDO ALONE BLOCK: No protection at "
                                + facts.locationTitle()
                                + " and no characters in hand — Lando dies alone!");
                outcome = LandoSafetyOutcome.BLOCKED_ALONE;
            } else if (facts.opponentThreatensCloudCity()) {
                operation = add(facts.actionId(), "V41-LANDO-CAUTION",
                        TraceOutputKind.BANDED, -400.0f,
                        "V41 LANDO CAUTION: Alone at " + facts.locationTitle()
                                + " — opponent at CC sites! Deploy protector first!");
                outcome = LandoSafetyOutcome.CAUTION;
            } else {
                outcome = LandoSafetyOutcome.SAFE_HAND;
            }
        }

        PolicyResult result = operation == null
                ? new PolicyResult("DEPLOY_LANDO_SAFETY_POLICY", List.of())
                : single("DEPLOY_LANDO_SAFETY_POLICY", operation);
        return new LandoSafetyEvaluation(result, outcome);
    }

    public static TdgwattOffObjectiveEvaluation evaluateTdgwattOffObjective(
            TdgwattOffObjectiveFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        boolean tdgwattBlocked = false;
        boolean opponentPlanet = false;

        if (facts.character() && facts.needsBespinPresence()) {
            operations.add(add(facts.actionId(), "V29-TDIGWATT-OFF-OBJECTIVE",
                    TraceOutputKind.BANDED, -500.0f,
                    "V29 TDIGWATT: Do NOT deploy characters to non-Cloud City locations!"));
            tdgwattBlocked = true;
            if (facts.opponentPlanet()) {
                operations.add(add(facts.actionId(), "V29-OPPONENT-PLANET",
                        TraceOutputKind.BANDED, -300.0f,
                        "V29 OPPONENT PLANET: This is the opponent's territory!"));
                opponentPlanet = true;
            }
        }

        return new TdgwattOffObjectiveEvaluation(
                new PolicyResult("DEPLOY_TDIGWATT_OFF_OBJECTIVE_POLICY",
                        operations), tdgwattBlocked, opponentPlanet);
    }

    public static ObjectiveTailEvaluation evaluateObjectiveTail(
            ObjectiveTailFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        boolean fortificationNeeded = false;
        boolean postFlipProtected = false;

        if (!facts.objectiveLocation() && !facts.flipBackLocation()) {
            if (facts.objectiveLocationNeedsHelp()) {
                float score = facts.flipped() ? -180.0f : -120.0f;
                if (facts.worstDeficit() > 6.0f) {
                    score -= 40.0f;
                }
                operations.add(add(facts.actionId(), "V22.2-OBJECTIVE-HELP",
                        TraceOutputKind.BANDED, score,
                        "V22.2: Objective locations need fortifying"
                                + (facts.flipped()
                                        ? " (POST-FLIP CRITICAL)" : "")
                                + " - don't deploy elsewhere"));
                fortificationNeeded = true;
            } else {
                operations.add(add(facts.actionId(), "V22-NON-OBJECTIVE",
                        TraceOutputKind.BANDED,
                        facts.flipped() ? -60.0f : -40.0f,
                        "V22: Non-objective location - prefer own locations"));
            }
        } else if (facts.flipped() && facts.flipBackLocation()) {
            operations.add(add(facts.actionId(), "V22.2-POST-FLIP-PROTECT",
                    TraceOutputKind.BANDED, 60.0f,
                    "V22.2 POST-FLIP: Deploying to protect flipped objective!"));
            postFlipProtected = true;
        }

        return new ObjectiveTailEvaluation(
                new PolicyResult("DEPLOY_OBJECTIVE_TAIL_POLICY", operations),
                fortificationNeeded, postFlipProtected);
    }

    public static LandoDestinationEvaluation evaluateLandoDestination(
            LandoDestinationFacts facts) {
        Objects.requireNonNull(facts, "facts");
        PolicyOperation operation = null;
        LandoDestinationOutcome outcome = LandoDestinationOutcome.NONE;

        if (facts.landoCharacter()) {
            String locationTitle = facts.locationTitle().toLowerCase(Locale.ROOT);
            if (locationTitle.contains("dining room")) {
                operation = add(facts.actionId(), "V24.10-LANDO-DINING",
                        TraceOutputKind.BANDED, 300.0f,
                        "V24.10 LANDO TO DINING ROOM: Optimal deploy — establishes occupation, can move to other sites!");
                outcome = LandoDestinationOutcome.DINING_ROOM;
            } else if (locationTitle.contains("cloud city")
                    || locationTitle.contains("upper walkway")
                    || locationTitle.contains("carbonite")
                    || locationTitle.contains("security tower")
                    || locationTitle.contains("platform")
                    || locationTitle.contains("lower corridor")) {
                operation = add(facts.actionId(), "V24.10-LANDO-CC",
                        TraceOutputKind.BANDED, -50.0f,
                        "V24.10 LANDO: CC site but not Dining Room — Lando can move here later, deploy to Dining Room first!");
                outcome = LandoDestinationOutcome.OTHER_CLOUD_CITY_SITE;
            }
        }

        PolicyResult result = operation == null
                ? new PolicyResult("DEPLOY_LANDO_DESTINATION_POLICY", List.of())
                : single("DEPLOY_LANDO_DESTINATION_POLICY", operation);
        return new LandoDestinationEvaluation(result, outcome);
    }

    public static LandoLobotEvaluation evaluateLandoLobot(
            LandoLobotFacts facts) {
        Objects.requireNonNull(facts, "facts");
        PolicyOperation operation = null;
        LandoLobotOutcome outcome = LandoLobotOutcome.NONE;
        if (facts.landoDeploy()) {
            if (facts.backupPresent()) {
                operation = add(facts.actionId(), "V29.2-LANDO",
                        TraceOutputKind.BANDED, 200.0f,
                        "V29.2 LANDO: Key piece + backup present — safe to deploy!");
                outcome = LandoLobotOutcome.LANDO_SAFE;
            } else {
                operation = add(facts.actionId(), "V47-LANDO",
                        TraceOutputKind.VETO, -9999.0f,
                        "V47 LANDO SOLO BLOCK: No friendlies at CC — Lando dies alone!");
                outcome = LandoLobotOutcome.LANDO_BLOCKED;
            }
        } else if (facts.lobotDeploy()) {
            if (facts.backupPresent()) {
                operation = add(facts.actionId(), "V29.2-LOBOT",
                        TraceOutputKind.BANDED, 150.0f,
                        "V29.2 LOBOT: Helps flip TDIGWATT + backup present!");
                outcome = LandoLobotOutcome.LOBOT_SAFE;
            } else {
                operation = add(facts.actionId(), "V47-LOBOT",
                        TraceOutputKind.VETO, -9999.0f,
                        "V47 LOBOT SOLO BLOCK: No friendlies at CC — Lobot dies alone!");
                outcome = LandoLobotOutcome.LOBOT_BLOCKED;
            }
        }
        PolicyResult result = operation == null
                ? new PolicyResult("DEPLOY_LANDO_LOBOT_POLICY", List.of())
                : single("DEPLOY_LANDO_LOBOT_POLICY", operation);
        return new LandoLobotEvaluation(result, outcome);
    }

    /**
     * Selects post-flip locations to reinforce. Exact structured requirements
     * are authoritative when present; otherwise this preserves the legacy
     * strongest-two selection and strict insertion-order tie behavior.
     */
    public static Set<String> selectPostFlipHoldLocations(
            Set<String> exactStructuredHoldLocations,
            Map<String, Float> occupiedObjectivePower) {
        return selectPostFlipHoldLocations(
                !exactStructuredHoldLocations.isEmpty(),
                exactStructuredHoldLocations,
                occupiedObjectivePower);
    }

    public static Set<String> selectPostFlipHoldLocations(
            boolean structuredAuthoritative,
            Set<String> exactStructuredHoldLocations,
            Map<String, Float> occupiedObjectivePower) {
        Objects.requireNonNull(exactStructuredHoldLocations,
                "exactStructuredHoldLocations");
        Objects.requireNonNull(occupiedObjectivePower,
                "occupiedObjectivePower");

        if (structuredAuthoritative) {
            return Collections.unmodifiableSet(
                    new LinkedHashSet<>(exactStructuredHoldLocations));
        }

        Set<String> holdLocations = new LinkedHashSet<>();
        for (int holdIndex = 0;
                holdIndex < 2 && holdIndex < occupiedObjectivePower.size();
                holdIndex++) {
            String bestLocation = null;
            float bestPower = -1.0f;
            for (Map.Entry<String, Float> entry
                    : occupiedObjectivePower.entrySet()) {
                if (!holdLocations.contains(entry.getKey())
                        && entry.getValue() > bestPower) {
                    bestPower = entry.getValue();
                    bestLocation = entry.getKey();
                }
            }
            if (bestLocation != null) {
                holdLocations.add(bestLocation);
            }
        }
        return Collections.unmodifiableSet(holdLocations);
    }

    public static FlipSitingEvaluation evaluateFlipSiting(
            FlipSitingFacts facts) {
        Objects.requireNonNull(facts, "facts");
        PolicyOperation operation = null;
        FlipSitingOutcome outcome = FlipSitingOutcome.NONE;

        if (!facts.flipped()) {
            if (facts.unoccupiedObjectiveLocations() > 0
                    && facts.deploysToUnoccupiedObjectiveLocation()) {
                float score = 250.0f;
                if (facts.huntDown() && facts.turnNumber() <= 3) {
                    score = facts.inquisitor() ? 1000.0f : 800.0f;
                } else if (facts.huntDown()) {
                    score = 500.0f;
                }
                String earlySuffix = facts.huntDown() && facts.turnNumber() <= 3
                        ? " — EARLY DEFENSE CRITICAL" : "";
                operation = add(facts.actionId(), "V36", TraceOutputKind.BANDED,
                        score, String.format(
                                "V36 DEFEND TERRITORY: Deploy to unoccupied obj location! (%d/%d occupied%s)",
                                facts.occupiedObjectiveLocations(),
                                facts.occupiedObjectiveLocations()
                                        + facts.unoccupiedObjectiveLocations(),
                                earlySuffix));
                outcome = FlipSitingOutcome.PREFLIP_DEFEND;
            } else if (facts.unoccupiedObjectiveLocations() > 0) {
                operation = add(facts.actionId(), "V31-PREFLIP",
                        TraceOutputKind.BANDED, -50.0f, String.format(
                                "V31 PRE-FLIP: %d obj locations still unoccupied — spread out instead of stacking!",
                                facts.unoccupiedObjectiveLocations()));
                outcome = FlipSitingOutcome.PREFLIP_SPREAD;
            }
        } else if (facts.deploysToHoldLocation()) {
            operation = add(facts.actionId(), "V31-POSTFLIP",
                    TraceOutputKind.BANDED, 200.0f,
                    "V31 POST-FLIP: Reinforce key hold location!");
            outcome = FlipSitingOutcome.POSTFLIP_HOLD;
        } else if (facts.deploysToAnyObjectiveLocation()
                && facts.occupiedObjectiveLocations() > 2) {
            operation = add(facts.actionId(), "V40-POSTFLIP",
                    TraceOutputKind.BANDED, 0.0f,
                    "V40 POST-FLIP: Deploying to 3rd obj loc (neutral)");
            outcome = FlipSitingOutcome.POSTFLIP_THIRD_NEUTRAL;
        }

        PolicyResult result = operation == null
                ? new PolicyResult("DEPLOY_FLIP_SITING_POLICY", List.of())
                : single("DEPLOY_FLIP_SITING_POLICY", operation);
        return new FlipSitingEvaluation(result, outcome);
    }

    public record Facts(String actionId, boolean undercoverSpy,
                        boolean objectiveAnalyzed, boolean objectiveRelevant,
                        float objectiveLocationBonus, boolean myLord,
                        boolean senator, boolean character,
                        boolean galacticSenateDestination,
                        boolean textNamesDestination,
                        boolean textRejectsDestination,
                        boolean textNamedDestinationDoomed,
                        String bareSiteTitle, float opponentPower,
                        float friendlySenatorPower) {
        public Facts {
            Objects.requireNonNull(actionId, "actionId");
            bareSiteTitle = bareSiteTitle == null ? "" : bareSiteTitle;
        }
    }

    public record CloudCityArmyFacts(String actionId, String locationTitle) {
        public CloudCityArmyFacts {
            Objects.requireNonNull(actionId, "actionId");
            locationTitle = locationTitle == null ? "" : locationTitle;
        }
    }

    public record ObjectiveFirstFacts(String actionId, String locationTitle) {
        public ObjectiveFirstFacts {
            Objects.requireNonNull(actionId, "actionId");
            locationTitle = locationTitle == null ? "" : locationTitle;
        }
    }

    public record KeyCharacterFacts(String actionId, String cardTitle) {
        public KeyCharacterFacts {
            Objects.requireNonNull(actionId, "actionId");
            cardTitle = cardTitle == null ? "" : cardTitle;
        }
    }

    public record CloudCityEngineFacts(String actionId, String cardTitle,
                                       boolean occupyBespin,
                                       boolean effectAlreadyOnTable) {
        public CloudCityEngineFacts {
            Objects.requireNonNull(actionId, "actionId");
            cardTitle = cardTitle == null ? "" : cardTitle;
        }
    }

    public record CloudCityEngineEvaluation(PolicyResult result,
                                            CloudCityEngineOutcome outcome) {
        public CloudCityEngineEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum CloudCityEngineOutcome {
        BLOCKED,
        ENGINE_PRIORITY,
        SAFE
    }

    public record GherantFacts(String actionId) {
        public GherantFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record MustContestFacts(String actionId, String locationTitle,
                                   float ourPower, float opponentPower) {
        public MustContestFacts {
            Objects.requireNonNull(actionId, "actionId");
            locationTitle = locationTitle == null ? "" : locationTitle;
        }
    }

    public record MustContestEvaluation(
            PolicyResult result, MustContestOutcome outcome) {
        public MustContestEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum MustContestOutcome {
        NONE,
        MUST_CONTEST
    }

    public record IsbAgentFacts(String actionId, boolean isbAgent,
                                float ability, int agentsOnTable,
                                int agentsNeeded, boolean preFlip,
                                boolean battleground) {
        public IsbAgentFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record IsbAgentEvaluation(PolicyResult result,
                                     IsbAgentOutcome outcome,
                                     float score,
                                     int agentsStillNeeded) {
        public IsbAgentEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum IsbAgentOutcome {
        NON_ISB,
        ISB_AGENT
    }

    public record HuntDownFacts(String actionId, boolean vader,
                                boolean inquisitor, boolean vaderOnTable,
                                boolean preFlip, boolean battleground) {
        public HuntDownFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record HuntDownEvaluation(PolicyResult result,
                                     HuntDownOutcome outcome) {
        public HuntDownEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum HuntDownOutcome {
        NONE,
        VADER,
        INQUISITOR,
        SAVE_FOR_VADER
    }

    public record CloudCitySpreadFacts(String actionId, float abilityHere,
                                       boolean landoAlone,
                                       int emptyLocations,
                                       int insecureLocations,
                                       int secureLocations) {
        public CloudCitySpreadFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record CloudCitySpreadEvaluation(PolicyResult result,
                                            CloudCitySpreadOutcome outcome) {
        public CloudCitySpreadEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum CloudCitySpreadOutcome {
        LANDO_SUPPORT,
        REINFORCE,
        SPREAD_DEFER,
        SPREAD,
        SECURE_REDIRECT,
        SECURE
    }

    public record LandoSafetyFacts(String actionId, boolean lando,
                                   String locationTitle,
                                   int friendlyCharactersHere,
                                   int opponentCharactersHere,
                                   int charactersInHand,
                                   boolean opponentThreatensCloudCity) {
        public LandoSafetyFacts {
            Objects.requireNonNull(actionId, "actionId");
            locationTitle = locationTitle == null ? "" : locationTitle;
        }
    }

    public record LandoSafetyEvaluation(PolicyResult result,
                                        LandoSafetyOutcome outcome) {
        public LandoSafetyEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum LandoSafetyOutcome {
        NONE,
        BLOCKED_ENEMY,
        SAFE_FRIENDLY,
        BLOCKED_ALONE,
        CAUTION,
        SAFE_HAND
    }

    public record TdgwattOffObjectiveFacts(String actionId, boolean character,
                                            boolean needsBespinPresence,
                                            boolean opponentPlanet) {
        public TdgwattOffObjectiveFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record TdgwattOffObjectiveEvaluation(
            PolicyResult result, boolean tdgwattBlocked,
            boolean opponentPlanet) {
        public TdgwattOffObjectiveEvaluation {
            Objects.requireNonNull(result, "result");
        }
    }

    public record ObjectiveTailFacts(String actionId, boolean flipped,
                                     boolean objectiveLocation,
                                     boolean flipBackLocation,
                                     boolean objectiveLocationNeedsHelp,
                                     float worstDeficit) {
        public ObjectiveTailFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record ObjectiveTailEvaluation(
            PolicyResult result, boolean fortificationNeeded,
            boolean postFlipProtected) {
        public ObjectiveTailEvaluation {
            Objects.requireNonNull(result, "result");
        }
    }

    public record LandoDestinationFacts(String actionId,
                                        boolean landoCharacter,
                                        String locationTitle) {
        public LandoDestinationFacts {
            Objects.requireNonNull(actionId, "actionId");
            locationTitle = locationTitle == null ? "" : locationTitle;
        }
    }

    public record LandoDestinationEvaluation(
            PolicyResult result, LandoDestinationOutcome outcome) {
        public LandoDestinationEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum LandoDestinationOutcome {
        NONE,
        DINING_ROOM,
        OTHER_CLOUD_CITY_SITE
    }

    public record LandoLobotFacts(String actionId, boolean landoDeploy,
                                  boolean lobotDeploy,
                                  boolean backupPresent) {
        public LandoLobotFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record LandoLobotEvaluation(PolicyResult result,
                                       LandoLobotOutcome outcome) {
        public LandoLobotEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum LandoLobotOutcome {
        NONE,
        LANDO_SAFE,
        LANDO_BLOCKED,
        LOBOT_SAFE,
        LOBOT_BLOCKED
    }

    public record FlipSitingFacts(String actionId, boolean flipped,
                                  int turnNumber, boolean huntDown,
                                  boolean inquisitor,
                                  int occupiedObjectiveLocations,
                                  int unoccupiedObjectiveLocations,
                                  boolean deploysToUnoccupiedObjectiveLocation,
                                  boolean deploysToHoldLocation,
                                  boolean deploysToAnyObjectiveLocation) {
        public FlipSitingFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record FlipSitingEvaluation(PolicyResult result,
                                       FlipSitingOutcome outcome) {
        public FlipSitingEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum FlipSitingOutcome {
        NONE,
        PREFLIP_DEFEND,
        PREFLIP_SPREAD,
        POSTFLIP_HOLD,
        POSTFLIP_THIRD_NEUTRAL
    }

    private static PolicyResult single(String owner,
                                       PolicyOperation operation) {
        return new PolicyResult(owner, List.of(operation));
    }

    private static PolicyOperation add(String actionId, String ruleId,
                                       TraceOutputKind outputKind,
                                       float delta, String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                TraceDomainId.DEPLOY_SITING, outputKind, delta, reason);
    }
}
