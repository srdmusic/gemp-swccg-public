package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MoveUnarmedVaderPolicyTest {
    @Test
    public void nonVaderNeverApplies() {
        for (boolean hasWeapon : new boolean[]{false, true}) {
            for (boolean saberInHand : new boolean[]{false, true}) {
                assertNone(MoveUnarmedVaderPolicy.evaluate(
                        false, hasWeapon, saberInHand));
            }
        }
    }

    @Test
    public void armedVaderNeverApplies() {
        for (boolean saberInHand : new boolean[]{false, true}) {
            assertNone(MoveUnarmedVaderPolicy.evaluate(
                    true, true, saberInHand));
        }
    }

    @Test
    public void unarmedVaderWithLightsaberMustEquipFirst() {
        MoveUnarmedVaderPolicy.Evaluation result =
                MoveUnarmedVaderPolicy.evaluate(true, false, true);

        assertEquals(MoveUnarmedVaderPolicy.Branch.EQUIP_FIRST,
                result.branch());
        assertTrue(result.applies());
        assertEquals(-250.0f, result.delta(), 0.0f);
        assertEquals(
                "V29.9 UNARMED VADER: Lightsaber in hand — EQUIP FIRST before attacking!",
                result.reason());
    }

    @Test
    public void unarmedVaderWithoutLightsaberGetsVulnerabilityPenalty() {
        MoveUnarmedVaderPolicy.Evaluation result =
                MoveUnarmedVaderPolicy.evaluate(true, false, false);

        assertEquals(MoveUnarmedVaderPolicy.Branch.UNARMED,
                result.branch());
        assertTrue(result.applies());
        assertEquals(-100.0f, result.delta(), 0.0f);
        assertEquals(
                "V29.9 UNARMED VADER: No weapon — vulnerable without lightsaber!",
                result.reason());
    }

    private static void assertNone(
            MoveUnarmedVaderPolicy.Evaluation result) {
        assertEquals(MoveUnarmedVaderPolicy.Branch.NONE, result.branch());
        assertFalse(result.applies());
        assertEquals(0.0f, result.delta(), 0.0f);
        assertNull(result.reason());
    }
}
