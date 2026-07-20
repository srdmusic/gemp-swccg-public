package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PullActionPolicyTest {

    @Test
    public void nonWeaponAndLocationPullsDoNotEnterWeaponOrder() {
        assertNoOperation(weaponOrder(false, false, 0, 0, false, 0));
        assertNoOperation(weaponOrder(true, true, 0, 1, true, 0));
    }

    @Test
    public void allArmedBlockPrecedesNoCharacterAndLightsaberChecks() {
        PullActionPolicy.WeaponOrderEvaluation result =
                weaponOrder(true, false, 0, 2, true, 0);

        assertEquals(PullActionPolicy.WeaponOrderOutcome.ALL_ARMED,
                result.outcome());
        assertOperation(result, "V67ar-weapon", -9999.0f,
                "V67ar UNIVERSAL BLOCK: every Rando character (2) already armed \u2014 pulled weapon would stack a 2nd weapon (forbidden)!");
    }

    @Test
    public void noCharacterBlockPrecedesLightsaberWielderCheck() {
        PullActionPolicy.WeaponOrderEvaluation result =
                weaponOrder(true, false, 0, 0, true, 0);

        assertEquals(PullActionPolicy.WeaponOrderOutcome.NO_CHARACTER,
                result.outcome());
        assertOperation(result, "V67ao-weapon", -9999.0f,
                "V67ao ORDER GATE: weapon pull blocked \u2014 no Rando character on table to hold the weapon. Deploy a character first!");
    }

    @Test
    public void lightsaberWithoutCapableWielderKeepsExactV149Veto() {
        PullActionPolicy.WeaponOrderEvaluation result =
                weaponOrder(true, false, 1, 0, true, 0);

        assertEquals(PullActionPolicy.WeaponOrderOutcome.NO_LIGHTSABER_WIELDER,
                result.outcome());
        assertOperation(result, "V149", -2000.0f,
                "V149 NO LIGHTSABER WIELDER: no unarmed [Warrior] ability-4+ character on table \u2014 don't pull a lightsaber nobody can wield");
    }

    @Test
    public void ordinaryWeaponIsReadyWhenAnyUnarmedCharacterExists() {
        assertReady(weaponOrder(true, false, 1, 3, false, 0));
    }

    @Test
    public void lightsaberIsReadyWhenCapableWielderExists() {
        assertReady(weaponOrder(true, false, 1, 3, true, 1));
    }

    @Test
    public void takeIntoHandParentMatrixKeepsExactScoresAndReserveNoOp() {
        assertTake(take(PullActionPolicy.TakeIntoHandKind.PALPATINE),
                "PULL-take-palpatine", TraceOutputKind.ORDERING,
                -30.0f, "Avoid taking Palpatine");
        assertTake(take(PullActionPolicy.TakeIntoHandKind.BOUNCE),
                "V29.7-bounce", TraceOutputKind.ORDERING, -300.0f,
                "V29.7 BOUNCE: Return own card from table to hand \u2014 DON'T undo your deploy!");
        assertEquals(0, take(PullActionPolicy.TakeIntoHandKind.RESERVE_LOG_ONLY)
                .operations().size());
        assertTake(take(PullActionPolicy.TakeIntoHandKind.LOST_PILE_NO_MATCH),
                "V63-lost-pile-no-match", TraceOutputKind.VETO, -9999.0f,
                "V63 LOST PILE EMPTY: no matching target in Lost Pile \u2014 search will FAIL and reveal our pile!");
        assertTake(take(PullActionPolicy.TakeIntoHandKind.LOST_PILE_MATCH),
                "PULL-take-lost-pile", TraceOutputKind.ORDERING, 30.0f,
                "Take card into hand from Lost Pile");
        assertTake(take(PullActionPolicy.TakeIntoHandKind.GENERIC),
                "PULL-take-generic", TraceOutputKind.ORDERING, 30.0f,
                "Take card into hand");
    }

    private static PullActionPolicy.WeaponOrderEvaluation weaponOrder(
            boolean weaponPull, boolean locationPull,
            int unarmedCharacters, int armedCharacters,
            boolean lightsaberPull, int capableWielders) {
        return PullActionPolicy.evaluateWeaponOrder(
                new PullActionPolicy.WeaponOrderFacts(
                        "pull-42", weaponPull, locationPull,
                        unarmedCharacters, armedCharacters,
                        lightsaberPull, capableWielders));
    }

    private static PolicyResult take(PullActionPolicy.TakeIntoHandKind kind) {
        return PullActionPolicy.scoreTakeIntoHand(
                new PullActionPolicy.TakeIntoHandFacts("pull-42", kind));
    }

    private static void assertTake(PolicyResult result, String ruleId,
                                   TraceOutputKind outputKind, float delta,
                                   String reason) {
        assertEquals("PULL_ACTION_POLICY", result.producerId());
        assertEquals(1, result.operations().size());
        PolicyOperation operation = result.operations().get(0);
        assertEquals("pull-42", operation.actionId());
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(TraceDomainId.PULL_SEARCH, operation.domainId());
        assertEquals(outputKind, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(delta, operation.delta(), 0.0f);
        assertEquals(reason, operation.reason());
    }

    private static void assertNoOperation(
            PullActionPolicy.WeaponOrderEvaluation evaluation) {
        assertEquals(PullActionPolicy.WeaponOrderOutcome.NONE,
                evaluation.outcome());
        assertEquals(0, evaluation.result().operations().size());
    }

    private static void assertReady(
            PullActionPolicy.WeaponOrderEvaluation evaluation) {
        assertEquals(PullActionPolicy.WeaponOrderOutcome.READY,
                evaluation.outcome());
        assertEquals(0, evaluation.result().operations().size());
    }

    private static void assertOperation(
            PullActionPolicy.WeaponOrderEvaluation evaluation,
            String ruleId, float delta, String reason) {
        assertEquals(1, evaluation.result().operations().size());
        PolicyOperation operation = evaluation.result().operations().get(0);
        assertEquals("pull-42", operation.actionId());
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(TraceDomainId.PULL_SEARCH, operation.domainId());
        assertEquals(TraceOutputKind.VETO, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(delta, operation.delta(), 0.0f);
        assertEquals(reason, operation.reason());
    }
}
