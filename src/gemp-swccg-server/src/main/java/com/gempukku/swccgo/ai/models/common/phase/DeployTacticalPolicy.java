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

    private static void add(List<PolicyOperation> operations,
                            String actionId, String ruleId,
                            float delta, String reason) {
        operations.add(PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                TraceDomainId.DEPLOY_SITING, TraceOutputKind.BANDED,
                delta, reason));
    }
}
