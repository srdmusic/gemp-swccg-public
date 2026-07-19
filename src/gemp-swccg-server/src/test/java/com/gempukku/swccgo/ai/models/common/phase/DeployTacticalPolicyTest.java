package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeployTacticalPolicyTest {

    @Test
    public void v166RequiresDrainThreatAndSurvivableProjectionAtEquality() {
        assertEmpty(v166(0.0f, 2, 2, 20.0f, 0.0f,
                0.0f, 0.0f, 1));
        assertEmpty(v166(10.0f, 0, 2, 20.0f, 0.0f,
                0.0f, 0.0f, 1));
        assertEmpty(v166(10.0f, 2, 1, 20.0f, 0.0f,
                0.0f, 0.0f, 1));
        assertEmpty(v166(10.0f, 2, 2, 10.99f, 0.0f,
                0.0f, 3.0f, 1));

        PolicyResult equality = v166(10.0f, 2, 2,
                2.0f, 3.0f, 6.0f, 3.0f, 1);
        assertEquals("DEPLOY_V166_CONTEST_DRAIN_POLICY", equality.producerId());
        assertOperation(equality, "V166", 400.0f,
                "V166 CONTEST DRAIN: opponent out-draining (net>=2) \u2014 deploy to contest Cloud City: Guest Quarters (their drain 2, 1 opp cards)");
    }

    @Test
    public void v166SoftSiteBonusPreservesEveryScoreBoundary() {
        assertDelta(v166Score(1), 400.0f);
        assertDelta(v166Score(2), 350.0f);
        assertDelta(v166Score(3), 300.0f);
        assertDelta(v166Score(4), 250.0f);
        assertDelta(v166Score(9), 250.0f);
    }

    @Test
    public void v169RequiresPositiveExcessAndEnoughReinforcementAtEquality() {
        assertEmpty(v169(0.0f, 0.0f, 0.0f, 2.0f));
        assertEmpty(v169(10.0f, 2.0f, 3.99f, 2.0f));

        PolicyResult equality = v169(10.0f, 2.0f, 4.0f, 2.0f);
        assertEquals("DEPLOY_V169_PROTECT_ENDANGERED_POLICY",
                equality.producerId());
        assertOperation(equality, "V169", 1100.0f,
                "V169 PROTECT: our characters at Cloud City: Guest Quarters are outpowered by 10 \u2014 deploy buddies to protect them (affordable reinforcement: +6, reserves held: 2)");
    }

    @Test
    public void v169ExcessBonusStartsAboveEightHundredAndCapsAtElevenHundred() {
        assertDelta(v169(1.0f, 0.0f, 0.0f, 0.0f), 830.0f);
        assertDelta(v169(5.0f, 1.0f, 0.0f, 0.0f), 950.0f);
        assertDelta(v169(10.0f, 6.0f, 0.0f, 0.0f), 1100.0f);
        assertDelta(v169(20.0f, 16.0f, 0.0f, 0.0f), 1100.0f);
    }

    @Test
    public void v170RequiresSpyOpponentPresenceAndPositiveDrain() {
        assertEmpty(v170(false, true, 2));
        assertEmpty(v170(true, false, 2));
        assertEmpty(v170(true, true, 0));

        PolicyResult result = v170(true, true, 1);
        assertEquals("DEPLOY_V170_SPY_DRAIN_BLOCK_POLICY", result.producerId());
        assertOperation(result, "V170", 675.0f,
                "V170 SPY BLOCK: deploy spy to Cloud City: Guest Quarters \u2014 blocks opponent drain of 1 (cheap denial)");
    }

    @Test
    public void v170DrainBonusPreservesScaleAndCap() {
        assertDelta(v170(true, true, 1), 675.0f);
        assertDelta(v170(true, true, 2), 750.0f);
        assertDelta(v170(true, true, 3), 825.0f);
        assertDelta(v170(true, true, 4), 900.0f);
        assertDelta(v170(true, true, 12), 900.0f);
    }

    @Test
    public void v172SoloDominanceRequiresPositivePowerAndExactTwoTimesBoundary() {
        assertEmpty(contact(true, true, 1,
                2.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 4.0f, 0.0f, 0));
        assertEmpty(contact(true, true, 1,
                2.0f, 6.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 0));
        assertEmpty(contact(true, true, 1,
                2.0f, 5.99f, 0.0f, 0.0f,
                0.0f, 4.0f, 0.0f, 0));

        PolicyResult equality = contact(true, true, 1,
                2.0f, 6.0f, 0.0f, 0.0f,
                0.0f, 4.0f, 0.0f, 0);
        assertOperation(equality, "V172", 600.0f,
                "V172 SOLO DOMINANCE: Cloud City: Guest Quarters \u2014 this body alone overpowers them (8 vs 4 eff, 2x) \u2014 deploy and battle");
    }

    @Test
    public void v172PrecedesV171WhenSoloAndWaveBothQualify() {
        PolicyResult result = contact(true, true, 3,
                2.0f, 6.0f, 20.0f, 2.0f,
                3.0f, 4.0f, 7.0f, 0);

        assertEquals("DEPLOY_V171_V172_CONTACT_POLICY", result.producerId());
        assertEquals(1, result.operations().size());
        assertEquals("V172", result.operations().get(0).ruleArmId().id());
    }

    @Test
    public void v171RequiresOpponentCharacterBuddyAndHitAdjustedNearParity() {
        assertEmpty(contact(false, true, 2,
                1.0f, 3.0f, 6.0f, 1.0f,
                2.0f, 8.0f, 4.0f, 1));
        assertEmpty(contact(true, false, 2,
                1.0f, 3.0f, 6.0f, 1.0f,
                2.0f, 8.0f, 4.0f, 1));
        assertEmpty(contact(true, true, 1,
                1.0f, 3.0f, 6.0f, 1.0f,
                2.0f, 8.0f, 4.0f, 1));
        assertEmpty(contact(true, true, 2,
                1.0f, 3.0f, 6.0f, 0.99f,
                2.0f, 8.0f, 4.0f, 1));
        assertEmpty(contact(true, true, 2,
                1.0f, 3.0f, 5.99f, 1.0f,
                2.0f, 8.0f, 4.0f, 1));

        PolicyResult equality = contact(true, true, 2,
                1.0f, 3.0f, 6.0f, 1.0f,
                2.0f, 8.0f, 4.0f, 1);
        assertOperation(equality, "V171", 600.0f,
                "V171 DEPLOY TO CONTACT: Cloud City: Guest Quarters opponent-occupied, affordable wave projects 10 (hit-adj 6) vs 8 eff (reserves held: 2) \u2014 deploy directly, battle THIS turn");
    }

    @Test
    public void v171HitDiscountUsesArmedOpponentsBuddyBodiesAndBiggestBody() {
        PolicyResult twoHits = contact(true, true, 3,
                0.0f, 3.0f, 17.0f, 1.9f,
                4.0f, 12.0f, 5.0f, 5);
        assertOperation(twoHits, "V171", 600.0f,
                "V171 DEPLOY TO CONTACT: Cloud City: Guest Quarters opponent-occupied, affordable wave projects 20 (hit-adj 10) vs 12 eff (reserves held: 4) \u2014 deploy directly, battle THIS turn");

        assertEmpty(contact(true, true, 3,
                0.0f, 3.0f, 16.99f, 1.9f,
                4.0f, 12.0f, 5.0f, 5));
    }

    @Test
    public void legacyScoreBandsKeepTheirRelativeBoundaries() {
        float v166Softest = delta(v166Score(1));
        float v171 = delta(contact(true, true, 2,
                0.0f, 2.0f, 4.0f, 1.0f,
                0.0f, 6.0f, 2.0f, 0));
        float v170Minimum = delta(v170(true, true, 1));
        float v170Maximum = delta(v170(true, true, 4));
        float v169Maximum = delta(v169(10.0f, 6.0f, 0.0f, 0.0f));

        assertTrue(v166Softest < v171);
        assertTrue(v171 < v170Minimum);
        assertTrue(v170Maximum < v169Maximum);
    }

    private static PolicyResult v166(float opponentPower, int opponentDrain,
                                     int netDrainBalance, float ourPower,
                                     float deployingPower, float wavePower,
                                     float opponentWeaponBonus,
                                     int opponentCardCount) {
        return DeployTacticalPolicy.scoreV166ContestDrain(
                new DeployTacticalPolicy.ContestDrainFacts(
                        "deploy-42", "Cloud City: Guest Quarters",
                        opponentPower, opponentDrain, netDrainBalance,
                        ourPower, deployingPower, wavePower,
                        opponentWeaponBonus, opponentCardCount));
    }

    private static PolicyResult v166Score(int opponentCardCount) {
        return v166(5.0f, 2, 2, 3.0f, 2.0f,
                0.0f, 0.0f, opponentCardCount);
    }

    private static PolicyResult v169(float excess, float deployingPower,
                                     float wavePower, float reservedForce) {
        return DeployTacticalPolicy.scoreV169ProtectEndangered(
                new DeployTacticalPolicy.ProtectEndangeredFacts(
                        "deploy-42", "Cloud City: Guest Quarters",
                        excess, deployingPower, wavePower, reservedForce));
    }

    private static PolicyResult v170(boolean spy, boolean opponentPresent,
                                     int opponentDrain) {
        return DeployTacticalPolicy.scoreV170SpyDrainBlock(
                new DeployTacticalPolicy.SpyDrainFacts(
                        "deploy-42", "Cloud City: Guest Quarters",
                        spy, opponentPresent, opponentDrain));
    }

    private static PolicyResult contact(boolean opponentPresent,
                                        boolean deployingCharacter,
                                        int handCharacterCount,
                                        float ourPower,
                                        float deployingPower,
                                        float wavePower,
                                        float buddyCount,
                                        float reservedForce,
                                        float opponentEffectivePower,
                                        float maxHandCharacterPower,
                                        int armedOpponentCount) {
        return DeployTacticalPolicy.scoreV171V172Contact(
                new DeployTacticalPolicy.ContactFacts(
                        "deploy-42", "Cloud City: Guest Quarters",
                        opponentPresent, deployingCharacter,
                        handCharacterCount, ourPower, deployingPower,
                        wavePower, buddyCount, reservedForce,
                        opponentEffectivePower, maxHandCharacterPower,
                        armedOpponentCount));
    }

    private static void assertEmpty(PolicyResult result) {
        assertEquals(0, result.operations().size());
    }

    private static void assertDelta(PolicyResult result, float expected) {
        assertEquals(expected, delta(result), 0.0f);
    }

    private static float delta(PolicyResult result) {
        assertEquals(1, result.operations().size());
        return result.operations().get(0).delta();
    }

    private static void assertOperation(PolicyResult result,
                                        String ruleId,
                                        float delta,
                                        String reason) {
        assertEquals(1, result.operations().size());
        PolicyOperation operation = result.operations().get(0);
        assertEquals("deploy-42", operation.actionId());
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(TraceDomainId.DEPLOY_SITING, operation.domainId());
        assertEquals(TraceOutputKind.BANDED, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(delta, operation.delta(), 0.0f);
        assertEquals(reason, operation.reason());
    }
}
