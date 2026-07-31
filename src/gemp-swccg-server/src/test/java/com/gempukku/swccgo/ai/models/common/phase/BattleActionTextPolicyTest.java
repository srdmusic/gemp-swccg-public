package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BattleActionTextPolicyTest {

    @Test
    public void v25PowerAndAbilityBandsRetainExactBoundaries() {
        assertArm(resolved(10, 0, 0, 0, 3),
                "V25-battle-no-opponent", -100.0f);
        assertArm(resolved(3, 7, 0, 0, 3),
                "V25-battle-suicide", -500.0f);

        assertArm(effectiveDiff(8.0f), "V25-battle-crush", 200.0f);
        assertArm(effectiveDiff(5.0f), "V25-battle-favorable", 120.0f);
        assertArm(effectiveDiff(2.0f), "V25-battle-marginal", 60.0f);
        assertArm(effectiveDiff(-2.0f), "V25-battle-even", 20.0f);
        assertArm(effectiveDiff(-2.01f), "V25-battle-unfavorable", -60.0f);
        assertArm(effectiveDiff(-8.0f), "V25-battle-unfavorable", -60.0f);
        assertArm(effectiveDiff(-8.01f), "V25-battle-unfavorable", -120.0f);
        assertArm(effectiveDiff(-15.0f), "V25-battle-unfavorable", -120.0f);
        assertArm(effectiveDiff(-15.01f), "V25-battle-unfavorable", -250.0f);

        // Both suicide comparisons are strict.
        assertArm(resolved(3, 6, 0, 0, 3),
                "V25-battle-unfavorable", -60.0f);
    }

    @Test
    public void fallbackAndReservePenaltyRemainIndependentAndOrdered() {
        PolicyResult lowReserve = BattleActionTextPolicy.scoreInitiation(
                new BattleActionTextFacts.InitiationFacts(
                        "battle", false, "", 0, 0, 0, 0, 2));
        PolicyResult readyReserve = BattleActionTextPolicy.scoreInitiation(
                new BattleActionTextFacts.InitiationFacts(
                        "battle", false, "", 0, 0, 0, 0, 3));

        assertEquals(List.of("V25-battle-no-location", "V25-battle-low-reserve"),
                lowReserve.operations().stream()
                        .map(operation -> operation.ruleArmId().id()).toList());
        assertRawDeltas(lowReserve.operations(), 30.0f, -50.0f);
        assertEquals("V25 BATTLE: Initiate battle (no location data)",
                lowReserve.operations().get(0).reason());
        assertEquals("V25 BATTLE: Low reserve (2) — bad destiny draws!",
                lowReserve.operations().get(1).reason());
        assertEquals(1, readyReserve.operations().size());
        assertRawFloat(30.0f, readyReserve.operations().get(0).delta());
    }

    @Test
    public void v25RemainsAdditiveWithBattleOneAtExactRawFloatScore() {
        BattleInitiationPolicy.SpecificBattleDecision battleOne =
                BattleInitiationPolicy.specificBattle(
                        "Site", 12, 7, 4, 3, 0, 5,
                        false, false, false);
        PolicyOperation v25 = only(BattleActionTextPolicy.scoreInitiation(
                resolved(12, 4, 4, 4, 3)));

        float combined = 100.0f
                + battleOne.contribution().delta()
                + v25.delta();

        assertRawFloat(150.0f, battleOne.contribution().delta());
        assertRawFloat(200.0f, v25.delta());
        assertRawFloat(450.0f, combined);
        assertEquals(PolicyOperationKind.ADD, v25.kind());
    }

    @Test
    public void negativeV25BandIsStillAdditiveNotHardVeto() {
        PolicyOperation v25 = only(BattleActionTextPolicy.scoreInitiation(
                effectiveDiff(-15.01f)));

        assertRawFloat(-250.0f, v25.delta());
        assertEquals(PolicyOperationKind.ADD, v25.kind());
        assertFalse(v25.reason().isBlank());
    }

    @Test
    public void resolvedNullTitleKeepsLegacyStringFormatting() {
        PolicyOperation operation = only(BattleActionTextPolicy.scoreInitiation(
                new BattleActionTextFacts.InitiationFacts(
                        "battle", true, null, 10, 0, 0, 0, 3)));

        assertEquals("V25 BATTLE: No opponent at null", operation.reason());
    }

    @Test
    public void v144BattleFreezeRemainsBattleOwned() {
        PolicyResult both = BattleActionTextPolicy.scoreYouAreBeatenMode(
                new BattleActionTextFacts.YouAreBeatenModeFacts("beaten", true, true));
        assertIds(both, "V144-you-are-beaten-freeze");
        assertRawDeltas(both.operations(), 500.0f);
        assertEquals("V144 YOU ARE BEATEN: Battle freeze in battle phase — strong use!",
                both.operations().get(0).reason());
        assertOperationKindsAreAdditive(both);
        assertTrue(BattleActionTextPolicy.scoreYouAreBeatenMode(
                new BattleActionTextFacts.YouAreBeatenModeFacts("beaten", true, false)).operations().isEmpty());
    }

    @Test
    public void hatredRetainsPrecedenceScoresAndJediBonus() {
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreHatred(
                new BattleActionTextFacts.HatredFacts("hatred", true, true, true, true, true))),
                "V37.1-hatred-wrong-turn", -600.0f,
                "V37.1 HATRED: Not our turn — save hatred for our deploy phase!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreHatred(
                new BattleActionTextFacts.HatredFacts("hatred", false, false, true, true, true))),
                "V35.7-hatred-no-inquisitor", -500.0f,
                "V35.7 HATRED: No Inquisitor on table — hatred requires Inquisitor!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreHatred(
                new BattleActionTextFacts.HatredFacts("hatred", false, true, true, true, true))),
                "V35.7-hatred-useful", 550.0f,
                "V35.7 HATRED: Inquisitor WITH opponents + JEDI — cancel game text! (+550)");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreHatred(
                new BattleActionTextFacts.HatredFacts("hatred", false, true, true, false, false))),
                "V35.7-hatred-useful", 350.0f,
                "V35.7 HATRED: Inquisitor WITH opponents — cancel game text! (+350)");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreHatred(
                new BattleActionTextFacts.HatredFacts("hatred", false, true, false, true, true))),
                "V35.3-hatred-no-opponent", -300.0f,
                "V35.3 HATRED: Vader/Inquisitor not at same site as opponents — save for later!");
    }

    @Test
    public void ihynUsesNamedBranchBeforeGenericSourceBranch() {
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreIHaveYouNow(
                new BattleActionTextFacts.IHaveYouNowFacts("ihyn", true, true, true, false))),
                "V29.9-ihyn-vader", 300.0f,
                "V29.9 IHYN: Vader in battle — PLAY I HAVE YOU NOW for devastating extra destiny draws!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreIHaveYouNow(
                new BattleActionTextFacts.IHaveYouNowFacts("ihyn", true, true, false, false))),
                "V29.9-ihyn-battle", 100.0f,
                "V29.9 IHYN: Play I Have You Now for extra battle destiny!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreIHaveYouNow(
                new BattleActionTextFacts.IHaveYouNowFacts("ihyn", true, false, true, true))),
                "V29.9-ihyn-save", -200.0f,
                "V29.9 IHYN: Save I Have You Now for battle!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreIHaveYouNow(
                new BattleActionTextFacts.IHaveYouNowFacts("ihyn", false, true, false, true))),
                "V29.9-ihyn-source", 200.0f,
                "V29.9 IHYN: Play I Have You Now during battle — extra destiny draws!");
        assertTrue(BattleActionTextPolicy.scoreIHaveYouNow(
                new BattleActionTextFacts.IHaveYouNowFacts("ihyn", false, false, false, true)).operations().isEmpty());
        assertEquals(1, BattleActionTextPolicy.scoreIHaveYouNow(
                new BattleActionTextFacts.IHaveYouNowFacts("ihyn", true, true, true, true)).operations().size());
    }

    @Test
    public void fmftdPreservesLostSynergyAndModePrecedence() {
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreFmftd(
                new BattleActionTextFacts.FmftdFacts("fmftd", BattleActionTextFacts.FmftdMode.LOST,
                        true, false, true, true, true))),
                "V35-fmftd-lost-full", 500.0f,
                "V35 FMFTD LOST: Inquisitor + Jedi + Hatred — ADD 2 BATTLE DESTINY!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreFmftd(
                new BattleActionTextFacts.FmftdFacts("fmftd", BattleActionTextFacts.FmftdMode.LOST,
                        true, false, true, true, false))),
                "V35-fmftd-lost-partial", 350.0f,
                "V35 FMFTD LOST: Inquisitor with Jedi or Hatred — add 1 battle destiny!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreFmftd(
                new BattleActionTextFacts.FmftdFacts("fmftd", BattleActionTextFacts.FmftdMode.LOST,
                        true, false, true, false, false))),
                "V35-fmftd-lost-inquisitor", 200.0f,
                "V35 FMFTD LOST: Inquisitor in battle — add destiny!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreFmftd(
                new BattleActionTextFacts.FmftdFacts("fmftd", BattleActionTextFacts.FmftdMode.LOST,
                        true, false, false, true, false))),
                "V35-fmftd-lost-limited", 50.0f,
                "V35 FMFTD LOST: No Inquisitor in battle — limited value");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreFmftd(
                new BattleActionTextFacts.FmftdFacts("fmftd", BattleActionTextFacts.FmftdMode.USED,
                        false, true, false, false, false))),
                "V35-fmftd-used-deploy", 350.0f,
                "V35 FMFTD USED: Place hatred on opponent — cancel game text!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreFmftd(
                new BattleActionTextFacts.FmftdFacts("fmftd", BattleActionTextFacts.FmftdMode.USED,
                        false, false, false, false, false))),
                "V35-fmftd-used-other", 150.0f,
                "V35 FMFTD USED: Place hatred — decent timing");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreFmftd(
                new BattleActionTextFacts.FmftdFacts("fmftd", BattleActionTextFacts.FmftdMode.GENERIC,
                        true, false, false, false, false))),
                "V35-fmftd-battle", 250.0f,
                "V35 FMFTD: Play during battle for extra destiny!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreFmftd(
                new BattleActionTextFacts.FmftdFacts("fmftd", BattleActionTextFacts.FmftdMode.GENERIC,
                        false, false, false, false, false))),
                "V35-fmftd-save", -100.0f,
                "V35 FMFTD: Save for battle if possible");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreFmftd(
                new BattleActionTextFacts.FmftdFacts("fmftd", BattleActionTextFacts.FmftdMode.LOST,
                        false, false, true, true, true))),
                "V35-fmftd-save", -100.0f,
                "V35 FMFTD: Save for battle if possible");
    }

    @Test
    public void recallsAndStunningLeaderRetainThresholds() {
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreVaderRecall(
                new BattleActionTextFacts.VaderRecallFacts(
                        "recall", true))),
                "V35-vader-recall-jedi", 300.0f,
                "V35 VADER RECALL: Take Vader into hand — Jedi elsewhere to hunt! Redeploy!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreVaderRecall(
                new BattleActionTextFacts.VaderRecallFacts(
                        "recall", false))),
                "V35-vader-recall-save", -100.0f,
                "V35 VADER RECALL: Take Vader into hand — no clear target, keep him deployed");
        assertOperation(onlyWeapons(
                        BattleActionTextPolicy
                                .scoreVirtualVaderRecall(
                                    new BattleActionTextFacts
                                                .ActionFacts(
                                                        "virtual-recall"))),
                "OBJECTIVE.POST_FLIP.VIRTUAL_HUNT_VADER_RECALL_SAFE",
                -100.0f,
                "VIRTUAL HUNT DOWN RECALL: another Vader remains on table;"
                        + " no tactical need to recall this one");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreInquisitorRecall(
                new BattleActionTextFacts.InquisitorRecallFacts("recall", true))),
                "V35.1-inquisitor-recall-block", -400.0f,
                "V35.1 INQUISITOR RECALL BLOCK: Opponents on the board — KEEP Inquisitor to fight!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreInquisitorRecall(
                new BattleActionTextFacts.InquisitorRecallFacts("recall", false))),
                "V35-inquisitor-recall", 100.0f,
                "V35 INQUISITOR RECALL: No opponents on board — safe to reposition");

        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreStunningLeader(
                new BattleActionTextFacts.StunningLeaderFacts("leader",
                        BattleActionTextFacts.StunningLeaderMode.OWN_INITIATED, 10, 100))),
                "V37.2-stunning-leader-own", -9999.0f,
                "V37.2 STUNNING LEADER: WE initiated — fight to WIN!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreStunningLeader(
                new BattleActionTextFacts.StunningLeaderFacts("leader",
                        BattleActionTextFacts.StunningLeaderMode.DEFENDING, 10, 15))),
                "V37.2-stunning-leader-close", -300.0f,
                "V37.2 STUNNING LEADER: Close fight — battle instead!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreStunningLeader(
                new BattleActionTextFacts.StunningLeaderFacts("leader",
                        BattleActionTextFacts.StunningLeaderMode.DEFENDING, 10, 15.01f))),
                "V37.2-stunning-leader-outmatched", 300.0f,
                "V37.2 STUNNING LEADER: Outmatched 10 vs 15 — exclude to survive!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreStunningLeader(
                new BattleActionTextFacts.StunningLeaderFacts("leader",
                        BattleActionTextFacts.StunningLeaderMode.OUTSIDE_BATTLE, 10, 20))),
                "V37.2-stunning-leader-outside", -200.0f,
                "V37.2 STUNNING LEADER: Not in battle — save!");
        assertTrue(BattleActionTextPolicy.scoreStunningLeader(
                new BattleActionTextFacts.StunningLeaderFacts("leader",
                        BattleActionTextFacts.StunningLeaderMode.UNRESOLVED, 10, 20)).operations().isEmpty());
    }

    @Test
    public void destinyArmsRetainExactBoundaryOrderAndReasons() {
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreAddBattleDestiny(
                new BattleActionTextFacts.ActionFacts("destiny"))),
                "BATTLE-add-destiny", 50.0f, "Adding battle destiny is great");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreBattleDestinyModifier(
                new BattleActionTextFacts.ActionFacts("destiny"))),
                "BATTLE-battle-destiny-modifier", 50.0f, "+1 to battle destiny - always use!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreWeaponDestinyModifier(
                new BattleActionTextFacts.ActionFacts("destiny"))),
                "BATTLE-weapon-destiny-modifier", 50.0f,
                "Boost weapon destiny - increases hit chance!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scorePreventOpponentBattleDestiny(
                new BattleActionTextFacts.ActionFacts("destiny"))),
                "BATTLE-prevent-opponent-destiny", 50.0f,
                "Prevent opponent battle destiny - denies their draw!");

        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreProtectDestiny(
                new BattleActionTextFacts.ProtectDestinyFacts("protect", 1,
                        BattleActionTextFacts.DestinyProtectionPhase.BATTLE))),
                "BATTLE-protect-destiny-early", -50.0f,
                "SAVE for battle turn! Turn 1 rarely battles");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreProtectDestiny(
                new BattleActionTextFacts.ProtectDestinyFacts("protect", 2,
                        BattleActionTextFacts.DestinyProtectionPhase.BATTLE))),
                "BATTLE-protect-destiny-battle", 50.0f,
                "Protect destiny draws - IN BATTLE NOW!");
        for (BattleActionTextFacts.DestinyProtectionPhase phase : new BattleActionTextFacts.DestinyProtectionPhase[] {
                BattleActionTextFacts.DestinyProtectionPhase.ACTIVATE,
                BattleActionTextFacts.DestinyProtectionPhase.CONTROL,
                BattleActionTextFacts.DestinyProtectionPhase.DEPLOY}) {
            assertOperation(onlyWeapons(BattleActionTextPolicy.scoreProtectDestiny(
                    new BattleActionTextFacts.ProtectDestinyFacts("protect", 2, phase))),
                    "BATTLE-protect-destiny-opportunity", 30.0f,
                    "Protect destiny draws - battle opportunity exists");
        }
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreProtectDestiny(
                new BattleActionTextFacts.ProtectDestinyFacts("protect", 2,
                        BattleActionTextFacts.DestinyProtectionPhase.OTHER))),
                "BATTLE-protect-destiny-save", -30.0f,
                "Save destiny protection for clear battle turn");
    }

    @Test
    public void v175RetainsCapDeltaReadFailureAndNoOps() {
        PolicyOperation capped = onlyWeapons(BattleActionTextPolicy.scoreKillShot(
                new BattleActionTextFacts.KillShotFacts("kill", "Yoda",
                        BattleActionTextFacts.KillShotTarget.OPPONENT, 20.0f, 20.0f)));
        assertOperation(capped, "V175-kill-shot", 900.0f,
                "V175 KILL SHOT: make Yoda lost (power 20, forfeit 20) — take it!");
        PolicyOperation fractional = onlyWeapons(BattleActionTextPolicy.scoreKillShot(
                new BattleActionTextFacts.KillShotFacts("kill", "Yoda",
                        BattleActionTextFacts.KillShotTarget.OPPONENT, 1.3f, 0.7f)));
        assertRawFloat(466.0f, fractional.delta());
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreKillShot(
                new BattleActionTextFacts.KillShotFacts("kill", "Han",
                        BattleActionTextFacts.KillShotTarget.OWN, 10, 10))),
                "V175-kill-shot-own", -100.0f,
                "V175: target is OUR character — don't make our own lost");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreKillShot(
                new BattleActionTextFacts.KillShotFacts(
                        "kill", "?", BattleActionTextFacts.KillShotTarget.UNRESOLVED, 0, 0))),
                "V175-kill-shot-unresolved", 0.0f,
                "V175: make-lost target not found on table — unknown");

        PolicyOperation positive = onlyWeapons(BattleActionTextPolicy.scoreSubstituteDestiny(
                new BattleActionTextFacts.SubstituteDestinyFacts("sub", BattleActionTextFacts.SubstituteReadStatus.READ,
                        1.3f, 4.1f)));
        assertRawFloat((4.1f - 1.3f) * 60.0f, positive.delta());
        assertEquals("V175 SUBSTITUTE DELTA: drawn 1 -> ability 4 (+3 gain)", positive.reason());
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreSubstituteDestiny(
                new BattleActionTextFacts.SubstituteDestinyFacts("sub", BattleActionTextFacts.SubstituteReadStatus.READ,
                        4, 4))),
                "V175-substitute-skip", -50.0f,
                "V175 SUBSTITUTE SKIP: drawn 4 already >= ability 4 — save the card");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreSubstituteDestiny(
                new BattleActionTextFacts.SubstituteDestinyFacts("sub", BattleActionTextFacts.SubstituteReadStatus.READ_FAILED,
                        -1, -1))),
                "V175-substitute-read-failed", 30.0f, "Substituting destiny is good");
    }

    @Test
    public void genericBattleArmsRemainAdditive() {
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreRaceDestiny(
                new BattleActionTextFacts.ActionFacts("race"))),
                "BATTLE-race-destiny", 50.0f,
                "Race destiny always high priority");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreGenericYouAreBeaten(
                new BattleActionTextFacts.GenericYouAreBeatenFacts("generic", true))),
                "V35.4-you-are-beaten-battle", 150.0f,
                "V35.4 YOU ARE BEATEN: During battle — use for attrition!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreGenericYouAreBeaten(
                new BattleActionTextFacts.GenericYouAreBeatenFacts("generic", false))),
                "V35.4-you-are-beaten-outside", -200.0f,
                "V35.4 YOU ARE BEATEN: Not in battle — save for combat!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreCancelWeaponTargeting(
                new BattleActionTextFacts.ActionFacts("generic"))),
                "BATTLE-cancel-weapon-targeting", 50.0f,
                "Cancel weapon targeting - protect our characters!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreImmuneToAttrition(
                new BattleActionTextFacts.ActionFacts("generic"))),
                "BATTLE-immune-to-attrition", 50.0f,
                "Make character immune to attrition - valuable protection!");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreProtectForfeit(
                new BattleActionTextFacts.ActionFacts("generic"))),
                "BATTLE-protect-forfeit", 40.0f,
                "Protect forfeit value during battle");
        assertOperation(onlyWeapons(BattleActionTextPolicy.scoreRetargetWeapon(
                new BattleActionTextFacts.ActionFacts("generic"))),
                "BATTLE-retarget-weapon", 50.0f,
                "Re-target weapon at enemy - turn their weapon against them!");
    }

    @Test
    public void coarseDestinyCapFiresOnlyWhenItRemovesMoreOpponentDraws() {
        assertOperation(
                onlyWeapons(
                    BattleActionTextPolicy
                        .scoreSymmetricBattleDestinyCap(
                            "coarse", 1, 3)),
                "BATTLE.COARSE.BATTLE_DESTINY_CAP",
                500.0f,
                "COARSE: limit the opponent's larger battle-destiny advantage to one draw");
        assertOperation(
                onlyWeapons(
                    BattleActionTextPolicy
                        .scoreSymmetricBattleDestinyCap(
                            "coarse", 2, 2)),
                "BATTLE.COARSE.BATTLE_DESTINY_CAP_SKIP",
                -50.0f,
                "COARSE: preserve our equal or larger battle-destiny draw");
        assertOperation(
                onlyWeapons(
                    BattleActionTextPolicy
                        .scoreSymmetricBattleDestinyCap(
                            "coarse", 3, 1)),
                "BATTLE.COARSE.BATTLE_DESTINY_CAP_SKIP",
                -50.0f,
                "COARSE: preserve our equal or larger battle-destiny draw");
    }

    private static BattleActionTextFacts.InitiationFacts resolved(
            float ourPower,
            float theirPower,
            float ourAbility,
            float theirAbility,
            int reserve) {
        return new BattleActionTextFacts.InitiationFacts(
                "battle", true, "Site",
                ourPower, theirPower, ourAbility, theirAbility, reserve);
    }

    private static BattleActionTextFacts.InitiationFacts effectiveDiff(float difference) {
        return resolved(100.0f + difference, 100.0f, 0, 0, 3);
    }

    private static void assertArm(
            BattleActionTextFacts.InitiationFacts facts,
            String ruleId,
            float delta) {
        PolicyOperation operation = only(BattleActionTextPolicy.scoreInitiation(facts));
        assertEquals(ruleId, operation.ruleArmId().id());
        assertRawFloat(delta, operation.delta());
        assertEquals(TraceDomainId.BATTLE_INITIATION, operation.domainId());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
    }

    private static PolicyOperation only(PolicyResult result) {
        assertEquals(1, result.operations().size());
        PolicyOperation operation = result.operations().get(0);
        assertEquals("BATTLE_ACTION_TEXT_INITIATION_POLICY", result.producerId());
        assertEquals(TraceDomainId.BATTLE_INITIATION, operation.domainId());
        return operation;
    }

    private static PolicyOperation onlyWeapons(PolicyResult result) {
        assertEquals(1, result.operations().size());
        assertEquals("BATTLE_ACTION_TEXT_POLICY", result.producerId());
        PolicyOperation operation = result.operations().get(0);
        assertEquals(TraceDomainId.BATTLE_WEAPONS, operation.domainId());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        return operation;
    }

    private static void assertOperation(PolicyOperation operation,
                                        String ruleId,
                                        float delta,
                                        String reason) {
        assertEquals(ruleId, operation.ruleArmId().id());
        assertRawFloat(delta, operation.delta());
        assertEquals(reason, operation.reason());
        assertEquals(TraceDomainId.BATTLE_WEAPONS, operation.domainId());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
    }

    private static void assertIds(PolicyResult result, String... ids) {
        assertEquals("BATTLE_ACTION_TEXT_POLICY", result.producerId());
        assertEquals(List.of(ids), result.operations().stream()
                .map(operation -> operation.ruleArmId().id()).toList());
        for (PolicyOperation operation : result.operations()) {
            assertEquals(TraceDomainId.BATTLE_WEAPONS, operation.domainId());
        }
    }

    private static void assertOperationKindsAreAdditive(PolicyResult result) {
        for (PolicyOperation operation : result.operations()) {
            assertEquals(PolicyOperationKind.ADD, operation.kind());
            assertEquals(TraceDomainId.BATTLE_WEAPONS, operation.domainId());
        }
    }

    private static void assertRawDeltas(
            List<PolicyOperation> operations,
            float... expected) {
        assertEquals(expected.length, operations.size());
        for (int index = 0; index < expected.length; index++) {
            assertRawFloat(expected[index], operations.get(index).delta());
            assertEquals(PolicyOperationKind.ADD, operations.get(index).kind());
        }
    }

    private static void assertRawFloat(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(actual));
    }
}
