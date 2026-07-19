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
