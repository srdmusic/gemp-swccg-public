package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure DEPLOY-2 tactical scoring over facts computed by a CardSelection adapter. */
public final class DeployTacticalPolicy {

    private DeployTacticalPolicy() {
    }

    public static PolicyResult scoreV166ContestDrain(ContestDrainFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);

        boolean contestCandidate = facts.opponentPower() > 0.0f
                && facts.opponentDrain() > 0
                && facts.netDrainBalance() >= 2;
        float projectedPower = facts.ourPower() + facts.deployingPower()
                + facts.affordableWavePower();
        float effectiveOpponentPower = facts.opponentPower()
                + facts.opponentWeaponBonus();
        if (contestCandidate && projectedPower >= effectiveOpponentPower - 2.0f) {
            float score = 250.0f + Math.max(0.0f,
                    150.0f - (facts.opponentCardCount() - 1) * 50.0f);
            add(operations, facts.actionId(), "V166", score, String.format(
                    "V166 CONTEST DRAIN: opponent out-draining (net>=2) \u2014 deploy to contest %s (their drain %d, %d opp cards)",
                    facts.locationTitle(), facts.opponentDrain(),
                    facts.opponentCardCount()));
        }

        return new PolicyResult("DEPLOY_V166_CONTEST_DRAIN_POLICY", operations);
    }

    public static PolicyResult scoreV169ProtectEndangered(
            ProtectEndangeredFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);

        float reinforcementPower = facts.deployingPower()
                + facts.affordableWavePower();
        if (facts.opponentPowerExcess() > 0.0f
                && reinforcementPower >= facts.opponentPowerExcess() - 4.0f) {
            float score = 800.0f + Math.min(300.0f,
                    facts.opponentPowerExcess() * 30.0f);
            add(operations, facts.actionId(), "V169", score, String.format(
                    "V169 PROTECT: our characters at %s are outpowered by %.0f \u2014 deploy buddies to protect them (affordable reinforcement: +%.0f, reserves held: %.0f)",
                    facts.locationTitle(), facts.opponentPowerExcess(),
                    reinforcementPower, facts.reservedForce()));
        }

        return new PolicyResult("DEPLOY_V169_PROTECT_ENDANGERED_POLICY", operations);
    }

    public static PolicyResult scoreV170SpyDrainBlock(SpyDrainFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);

        if (facts.deployingSpy() && facts.opponentPresent()
                && facts.opponentDrain() >= 1) {
            float score = 600.0f + Math.min(300.0f,
                    facts.opponentDrain() * 75.0f);
            add(operations, facts.actionId(), "V170", score, String.format(
                    "V170 SPY BLOCK: deploy spy to %s \u2014 blocks opponent drain of %d (cheap denial)",
                    facts.locationTitle(), facts.opponentDrain()));
        }

        return new PolicyResult("DEPLOY_V170_SPY_DRAIN_BLOCK_POLICY", operations);
    }

    public static PolicyResult scoreV171V172Contact(ContactFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);

        if (!facts.opponentPresent() || !facts.deployingCharacter()) {
            return new PolicyResult("DEPLOY_V171_V172_CONTACT_POLICY", operations);
        }

        ContactAssessment assessment = assessV171V172Contact(facts);

        if (assessment.soloDominant()) {
            add(operations, facts.actionId(), "V172", 600.0f, String.format(
                    "V172 SOLO DOMINANCE: %s \u2014 this body alone overpowers them (%.0f vs %.0f eff, 2x) \u2014 deploy and battle",
                    facts.locationTitle(), facts.ourPower() + facts.deployingPower(),
                    facts.opponentEffectivePower()));
        } else if (assessment.waveViable()) {
            add(operations, facts.actionId(), "V171", 600.0f, String.format(
                    "V171 DEPLOY TO CONTACT: %s opponent-occupied, affordable wave projects %.0f (hit-adj %.0f) vs %.0f eff (reserves held: %.0f) \u2014 deploy directly, battle THIS turn",
                    facts.locationTitle(), assessment.projectedPower(),
                    assessment.hitAdjustedPower(),
                    facts.opponentEffectivePower(), facts.reservedForce()));
        }

        return new PolicyResult("DEPLOY_V171_V172_CONTACT_POLICY", operations);
    }

    /** Shared V171/V172 contact math for action scoring and funded plan checks. */
    public static ContactAssessment assessV171V172Contact(ContactFacts facts) {
        Objects.requireNonNull(facts, "facts");
        float projectedPower = facts.ourPower() + facts.deployingPower()
                + facts.affordableWavePower();
        float biggestBody = Math.max(facts.deployingPower(),
                facts.maxHandCharacterPower());
        float hitDiscount = Math.min(facts.armedOpponentCount(),
                (int) facts.affordableBuddyCount() + 1) * biggestBody;
        float hitAdjustedPower = projectedPower - hitDiscount;
        boolean eligible = facts.opponentPresent() && facts.deployingCharacter();
        boolean soloDominant = eligible && facts.deployingPower() > 0.0f
                && facts.opponentEffectivePower() > 0.0f
                && facts.ourPower() + facts.deployingPower()
                >= 2.0f * facts.opponentEffectivePower();
        boolean contactGapClosed = eligible
                && hitAdjustedPower >= facts.opponentEffectivePower() - 2.0f;
        boolean waveViable = eligible && !soloDominant
                && facts.handCharacterCount() >= 2
                && facts.affordableBuddyCount() >= 1.0f
                && contactGapClosed;
        return new ContactAssessment(projectedPower, hitAdjustedPower,
                contactGapClosed, soloDominant, waveViable);
    }

    /**
     * Reuses the V171/V172 contact result for a complete response formation.
     * Existing and planned ability are additional whole-formation evidence;
     * no unit-count ceiling or second combat threshold is introduced here.
     */
    public static ResponseFormationAssessment
            assessPersistentResponseFormation(ContactFacts facts,
                                               float existingFriendlyAbility,
                                               float plannedWaveAbility) {
        Objects.requireNonNull(facts, "facts");
        if (existingFriendlyAbility < 0.0f || plannedWaveAbility < 0.0f) {
            throw new IllegalArgumentException(
                    "formation ability must be nonnegative");
        }
        ContactAssessment contact = assessV171V172Contact(facts);
        float projectedAbility = existingFriendlyAbility
                + plannedWaveAbility;
        ResponseFormationRoute route = ResponseFormationRoute.NONE;
        if (contact.soloDominant()) {
            route = ResponseFormationRoute.V172_SOLO;
        } else if (contact.waveViable()) {
            route = ResponseFormationRoute.V171_WAVE;
        } else if (facts.ourPower() > 0.0f
                && facts.deployingPower() + facts.affordableWavePower()
                > 0.0f
                && contact.contactGapClosed()
                && projectedAbility >= 4.0f) {
            route = ResponseFormationRoute
                    .EXISTING_FORMATION_REINFORCEMENT;
        }
        return new ResponseFormationAssessment(route,
                contact.projectedPower(), projectedAbility,
                route != ResponseFormationRoute.NONE);
    }

    public static DrainContestEvaluation evaluateV53V51Drain(
            DrainContestFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(2);
        List<DrainContestOutcome> outcomes = new ArrayList<>(2);

        float effectiveOurPower = facts.ourPower();
        if (facts.undercoverSpyPower() > 0.0f) {
            add(operations, facts.actionId(), "V53", 200.0f, String.format(
                    "V53 SPY ALLY: Our spy at %s has power %.0f — deploy here to flip and fight together!",
                    facts.locationTitle(), facts.undercoverSpyPower()));
            outcomes.add(DrainContestOutcome.SPY_ALLY);
            effectiveOurPower += facts.undercoverSpyPower();
        }

        if (facts.opponentPower() > 0.0f) {
            if (facts.opponentDrain() >= 3.0f && effectiveOurPower == 0.0f) {
                add(operations, facts.actionId(), "V51", 600.0f, String.format(
                        "V51 DRAIN EMERGENCY: %s drains %.0f at %s — FLOOD this location!",
                        facts.opponentId(), facts.opponentDrain(),
                        facts.locationTitle()));
                outcomes.add(DrainContestOutcome.DRAIN_EMERGENCY);
            } else if (facts.opponentDrain() >= 3.0f
                    && effectiveOurPower > 0.0f) {
                add(operations, facts.actionId(), "V51", 500.0f, String.format(
                        "V51 DRAIN REINFORCE: %s drains %.0f at %s — keep piling on!",
                        facts.opponentId(), facts.opponentDrain(),
                        facts.locationTitle()));
                outcomes.add(DrainContestOutcome.DRAIN_REINFORCE);
            } else if (facts.opponentDrain() >= 2.0f && effectiveOurPower == 0.0f) {
                add(operations, facts.actionId(), "V51", 500.0f, String.format(
                        "V51 CONTEST BATTLEGROUND: %s drains %.0f at %s — this is THE decisive fight!",
                        facts.opponentId(), facts.opponentDrain(),
                        facts.locationTitle()));
                outcomes.add(DrainContestOutcome.CONTEST_BATTLEGROUND);
            } else if (facts.opponentDrain() >= 2.0f
                    && effectiveOurPower > 0.0f) {
                add(operations, facts.actionId(), "V51", 500.0f, String.format(
                        "V51 REINFORCE BATTLEGROUND: %s drains %.0f at %s — reinforce for battle!",
                        facts.opponentId(), facts.opponentDrain(),
                        facts.locationTitle()));
                outcomes.add(DrainContestOutcome.REINFORCE_BATTLEGROUND);
            } else if (effectiveOurPower == 0.0f) {
                float score = 200.0f + facts.opponentDrain() * 100.0f;
                add(operations, facts.actionId(), "V36", score, String.format(
                        "V36 CONTEST DRAIN: %s drains %.0f at %s UNCONTESTED — deploy to stop the bleeding!",
                        facts.opponentId(), facts.opponentDrain(),
                        facts.locationTitle()));
                outcomes.add(DrainContestOutcome.CONTEST_DRAIN);
            }
        }

        return new DrainContestEvaluation(
                new PolicyResult("DEPLOY_V53_V51_DRAIN_POLICY", operations),
                List.copyOf(outcomes));
    }

    /**
     * V296: bridge the existing V36/V51 drain contest owner into two-step
     * starship deploy decisions. A ship may chase the drain only when the
     * resulting raw power is at least tied, so the old pressure rule cannot
     * turn into a space suicide bonus.
     */
    public static DrainContestEvaluation evaluateStarshipDrainContact(
            StarshipDrainContactFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.opponentPower() <= 0.0f || facts.opponentDrain() <= 0.0f
                || facts.ourPower() + facts.deployingPower()
                < facts.opponentPower()) {
            return new DrainContestEvaluation(
                    new PolicyResult("DEPLOY_STARSHIP_DRAIN_CONTACT_POLICY", List.of()),
                    List.of());
        }
        return evaluateV53V51Drain(new DrainContestFacts(
                facts.actionId(), facts.opponentId(), facts.locationTitle(),
                facts.opponentPower(), facts.ourPower(), 0.0f,
                facts.opponentDrain()));
    }

    /**
     * Reuses the shipped V296 non-losing space-contact predicate for a whole
     * already-generated space plan. No separate space-combat threshold lives
     * in the persistent-response owner.
     */
    public static ResponseFormationAssessment
            assessPersistentSpaceResponse(
                    StarshipDrainContactFacts facts,
                    boolean operationalPackage,
                    float existingFriendlyAbility,
                    float plannedWaveAbility) {
        if (existingFriendlyAbility < 0.0f
                || plannedWaveAbility < 0.0f) {
            throw new IllegalArgumentException(
                    "formation ability must be nonnegative");
        }
        DrainContestEvaluation contact = operationalPackage
                ? evaluateStarshipDrainContact(facts)
                : new DrainContestEvaluation(new PolicyResult(
                    "DEPLOY_STARSHIP_DRAIN_CONTACT_POLICY", List.of()),
                    List.of());
        boolean viable = !contact.result().operations().isEmpty();
        return new ResponseFormationAssessment(
                viable ? ResponseFormationRoute.V296_SPACE_CONTACT
                    : ResponseFormationRoute.NONE,
                facts.ourPower() + facts.deployingPower(),
                existingFriendlyAbility + plannedWaveAbility,
                viable);
    }

    public static PolicyResult scoreV51VaderFlip(VaderFlipFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);

        if (facts.completesObjective()) {
            operations.add(PolicyOperation.add(
                    facts.actionId(), TraceRuleId.of("V51"),
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.BANDED, 300.0f,
                    String.format(
                        "V51 VADER FLIP: Deploy Vader to %s, all live flip conditions are met",
                        facts.locationTitle())));
        }

        return new PolicyResult("DEPLOY_V51_VADER_FLIP_POLICY", operations);
    }

    public static PowerDangerEvaluation evaluateV50PowerDanger(
            PowerDangerFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);

        if (facts.ourPowerAfterDeploy() >= facts.opponentPower()) {
            return new PowerDangerEvaluation(
                    new PolicyResult("DEPLOY_V50_POWER_DANGER_POLICY", operations),
                    PowerDangerOutcome.NONE);
        }

        if (facts.turnNumber() <= 3) {
            add(operations, facts.actionId(), "V50", -200.0f, String.format(
                    "V50 EARLY DANGER: Turn %d — deploying %s to %s would leave us at power %.0f vs opponent %.0f — wait for backup!",
                    facts.turnNumber(), facts.cardTitle(), facts.locationTitle(),
                    facts.ourPowerAfterDeploy(), facts.opponentPower()));
            return new PowerDangerEvaluation(
                    new PolicyResult("DEPLOY_V50_POWER_DANGER_POLICY", operations),
                    PowerDangerOutcome.EARLY_DANGER);
        }

        add(operations, facts.actionId(), "V50", 0.0f, String.format(
                "V50 LATE DEPLOY: Turn %d — deploying %s to %s despite power %.0f vs %.0f — must stay active!",
                facts.turnNumber(), facts.cardTitle(), facts.locationTitle(),
                facts.ourPowerAfterDeploy(), facts.opponentPower()));
        return new PowerDangerEvaluation(
                new PolicyResult("DEPLOY_V50_POWER_DANGER_POLICY", operations),
                PowerDangerOutcome.LATE_DEPLOY);
    }

    public static PolicyResult scoreV34DirectEngage(DirectEngageFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);

        float score = 250.0f;
        if (facts.opponentPower() >= 6.0f) {
            score += 100.0f;
        }
        if (facts.priorityTargetPresent()
                && facts.deployingPrimaryHunter()) {
            score += 600.0f;
        }
        if (facts.priorityTargetPresent()
                && facts.deployingInquisitor()) {
            score += 250.0f;
        }
        if (facts.hatredPresent() && facts.deployingInquisitor()) {
            score += facts.inquisitorHatredScore();
        }

        add(operations, facts.actionId(), "V34", score, String.format(
                "V34 DIRECT ENGAGE: Deploy %s to %s (opp power %.0f%s%s) — contest!",
                facts.cardTitle(), facts.locationTitle(), facts.opponentPower(),
                facts.priorityTargetPresent() ? " PRIORITY TARGET" : "",
                facts.hatredPresent() ? " HATRED" : ""));
        return new PolicyResult("DEPLOY_V34_DIRECT_ENGAGE_POLICY", operations);
    }

    public static PolicyResult scoreV36EmptyDeploy(EmptyDeployFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);

        add(operations, facts.actionId(), "V36", 0.0f, String.format(
                "V36 EMPTY DEPLOY: %s to %s — no opponents here%s (penalty 0)",
                facts.cardTitle(), facts.locationTitle(),
                facts.hasDrainValue() ? " but has drain icons" : ""));
        return new PolicyResult("DEPLOY_V36_EMPTY_DEPLOY_POLICY", operations);
    }

    public static PolicyResult scoreV51V43SpyPlacement(SpyPlacementFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(
                facts.highDrainTargets().size() + 2);

        for (int targetIndex = 0;
                targetIndex < facts.highDrainTargets().size(); targetIndex++) {
            SpyDrainTarget target = facts.highDrainTargets().get(targetIndex);
            String ruleId = targetIndex == 0
                    ? "V51" : "V51#" + (targetIndex + 1);
            add(operations, facts.actionId(), ruleId, 1000.0f, String.format(
                    "V51 SPY CRIPPLE: Spy at %s cuts drain from %.0f — opponent's army is WASTED!",
                    target.locationTitle(), target.opponentDrain()));
        }

        if (facts.deploysToOpponentLocation()
                && facts.highDrainTargets().isEmpty()) {
            add(operations, facts.actionId(), "V43", 200.0f,
                    "V43 SPY TO ENEMY: Deploy spy to opponent location — blocks their drain!");
        } else if (facts.deploysToFriendlyLocation()) {
            add(operations, facts.actionId(), "V43", -500.0f,
                    "V43 SPY WASTED: Spy at friendly location does NOTHING — send to opponent!");
        }

        if (!facts.opponentHasDrainTwoPlus()
                && !facts.deploysToOpponentLocation()) {
            add(operations, facts.actionId(), "V51", -300.0f,
                    "V51 SPY NO TARGET: Opponent has no drain 2+ sites — deploy a fighter instead!");
        }

        return new PolicyResult("DEPLOY_V51_V43_SPY_PLACEMENT_POLICY", operations);
    }

    public static EvazanComboEvaluation scoreEvazanCombo(
            EvazanComboFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);
        EvazanComboOutcome outcome = EvazanComboOutcome.NONE;

        if (facts.deployingEvazan() && facts.weaponPartnerInPlay()) {
            add(operations, facts.actionId(), "V24.3A-evazan", 150.0f,
                    "V24.3 EVAZAN COMBO: Weapon character on table — deploy Evazan for kill combo!");
            outcome = EvazanComboOutcome.DEPLOY_EVAZAN;
        } else if (facts.deployingWeaponCharacter()
                && facts.evazanInPlay()) {
            add(operations, facts.actionId(), "V24.3A-weapon", 100.0f,
                    "V24.3 EVAZAN COMBO: Dr. Evazan on table — deploy weapon character for kill combo!");
            outcome = EvazanComboOutcome.DEPLOY_WEAPON_CHARACTER;
        }

        return new EvazanComboEvaluation(
                new PolicyResult("DEPLOY_V24_3A_EVAZAN_COMBO_POLICY", operations),
                outcome);
    }

    public static V2415DrainEvaluation evaluateV2415Drain(
            V2415DrainFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);

        if (facts.rawDrain() <= 0.0f) {
            float penalty = -50.0f - facts.powerValue() * 10.0f;
            add(operations, facts.actionId(), "V24.15", penalty,
                    "V24.15 ZERO DRAIN: Location has 0 drain — character wasted here!");
            return new V2415DrainEvaluation(
                    new PolicyResult("DEPLOY_TACTICAL_RESIDUAL_V24_15_POLICY",
                            operations),
                    V2415DrainOutcome.ZERO_DRAIN, penalty);
        }

        boolean netNegative = facts.initiationCost() > 0.0f
                && facts.rawDrain() - facts.initiationCost() <= -2.0f;
        if (netNegative && !facts.objectiveRelevant()
                && !facts.v166Contest()) {
            float penalty = -Math.min(700.0f,
                    300.0f + facts.powerValue() * 30.0f);
            add(operations, facts.actionId(), "V24.15", penalty, String.format(
                    "V24.15 EFFECTIVE DRAIN: raw %.0f - initiate cost %.0f <= -2 at %s (a drain here is a net loss) — don't pile bodies",
                    facts.rawDrain(), facts.initiationCost(), facts.locationTitle()));
            return new V2415DrainEvaluation(
                    new PolicyResult("DEPLOY_TACTICAL_RESIDUAL_V24_15_POLICY",
                            operations),
                    V2415DrainOutcome.EFFECTIVE_DRAIN, penalty);
        }

        return new V2415DrainEvaluation(
                new PolicyResult("DEPLOY_TACTICAL_RESIDUAL_V24_15_POLICY",
                        operations),
                V2415DrainOutcome.NONE, 0.0f);
    }

    public static UniversalSpyEvaluation evaluateV59UniversalSpy(
            UniversalSpyFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);

        if (facts.opponentPower() > 0.0f && facts.ourPower() == 0.0f) {
            add(operations, facts.actionId(), "V59", 600.0f,
                    "V59 SPY UNIVERSAL: Opp has power " + (int) facts.opponentPower()
                            + ", we have 0 — IDEAL spy site, blocks their drain!");
            return universalSpyEvaluation(operations,
                    UniversalSpyOutcome.OPPONENT_ONLY);
        }
        if (facts.opponentPower() > 0.0f && facts.ourPower() > 0.0f) {
            add(operations, facts.actionId(), "V59", -200.0f,
                    "V59 SPY UNIVERSAL: Both sides present at "
                            + facts.locationTitle()
                            + " — spy blocks OWN drain while undercover");
            return universalSpyEvaluation(operations,
                    UniversalSpyOutcome.BOTH_SIDES);
        }
        if (facts.opponentPower() == 0.0f && facts.ourPower() > 0.0f) {
            add(operations, facts.actionId(), "V59", -2000.0f,
                    "V59 SPY UNIVERSAL: Only we have presence at "
                            + facts.locationTitle()
                            + " — spy would block OWN drain!");
            return universalSpyEvaluation(operations,
                    UniversalSpyOutcome.FRIENDLY_ONLY);
        }

        add(operations, facts.actionId(), "V59", -300.0f,
                "V59 SPY UNIVERSAL: " + facts.locationTitle()
                        + " is empty — no drain to block");
        return universalSpyEvaluation(operations, UniversalSpyOutcome.EMPTY);
    }

    public static ContestEvaluation evaluateV223Contest(
            ContestFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(3);

        if (facts.opponentPower() > 0.0f
                && facts.ourPower() < facts.opponentPower()) {
            float powerDifference = facts.opponentPower() - facts.ourPower();
            float contestPenalty = -80.0f;
            if (powerDifference >= 5.0f) {
                contestPenalty = -150.0f;
            }
            if (powerDifference >= 10.0f) {
                contestPenalty = -250.0f;
            }
            if (powerDifference >= 15.0f) {
                contestPenalty = -350.0f;
            }

            boolean tipsBalance = facts.deployingPower() > 0.0f
                    && facts.ourPower() + facts.deployingPower()
                    >= facts.opponentPower();
            if (tipsBalance) {
                contestPenalty = Math.min(contestPenalty + 100.0f, -20.0f);
                add(operations, facts.actionId(), "V22.3-TIPS-BALANCE", 0.0f,
                        "V22.3: Would tip balance at contested location ("
                                + (int) (facts.ourPower()
                                + facts.deployingPower()) + " vs "
                                + (int) facts.opponentPower() + ")");
            }

            float objectiveOverride = 0.0f;
            if (facts.objectiveRelevant()) {
                objectiveOverride = Math.min(200.0f,
                        Math.abs(contestPenalty) * 0.6f);
                addObjective(operations, facts.actionId(), "V22.7", objectiveOverride,
                        "V22.7: Objective-critical location, prefer contesting");
            }

            if (facts.applyContestPenalty()) {
                add(operations, facts.actionId(), "V22.3", contestPenalty,
                        "CONTESTED & LOSING (" + (int) facts.ourPower()
                                + " vs " + (int) facts.opponentPower()
                                + " power, gap=" + (int) powerDifference + ")");
            }
            return new ContestEvaluation(
                    new PolicyResult("DEPLOY_TACTICAL_RESIDUAL_V22_3_POLICY",
                            operations),
                    ContestOutcome.LOSING, contestPenalty, objectiveOverride,
                    facts.applyContestPenalty());
        }

        if (facts.opponentPower() > 0.0f
                && facts.ourPower() > facts.opponentPower() + 4.0f) {
            add(operations, facts.actionId(), "V22", -20.0f,
                    "Already winning big here");
            return new ContestEvaluation(
                    new PolicyResult("DEPLOY_TACTICAL_RESIDUAL_V22_3_POLICY",
                            operations),
                    ContestOutcome.WINNING_BIG, 0.0f, 0.0f, false);
        }

        if (facts.opponentPower() > 0.0f
                && facts.ourPower() >= facts.opponentPower()) {
            add(operations, facts.actionId(), "V22", 10.0f,
                    "Can reinforce winning position");
            return new ContestEvaluation(
                    new PolicyResult("DEPLOY_TACTICAL_RESIDUAL_V22_3_POLICY",
                            operations),
                    ContestOutcome.REINFORCE, 0.0f, 0.0f, false);
        }

        return new ContestEvaluation(
                new PolicyResult("DEPLOY_TACTICAL_RESIDUAL_V22_3_POLICY",
                        operations),
                ContestOutcome.NONE, 0.0f, 0.0f, false);
    }

    public static FallbackSpyEvaluation evaluateV2414BFallbackSpy(
            FallbackSpyFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);

        if (facts.friendlySpyPresent()) {
            add(operations, facts.actionId(), "V24.14B", -1200.0f,
                    "V24.14B SPY DOUBLED: a friendly spy already blocks here — send this spy to an open enemy drain!");
            return fallbackSpyEvaluation(operations,
                    FallbackSpyOutcome.FRIENDLY_SPY_DOUBLED);
        }
        if (facts.opponentPower() > 0.0f && facts.ourPower() == 0.0f) {
            add(operations, facts.actionId(), "V24.14B", 300.0f,
                    "V24.14B SPY: Opponent controls here, we don't — block their force drain!");
            return fallbackSpyEvaluation(operations,
                    FallbackSpyOutcome.OPPONENT_ONLY);
        }
        if (facts.opponentPower() > 0.0f && facts.ourPower() > 0.0f) {
            if (facts.ourPower() + facts.spyPower()
                    >= facts.opponentPower()) {
                add(operations, facts.actionId(), "V24.14B", 50.0f,
                        "V24.14B SPY FLIP-BUDDY: our character + this spy can contest here — OK to break cover and fight!");
                return fallbackSpyEvaluation(operations,
                        FallbackSpyOutcome.FLIP_BUDDY);
            }
            if (facts.objectiveOrFlipBackLocation()) {
                add(operations, facts.actionId(), "V24.14B", -500.0f,
                        "V24.14B SPY: Both sides at CC, can't flip-and-win — spy blocks OUR drain undercover!");
                return fallbackSpyEvaluation(operations,
                        FallbackSpyOutcome.BOTH_SIDES_OBJECTIVE);
            }
            add(operations, facts.actionId(), "V24.14B", -800.0f,
                    "V24.14B SPY: Both sides present, can't flip-and-win — spy wasted, route to an open drain!");
            return fallbackSpyEvaluation(operations,
                    FallbackSpyOutcome.BOTH_SIDES_NON_OBJECTIVE);
        }
        if (facts.opponentPower() == 0.0f && facts.ourPower() > 0.0f) {
            add(operations, facts.actionId(), "V24.14B", -2000.0f,
                    "V24.14B SPY: Only we have presence — spy blocks OUR drain!");
            return fallbackSpyEvaluation(operations,
                    FallbackSpyOutcome.FRIENDLY_ONLY);
        }

        if (facts.objectiveOrFlipBackLocation()) {
            add(operations, facts.actionId(), "V24.14B", -300.0f,
                    "V24.14B SPY: Empty CC site — don't waste spy here!");
            return fallbackSpyEvaluation(operations,
                    FallbackSpyOutcome.EMPTY_OBJECTIVE);
        }
        add(operations, facts.actionId(), "V24.14B", -100.0f,
                "V24.14B SPY: Empty non-CC location — no drain to block");
        return fallbackSpyEvaluation(operations,
                FallbackSpyOutcome.EMPTY_NON_OBJECTIVE);
    }

    public static V243BPartnerEvaluation evaluateV243BPartner(
            V243BPartnerFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);

        if (facts.comboPartnerPresent()) {
            add(operations, facts.actionId(), "V24.3B", 200.0f,
                    "V24.3 EVAZAN COMBO: Deploy here — combo partner at this site for weapon kill combo!");
            return new V243BPartnerEvaluation(
                    new PolicyResult("DEPLOY_TACTICAL_RESIDUAL_V24_3B_POLICY",
                            operations),
                    V243BPartnerOutcome.PARTNER_PRESENT);
        }

        return new V243BPartnerEvaluation(
                new PolicyResult("DEPLOY_TACTICAL_RESIDUAL_V24_3B_POLICY",
                        operations),
                V243BPartnerOutcome.NONE);
    }

    private static UniversalSpyEvaluation universalSpyEvaluation(
            List<PolicyOperation> operations, UniversalSpyOutcome outcome) {
        return new UniversalSpyEvaluation(
                new PolicyResult("DEPLOY_TACTICAL_RESIDUAL_V59_POLICY",
                        operations), outcome);
    }

    private static FallbackSpyEvaluation fallbackSpyEvaluation(
            List<PolicyOperation> operations, FallbackSpyOutcome outcome) {
        return new FallbackSpyEvaluation(
                new PolicyResult("DEPLOY_TACTICAL_RESIDUAL_V24_14B_POLICY",
                        operations), outcome);
    }

    public record V2415DrainFacts(String actionId, String locationTitle,
                                  float rawDrain, float initiationCost,
                                  float powerValue, boolean objectiveRelevant,
                                  boolean v166Contest) {
        public V2415DrainFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record V2415DrainEvaluation(PolicyResult result,
                                        V2415DrainOutcome outcome,
                                        float delta) {
        public V2415DrainEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum V2415DrainOutcome {
        NONE,
        ZERO_DRAIN,
        EFFECTIVE_DRAIN
    }

    public record UniversalSpyFacts(String actionId, String locationTitle,
                                    float opponentPower, float ourPower) {
        public UniversalSpyFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record UniversalSpyEvaluation(PolicyResult result,
                                         UniversalSpyOutcome outcome) {
        public UniversalSpyEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum UniversalSpyOutcome {
        OPPONENT_ONLY,
        BOTH_SIDES,
        FRIENDLY_ONLY,
        EMPTY
    }

    public record ContestFacts(String actionId, float ourPower,
                               float opponentPower, float deployingPower,
                               boolean objectiveRelevant,
                               boolean applyContestPenalty) {
        public ContestFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record ContestEvaluation(PolicyResult result, ContestOutcome outcome,
                                    float contestPenalty,
                                    float objectiveOverride,
                                    boolean contestPenaltyApplied) {
        public ContestEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum ContestOutcome {
        NONE,
        LOSING,
        WINNING_BIG,
        REINFORCE
    }

    public record FallbackSpyFacts(String actionId, String locationTitle,
                                   boolean friendlySpyPresent,
                                   float opponentPower, float ourPower,
                                   float spyPower,
                                   boolean objectiveOrFlipBackLocation) {
        public FallbackSpyFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record FallbackSpyEvaluation(PolicyResult result,
                                        FallbackSpyOutcome outcome) {
        public FallbackSpyEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum FallbackSpyOutcome {
        FRIENDLY_SPY_DOUBLED,
        OPPONENT_ONLY,
        FLIP_BUDDY,
        BOTH_SIDES_OBJECTIVE,
        BOTH_SIDES_NON_OBJECTIVE,
        FRIENDLY_ONLY,
        EMPTY_OBJECTIVE,
        EMPTY_NON_OBJECTIVE
    }

    public record V243BPartnerFacts(String actionId,
                                    boolean comboPartnerPresent) {
        public V243BPartnerFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record V243BPartnerEvaluation(PolicyResult result,
                                         V243BPartnerOutcome outcome) {
        public V243BPartnerEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum V243BPartnerOutcome {
        NONE,
        PARTNER_PRESENT
    }

    public record ContestDrainFacts(String actionId, String locationTitle,
                                    float opponentPower, int opponentDrain,
                                    int netDrainBalance, float ourPower,
                                    float deployingPower,
                                    float affordableWavePower,
                                    float opponentWeaponBonus,
                                    int opponentCardCount) {
        public ContestDrainFacts {
            Objects.requireNonNull(actionId, "actionId");
            locationTitle = locationTitle == null ? "" : locationTitle;
        }
    }

    public record ProtectEndangeredFacts(String actionId, String locationTitle,
                                         float opponentPowerExcess,
                                         float deployingPower,
                                         float affordableWavePower,
                                         float reservedForce) {
        public ProtectEndangeredFacts {
            Objects.requireNonNull(actionId, "actionId");
            locationTitle = locationTitle == null ? "" : locationTitle;
        }
    }

    public record SpyDrainFacts(String actionId, String locationTitle,
                                boolean deployingSpy,
                                boolean opponentPresent,
                                int opponentDrain) {
        public SpyDrainFacts {
            Objects.requireNonNull(actionId, "actionId");
            locationTitle = locationTitle == null ? "" : locationTitle;
        }
    }

    /** V173/V174 wave and reserve values are produced by the adapter, then supplied here. */
    public record ContactFacts(String actionId, String locationTitle,
                               boolean opponentPresent,
                               boolean deployingCharacter,
                               int handCharacterCount,
                               float ourPower,
                               float deployingPower,
                               float affordableWavePower,
                               float affordableBuddyCount,
                               float reservedForce,
                               float opponentEffectivePower,
                               float maxHandCharacterPower,
                               int armedOpponentCount) {
        public ContactFacts {
            Objects.requireNonNull(actionId, "actionId");
            locationTitle = locationTitle == null ? "" : locationTitle;
        }
    }

    public enum ResponseFormationRoute {
        V170_SPY,
        V171_WAVE,
        V172_SOLO,
        V296_SPACE_CONTACT,
        EXISTING_FORMATION_REINFORCEMENT,
        EXISTING_LEGAL_ALTERNATIVE,
        NOT_APPLICABLE,
        NONE
    }

    public record ResponseFormationAssessment(
            ResponseFormationRoute route,
            float projectedFriendlyPower,
            float projectedFriendlyAbility,
            boolean viable) {
        public ResponseFormationAssessment {
            Objects.requireNonNull(route, "route");
        }
    }

    public record ContactAssessment(float projectedPower,
                                    float hitAdjustedPower,
                                    boolean contactGapClosed,
                                    boolean soloDominant,
                                    boolean waveViable) {
        public boolean viable() {
            return soloDominant || waveViable;
        }
    }

    public record DrainContestFacts(String actionId, String opponentId,
                                    String locationTitle,
                                    float opponentPower, float ourPower,
                                    float undercoverSpyPower,
                                    float opponentDrain) {
        public DrainContestFacts {
            Objects.requireNonNull(actionId, "actionId");
            opponentId = opponentId == null ? "" : opponentId;
            locationTitle = locationTitle == null ? "" : locationTitle;
        }
    }

    public record StarshipDrainContactFacts(
            String actionId, String opponentId, String locationTitle,
            float opponentPower, float ourPower, float deployingPower,
            float opponentDrain) {
        public StarshipDrainContactFacts {
            Objects.requireNonNull(actionId, "actionId");
            opponentId = opponentId == null ? "" : opponentId;
            locationTitle = locationTitle == null ? "" : locationTitle;
        }
    }

    public record DrainContestEvaluation(PolicyResult result,
                                         List<DrainContestOutcome> outcomes) {
        public DrainContestEvaluation {
            Objects.requireNonNull(result, "result");
            outcomes = List.copyOf(outcomes);
        }
    }

    public enum DrainContestOutcome {
        SPY_ALLY,
        DRAIN_EMERGENCY,
        DRAIN_REINFORCE,
        CONTEST_BATTLEGROUND,
        REINFORCE_BATTLEGROUND,
        CONTEST_DRAIN
    }

    public record VaderFlipFacts(String actionId, String locationTitle,
                                 boolean completesObjective) {
        public VaderFlipFacts {
            Objects.requireNonNull(actionId, "actionId");
            locationTitle = locationTitle == null ? "" : locationTitle;
        }
    }

    public record PowerDangerFacts(String actionId, int turnNumber,
                                   String cardTitle, String locationTitle,
                                   float ourPowerAfterDeploy,
                                   float opponentPower) {
        public PowerDangerFacts {
            Objects.requireNonNull(actionId, "actionId");
            cardTitle = cardTitle == null ? "" : cardTitle;
            locationTitle = locationTitle == null ? "" : locationTitle;
        }
    }

    public record PowerDangerEvaluation(PolicyResult result,
                                        PowerDangerOutcome outcome) {
        public PowerDangerEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum PowerDangerOutcome {
        NONE,
        EARLY_DANGER,
        LATE_DEPLOY
    }

    public record DirectEngageFacts(String actionId, String cardTitle,
                                    String locationTitle,
                                    float opponentPower,
                                    boolean priorityTargetPresent,
                                    boolean hatredPresent,
                                    boolean deployingPrimaryHunter,
                                    boolean deployingInquisitor,
                                    float inquisitorHatredScore) {
        public DirectEngageFacts {
            Objects.requireNonNull(actionId, "actionId");
            cardTitle = cardTitle == null ? "" : cardTitle;
            locationTitle = locationTitle == null ? "" : locationTitle;
        }
    }

    public record EmptyDeployFacts(String actionId, String cardTitle,
                                   String locationTitle,
                                   boolean hasDrainValue) {
        public EmptyDeployFacts {
            Objects.requireNonNull(actionId, "actionId");
            cardTitle = cardTitle == null ? "" : cardTitle;
            locationTitle = locationTitle == null ? "" : locationTitle;
        }
    }

    public record SpyDrainTarget(String locationTitle,
                                 float opponentDrain) {
        public SpyDrainTarget {
            locationTitle = locationTitle == null ? "" : locationTitle;
        }
    }

    public record SpyPlacementFacts(String actionId,
                                    List<SpyDrainTarget> highDrainTargets,
                                    boolean deploysToOpponentLocation,
                                    boolean deploysToFriendlyLocation,
                                    boolean opponentHasDrainTwoPlus) {
        public SpyPlacementFacts {
            Objects.requireNonNull(actionId, "actionId");
            highDrainTargets = List.copyOf(highDrainTargets);
        }
    }

    public record EvazanComboFacts(
            String actionId, boolean deployingEvazan,
            boolean deployingWeaponCharacter,
            boolean weaponPartnerInPlay, boolean evazanInPlay) {
        public EvazanComboFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record EvazanComboEvaluation(PolicyResult result,
                                        EvazanComboOutcome outcome) {
        public EvazanComboEvaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public enum EvazanComboOutcome {
        NONE,
        DEPLOY_EVAZAN,
        DEPLOY_WEAPON_CHARACTER
    }

    private static void add(List<PolicyOperation> operations,
                            String actionId, String ruleId,
                            float delta, String reason) {
        operations.add(PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                TraceDomainId.DEPLOY_SITING, TraceOutputKind.BANDED,
                delta, reason));
    }

    private static void addObjective(
            List<PolicyOperation> operations, String actionId,
            String ruleId, float delta, String reason) {
        operations.add(PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED, delta, reason));
    }
}
