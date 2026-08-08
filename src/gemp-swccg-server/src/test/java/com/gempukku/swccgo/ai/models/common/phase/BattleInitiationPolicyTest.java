package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BattleInitiationPolicyTest {

    @Test
    public void barrierRiskPreservesThresholdsAndHuntExpendability() {
        assertFalse(BattleInitiationPolicy.barrierRisk(
                true, 12.0f, 10.0f, 5.0f, 1, false, 0.3f).applies());

        BattleInitiationPolicy.Contribution solo =
                BattleInitiationPolicy.barrierRisk(
                        true, 12.0f, 14.0f, 5.0f, 1, false, 0.3f);
        BattleInitiationPolicy.Contribution severeHunt =
                BattleInitiationPolicy.barrierRisk(
                        true, 12.0f, 16.0f, 5.0f, 0, true, 0.3f);

        assertContribution(solo, -250.0f, "crushed");
        assertContribution(severeHunt, -105.0f, "NO ONE LEFT");
    }

    @Test
    public void objectiveAggressionPreservesExactBands() {
        assertFalse(BattleInitiationPolicy.huntAggression(
                true, false, true, true).applies());
        assertContribution(BattleInitiationPolicy.huntAggression(
                true, true, true, false), 80.0f, "Vader hunts and destroys!");
        assertContribution(BattleInitiationPolicy.huntAggression(
                true, true, true, true), 200.0f, "LUKE IS HERE");

        assertFalse(BattleInitiationPolicy.inquisitorDestiny(
                false, true, true, true).applies());
        assertContribution(BattleInitiationPolicy.inquisitorDestiny(
                true, true, false, false), 120.0f, "+1 total battle destiny");
        assertContribution(BattleInitiationPolicy.inquisitorDestiny(
                true, true, true, true), 350.0f, "+2 total battle destiny");
    }

    @Test
    public void predictionKeepsDefeatPrecedenceOverPyrrhicDamage() {
        BattleInitiationPolicy.PredictionDecision defeat =
                BattleInitiationPolicy.prediction("Site", 0.34f, 20.0f);
        BattleInitiationPolicy.PredictionDecision pyrrhic =
                BattleInitiationPolicy.prediction("Site", 0.35f, 10.0f);
        BattleInitiationPolicy.PredictionDecision clear =
                BattleInitiationPolicy.prediction("Site", 0.35f, 9.99f);

        assertEquals(BattleInitiationPolicy.PredictionBranch.PROBABLE_DEFEAT,
                defeat.branch());
        assertContribution(defeat.contribution(), -800.0f, "winRate 34%");
        assertEquals(BattleInitiationPolicy.PredictionBranch.PYRRHIC,
                pyrrhic.branch());
        assertContribution(pyrrhic.contribution(), -500.0f, "avg damage taken 10.0");
        assertEquals(BattleInitiationPolicy.PredictionBranch.NONE, clear.branch());
        assertFalse(clear.contribution().applies());
    }

    @Test
    public void specificBattlePreservesPowerAndWeaponLadder() {
        assertSpecific(ourBattle(10, 0, 0, 0, 0, 10),
                BattleInitiationPolicy.SpecificBattleBranch.NO_OPPONENT,
                -20.0f, false);
        assertSpecific(ourBattle(10, 15, 0, 0, 0, -5),
                BattleInitiationPolicy.SpecificBattleBranch.OUTGUNNED,
                -300.0f, false);
        assertSpecific(ourBattle(10, 21, 0, 0, 0, -11),
                BattleInitiationPolicy.SpecificBattleBranch.OUTGUNNED,
                -600.0f, false);
        assertSpecific(ourBattle(10, 15, 0, 0, 2, 1),
                BattleInitiationPolicy.SpecificBattleBranch.WEAPONS_NOT_ENOUGH,
                -150.0f, false);
        assertSpecific(ourBattle(12, 7, 4, 3, 0, 5),
                BattleInitiationPolicy.SpecificBattleBranch.FAVORABLE,
                150.0f, true);

        BattleInitiationPolicy.SpecificBattleDecision armedVader =
                BattleInitiationPolicy.specificBattle(
                        "Site", 10, 8, 4, 4, 3, 5,
                        true, true, true);
        assertSpecific(armedVader,
                BattleInitiationPolicy.SpecificBattleBranch.FAVORABLE,
                280.0f, true);
        assertTrue(armedVader.contribution().reason().endsWith(" + IHYN!"));

        assertSpecific(ourBattle(10, 8, 4, 4, 1, 2),
                BattleInitiationPolicy.SpecificBattleBranch.ARMED_MARGINAL,
                80.0f, false);
        // ADJUSTED 2026-08-08 (passivity fix, m01683): unarmed marginal flipped from
        // -50/non-favorable to +30/favorable — a real unarmed power edge is a fight
        // worth taking (live log: unarmed +3 edge netted ~+15 vs Pass and was skipped).
        // assertSpecific(ourBattle(10, 8, 4, 4, 0, 2),
        //         BattleInitiationPolicy.SpecificBattleBranch.UNARMED_MARGINAL,
        //         -50.0f, false);
        assertSpecific(ourBattle(10, 8, 4, 4, 0, 2),
                BattleInitiationPolicy.SpecificBattleBranch.UNARMED_MARGINAL,
                30.0f, true);
        assertSpecific(ourBattle(10, 10, 4, 4, 0, -8),
                BattleInitiationPolicy.SpecificBattleBranch.UNFAVORABLE,
                -100.0f, false);
        assertSpecific(ourBattle(10, 10, 4, 4, 0, -9),
                BattleInitiationPolicy.SpecificBattleBranch.UNFAVORABLE,
                -200.0f, false);
        assertSpecific(ourBattle(10, 10, 4, 4, 0, -16),
                BattleInitiationPolicy.SpecificBattleBranch.UNFAVORABLE,
                -400.0f, false);
    }

    // ADDED 2026-08-08 (passivity fix, m01683): the FAVORABLE arm is now
    // advantage-proportional (+25 per point of weapon-adjusted diff over the
    // threshold, total capped at +400) instead of a flat +150.
    @Test
    public void favorableArmPaysProportionallyToAdvantageAndCapsAt400() {
        BattleInitiationPolicy.SpecificBattleDecision diff5 =
                ourBattle(15, 10, 4, 4, 0, 5);
        BattleInitiationPolicy.SpecificBattleDecision diff10 =
                ourBattle(20, 10, 4, 4, 0, 10);
        BattleInitiationPolicy.SpecificBattleDecision diff20 =
                ourBattle(30, 10, 4, 4, 0, 20);

        assertSpecific(diff5,
                BattleInitiationPolicy.SpecificBattleBranch.FAVORABLE,
                150.0f, true);
        assertSpecific(diff10,
                BattleInitiationPolicy.SpecificBattleBranch.FAVORABLE,
                275.0f, true);
        assertTrue("diff 10 must pay more than diff 5",
                diff10.contribution().delta() > diff5.contribution().delta());
        assertSpecific(diff20,
                BattleInitiationPolicy.SpecificBattleBranch.FAVORABLE,
                400.0f, true);
    }

    @Test
    public void fallbackPreservesContributionOrderAndEarlyWins() {
        BattleInitiationPolicy.FallbackDecision pyrrhic =
                BattleInitiationPolicy.fallbackLocation(
                        "Site", 3, 10, 2, 5, -7,
                        2, 6, true);
        assertFalse(pyrrhic.favorable());
        assertEquals(List.of(-500.0f, -80.0f, -25.0f),
                pyrrhic.contributions().stream()
                        .map(BattleInitiationPolicy.Contribution::delta).toList());

        BattleInitiationPolicy.FallbackDecision weaponFavored =
                BattleInitiationPolicy.fallbackLocation(
                        "Site", 3, 10, 5, 4, 2,
                        0, 0, false);
        assertTrue(weaponFavored.favorable());
        assertEquals(List.of(-80.0f, 40.0f),
                weaponFavored.contributions().stream()
                        .map(BattleInitiationPolicy.Contribution::delta).toList());

        BattleInitiationPolicy.FallbackDecision abilityBattle =
                BattleInitiationPolicy.fallbackLocation(
                        "Site", 8, 10, 4, 4, 0,
                        0, 0, false);
        assertTrue(abilityBattle.favorable());
        assertContribution(abilityBattle.contributions().get(0),
                40.0f, "V164a ABILITY BATTLE");
    }

    @Test
    public void scanMustFightReserveAndInterruptBranchesStayExact() {
        assertContribution(BattleInitiationPolicy.scanOutcome(false, true),
                -60.0f, "No favorable battles");
        assertContribution(BattleInitiationPolicy.scanOutcome(false, false),
                -20.0f, "No contested locations");
        assertFalse(BattleInitiationPolicy.scanOutcome(true, true).applies());

        assertFalse(BattleInitiationPolicy.mustFight(2, false, true).applies());
        assertContribution(BattleInitiationPolicy.mustFight(2, true, true),
                200.0f, "V34 MUST-FIGHT");

        assertReserve(9, 0, BattleInitiationPolicy.ReserveBranch.OVERPOWER,
                0.0f, false);
        assertReserve(7, 0, BattleInitiationPolicy.ReserveBranch.EMPTY,
                -800.0f, true);
        assertReserve(7, 1, BattleInitiationPolicy.ReserveBranch.CRITICAL,
                -400.0f, true);
        assertReserve(7, 2, BattleInitiationPolicy.ReserveBranch.LOW,
                -200.0f, true);
        assertReserve(7, 3, BattleInitiationPolicy.ReserveBranch.NONE,
                0.0f, false);

        assertInterrupt(true, 0,
                BattleInitiationPolicy.InterruptBranch.DTF_BLOCKED, -100.0f);
        assertInterrupt(true, 2,
                BattleInitiationPolicy.InterruptBranch.DTF_BLOCKED, -60.0f);
        assertInterrupt(true, 3,
                BattleInitiationPolicy.InterruptBranch.DTF_READY, 0.0f);
        assertInterrupt(false, 1,
                BattleInitiationPolicy.InterruptBranch.STANDARD_CRITICAL, -40.0f);
        assertInterrupt(false, 3,
                BattleInitiationPolicy.InterruptBranch.STANDARD_LOW, -15.0f);
        assertInterrupt(false, 4,
                BattleInitiationPolicy.InterruptBranch.NONE, 0.0f);
    }

    @Test
    public void lifeForceContributionsKeepLegacyOrder() {
        assertEquals(List.of(15.0f, 30.0f),
                BattleInitiationPolicy.lifeForce(true, false, 6, 6).stream()
                        .map(BattleInitiationPolicy.Contribution::delta).toList());
        assertEquals(List.of(-20.0f),
                BattleInitiationPolicy.lifeForce(false, true, 7, 6).stream()
                        .map(BattleInitiationPolicy.Contribution::delta).toList());
        assertEquals(List.of(15.0f),
                BattleInitiationPolicy.lifeForce(true, true, 7, 6).stream()
                        .map(BattleInitiationPolicy.Contribution::delta).toList());
    }

    private static BattleInitiationPolicy.SpecificBattleDecision ourBattle(
            float ourPower, float theirPower,
            float ourAbility, float theirAbility,
            float weaponBonus, float effectiveDiff) {
        return BattleInitiationPolicy.specificBattle(
                "Site", ourPower, theirPower,
                ourAbility, theirAbility,
                weaponBonus, effectiveDiff,
                false, false, false);
    }

    private static void assertSpecific(
            BattleInitiationPolicy.SpecificBattleDecision decision,
            BattleInitiationPolicy.SpecificBattleBranch branch,
            float delta,
            boolean favorable) {
        assertEquals(branch, decision.branch());
        assertEquals(favorable, decision.favorable());
        assertEquals(delta, decision.contribution().delta(), 0.001f);
    }

    private static void assertReserve(
            float margin, int reserve,
            BattleInitiationPolicy.ReserveBranch branch,
            float delta, boolean applies) {
        BattleInitiationPolicy.ReserveDecision decision =
                BattleInitiationPolicy.reserve(margin, reserve);
        assertEquals(branch, decision.branch());
        assertEquals(applies, decision.contribution().applies());
        assertEquals(delta, decision.contribution().delta(), 0.001f);
    }

    private static void assertInterrupt(
            boolean drawTheirFire, int force,
            BattleInitiationPolicy.InterruptBranch branch,
            float delta) {
        BattleInitiationPolicy.InterruptDecision decision =
                BattleInitiationPolicy.interruptForce(drawTheirFire, force, 5);
        assertEquals(branch, decision.branch());
        assertEquals(delta, decision.contribution().delta(), 0.001f);
    }

    private static void assertContribution(
            BattleInitiationPolicy.Contribution contribution,
            float delta,
            String reasonFragment) {
        assertTrue(contribution.applies());
        assertEquals(delta, contribution.delta(), 0.001f);
        assertTrue(contribution.reason(),
                contribution.reason().contains(reasonFragment));
    }
}
