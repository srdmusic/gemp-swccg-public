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

        float projectedPower = facts.ourPower() + facts.deployingPower()
                + facts.affordableWavePower();
        float biggestBody = Math.max(facts.deployingPower(),
                facts.maxHandCharacterPower());
        float hitDiscount = Math.min(facts.armedOpponentCount(),
                (int) facts.affordableBuddyCount() + 1) * biggestBody;
        boolean soloDominant = facts.deployingPower() > 0.0f
                && facts.opponentEffectivePower() > 0.0f
                && facts.ourPower() + facts.deployingPower()
                >= 2.0f * facts.opponentEffectivePower();

        if (soloDominant) {
            add(operations, facts.actionId(), "V172", 600.0f, String.format(
                    "V172 SOLO DOMINANCE: %s \u2014 this body alone overpowers them (%.0f vs %.0f eff, 2x) \u2014 deploy and battle",
                    facts.locationTitle(), facts.ourPower() + facts.deployingPower(),
                    facts.opponentEffectivePower()));
        } else if (facts.handCharacterCount() >= 2
                && facts.affordableBuddyCount() >= 1.0f
                && projectedPower - hitDiscount
                >= facts.opponentEffectivePower() - 2.0f) {
            add(operations, facts.actionId(), "V171", 600.0f, String.format(
                    "V171 DEPLOY TO CONTACT: %s opponent-occupied, affordable wave projects %.0f (hit-adj %.0f) vs %.0f eff (reserves held: %.0f) \u2014 deploy directly, battle THIS turn",
                    facts.locationTitle(), projectedPower,
                    projectedPower - hitDiscount,
                    facts.opponentEffectivePower(), facts.reservedForce()));
        }

        return new PolicyResult("DEPLOY_V171_V172_CONTACT_POLICY", operations);
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

    public static PolicyResult scoreV51VaderFlip(VaderFlipFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>(1);

        if (facts.opponentSite()) {
            add(operations, facts.actionId(), "V51", 900.0f, String.format(
                    "V51 VADER FLIP: Deploy Vader to %s — FLIPS OBJECTIVE IMMEDIATELY!",
                    facts.locationTitle()));
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
        if (facts.jediPresent() && facts.deployingVader()) {
            score += 600.0f;
        }
        if (facts.jediPresent() && facts.deployingInquisitor()) {
            score += 250.0f;
        }
        if (facts.hatredPresent() && facts.deployingInquisitor()) {
            score += facts.inquisitorHatredScore();
        }

        add(operations, facts.actionId(), "V34", score, String.format(
                "V34 DIRECT ENGAGE: Deploy %s to %s (opp power %.0f%s%s) — contest!",
                facts.cardTitle(), facts.locationTitle(), facts.opponentPower(),
                facts.jediPresent() ? " JEDI" : "",
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
                                 boolean opponentSite) {
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
                                    boolean jediPresent,
                                    boolean hatredPresent,
                                    boolean deployingVader,
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
}
