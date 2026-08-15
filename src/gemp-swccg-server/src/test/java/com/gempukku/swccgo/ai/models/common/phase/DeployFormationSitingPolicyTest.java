package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.phase.DeployFormationSitingPolicy.CharacterFormationFacts;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeployFormationSitingPolicyTest {

    private static final String ACTION_ID = "deploy-site-42";
    private static final String DESTINATION = "Cloud City: Guest Quarters";

    @Test
    public void v67bnUsesInclusiveFourToFiveDeficitAndRequiresCommitment() {
        assertRuleAbsent(evaluate(formation(0, 6.0f, 10.0f, false)), "V67bn");
        assertRuleAbsent(evaluate(formation(1, 6.0f, 9.99f, false)), "V67bn");
        assertDelta(evaluate(formation(1, 6.0f, 10.0f, false)), "V67bn", 800.0f);
        assertDelta(evaluate(formation(3, 6.0f, 11.0f, false)), "V67bn", 800.0f);
        assertRuleAbsent(evaluate(formation(1, 6.0f, 11.01f, false)), "V67bn");
        assertRuleAbsent(evaluate(formation(1, 6.0f, 10.0f, true)), "V67bn");

        assertOperation(operation(evaluate(formation(2, 6.0f, 10.0f, false)), "V67bn"),
                "V67bn", TraceDomainId.DEPLOY_SITING, 800.0f,
                "V67bn REINFORCE OUTGUNNED (Braveheart): 2 friendly char(s) at Cloud City: Guest Quarters (our 6 vs opp 10, deficit 4) \u2014 NO ESCAPE, DEPLOY HERE to minimize overflow!");
    }

    @Test
    public void emptyDestinationTopologyPreservesPriorityScalingAndReasons() {
        DeployFormationSitingPolicy.EmptyDestinationTopologyEvaluation contested =
                emptyTopology(3, 1);
        assertEquals(
                DeployFormationSitingPolicy.EmptyDestinationTopologyOutcome.CONTESTED_SOLO,
                contested.outcome());
        assertOperation(contested.result().operations().get(0),
                "V29-concentrate-contested", TraceDomainId.DEPLOY_SITING, -200.0f,
                "V29 CONCENTRATE: 1 solo friendly(s) CONTESTED — reinforce them, don't spread!");
        assertEquals(-400.0f, emptyTopology(3, 2).delta(), 0.0f);

        DeployFormationSitingPolicy.EmptyDestinationTopologyEvaluation solo =
                emptyTopology(1, 0);
        assertEquals(DeployFormationSitingPolicy.EmptyDestinationTopologyOutcome.SOLO,
                solo.outcome());
        assertOperation(solo.result().operations().get(0),
                "V29-concentrate-solo", TraceDomainId.DEPLOY_SITING, -100.0f,
                "V29 CONCENTRATE: 1 solo friendly(s) need reinforcement — don't spread thin!");
        assertEquals(-200.0f, emptyTopology(2, 0).delta(), 0.0f);

        DeployFormationSitingPolicy.EmptyDestinationTopologyEvaluation establish =
                emptyTopology(0, 0);
        assertEquals(
                DeployFormationSitingPolicy.EmptyDestinationTopologyOutcome.ESTABLISH_EMPTY,
                establish.outcome());
        assertOperation(establish.result().operations().get(0),
                "V29-establish-empty", TraceDomainId.DEPLOY_SITING, 20.0f,
                "Establish at empty location (no solo friendlies elsewhere)");
    }

    @Test
    public void reinforcementTopologyPreservesV67bnInclusiveDeficitAndEscapePriority() {
        assertEquals(DeployFormationSitingPolicy.ReinforcementTopologyOutcome.NONE,
                reinforcement(1, 6.0f, Math.nextDown(10.0f), false).outcome());

        DeployFormationSitingPolicy.ReinforcementTopologyEvaluation deficitFour =
                reinforcement(1, 6.0f, 10.0f, false);
        assertEquals(
                DeployFormationSitingPolicy.ReinforcementTopologyOutcome.V67BN_NO_ESCAPE,
                deficitFour.outcome());
        assertOperation(deficitFour.result().operations().get(0),
                "V67bn", TraceDomainId.DEPLOY_SITING, 800.0f,
                "V67bn REINFORCE OUTGUNNED (Braveheart): 1 friendly char(s) at Cloud City: Guest Quarters (our 6 vs opp 10, deficit 4) — NO ESCAPE, DEPLOY HERE to minimize overflow!");
        assertEquals(800.0f, reinforcement(3, 6.0f, 11.0f, false).delta(), 0.0f);
        assertEquals(DeployFormationSitingPolicy.ReinforcementTopologyOutcome.NONE,
                reinforcement(1, 6.0f, Math.nextUp(11.0f), false).outcome());
        assertEquals(DeployFormationSitingPolicy.ReinforcementTopologyOutcome.NONE,
                reinforcement(0, 6.0f, 10.0f, false).outcome());

        DeployFormationSitingPolicy.ReinforcementTopologyEvaluation escape =
                reinforcement(1, 3.0f, 7.0f, true);
        assertEquals(DeployFormationSitingPolicy.ReinforcementTopologyOutcome.V67BU_ESCAPE,
                escape.outcome());
        assertTrue(escape.result().operations().isEmpty());
        assertEquals(0.0f, escape.delta(), 0.0f);
    }

    @Test
    public void reinforcementTopologyPreservesWeakSoloAndStrictPairFallbacks() {
        DeployFormationSitingPolicy.ReinforcementTopologyEvaluation uncontestedSolo =
                reinforcement(1, 5.0f, 0.0f, false);
        assertEquals(DeployFormationSitingPolicy.ReinforcementTopologyOutcome.LEGACY_SOLO,
                uncontestedSolo.outcome());
        assertOperation(uncontestedSolo.result().operations().get(0),
                "V29-reinforce-solo", TraceDomainId.DEPLOY_SITING, 150.0f,
                "V29 REINFORCE SOLO CHARACTER (power 5) - don't leave them alone!");

        assertEquals(250.0f,
                reinforcement(1, 5.0f, Float.MIN_VALUE, false).delta(), 0.0f);
        assertEquals(DeployFormationSitingPolicy.ReinforcementTopologyOutcome.NONE,
                reinforcement(1, Math.nextUp(5.0f), 0.0f, false).outcome());

        DeployFormationSitingPolicy.ReinforcementTopologyEvaluation hopelessSolo =
                reinforcement(1, 3.0f, 9.0f, false);
        assertEquals(DeployFormationSitingPolicy.ReinforcementTopologyOutcome.LEGACY_SOLO,
                hopelessSolo.outcome());
        assertEquals(250.0f, hopelessSolo.delta(), 0.0f);

        assertEquals(DeployFormationSitingPolicy.ReinforcementTopologyOutcome.NONE,
                reinforcement(2, 4.0f, 6.0f, false).outcome());
        DeployFormationSitingPolicy.ReinforcementTopologyEvaluation pair =
                reinforcement(2, 4.0f, Math.nextUp(6.0f), false);
        assertEquals(
                DeployFormationSitingPolicy.ReinforcementTopologyOutcome.OUTNUMBERED_PAIR,
                pair.outcome());
        assertOperation(pair.result().operations().get(0),
                "V29-reinforce-pair", TraceDomainId.DEPLOY_SITING, 100.0f,
                "V29: Reinforce outnumbered pair at Cloud City: Guest Quarters");
    }

    @Test
    public void v295BuddyPreservesEveryLiveTopologyBranch() {
        assertBuddy(character(true, true, true, 0, 0, false, 0.0f), 40.0f,
                "V29.5 BUDDY: Own location \u2014 home field advantage");
        assertBuddy(character(true, false, true, 0, 0, true, 0.0f), -20.0f,
                "V29.6 BUDDY: Opponent's location but empty table \u2014 must deploy somewhere!");
        assertBuddy(character(true, false, true, 0, 0, false, 0.0f), -150.0f,
                "V29.5 BUDDY: Opponent's location, deploying ALONE \u2014 risky!");
        assertBuddy(character(true, false, true, 0, 2, false, 0.0f), -100.0f,
                "V29.5 BUDDY: Opponent's location with enemies, NO friendlies \u2014 dangerous!");
        assertBuddy(character(true, false, true, 1, 2, false, 0.0f), 10.0f,
                "V29.5 BUDDY: Opponent's location but friendlies present");
        assertRuleAbsent(evaluate(character(true, false, false, 0, 0, false, 0.0f)),
                "V29.5-buddy");
    }

    @Test
    public void v113UsesAbilityThreeInclusiveAndRequiresSoloDeployment() {
        assertRuleAbsent(evaluate(character(true, true, false, 0, 0, false, 2.99f)), "V113");
        assertDelta(evaluate(character(true, true, false, 0, 0, false, 3.0f)), "V113", -300.0f);
        assertRuleAbsent(evaluate(character(true, true, false, 1, 0, false, 8.0f)), "V113");
        assertRuleAbsent(evaluate(character(true, true, false, 0, 0, true, 8.0f)), "V113");
        assertRuleAbsent(evaluate(character(false, true, false, 0, 0, false, 8.0f)), "V113");

        assertOperation(operation(evaluate(character(true, true, false, 0, 0, false, 3.0f)), "V113"),
                "V113", TraceDomainId.SOLO_FORMATION, -300.0f,
                "V113 SOLO VULNERABILITY: Darth Vader (ability 3) alone at Cloud City: Guest Quarters \u2014 opponent can overwhelm next turn!");
    }

    @Test
    public void liveOperationsRetainLegacySourceOrder() {
        PolicyResult result = evaluate(new CharacterFormationFacts(true, DESTINATION,
                false, true, 1, 1, false, "Darth Vader", 3.0f,
                6.0f, 10.0f, false));

        assertEquals("DEPLOY_FORMATION_SITING_POLICY", result.producerId());
        assertEquals(2, result.operations().size());
        assertEquals("V67bn", result.operations().get(0).ruleArmId().id());
        assertEquals("V29.5-buddy", result.operations().get(1).ruleArmId().id());

        PolicyResult solo = evaluate(character(true, false, true,
                0, 1, false, 3.0f));
        assertEquals(2, solo.operations().size());
        assertEquals("V29.5-buddy", solo.operations().get(0).ruleArmId().id());
        assertEquals("V113", solo.operations().get(1).ruleArmId().id());
    }

    @Test
    public void legacySoloBranchesPreserveExactScoresReasonsAndPriority() {
        DeployFormationSitingPolicy.LegacySoloEvaluation withEscape =
                DeployFormationSitingPolicy.evaluateLegacySolo(
                        soloFacts(true, true, true, true, true));
        assertEquals(DeployFormationSitingPolicy.LegacySoloOutcome.OBJECTIVE_WITH_ESCAPE,
                withEscape.outcome());
        assertOperation(withEscape.result().operations().get(0), "V29-obj-flip",
                TraceDomainId.OBJECTIVE_INTENT, 300.0f,
                "V29 OBJ-FLIP: Weak Trooper solo at 'Cloud City: Guest Quarters' to help flip objective — escape route exists!");

        DeployFormationSitingPolicy.LegacySoloEvaluation noEscape =
                DeployFormationSitingPolicy.evaluateLegacySolo(
                        soloFacts(true, true, true, false, true));
        assertEquals(DeployFormationSitingPolicy.LegacySoloOutcome.OBJECTIVE_NO_ESCAPE,
                noEscape.outcome());
        assertDelta(noEscape.result(), "V29-obj-flip", -150.0f);
        assertEquals(TraceDomainId.OBJECTIVE_INTENT,
                noEscape.result().operations().get(0).domainId());

        DeployFormationSitingPolicy.LegacySoloEvaluation staging =
                DeployFormationSitingPolicy.evaluateLegacySolo(
                        soloFacts(true, true, false, false, true));
        assertEquals(DeployFormationSitingPolicy.LegacySoloOutcome.STAGING,
                staging.outcome());
        assertDelta(staging.result(), "V38-staging", -80.0f);

        DeployFormationSitingPolicy.LegacySoloEvaluation caution =
                DeployFormationSitingPolicy.evaluateLegacySolo(
                        soloFacts(true, true, false, false, false));
        assertEquals(DeployFormationSitingPolicy.LegacySoloOutcome.CAUTION,
                caution.outcome());
        assertDelta(caution.result(), "V38-solo-caution", -150.0f);

        assertTrue(DeployFormationSitingPolicy.evaluateLegacySolo(
                soloFacts(false, true, true, true, true)).result().operations().isEmpty());
        assertTrue(DeployFormationSitingPolicy.evaluateLegacySolo(
                soloFacts(true, false, true, true, true)).result().operations().isEmpty());
    }

    @Test
    public void strongReinforcementPreservesVaderPriorityAndBuddyThreshold() {
        DeployFormationSitingPolicy.StrongReinforcementEvaluation vader =
                DeployFormationSitingPolicy.evaluateStrongReinforcement(
                        strongFacts(true, true, true, 6.0f, 2.0f));
        assertEquals(DeployFormationSitingPolicy.StrongReinforcementOutcome.VADER,
                vader.outcome());
        assertDelta(vader.result(), "V38-reinforce-vader", 400.0f);

        DeployFormationSitingPolicy.StrongReinforcementEvaluation ally =
                DeployFormationSitingPolicy.evaluateStrongReinforcement(
                        strongFacts(true, false, true, 4.0f, 2.0f));
        assertEquals(DeployFormationSitingPolicy.StrongReinforcementOutcome.ALLY,
                ally.outcome());
        assertDelta(ally.result(), "V38-reinforce-ally", 200.0f);

        DeployFormationSitingPolicy.StrongReinforcementEvaluation threshold =
                DeployFormationSitingPolicy.evaluateStrongReinforcement(
                        strongFacts(true, false, true, 4.0f, 3.0f));
        assertEquals(DeployFormationSitingPolicy.StrongReinforcementOutcome.ALLY_REACHES_THRESHOLD,
                threshold.outcome());
        assertDelta(threshold.result(), "V38-reinforce-ally", 300.0f);
    }

    @Test
    public void buddySeekRequiresOneVulnerableAllyAtABattleground() {
        DeployFormationSitingPolicy.BuddySeekEvaluation nonBattleground =
                DeployFormationSitingPolicy.evaluateBuddySeek(
                        buddySeekFacts(true, true, false));
        assertEquals(DeployFormationSitingPolicy.BuddySeekOutcome.NON_BATTLEGROUND_SKIP,
                nonBattleground.outcome());
        assertTrue(nonBattleground.result().operations().isEmpty());

        DeployFormationSitingPolicy.BuddySeekEvaluation protect =
                DeployFormationSitingPolicy.evaluateBuddySeek(
                        buddySeekFacts(true, true, true));
        assertEquals(DeployFormationSitingPolicy.BuddySeekOutcome.PROTECT,
                protect.outcome());
        assertOperation(protect.result().operations().get(0), "V29-buddy-seek",
                TraceDomainId.SOLO_FORMATION, 200.0f,
                "V29 BUDDY-SEEK: Deploy to protect vulnerable Lando (power 3) at Cloud City: Guest Quarters!");

        assertTrue(DeployFormationSitingPolicy.evaluateBuddySeek(
                buddySeekFacts(true, false, true)).result().operations().isEmpty());
    }

    @Test
    public void huntGroupingPreservesEngageEmptyAndScatterBranches() {
        DeployFormationSitingPolicy.HuntGroupingEvaluation engage =
                DeployFormationSitingPolicy.evaluateHuntGrouping(
                        huntFacts(true, true, false, 5, 6.0f, false));
        assertEquals(DeployFormationSitingPolicy.HuntGroupingOutcome.GROUP_AND_ENGAGE,
                engage.outcome());
        assertDelta(engage.result(), "V35.1-hunt-group", 300.0f);
        assertEquals(TraceDomainId.OBJECTIVE_INTENT,
                engage.result().operations().get(0).domainId());

        DeployFormationSitingPolicy.HuntGroupingEvaluation empty =
                DeployFormationSitingPolicy.evaluateHuntGrouping(
                        huntFacts(true, true, false, 4, 0.0f, false));
        assertEquals(DeployFormationSitingPolicy.HuntGroupingOutcome.GROUP_EMPTY,
                empty.outcome());
        assertDelta(empty.result(), "V35.1-hunt-group", 50.0f);

        DeployFormationSitingPolicy.HuntGroupingEvaluation scatter =
                DeployFormationSitingPolicy.evaluateHuntGrouping(
                        huntFacts(true, false, false, 4, 0.0f, false));
        assertEquals(DeployFormationSitingPolicy.HuntGroupingOutcome.SCATTER_NEUTRAL,
                scatter.outcome());
        assertDelta(scatter.result(), "V40-hunt-scatter", 0.0f);

        assertTrue(DeployFormationSitingPolicy.evaluateHuntGrouping(
                huntFacts(true, false, false, 4, 0.0f, true)).result().operations().isEmpty());
        assertTrue(DeployFormationSitingPolicy.evaluateHuntGrouping(
                huntFacts(true, true, true, 4, 6.0f, false)).result().operations().isEmpty());
    }

    @Test
    public void positiveSiteAndFormationScoresRetainLegacyOrder() {
        PolicyResult highDrain = DeployFormationSitingPolicy.scoreHighDrainSite(
                new DeployFormationSitingPolicy.HighDrainSiteFacts(
                        ACTION_ID, DESTINATION, 2));
        assertDelta(highDrain, "V40-high-drain", 200.0f);
        assertTrue(DeployFormationSitingPolicy.scoreHighDrainSite(
                new DeployFormationSitingPolicy.HighDrainSiteFacts(
                        ACTION_ID, DESTINATION, 1)).operations().isEmpty());

        PolicyResult goodDrain = DeployFormationSitingPolicy.scoreGoodDrainSite(
                new DeployFormationSitingPolicy.GoodDrainSiteFacts(
                        ACTION_ID, DESTINATION, true, false));
        assertDelta(goodDrain, "V40-good-drain", 100.0f);
        assertTrue(DeployFormationSitingPolicy.scoreGoodDrainSite(
                new DeployFormationSitingPolicy.GoodDrainSiteFacts(
                        ACTION_ID, DESTINATION, false, false)).operations().isEmpty());

        DeployFormationSitingPolicy.PositiveFormationEvaluation crossing =
                DeployFormationSitingPolicy.evaluatePositiveFormation(
                        positive("Trooper With Blaster", 1, 3.0f, 4.0f, 2.0f));
        assertEquals(List.of(
                        DeployFormationSitingPolicy.PositiveFormationOutcome.FORTIFY_BATTLEGROUND,
                        DeployFormationSitingPolicy.PositiveFormationOutcome.BUDDY_DESTINY,
                        DeployFormationSitingPolicy.PositiveFormationOutcome.ARMED),
                crossing.outcomes());
        assertEquals(List.of("V51-fortify", "V51-buddy-destiny", "V51-armed"),
                crossing.result().operations().stream()
                        .map(operation -> operation.ruleArmId().id()).toList());
        assertEquals(List.of(500.0f, 400.0f, 150.0f),
                crossing.result().operations().stream()
                        .map(PolicyOperation::delta).toList());

        DeployFormationSitingPolicy.PositiveFormationEvaluation full =
                DeployFormationSitingPolicy.evaluatePositiveFormation(
                        positive("Officer", 1, 5.0f, 2.0f, 1.0f));
        assertEquals(List.of(
                        DeployFormationSitingPolicy.PositiveFormationOutcome.REINFORCE,
                        DeployFormationSitingPolicy.PositiveFormationOutcome.BUDDY_FULL),
                full.outcomes());

        DeployFormationSitingPolicy.PositiveFormationEvaluation establish =
                DeployFormationSitingPolicy.evaluatePositiveFormation(
                        positive("Officer", 0, 0.0f, 2.0f, 2.0f));
        assertEquals(List.of(
                        DeployFormationSitingPolicy.PositiveFormationOutcome.ESTABLISH_BATTLEGROUND),
                establish.outcomes());
        assertDelta(establish.result(), "V51-establish", 400.0f);
    }

    @Test
    public void abilityFourThresholdPreservesEveryLegacyBranch() {
        DeployFormationSitingPolicy.AbilityThresholdEvaluation fixes =
                abilityThreshold(3.0f, 1, 1.0f, false);
        assertEquals(DeployFormationSitingPolicy.AbilityThresholdOutcome.FIXES_DEFICIT,
                fixes.outcome());
        assertOperation(fixes.result().operations().get(0),
                "V32-ability-fix", TraceDomainId.DEPLOY_SITING, 150.0f,
                "V32 ABILITY FIX: Deploy brings ability from 3 to 4 (>= 4) at Cloud City: Guest Quarters!");

        DeployFormationSitingPolicy.AbilityThresholdEvaluation solo =
                abilityThreshold(0.0f, 0, 2.0f, false);
        assertEquals(DeployFormationSitingPolicy.AbilityThresholdOutcome.SOLO_NO_FOLLOW_UP,
                solo.outcome());
        assertOperation(solo.result().operations().get(0),
                "V40-ability-solo", TraceDomainId.DEPLOY_SITING, 0.0f,
                "V40 ABILITY: Solo deploy with ability 2 < 4 at Cloud City: Guest Quarters — deploy anyway");

        DeployFormationSitingPolicy.AbilityThresholdEvaluation followUp =
                abilityThreshold(0.0f, 0, 2.0f, true);
        assertEquals(DeployFormationSitingPolicy.AbilityThresholdOutcome.SOLO_WITH_FOLLOW_UP,
                followUp.outcome());
        assertDelta(followUp.result(), "V40-ability-solo-follow-up", 0.0f);

        DeployFormationSitingPolicy.AbilityThresholdEvaluation shared =
                abilityThreshold(1.0f, 1, 2.0f, false);
        assertEquals(DeployFormationSitingPolicy.AbilityThresholdOutcome.SHARED_BELOW_THRESHOLD,
                shared.outcome());
        assertDelta(shared.result(), "V40-ability-shared", 0.0f);

        DeployFormationSitingPolicy.AbilityThresholdEvaluation sufficient =
                abilityThreshold(0.0f, 0, 4.0f, false);
        assertEquals(DeployFormationSitingPolicy.AbilityThresholdOutcome.NONE,
                sufficient.outcome());
        assertTrue(sufficient.result().operations().isEmpty());
    }

    @Test
    public void buddyAbilityPreservesNonBattlegroundAndThresholdLadder() {
        DeployFormationSitingPolicy.BuddyAbilityEvaluation nonBgStack =
                buddyAbility(false, true, "Sidious", 0.0f, 3.0f);
        assertEquals(DeployFormationSitingPolicy.BuddyAbilityOutcome.NON_BATTLEGROUND_STACK,
                nonBgStack.outcome());
        assertOperation(nonBgStack.result().operations().get(0),
                "V67ag-non-bg-stack", TraceDomainId.DEPLOY_SITING, -300.0f,
                "V67ag NON-BG STACK PENALTY: Cloud City: Guest Quarters already has Sidious — additional character at non-BG can't battle, deploys to a battleground instead!");

        DeployFormationSitingPolicy.BuddyAbilityEvaluation nonBgEmpty =
                buddyAbility(false, false, null, 0.0f, 3.0f);
        assertEquals(DeployFormationSitingPolicy.BuddyAbilityOutcome.NON_BATTLEGROUND_SKIP,
                nonBgEmpty.outcome());
        assertTrue(nonBgEmpty.result().operations().isEmpty());

        DeployFormationSitingPolicy.BuddyAbilityEvaluation fixes =
                buddyAbility(true, false, null, 5.0f, 2.0f);
        assertEquals(DeployFormationSitingPolicy.BuddyAbilityOutcome.REACHES_THRESHOLD,
                fixes.outcome());
        assertDelta(fixes.result(), "V33-buddy-fix", 150.0f);

        DeployFormationSitingPolicy.BuddyAbilityEvaluation reinforces =
                buddyAbility(true, false, null, 3.0f, 2.0f);
        assertEquals(DeployFormationSitingPolicy.BuddyAbilityOutcome.REINFORCES,
                reinforces.outcome());
        assertDelta(reinforces.result(), "V33-buddy-bonus", 100.0f);

        assertTrue(buddyAbility(true, false, null, 0.0f, 2.0f)
                .result().operations().isEmpty());
        assertTrue(buddyAbility(true, false, null, 7.0f, 2.0f)
                .result().operations().isEmpty());
    }

    @Test
    public void characterBattlegroundPreferencePreservesBranchPriorityAndReasons() {
        PolicyResult battleground = battlegroundPreference(true, true, 0);
        assertEquals("DEPLOY_FORMATION_SITING_POLICY", battleground.producerId());
        assertOperation(battleground.operations().get(0),
                "V29.7-battleground", TraceDomainId.DEPLOY_SITING, 80.0f,
                "V29.7 BATTLEGROUND: Deploy to battleground site — force drains and battles!");

        PolicyResult noAlternative = battlegroundPreference(false, false, 7);
        assertOperation(noAlternative.operations().get(0),
                "V29.7-battleground", TraceDomainId.DEPLOY_SITING, 0.0f,
                "V29.7 BATTLEGROUND: Non-BG but no battlegrounds on table — acceptable");
    }

    @Test
    public void characterBattlegroundPreferenceUsesStrictPositiveDrainBoundary() {
        PolicyResult withDrain = battlegroundPreference(false, true, 1);
        assertOperation(withDrain.operations().get(0),
                "V67ah-non-bg", TraceDomainId.DEPLOY_SITING, -100.0f,
                "V67ah NON-BG (with drain): mostly useless except as drain staging — mild penalty");

        PolicyResult withoutDrain = battlegroundPreference(false, true, 0);
        assertOperation(withoutDrain.operations().get(0),
                "V67ah-non-bg", TraceDomainId.DEPLOY_SITING, -350.0f,
                "V67ah NON-BG (no drain): truly useless — no battles AND no drain potential");
        assertDelta(battlegroundPreference(false, true, -1),
                "V67ah-non-bg", -350.0f);
    }

    private static PolicyResult evaluate(CharacterFormationFacts facts) {
        return DeployFormationSitingPolicy.evaluate(ACTION_ID, facts);
    }

    private static CharacterFormationFacts formation(int friendlies,
                                                      float ourPower,
                                                      float opponentPower,
                                                      boolean canEscape) {
        return new CharacterFormationFacts(true, DESTINATION,
                false, true, friendlies, 1, false,
                "Darth Vader", 6.0f, ourPower, opponentPower, canEscape);
    }

    private static CharacterFormationFacts character(boolean eligible,
                                                      boolean ourLocation,
                                                      boolean opponentLocation,
                                                      int friendlies,
                                                      int opponents,
                                                      boolean emptyTable,
                                                      float ability) {
        return new CharacterFormationFacts(eligible, DESTINATION,
                ourLocation, opponentLocation, friendlies, opponents,
                emptyTable, "Darth Vader", ability,
                0.0f, 0.0f, false);
    }

    private static DeployFormationSitingPolicy.EmptyDestinationTopologyEvaluation
            emptyTopology(int soloLocations, int contestedSoloLocations) {
        return DeployFormationSitingPolicy.evaluateEmptyDestinationTopology(
                new DeployFormationSitingPolicy.EmptyDestinationTopologyFacts(
                        ACTION_ID, DESTINATION, soloLocations,
                        contestedSoloLocations));
    }

    private static DeployFormationSitingPolicy.ReinforcementTopologyEvaluation
            reinforcement(int friendlies, float ourPower,
                          float opponentPower, boolean canEscape) {
        return DeployFormationSitingPolicy.evaluateReinforcementTopology(
                new DeployFormationSitingPolicy.ReinforcementTopologyFacts(
                        ACTION_ID, DESTINATION, friendlies, ourPower,
                        opponentPower, canEscape));
    }

    private static DeployFormationSitingPolicy.LegacySoloFacts soloFacts(
            boolean eligible, boolean wouldBeSolo, boolean objectiveFlip,
            boolean escape, boolean staging) {
        return new DeployFormationSitingPolicy.LegacySoloFacts(
                ACTION_ID, "Weak Trooper", 3, DESTINATION,
                eligible, wouldBeSolo, objectiveFlip, escape, staging);
    }

    private static DeployFormationSitingPolicy.StrongReinforcementFacts strongFacts(
            boolean eligible, boolean vader, boolean strongAlly,
            float allyAbility, float deployingAbility) {
        return new DeployFormationSitingPolicy.StrongReinforcementFacts(
                ACTION_ID, "Officer", eligible, vader, strongAlly,
                allyAbility, deployingAbility, 7.0f);
    }

    private static DeployFormationSitingPolicy.BuddySeekFacts buddySeekFacts(
            boolean eligible, boolean vulnerable, boolean battleground) {
        return new DeployFormationSitingPolicy.BuddySeekFacts(
                ACTION_ID, eligible, vulnerable, battleground,
                "Lando", 3, DESTINATION);
    }

    private static DeployFormationSitingPolicy.HuntGroupingFacts huntFacts(
            boolean eligible, boolean atVader, boolean deployingVader,
            int cardPower, float opponentPower, boolean objectiveRelevant) {
        return new DeployFormationSitingPolicy.HuntGroupingFacts(
                ACTION_ID, eligible, "The Grand Inquisitor", cardPower,
                DESTINATION, atVader, deployingVader, opponentPower,
                objectiveRelevant);
    }

    private static DeployFormationSitingPolicy.PositiveFormationFacts positive(
            String cardName, int friendlies, float friendlyAbility,
            float deployingAbility, float drain) {
        return new DeployFormationSitingPolicy.PositiveFormationFacts(
                ACTION_ID, cardName, DESTINATION, friendlies,
                friendlyAbility, deployingAbility, drain);
    }

    private static DeployFormationSitingPolicy.AbilityThresholdEvaluation
            abilityThreshold(float currentAbility, int friendlyCount,
                             float deployingAbility, boolean followUp) {
        return DeployFormationSitingPolicy.evaluateAbilityThreshold(
                new DeployFormationSitingPolicy.AbilityThresholdFacts(
                        ACTION_ID, DESTINATION, currentAbility,
                        friendlyCount, deployingAbility, followUp));
    }

    private static DeployFormationSitingPolicy.BuddyAbilityEvaluation
            buddyAbility(boolean battleground, boolean friendlyPresent,
                         String existingTitle, float currentAbility,
                         float deployingAbility) {
        return DeployFormationSitingPolicy.evaluateBuddyAbility(
                new DeployFormationSitingPolicy.BuddyAbilityFacts(
                        ACTION_ID, DESTINATION, battleground,
                        friendlyPresent, existingTitle, currentAbility,
                        deployingAbility, 7));
    }

    private static PolicyResult battlegroundPreference(
            boolean battlegroundSite, boolean anyBattlegroundExists,
            int opponentForceIcons) {
        return DeployFormationSitingPolicy.scoreCharacterBattlegroundPreference(
                new DeployFormationSitingPolicy.CharacterBattlegroundPreferenceFacts(
                        ACTION_ID, battlegroundSite, anyBattlegroundExists,
                        opponentForceIcons));
    }

    private static void assertBuddy(CharacterFormationFacts facts,
                                    float expected,
                                    String expectedReason) {
        assertOperation(operation(evaluate(facts), "V29.5-buddy"),
                "V29.5-buddy", TraceDomainId.SOLO_FORMATION,
                expected, expectedReason);
    }

    private static void assertDelta(PolicyResult result, String ruleId,
                                    float expected) {
        assertEquals(expected, operation(result, ruleId).delta(), 0.0f);
    }

    private static PolicyOperation operation(PolicyResult result,
                                             String ruleId) {
        List<PolicyOperation> matching = result.operations().stream()
                .filter(candidate -> ruleId.equals(candidate.ruleArmId().id()))
                .toList();
        assertEquals("operation count for " + ruleId, 1, matching.size());
        return matching.get(0);
    }

    private static void assertRuleAbsent(PolicyResult result, String ruleId) {
        assertEquals(0, result.operations().stream()
                .filter(candidate -> ruleId.equals(candidate.ruleArmId().id()))
                .count());
    }

    private static void assertOperation(PolicyOperation operation,
                                        String ruleId,
                                        TraceDomainId domain,
                                        float delta,
                                        String reason) {
        assertEquals(ACTION_ID, operation.actionId());
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(domain, operation.domainId());
        assertEquals(TraceOutputKind.BANDED, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(delta, operation.delta(), 0.0f);
        assertEquals(reason, operation.reason());
    }
}
