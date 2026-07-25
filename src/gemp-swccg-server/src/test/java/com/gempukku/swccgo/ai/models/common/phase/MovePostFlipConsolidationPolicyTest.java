package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MovePostFlipConsolidationPolicyTest {
    @Test
    public void objectiveFragmentsMatchCaseInsensitiveSubstrings() {
        Set<String> fragments = Set.of("Cloud City", "Bespin");

        assertTrue(MovePostFlipConsolidationPolicy.isObjectiveLocation(
                "Cloud City: Downtown Plaza", fragments));
        assertTrue(MovePostFlipConsolidationPolicy.isObjectiveLocation(
                "BESPIN", fragments));
        assertFalse(MovePostFlipConsolidationPolicy.isObjectiveLocation(
                "Dagobah: Bog Clearing", fragments));
        assertFalse(MovePostFlipConsolidationPolicy.isObjectiveLocation(
                null, fragments));
    }

    @Test(expected = NullPointerException.class)
    public void nullFragmentPreservesFailIntoAdapterCatch() {
        MovePostFlipConsolidationPolicy.isObjectiveLocation(
                "Cloud City: Downtown Plaza",
                java.util.Collections.singleton(null));
    }

    @Test
    public void fewerThanThreeOccupiedLocationsNeverApplies() {
        Map<String, Float> powers = linkedPowers(
                "Site A", 3.0f,
                "Site B", 6.0f);

        assertNone(MovePostFlipConsolidationPolicy.evaluate(
                "Site A", true, powers));
    }

    @Test
    public void nonObjectiveCurrentLocationNeverApplies() {
        Map<String, Float> powers = linkedPowers(
                "Site A", 3.0f,
                "Site B", 6.0f,
                "Site C", 8.0f);

        assertNone(MovePostFlipConsolidationPolicy.evaluate(
                "Site A", false, powers));
    }

    @Test
    public void strictWeakestSelectionKeepsFirstLocationOnTie() {
        Map<String, Float> powers = linkedPowers(
                "Site A", 3.0f,
                "Site B", 3.0f,
                "Site C", 8.0f);

        MovePostFlipConsolidationPolicy.Evaluation first =
                MovePostFlipConsolidationPolicy.evaluate(
                        "Site A", true, powers);
        MovePostFlipConsolidationPolicy.Evaluation second =
                MovePostFlipConsolidationPolicy.evaluate(
                        "Site B", true, powers);

        assertTrue(first.applies());
        assertNone(second);
    }

    @Test
    public void currentLocationComparisonRemainsCaseSensitive() {
        Map<String, Float> powers = linkedPowers(
                "Site A", 3.0f,
                "Site B", 6.0f,
                "Site C", 8.0f);

        assertNone(MovePostFlipConsolidationPolicy.evaluate(
                "site a", true, powers));
    }

    @Test
    public void weakestCurrentLocationGetsExactR2Contribution() {
        Map<String, Float> powers = linkedPowers(
                "Site A", 3.4f,
                "Site B", 6.0f,
                "Site C", 8.0f);

        MovePostFlipConsolidationPolicy.Evaluation result =
                MovePostFlipConsolidationPolicy.evaluate(
                        "Site A", true, powers);

        assertTrue(result.applies());
        assertEquals("Site A", result.weakestLocationTitle());
        assertEquals(3.4f, result.weakestPower(), 0.0f);
        assertEquals(200.0f, result.delta(), 0.0f);
        assertTrue(result.claimDoctrine());
        assertEquals(
                "V31 POST-FLIP CONSOLIDATE: At weakest obj loc Site A"
                        + " (power 3) — move to reinforce stronger position!",
                result.reason());
    }

    @Test
    public void exactFlipBackLocationCannotBeEvacuatedByV31() {
        Map<String, Float> powers = linkedPowers(
                "Naboo: Theed Palace Throne Room", 3.0f,
                "Naboo", 8.0f,
                "Naboo: Swamp", 6.0f);

        assertNone(MovePostFlipConsolidationPolicy.evaluate(
                "Naboo: Theed Palace Throne Room",
                true,
                true,
                powers));
    }

    @Test
    public void surplusThirdLocationStillGetsExactR2Contribution() {
        Map<String, Float> powers = linkedPowers(
                "Naboo", 8.0f,
                "Naboo: Theed Palace Throne Room", 6.0f,
                "Naboo: Swamp", 3.0f);

        MovePostFlipConsolidationPolicy.Evaluation result =
                MovePostFlipConsolidationPolicy.evaluate(
                        "Naboo: Swamp",
                        true,
                        false,
                        powers);

        assertTrue(result.applies());
        assertEquals("Naboo: Swamp", result.weakestLocationTitle());
        assertEquals(200.0f, result.delta(), 0.0f);
        assertTrue(result.claimDoctrine());
    }

    private static Map<String, Float> linkedPowers(Object... entries) {
        Map<String, Float> result = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put((String) entries[index], (Float) entries[index + 1]);
        }
        return result;
    }

    private static void assertNone(
            MovePostFlipConsolidationPolicy.Evaluation result) {
        assertFalse(result.applies());
        assertNull(result.weakestLocationTitle());
        assertEquals(0.0f, result.weakestPower(), 0.0f);
        assertNull(result.reason());
        assertEquals(0.0f, result.delta(), 0.0f);
        assertFalse(result.claimDoctrine());
    }
}
