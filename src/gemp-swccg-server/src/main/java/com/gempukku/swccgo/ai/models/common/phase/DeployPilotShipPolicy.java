package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure DEPLOY-3 pilot, ship, and vehicle scoring over adapter-produced facts. */
public final class DeployPilotShipPolicy {
    public enum AdapterStep {
        FALL_THROUGH,
        CONTINUE_CANDIDATE
    }

    public record Evaluation(PolicyResult result, AdapterStep adapterStep,
                             Float resetScore) {
        public Evaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(adapterStep, "adapterStep");
        }
    }

    private DeployPilotShipPolicy() {
    }

    public static PolicyResult evaluateMatchingPilot(MatchingPilotFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(2);
        if (facts.matchingShipInHand()) {
            addAttach(operations, facts.actionId(), "V30-pilot-combo",
                    TraceOutputKind.BANDED, 1000.0f,
                    String.format("V30 MATCHING COMBO: %s + %s both in hand - deploy together NOW!",
                            facts.pilotTitle(), facts.matchingShipTitle()));
            if (!facts.objectiveLocation().isBlank()) {
                addSiting(operations, facts.actionId(), "V30-pilot-objective",
                        TraceOutputKind.BANDED, 1000.0f,
                        String.format("V30 OBJECTIVE SYSTEM: Deploy to %s - matches objective location!",
                                facts.objectiveLocation()));
            }
        } else if (facts.matchingShipInPlay()) {
            addAttach(operations, facts.actionId(), "V30-pilot-in-play",
                    TraceOutputKind.BANDED, 300.0f,
                    String.format("V30 MATCHING SHIP IN PLAY: %s is deployed - get %s aboard!",
                            facts.matchingShipTitle(), facts.pilotTitle()));
        } else if (facts.matchingShipInReserve() && facts.amsdInPlay()) {
            addAttach(operations, facts.actionId(), "V30-pilot-amsd",
                    TraceOutputKind.BANDED, -500.0f,
                    String.format("V30 AMSD AVAILABLE: %s in reserve + AMSD on table - prefer AMSD pull, manual OK as fallback",
                            facts.matchingShipTitle()));
        }
        return new PolicyResult("DEPLOY_MATCHING_PILOT_POLICY", operations);
    }

    public static PolicyResult evaluateMatchingShip(MatchingShipFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(2);
        if (facts.matchingPilotInHand()) {
            addAttach(operations, facts.actionId(), "V30-ship-combo",
                    TraceOutputKind.BANDED, 1000.0f,
                    String.format("V30 MATCHING COMBO: %s + pilot %s both in hand - deploy together NOW!",
                            facts.shipTitle(), facts.matchingPilotTitle()));
            if (!facts.objectiveLocation().isBlank()) {
                addSiting(operations, facts.actionId(), "V30-ship-objective",
                        TraceOutputKind.BANDED, 1000.0f,
                        String.format("V30 OBJECTIVE SYSTEM: Deploy to %s - matches objective!",
                                facts.objectiveLocation()));
            }
        }
        return new PolicyResult("DEPLOY_MATCHING_SHIP_POLICY", operations);
    }

    public static PolicyResult evaluateCrew(CrewFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (facts.deployingAsset()
                && !facts.assetHasPermanentPilot()
                && !facts.verifiedCrewPackage()
                && !facts.affordablePilotInHand()
                && !facts.freePilotOnTable()) {
            String reason = facts.pilotInHand()
                    ? String.format("pilot in hand but unaffordable (vehicle=%d, force=%d) - wait for force",
                            facts.assetCost(), facts.availableForce())
                    : "no Icon.PILOT or Trooper character available";
            addAttach(operations, facts.actionId(), "V30-crew-required",
                    TraceOutputKind.VETO, -1500.0f,
                    "VEHICLE/SHIP NEEDS PILOT: " + reason + " - useless solo");
        } else if (facts.deployingPilotCandidate()
                && !facts.unmannedAssetTitle().isBlank()) {
            addAttach(operations, facts.actionId(), "V30-crew-unmanned",
                    TraceOutputKind.BANDED, 400.0f,
                    "PILOT FOR UNMANNED VEHICLE/SHIP: '" + facts.unmannedAssetTitle()
                            + "' on table without a pilot - get this pilot aboard!");
        }
        return new PolicyResult("DEPLOY_CREW_POLICY", operations);
    }

    public static PolicyResult evaluateShipAbility(ShipAbilityFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(2);
        if (facts.matchingPilotAffordable() && !facts.matchingPilotTitle().isBlank()) {
            addAttach(operations, facts.actionId(), "V35.6-named-pilot",
                    TraceOutputKind.BANDED, 300.0f,
                    String.format("V35.6 NAMED PILOT: %s has matching pilot %s in hand (ability %.0f+%.0f=%.0f) - deploy together!",
                            facts.shipTitle(), facts.matchingPilotTitle(),
                            facts.shipAbility(), facts.matchingPilotAbility(),
                            facts.totalAbilityWithPilot()));
        }

        if (facts.shipAbility() < 4.0f) {
            if (!facts.anyPilotHelps()) {
                addAttach(operations, facts.actionId(), "V35.6-ability",
                        TraceOutputKind.BANDED, -50.0f,
                        String.format("V40 SHIP ABILITY: %s ability %.0f - no pilot can reach 4 (mild warning)",
                                facts.shipTitle(), facts.shipAbility()));
            } else if (!facts.matchingPilotAffordable()) {
                addAttach(operations, facts.actionId(), "V35.6-ability",
                        TraceOutputKind.BANDED, -50.0f,
                        String.format("V40 SHIP ABILITY: %s needs pilot but can't afford both (mild warning)",
                                facts.shipTitle()));
            } else {
                addAttach(operations, facts.actionId(), "V35.6-ability",
                        TraceOutputKind.BANDED, -50.0f,
                        String.format("V40 SHIP: %s needs %s aboard for ability 4 - deploy together!",
                                facts.shipTitle(), facts.matchingPilotTitle().isBlank()
                                        ? "a pilot" : facts.matchingPilotTitle()));
            }
        }
        return new PolicyResult("DEPLOY_SHIP_ABILITY_POLICY", operations);
    }

    public static PolicyResult evaluateShipThreat(ShipThreatFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (facts.opponentPower() > 0.0f
                && facts.opponentPower() > facts.shipPower() * 1.5f) {
            addSiting(operations, facts.actionId(), "V35.5",
                    TraceOutputKind.BANDED, -100.0f,
                    String.format("V40 SHIP CAUTION: %s (power %.0f) vs opponent ships (power %.0f) at %s (mild caution)",
                            facts.shipTitle(), facts.shipPower(), facts.opponentPower(),
                            facts.systemTitle()));
        }
        return new PolicyResult("DEPLOY_SHIP_THREAT_POLICY", operations);
    }

    public static PolicyResult evaluateObjectivePilotDestination(
            ObjectivePilotDestinationFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (facts.invasionNeimoidianPilot() && !facts.capitalShipTitle().isBlank()) {
            if (facts.correctCapitalDestination()) {
                addAttach(operations, facts.actionId(), "V121",
                        TraceOutputKind.BANDED, 300.0f,
                        "V121 INVASION (CS): aboard capital ship - correct placement");
            } else {
                addAttach(operations, facts.actionId(), "V121",
                        TraceOutputKind.VETO, -1500.0f,
                        "V121 INVASION (CS): Neimoidian pilot must deploy aboard '"
                                + facts.capitalShipTitle() + "', not '"
                                + facts.destinationTitle() + "'");
            }
        }
        return new PolicyResult("DEPLOY_OBJECTIVE_PILOT_DESTINATION_POLICY", operations);
    }

    public static PolicyResult evaluateLowAbilityPilotBoarding(
            LowAbilityPilotBoardingFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.pilot() || facts.ability() == null
                || facts.ability() >= 5.0f || !facts.assetDestinationOffered()) {
            return new PolicyResult("DEPLOY_LOW_ABILITY_PILOT_BOARDING_POLICY", List.of());
        }
        if (facts.destinationIsAsset()) {
            return oneAttach(facts.actionId(), "V30-low-ability-pilot-boarding",
                    TraceOutputKind.ORDERING, 3000.0f,
                    "V30 PILOT PROTECTION: ability under 5 boards an offered vehicle or starship");
        }
        return oneAttach(facts.actionId(), "V30-low-ability-pilot-boarding",
                TraceOutputKind.VETO, -5000.0f,
                "V30 PILOT PROTECTION: ability under 5 must board an offered vehicle or starship");
    }

    public static PolicyResult evaluateAssetTail(AssetTailFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(5);
        if (facts.starshipOrVehicle()) {
            addSiting(operations, facts.actionId(), "asset-base",
                    TraceOutputKind.BANDED, 15.0f,
                    "Starship/Vehicle deployment");

            if (facts.executorOrFlagship() && facts.objectiveNeedsBespin()) {
                if (!facts.bespinOnTable()) {
                    addSiting(operations, facts.actionId(), "V24.10",
                            TraceOutputKind.VETO, -9999.0f,
                            "V24.10 EXECUTOR BLOCKED: Bespin system NOT on table - deploy Bespin FIRST!");
                } else {
                    String rule = facts.turnNumber() <= 2 ? "V24.9" : "V24.6";
                    String reason = facts.turnNumber() <= 2
                            ? "V24.9 EXECUTOR CRITICAL: Bespin on table - MUST deploy NOW!"
                            : "V24.6 EXECUTOR: Key ship for TDIGWATT - deploy to Bespin!";
                    addSiting(operations, facts.actionId(), rule,
                            TraceOutputKind.BANDED, 800.0f, reason);
                }
            }

            if (facts.objectiveNeedsBespin() && !facts.bespinPresence()) {
                if (facts.opponentAtBespin()) {
                    addSiting(operations, facts.actionId(), "V23",
                            TraceOutputKind.BANDED, 300.0f,
                            "V23 BESPIN CONTEST: Opponent controls Bespin - deploy ship to contest IMMEDIATELY!");
                } else {
                    addSiting(operations, facts.actionId(), "V23",
                            TraceOutputKind.BANDED, 250.0f,
                            "V23 BESPIN CRITICAL: Deploy ship to enable Dark Deal + CC Occupation!");
                }
            }
        }

        if (facts.pilot()) {
            addAttach(operations, facts.actionId(), "pilot-base",
                    TraceOutputKind.BANDED, 10.0f, "Pilot character");
        }

        if (facts.executorPilot()) {
            if (facts.deployingAboardShip()) {
                addAttach(operations, facts.actionId(), "V40.1",
                        TraceOutputKind.BANDED, 300.0f,
                        "V40.1 PILOT ABOARD: Deploy aboard ship!");
            } else {
                addAttach(operations, facts.actionId(), "V47",
                        TraceOutputKind.VETO, -9999.0f,
                        "V47 EXECUTOR PILOT GROUND BLOCK: " + facts.cardTitle()
                                + " must deploy aboard a ship, not to ground!");
            }
        }

        if (facts.matchingAction()) {
            addAttach(operations, facts.actionId(), "matching-pilot-base",
                    TraceOutputKind.BANDED, 30.0f,
                    "Matching pilot/ship synergy");
        }
        return new PolicyResult("DEPLOY_ASSET_TAIL_POLICY", operations);
    }

    public static PolicyResult evaluatePilotCandidate(PilotCandidateFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(3);
        addPilotQuality(operations, facts.actionId(), facts.ability(),
                facts.power(), facts.deployCost());
        return new PolicyResult("DEPLOY_PILOT_CANDIDATE_POLICY", operations);
    }

    public static PolicyResult evaluateExecutorDestination(
            ExecutorDestinationFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        if (facts.bespinSystem()) {
            addSiting(operations, facts.actionId(), "V24.10-executor-bespin",
                    TraceOutputKind.BANDED, 500.0f,
                    "V24.10 EXECUTOR TO BESPIN: This is THE correct system — entire TDIGWATT engine depends on it!");
        } else {
            addSiting(operations, facts.actionId(), "V24.10-executor-wrong-system",
                    TraceOutputKind.VETO, -9999.0f,
                    "V24.10 EXECUTOR WRONG SYSTEM: Executor MUST go to Bespin, not "
                            + facts.destinationTitle() + "!");
        }
        return new PolicyResult("DEPLOY_EXECUTOR_DESTINATION_POLICY", operations);
    }

    public static Evaluation evaluateShipBoarding(ShipBoardingFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);

        if (!facts.character() && !facts.attachedDeployment()) {
            addAttach(operations, facts.actionId(), "V29-cargo",
                    TraceOutputKind.VETO, -300.0f,
                    "⚠️ DEPLOY TO CARGO BAY = 0 POWER!");
            return new Evaluation(
                    new PolicyResult("DEPLOY_SHIP_BOARDING_POLICY", operations),
                    AdapterStep.CONTINUE_CANDIDATE, null);
        }

        if (facts.attachedDeployment()) {
            return new Evaluation(
                    new PolicyResult("DEPLOY_SHIP_BOARDING_POLICY", operations),
                    AdapterStep.FALL_THROUGH, null);
        }

        if (facts.referencedShipMatchesDestination()) {
            float bonus = facts.addsForceDrain() ? 650.0f : 600.0f;
            addAttach(operations, facts.actionId(), "V29-ship-reference",
                    TraceOutputKind.BANDED, bonus,
                    "V29 SHIP-REF: Game text mentions " + facts.matchedShipName()
                            + " — abilities activate aboard this ship!");
        } else if (!facts.matchedShipName().isBlank()) {
            addAttach(operations, facts.actionId(), "V29-other-ship-reference",
                    TraceOutputKind.BANDED, 50.0f,
                    "V29 ABOARD SHIP: Game text references "
                            + facts.matchedShipName()
                            + " (not this ship) — mild bonus for ship boarding");
        } else if (facts.executorDestination()) {
            addAttach(operations, facts.actionId(), "V29-executor",
                    TraceOutputKind.BANDED, 100.0f,
                    "V29 CHARACTER ABOARD EXECUTOR: Adds ability/power to flagship");
        } else {
            addAttach(operations, facts.actionId(), "V29-character-aboard",
                    TraceOutputKind.BANDED, 50.0f,
                    "V29 CHARACTER ABOARD SHIP: Pilot/passenger deploy");
        }

        return new Evaluation(
                new PolicyResult("DEPLOY_SHIP_BOARDING_POLICY", operations),
                AdapterStep.FALL_THROUGH, null);
    }

    public static Evaluation evaluateSimultaneousPilotGuard(
            SimultaneousPilotGuardFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);

        if (facts.starDestroyerDeploy()
                && !facts.imperialPilot()
                && !facts.firstOrderPilot()) {
            addAttach(operations, facts.actionId(), "pilot-sd-block",
                    TraceOutputKind.VETO, -500.0f,
                    "SD BLOCKED: non-Imperial/FO can't pilot Star Destroyers!");
            return new Evaluation(
                    new PolicyResult("DEPLOY_SIMULTANEOUS_PILOT_GUARD_POLICY", operations),
                    AdapterStep.CONTINUE_CANDIDATE, -500.0f);
        }

        if (facts.starDestroyerDeploy()) {
            addAttach(operations, facts.actionId(), "pilot-sd-valid",
                    TraceOutputKind.ORDERING, 100.0f,
                    "SD: Imperial/First Order pilot — valid!");
        }

        return new Evaluation(
                new PolicyResult("DEPLOY_SIMULTANEOUS_PILOT_GUARD_POLICY", operations),
                AdapterStep.FALL_THROUGH, null);
    }

    public static PolicyResult evaluateSimultaneousPilotChoice(
            SimultaneousPilotChoiceFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(3);
        if (facts.plannedPilot()) {
            addAttach(operations, facts.actionId(), "pilot-plan-match",
                    TraceOutputKind.ORDERING, 200.0f,
                    "PLANNED pilot for " + facts.shipName());
        } else {
            if (facts.deployCost() != null) {
                addAttach(operations, facts.actionId(), "pilot-deploy-cost",
                        TraceOutputKind.ORDERING,
                        Math.max(0.0f, 30.0f - facts.deployCost() * 5.0f),
                        "Deploy cost " + facts.deployCost().intValue());
            }
            if (facts.ability() != null) {
                addAttach(operations, facts.actionId(), "pilot-ability",
                        TraceOutputKind.ORDERING, facts.ability() * 10.0f,
                        "Ability " + facts.ability().intValue());
            }
            if (facts.matchingPilot()) {
                addAttach(operations, facts.actionId(), "pilot-ship-match",
                        TraceOutputKind.ORDERING, 50.0f,
                        "Matching pilot for " + facts.shipName() + "!");
            }
        }
        return new PolicyResult("DEPLOY_SIMULTANEOUS_PILOT_CHOICE_POLICY", operations);
    }

    private static void addPilotQuality(List<PolicyOperation> operations,
                                        String actionId, Float ability,
                                        Float power, Float deployCost) {
        if (ability != null) {
            addAttach(operations, actionId, "pilot-ability",
                    TraceOutputKind.ORDERING, ability * 10.0f,
                    "Ability " + ability.intValue());
        }
        if (power != null && power >= 3.0f) {
            addAttach(operations, actionId, "pilot-power",
                    TraceOutputKind.ORDERING, 20.0f,
                    "Good power bonus (" + power.intValue() + ")");
        }
        if (deployCost != null) {
            addAttach(operations, actionId, "pilot-deploy-cost",
                    TraceOutputKind.ORDERING,
                    Math.max(0.0f, 30.0f - deployCost * 5.0f),
                    "Deploy cost " + deployCost.intValue());
        }
    }

    public record MatchingPilotFacts(String actionId, String pilotTitle,
                                     String matchingShipTitle,
                                     boolean matchingShipInHand,
                                     boolean matchingShipInPlay,
                                     boolean matchingShipInReserve,
                                     boolean amsdInPlay,
                                     String objectiveLocation) {
        public MatchingPilotFacts {
            Objects.requireNonNull(actionId, "actionId");
            pilotTitle = pilotTitle == null ? "" : pilotTitle;
            matchingShipTitle = matchingShipTitle == null ? "" : matchingShipTitle;
            objectiveLocation = objectiveLocation == null ? "" : objectiveLocation;
        }
    }

    public record MatchingShipFacts(String actionId, String shipTitle,
                                    String matchingPilotTitle,
                                    boolean matchingPilotInHand,
                                    String objectiveLocation) {
        public MatchingShipFacts {
            Objects.requireNonNull(actionId, "actionId");
            shipTitle = shipTitle == null ? "" : shipTitle;
            matchingPilotTitle = matchingPilotTitle == null ? "" : matchingPilotTitle;
            objectiveLocation = objectiveLocation == null ? "" : objectiveLocation;
        }
    }

    public record CrewFacts(String actionId, boolean deployingAsset,
                            boolean assetHasPermanentPilot,
                            boolean verifiedCrewPackage,
                            boolean pilotInHand,
                            boolean affordablePilotInHand,
                            boolean freePilotOnTable, int assetCost,
                            int availableForce,
                            boolean deployingPilotCandidate,
                            String unmannedAssetTitle) {
        public CrewFacts {
            Objects.requireNonNull(actionId, "actionId");
            unmannedAssetTitle = unmannedAssetTitle == null ? "" : unmannedAssetTitle;
        }
    }

    public record ShipAbilityFacts(String actionId, String shipTitle,
                                   float shipAbility,
                                   boolean matchingPilotAffordable,
                                   String matchingPilotTitle,
                                   float matchingPilotAbility,
                                   float totalAbilityWithPilot,
                                   boolean anyPilotHelps) {
        public ShipAbilityFacts {
            Objects.requireNonNull(actionId, "actionId");
            shipTitle = shipTitle == null ? "" : shipTitle;
            matchingPilotTitle = matchingPilotTitle == null ? "" : matchingPilotTitle;
        }
    }

    public record ShipThreatFacts(String actionId, String shipTitle,
                                  String systemTitle, float shipPower,
                                  float opponentPower) {
        public ShipThreatFacts {
            Objects.requireNonNull(actionId, "actionId");
            shipTitle = shipTitle == null ? "" : shipTitle;
            systemTitle = systemTitle == null ? "" : systemTitle;
        }
    }

    public record ObjectivePilotDestinationFacts(
            String actionId, boolean invasionNeimoidianPilot,
            String capitalShipTitle, String destinationTitle,
            boolean correctCapitalDestination) {
        public ObjectivePilotDestinationFacts {
            Objects.requireNonNull(actionId, "actionId");
            capitalShipTitle = capitalShipTitle == null ? "" : capitalShipTitle;
            destinationTitle = destinationTitle == null ? "" : destinationTitle;
        }
    }

    public record LowAbilityPilotBoardingFacts(
            String actionId, boolean pilot, Float ability,
            boolean assetDestinationOffered, boolean destinationIsAsset) {
        public LowAbilityPilotBoardingFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record AssetTailFacts(String actionId, String cardTitle,
                                 boolean starshipOrVehicle,
                                 boolean executorOrFlagship,
                                 boolean objectiveNeedsBespin,
                                 boolean bespinOnTable, int turnNumber,
                                 boolean bespinPresence,
                                 boolean opponentAtBespin,
                                 boolean pilot, boolean executorPilot,
                                 boolean deployingAboardShip,
                                 boolean matchingAction) {
        public AssetTailFacts {
            Objects.requireNonNull(actionId, "actionId");
            cardTitle = cardTitle == null ? "" : cardTitle;
        }
    }

    public record PilotCandidateFacts(String actionId, Float ability,
                                      Float power, Float deployCost) {
        public PilotCandidateFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record ExecutorDestinationFacts(String actionId,
                                           boolean bespinSystem,
                                           String destinationTitle) {
        public ExecutorDestinationFacts {
            Objects.requireNonNull(actionId, "actionId");
            destinationTitle = destinationTitle == null ? "null" : destinationTitle;
        }
    }

    public record ShipBoardingFacts(
            String actionId, boolean character, boolean attachedDeployment,
            String matchedShipName,
            boolean referencedShipMatchesDestination,
            boolean executorDestination, boolean addsForceDrain) {
        public ShipBoardingFacts {
            Objects.requireNonNull(actionId, "actionId");
            matchedShipName = matchedShipName == null ? "" : matchedShipName;
        }
    }

    public record SimultaneousPilotGuardFacts(
            String actionId, boolean starDestroyerDeploy,
            boolean imperialPilot, boolean firstOrderPilot) {
        public SimultaneousPilotGuardFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record SimultaneousPilotChoiceFacts(
            String actionId, String shipName, boolean plannedPilot,
            Float deployCost, Float ability, boolean matchingPilot) {
        public SimultaneousPilotChoiceFacts {
            Objects.requireNonNull(actionId, "actionId");
            shipName = shipName == null ? "null" : shipName;
        }
    }

    private static void addAttach(List<PolicyOperation> operations,
                                  String actionId, String ruleId,
                                  TraceOutputKind outputKind, float delta,
                                  String reason) {
        add(operations, actionId, ruleId, TraceDomainId.DEPLOY_ATTACH,
                outputKind, delta, reason);
    }

    private static PolicyResult oneAttach(String actionId, String ruleId,
                                          TraceOutputKind outputKind, float delta,
                                          String reason) {
        List<PolicyOperation> operations = new ArrayList<>(1);
        addAttach(operations, actionId, ruleId, outputKind, delta, reason);
        return new PolicyResult("DEPLOY_LOW_ABILITY_PILOT_BOARDING_POLICY", operations);
    }

    private static void addSiting(List<PolicyOperation> operations,
                                  String actionId, String ruleId,
                                  TraceOutputKind outputKind, float delta,
                                  String reason) {
        add(operations, actionId, ruleId, TraceDomainId.DEPLOY_SITING,
                outputKind, delta, reason);
    }

    private static void add(List<PolicyOperation> operations, String actionId,
                            String ruleId, TraceDomainId domainId,
                            TraceOutputKind outputKind, float delta,
                            String reason) {
        operations.add(PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                domainId, outputKind, delta, reason));
    }
}
