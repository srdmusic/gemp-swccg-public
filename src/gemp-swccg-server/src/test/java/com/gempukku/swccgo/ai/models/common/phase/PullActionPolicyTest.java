package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
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
