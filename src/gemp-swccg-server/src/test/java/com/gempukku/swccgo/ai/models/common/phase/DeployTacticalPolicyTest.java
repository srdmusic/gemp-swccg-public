package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyContributionLedger;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public void v171AssessmentRejectsTheReplayFivePowerWaveIntoNine() {
        DeployTacticalPolicy.ContactAssessment replay =
                DeployTacticalPolicy.assessV171V172Contact(
                        new DeployTacticalPolicy.ContactFacts(
                                "objective-plan", "Naboo: Theed Palace Throne Room",
                                true, true, 2, 0.0f, 3.0f, 2.0f,
                                1.0f, 0.0f, 9.0f, 3.0f, 0));
        assertFalse(replay.viable());
        assertEquals(5.0f, replay.projectedPower(), 0.0f);

        DeployTacticalPolicy.ContactAssessment nearParity =
                DeployTacticalPolicy.assessV171V172Contact(
                        new DeployTacticalPolicy.ContactFacts(
                                "objective-plan", "Naboo: Theed Palace Throne Room",
                                true, true, 2, 0.0f, 4.0f, 3.0f,
                                1.0f, 0.0f, 9.0f, 4.0f, 0));
        assertTrue(nearParity.waveViable());
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
    public void v53SpyPowerPrecedesAndChangesTheV51DrainBand() {
        DeployTacticalPolicy.DrainContestEvaluation result = drainContest(
                6.0f, 0.0f, 4.0f, 3.0f);

        assertEquals(2, result.result().operations().size());
        assertOperationAt(result.result(), 0, "V53", 200.0f,
                "V53 SPY ALLY: Our spy at Cloud City: Guest Quarters has power 4 — deploy here to flip and fight together!");
        assertOperationAt(result.result(), 1, "V51", 500.0f,
                "V51 DRAIN REINFORCE: opponent drains 3 at Cloud City: Guest Quarters — keep piling on!");
        assertEquals(DeployTacticalPolicy.DrainContestOutcome.SPY_ALLY,
                result.outcomes().get(0));
        assertEquals(DeployTacticalPolicy.DrainContestOutcome.DRAIN_REINFORCE,
                result.outcomes().get(1));
    }

    @Test
    public void v51AndV36DrainContestBandsKeepExactThresholds() {
        assertDrainOutcome(drainContest(6.0f, 0.0f, 0.0f, 3.0f),
                "V51", 600.0f,
                DeployTacticalPolicy.DrainContestOutcome.DRAIN_EMERGENCY);
        assertDrainOutcome(drainContest(6.0f, 1.0f, 0.0f, 3.0f),
                "V51", 500.0f,
                DeployTacticalPolicy.DrainContestOutcome.DRAIN_REINFORCE);
        assertDrainOutcome(drainContest(6.0f, 0.0f, 0.0f, 2.0f),
                "V51", 500.0f,
                DeployTacticalPolicy.DrainContestOutcome.CONTEST_BATTLEGROUND);
        assertDrainOutcome(drainContest(6.0f, 1.0f, 0.0f, 2.0f),
                "V51", 500.0f,
                DeployTacticalPolicy.DrainContestOutcome.REINFORCE_BATTLEGROUND);
        assertDrainOutcome(drainContest(6.0f, 0.0f, 0.0f, 1.0f),
                "V36", 300.0f,
                DeployTacticalPolicy.DrainContestOutcome.CONTEST_DRAIN);
        assertEmpty(drainContest(6.0f, 1.0f, 0.0f, 1.0f).result());
        assertEmpty(drainContest(6.0f, -1.0f, 0.0f, 3.0f).result());
        assertEmpty(drainContest(0.0f, 0.0f, 0.0f, 3.0f).result());
    }

    @Test
    public void starshipDrainContactRequiresARealDrainAndNonLosingPower() {
        assertDrainOutcome(starshipDrainContact(8.0f, 0.0f, 10.0f, 2.0f),
                "V51", 500.0f,
                DeployTacticalPolicy.DrainContestOutcome.CONTEST_BATTLEGROUND);
        assertDrainOutcome(starshipDrainContact(8.0f, 10.0f, 3.0f, 2.0f),
                "V51", 500.0f,
                DeployTacticalPolicy.DrainContestOutcome.REINFORCE_BATTLEGROUND);
        assertEmpty(starshipDrainContact(8.0f, 0.0f, 3.0f, 2.0f).result());
        assertEmpty(starshipDrainContact(8.0f, 0.0f, 10.0f, 0.0f).result());
    }

    @Test
    public void v51VaderFlipRequiresAnOpponentSite() {
        assertEmpty(DeployTacticalPolicy.scoreV51VaderFlip(
                new DeployTacticalPolicy.VaderFlipFacts(
                        "deploy-42", "Cloud City: Guest Quarters", false)));
        assertOperation(DeployTacticalPolicy.scoreV51VaderFlip(
                        new DeployTacticalPolicy.VaderFlipFacts(
                                "deploy-42", "Cloud City: Guest Quarters", true)),
                "V51", 900.0f,
                "V51 VADER FLIP: Deploy Vader to Cloud City: Guest Quarters — FLIPS OBJECTIVE IMMEDIATELY!");
    }

    @Test
    public void v50PowerDangerPreservesEarlyContinueAndLateNeutralBands() {
        DeployTacticalPolicy.PowerDangerEvaluation even = powerDanger(3, 6.0f, 6.0f);
        assertEmpty(even.result());
        assertEquals(DeployTacticalPolicy.PowerDangerOutcome.NONE, even.outcome());

        DeployTacticalPolicy.PowerDangerEvaluation early = powerDanger(3, 5.99f, 6.0f);
        assertOperation(early.result(), "V50", -200.0f,
                "V50 EARLY DANGER: Turn 3 — deploying Darth Vader to Cloud City: Guest Quarters would leave us at power 6 vs opponent 6 — wait for backup!");
        assertEquals(DeployTacticalPolicy.PowerDangerOutcome.EARLY_DANGER,
                early.outcome());

        DeployTacticalPolicy.PowerDangerEvaluation late = powerDanger(4, 5.99f, 6.0f);
        assertOperation(late.result(), "V50", 0.0f,
                "V50 LATE DEPLOY: Turn 4 — deploying Darth Vader to Cloud City: Guest Quarters despite power 6 vs 6 — must stay active!");
        assertEquals(DeployTacticalPolicy.PowerDangerOutcome.LATE_DEPLOY,
                late.outcome());
    }

    @Test
    public void v34DirectEngageKeepsEveryAdditiveBonus() {
        assertDelta(directEngage(5.99f, false, false,
                false, false, 300.0f), 250.0f);
        assertDelta(directEngage(6.0f, false, false,
                false, false, 300.0f), 350.0f);

        PolicyResult allBonuses = directEngage(6.0f, true, true,
                true, true, 300.0f);
        assertOperation(allBonuses, "V34", 1500.0f,
                "V34 DIRECT ENGAGE: Deploy Darth Vader to Cloud City: Guest Quarters (opp power 6 JEDI HATRED) — contest!");
    }

    @Test
    public void v36EmptyDeployRemainsAZeroScoreReason() {
        assertOperation(DeployTacticalPolicy.scoreV36EmptyDeploy(
                        new DeployTacticalPolicy.EmptyDeployFacts(
                                "deploy-42", "Darth Vader",
                                "Cloud City: Guest Quarters", false)),
                "V36", 0.0f,
                "V36 EMPTY DEPLOY: Darth Vader to Cloud City: Guest Quarters — no opponents here (penalty 0)");
        assertOperation(DeployTacticalPolicy.scoreV36EmptyDeploy(
                        new DeployTacticalPolicy.EmptyDeployFacts(
                                "deploy-42", "Darth Vader",
                                "Cloud City: Guest Quarters", true)),
                "V36", 0.0f,
                "V36 EMPTY DEPLOY: Darth Vader to Cloud City: Guest Quarters — no opponents here but has drain icons (penalty 0)");
    }

    @Test
    public void v51SpyCripplePreservesRepeatedTargetContributions() {
        PolicyResult result = spyPlacement(
                java.util.List.of(
                        new DeployTacticalPolicy.SpyDrainTarget(
                                "Cloud City: Guest Quarters", 2.0f),
                        new DeployTacticalPolicy.SpyDrainTarget(
                                "Cloud City: Downtown Plaza", 3.0f)),
                true, false, true);

        assertEquals(2, result.operations().size());
        assertOperationAt(result, 0, "V51", 1000.0f,
                "V51 SPY CRIPPLE: Spy at Cloud City: Guest Quarters cuts drain from 2 — opponent's army is WASTED!");
        assertOperationAt(result, 1, "V51#2", 1000.0f,
                "V51 SPY CRIPPLE: Spy at Cloud City: Downtown Plaza cuts drain from 3 — opponent's army is WASTED!");

        PolicyContributionLedger ledger =
                new PolicyContributionLedger("deploy-spy-ledger");
        ledger.register(result);
        assertEquals(2, ledger.operationsFor("deploy-42").size());
    }

    @Test
    public void v43AndV51SpyFallbacksKeepTheirIndependentOrder() {
        assertOperation(spyPlacement(java.util.List.of(), true, false, false),
                "V43", 200.0f,
                "V43 SPY TO ENEMY: Deploy spy to opponent location — blocks their drain!");

        PolicyResult friendlyNoTarget = spyPlacement(
                java.util.List.of(), false, true, false);
        assertEquals(2, friendlyNoTarget.operations().size());
        assertOperationAt(friendlyNoTarget, 0, "V43", -500.0f,
                "V43 SPY WASTED: Spy at friendly location does NOTHING — send to opponent!");
        assertOperationAt(friendlyNoTarget, 1, "V51", -300.0f,
                "V51 SPY NO TARGET: Opponent has no drain 2+ sites — deploy a fighter instead!");

        assertOperation(spyPlacement(java.util.List.of(), false, true, true),
                "V43", -500.0f,
                "V43 SPY WASTED: Spy at friendly location does NOTHING — send to opponent!");
        assertOperation(spyPlacement(java.util.List.of(), false, false, false),
                "V51", -300.0f,
                "V51 SPY NO TARGET: Opponent has no drain 2+ sites — deploy a fighter instead!");
    }

    @Test
    public void evazanDeployWithWeaponPartnerKeepsExactComboBonus() {
        DeployTacticalPolicy.EvazanComboEvaluation result =
                evazanCombo(true, false, true, false);

        assertEquals(DeployTacticalPolicy.EvazanComboOutcome.DEPLOY_EVAZAN,
                result.outcome());
        assertOperation(result.result(), "V24.3A-evazan", 150.0f,
                "V24.3 EVAZAN COMBO: Weapon character on table — deploy Evazan for kill combo!");
    }

    @Test
    public void weaponCharacterDeployWithEvazanKeepsExactComboBonus() {
        DeployTacticalPolicy.EvazanComboEvaluation result =
                evazanCombo(false, true, false, true);

        assertEquals(
                DeployTacticalPolicy.EvazanComboOutcome.DEPLOY_WEAPON_CHARACTER,
                result.outcome());
        assertOperation(result.result(), "V24.3A-weapon", 100.0f,
                "V24.3 EVAZAN COMBO: Dr. Evazan on table — deploy weapon character for kill combo!");
    }

    @Test
    public void evazanBranchPrecedesWeaponBranchWhenBothFactsAreTrue() {
        DeployTacticalPolicy.EvazanComboEvaluation result =
                evazanCombo(true, true, true, true);

        assertEquals(DeployTacticalPolicy.EvazanComboOutcome.DEPLOY_EVAZAN,
                result.outcome());
        assertOperation(result.result(), "V24.3A-evazan", 150.0f,
                "V24.3 EVAZAN COMBO: Weapon character on table — deploy Evazan for kill combo!");
    }

    @Test
    public void evazanComboNoOpCasesStayNeutral() {
        for (DeployTacticalPolicy.EvazanComboEvaluation result :
                java.util.List.of(
                        evazanCombo(false, false, true, true),
                        evazanCombo(true, false, false, false),
                        evazanCombo(false, true, false, false))) {
            assertEquals(DeployTacticalPolicy.EvazanComboOutcome.NONE,
                    result.outcome());
            assertEmpty(result.result());
        }
    }

    @Test
    public void v2415PreservesZeroEffectiveAndSuppressedDrainBoundaries() {
        DeployTacticalPolicy.V2415DrainEvaluation zero = v2415(
                0.0f, 4.0f, 3.0f, false, false);
        assertEquals(DeployTacticalPolicy.V2415DrainOutcome.ZERO_DRAIN,
                zero.outcome());
        assertOperation(zero.result(), "V24.15", -80.0f,
                "V24.15 ZERO DRAIN: Location has 0 drain — character wasted here!");
        assertEquals(DeployTacticalPolicy.V2415DrainOutcome.NONE,
                v2415(Math.nextUp(0.0f), 0.0f, 3.0f,
                        false, false).outcome());

        DeployTacticalPolicy.V2415DrainEvaluation effective = v2415(
                1.0f, 3.0f, 3.0f, false, false);
        assertEquals(DeployTacticalPolicy.V2415DrainOutcome.EFFECTIVE_DRAIN,
                effective.outcome());
        assertOperation(effective.result(), "V24.15", -390.0f,
                "V24.15 EFFECTIVE DRAIN: raw 1 - initiate cost 3 <= -2 at Cloud City: Guest Quarters (a drain here is a net loss) — don't pile bodies");
        assertEquals(DeployTacticalPolicy.V2415DrainOutcome.NONE,
                v2415(1.0f, Math.nextDown(3.0f), 3.0f,
                        false, false).outcome());
        assertEquals(DeployTacticalPolicy.V2415DrainOutcome.NONE,
                v2415(1.0f, 3.0f, 3.0f, true, false).outcome());
        assertEquals(DeployTacticalPolicy.V2415DrainOutcome.NONE,
                v2415(1.0f, 3.0f, 3.0f, false, true).outcome());
        assertEquals(-700.0f, v2415(1.0f, 3.0f, 20.0f,
                false, false).delta(), 0.0f);
    }

    @Test
    public void v59UniversalSpyPreservesAllPresenceBandsAndStrictZero() {
        assertUniversalSpy(2.0f, 0.0f,
                DeployTacticalPolicy.UniversalSpyOutcome.OPPONENT_ONLY, 600.0f,
                "V59 SPY UNIVERSAL: Opp has power 2, we have 0 — IDEAL spy site, blocks their drain!");
        assertUniversalSpy(2.0f, Float.MIN_VALUE,
                DeployTacticalPolicy.UniversalSpyOutcome.BOTH_SIDES, -200.0f,
                "V59 SPY UNIVERSAL: Both sides present at Cloud City: Guest Quarters — spy blocks OWN drain while undercover");
        assertUniversalSpy(0.0f, 2.0f,
                DeployTacticalPolicy.UniversalSpyOutcome.FRIENDLY_ONLY, -2000.0f,
                "V59 SPY UNIVERSAL: Only we have presence at Cloud City: Guest Quarters — spy would block OWN drain!");
        assertUniversalSpy(0.0f, 0.0f,
                DeployTacticalPolicy.UniversalSpyOutcome.EMPTY, -300.0f,
                "V59 SPY UNIVERSAL: Cloud City: Guest Quarters is empty — no drain to block");
    }

    @Test
    public void v223PreservesTiersTipOverridePlannedAndReinforceBoundaries() {
        assertDelta(contest(0.0f, Math.nextDown(5.0f), 0.0f,
                false, true).result(), -80.0f);
        assertDelta(contest(0.0f, 5.0f, 0.0f,
                false, true).result(), -150.0f);
        assertDelta(contest(0.0f, 10.0f, 0.0f,
                false, true).result(), -250.0f);
        assertDelta(contest(0.0f, 15.0f, 0.0f,
                false, true).result(), -350.0f);

        DeployTacticalPolicy.ContestEvaluation tippedObjective = contest(
                0.0f, 10.0f, 10.0f, true, true);
        assertOperationAt(tippedObjective.result(), 0,
                "V22.3-TIPS-BALANCE", 0.0f,
                "V22.3: Would tip balance at contested location (10 vs 10)");
        assertOperationAt(tippedObjective.result(), 1, "V22.7", 90.0f,
                "V22.7: Objective-critical location — must contest!");
        assertOperationAt(tippedObjective.result(), 2, "V22.3", -150.0f,
                "CONTESTED & LOSING (0 vs 10 power, gap=10)");
        PolicyContributionLedger contestLedger =
                new PolicyContributionLedger("deploy-contest-ledger");
        contestLedger.register(tippedObjective.result());
        assertEquals(3, contestLedger.orderedOperations().size());

        DeployTacticalPolicy.ContestEvaluation plannedTarget = contest(
                0.0f, 10.0f, 10.0f, true, false);
        assertEquals(2, plannedTarget.result().operations().size());
        assertEquals(-150.0f, plannedTarget.contestPenalty(), 0.0f);
        assertTrue(!plannedTarget.contestPenaltyApplied());
        assertOperation(contest(5.0f, 1.0f, 0.0f, false, true).result(),
                "V22", 10.0f, "Can reinforce winning position");
        assertOperation(contest(Math.nextUp(5.0f), 1.0f, 0.0f,
                false, true).result(), "V22", -20.0f,
                "Already winning big here");
        assertEmpty(contest(Float.NaN, 1.0f, 0.0f,
                false, true).result());
        assertEmpty(contest(1.0f, 0.0f, 0.0f,
                false, true).result());
    }

    @Test
    public void fallbackSpyAndV243bKeepEveryDestinationOutcome() {
        assertFallbackSpy(true, 5.0f, 0.0f, 0.0f, false,
                DeployTacticalPolicy.FallbackSpyOutcome.FRIENDLY_SPY_DOUBLED,
                -1200.0f,
                "V24.14B SPY DOUBLED: a friendly spy already blocks here — send this spy to an open enemy drain!");
        assertFallbackSpy(false, 5.0f, 0.0f, 0.0f, false,
                DeployTacticalPolicy.FallbackSpyOutcome.OPPONENT_ONLY, 300.0f,
                "V24.14B SPY: Opponent controls here, we don't — block their force drain!");
        assertFallbackSpy(false, 5.0f, 3.0f, 2.0f, false,
                DeployTacticalPolicy.FallbackSpyOutcome.FLIP_BUDDY, 50.0f,
                "V24.14B SPY FLIP-BUDDY: our character + this spy can contest here — OK to break cover and fight!");
        assertFallbackSpy(false, 5.0f, 3.0f, 1.9f, true,
                DeployTacticalPolicy.FallbackSpyOutcome.BOTH_SIDES_OBJECTIVE,
                -500.0f,
                "V24.14B SPY: Both sides at CC, can't flip-and-win — spy blocks OUR drain undercover!");
        assertFallbackSpy(false, 5.0f, 3.0f, 1.9f, false,
                DeployTacticalPolicy.FallbackSpyOutcome.BOTH_SIDES_NON_OBJECTIVE,
                -800.0f,
                "V24.14B SPY: Both sides present, can't flip-and-win — spy wasted, route to an open drain!");
        assertFallbackSpy(false, 0.0f, 3.0f, 0.0f, false,
                DeployTacticalPolicy.FallbackSpyOutcome.FRIENDLY_ONLY, -2000.0f,
                "V24.14B SPY: Only we have presence — spy blocks OUR drain!");
        assertFallbackSpy(false, 0.0f, 0.0f, 0.0f, true,
                DeployTacticalPolicy.FallbackSpyOutcome.EMPTY_OBJECTIVE, -300.0f,
                "V24.14B SPY: Empty CC site — don't waste spy here!");
        assertFallbackSpy(false, 0.0f, 0.0f, 0.0f, false,
                DeployTacticalPolicy.FallbackSpyOutcome.EMPTY_NON_OBJECTIVE,
                -100.0f,
                "V24.14B SPY: Empty non-CC location — no drain to block");

        assertEmpty(v243b(false).result());
        DeployTacticalPolicy.V243BPartnerEvaluation partner = v243b(true);
        assertEquals(DeployTacticalPolicy.V243BPartnerOutcome.PARTNER_PRESENT,
                partner.outcome());
        assertOperation(partner.result(), "V24.3B", 200.0f,
                "V24.3 EVAZAN COMBO: Deploy here — combo partner at this site for weapon kill combo!");
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

    private static DeployTacticalPolicy.DrainContestEvaluation drainContest(
            float opponentPower, float ourPower, float spyPower,
            float opponentDrain) {
        return DeployTacticalPolicy.evaluateV53V51Drain(
                new DeployTacticalPolicy.DrainContestFacts(
                        "deploy-42", "opponent",
                        "Cloud City: Guest Quarters", opponentPower,
                        ourPower, spyPower, opponentDrain));
    }

    private static DeployTacticalPolicy.DrainContestEvaluation starshipDrainContact(
            float opponentPower, float ourPower, float deployingPower,
            float opponentDrain) {
        return DeployTacticalPolicy.evaluateStarshipDrainContact(
                new DeployTacticalPolicy.StarshipDrainContactFacts(
                        "deploy-42", "opponent", "Jakku",
                        opponentPower, ourPower, deployingPower,
                        opponentDrain));
    }

    private static DeployTacticalPolicy.PowerDangerEvaluation powerDanger(
            int turn, float ourPower, float opponentPower) {
        return DeployTacticalPolicy.evaluateV50PowerDanger(
                new DeployTacticalPolicy.PowerDangerFacts(
                        "deploy-42", turn, "Darth Vader",
                        "Cloud City: Guest Quarters", ourPower,
                        opponentPower));
    }

    private static PolicyResult directEngage(float opponentPower,
                                             boolean jediPresent,
                                             boolean hatredPresent,
                                             boolean deployingVader,
                                             boolean deployingInquisitor,
                                             float hatredScore) {
        return DeployTacticalPolicy.scoreV34DirectEngage(
                new DeployTacticalPolicy.DirectEngageFacts(
                        "deploy-42", "Darth Vader",
                        "Cloud City: Guest Quarters", opponentPower,
                        jediPresent, hatredPresent, deployingVader,
                        deployingInquisitor, hatredScore));
    }

    private static PolicyResult spyPlacement(
            java.util.List<DeployTacticalPolicy.SpyDrainTarget> targets,
            boolean opponentLocation, boolean friendlyLocation,
            boolean opponentHasDrainTwoPlus) {
        return DeployTacticalPolicy.scoreV51V43SpyPlacement(
                new DeployTacticalPolicy.SpyPlacementFacts(
                        "deploy-42", targets, opponentLocation,
                        friendlyLocation, opponentHasDrainTwoPlus));
    }

    private static DeployTacticalPolicy.EvazanComboEvaluation evazanCombo(
            boolean deployingEvazan, boolean deployingWeaponCharacter,
            boolean weaponPartnerInPlay, boolean evazanInPlay) {
        return DeployTacticalPolicy.scoreEvazanCombo(
                new DeployTacticalPolicy.EvazanComboFacts(
                        "deploy-42", deployingEvazan,
                        deployingWeaponCharacter, weaponPartnerInPlay,
                        evazanInPlay));
    }

    private static DeployTacticalPolicy.V2415DrainEvaluation v2415(
            float rawDrain, float initiationCost, float powerValue,
            boolean objectiveRelevant, boolean v166Contest) {
        return DeployTacticalPolicy.evaluateV2415Drain(
                new DeployTacticalPolicy.V2415DrainFacts(
                        "deploy-42", "Cloud City: Guest Quarters", rawDrain,
                        initiationCost, powerValue, objectiveRelevant,
                        v166Contest));
    }

    private static DeployTacticalPolicy.UniversalSpyEvaluation universalSpy(
            float opponentPower, float ourPower) {
        return DeployTacticalPolicy.evaluateV59UniversalSpy(
                new DeployTacticalPolicy.UniversalSpyFacts(
                        "deploy-42", "Cloud City: Guest Quarters",
                        opponentPower, ourPower));
    }

    private static void assertUniversalSpy(float opponentPower, float ourPower,
                                           DeployTacticalPolicy.UniversalSpyOutcome outcome,
                                           float score, String reason) {
        DeployTacticalPolicy.UniversalSpyEvaluation evaluation = universalSpy(
                opponentPower, ourPower);
        assertEquals(outcome, evaluation.outcome());
        assertOperation(evaluation.result(), "V59", score, reason);
    }

    private static DeployTacticalPolicy.ContestEvaluation contest(
            float ourPower, float opponentPower, float deployingPower,
            boolean objectiveRelevant, boolean applyContestPenalty) {
        return DeployTacticalPolicy.evaluateV223Contest(
                new DeployTacticalPolicy.ContestFacts("deploy-42", ourPower,
                        opponentPower, deployingPower, objectiveRelevant,
                        applyContestPenalty));
    }

    private static DeployTacticalPolicy.FallbackSpyEvaluation fallbackSpy(
            boolean friendlySpyPresent, float opponentPower, float ourPower,
            float spyPower, boolean objectiveOrFlipBackLocation) {
        return DeployTacticalPolicy.evaluateV2414BFallbackSpy(
                new DeployTacticalPolicy.FallbackSpyFacts("deploy-42",
                        "Cloud City: Guest Quarters", friendlySpyPresent,
                        opponentPower, ourPower, spyPower,
                        objectiveOrFlipBackLocation));
    }

    private static void assertFallbackSpy(boolean friendlySpyPresent,
                                          float opponentPower, float ourPower,
                                          float spyPower,
                                          boolean objectiveOrFlipBackLocation,
                                          DeployTacticalPolicy.FallbackSpyOutcome outcome,
                                          float score, String reason) {
        DeployTacticalPolicy.FallbackSpyEvaluation evaluation = fallbackSpy(
                friendlySpyPresent, opponentPower, ourPower, spyPower,
                objectiveOrFlipBackLocation);
        assertEquals(outcome, evaluation.outcome());
        assertOperation(evaluation.result(), "V24.14B", score, reason);
    }

    private static DeployTacticalPolicy.V243BPartnerEvaluation v243b(
            boolean comboPartnerPresent) {
        return DeployTacticalPolicy.evaluateV243BPartner(
                new DeployTacticalPolicy.V243BPartnerFacts("deploy-42",
                        comboPartnerPresent));
    }

    private static void assertDrainOutcome(
            DeployTacticalPolicy.DrainContestEvaluation evaluation,
            String ruleId, float score,
            DeployTacticalPolicy.DrainContestOutcome outcome) {
        assertEquals(1, evaluation.result().operations().size());
        assertEquals(ruleId,
                evaluation.result().operations().get(0).ruleArmId().id());
        assertEquals(score,
                evaluation.result().operations().get(0).delta(), 0.0f);
        assertEquals(java.util.List.of(outcome), evaluation.outcomes());
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
        assertOperationAt(result, 0, ruleId, delta, reason);
    }

    private static void assertOperationAt(PolicyResult result, int index,
                                          String ruleId,
                                          float delta, String reason) {
        PolicyOperation operation = result.operations().get(index);
        assertEquals("deploy-42", operation.actionId());
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(TraceDomainId.DEPLOY_SITING, operation.domainId());
        assertEquals(TraceOutputKind.BANDED, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(delta, operation.delta(), 0.0f);
        assertEquals(reason, operation.reason());
    }
}
