package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MoveWeaponHunterPolicyTest {
    @Test
    public void emptyWeaponTitlesProduceNoWeapon() {
        List<List<String>> cases = Arrays.asList(
                null, List.<String>of());
        for (List<String> titles : cases) {
            MoveWeaponHunterPolicy.WeaponFacts result =
                    MoveWeaponHunterPolicy.weaponFacts(titles);
            assertFalse(result.hasWeapon());
            assertFalse(result.lightsaber());
            assertNull(result.weaponName());
        }
    }

    @Test
    public void lastWeaponNameAndAnyLightsaberArePreserved() {
        MoveWeaponHunterPolicy.WeaponFacts result =
                MoveWeaponHunterPolicy.weaponFacts(
                        List.of("Vader's Lightsaber", "Blaster Rifle"));

        assertTrue(result.hasWeapon());
        assertTrue(result.lightsaber());
        assertEquals("Blaster Rifle", result.weaponName());
    }

    @Test
    public void nullLastWeaponNameIsPreserved() {
        MoveWeaponHunterPolicy.WeaponFacts result =
                MoveWeaponHunterPolicy.weaponFacts(
                        Arrays.asList("Vibro-Ax", null));

        assertTrue(result.hasWeapon());
        assertFalse(result.lightsaber());
        assertNull(result.weaponName());
    }

    @Test
    public void inactiveProfileRequiresAnAttachedWeapon() {
        MoveWeaponHunterPolicy.HunterProfile result =
                MoveWeaponHunterPolicy.profile(
                        MoveWeaponHunterPolicy.weaponFacts(List.of()),
                        "Darth Vader", 6.0f, true);

        assertFalse(result.active());
        assertFalse(result.canBeat(1, 0.0f));
    }

    @Test
    public void profilePreservesWeaponAndIhynPowerBonuses() {
        MoveWeaponHunterPolicy.HunterProfile saberVader =
                MoveWeaponHunterPolicy.profile(
                        MoveWeaponHunterPolicy.weaponFacts(
                                List.of("Vader's Lightsaber")),
                        "Darth Vader", 6.0f, true);
        assertTrue(saberVader.vader());
        assertTrue(saberVader.hasIHaveYouNow());
        assertEquals(13.0f, saberVader.effectivePower(), 0.0f);

        MoveWeaponHunterPolicy.HunterProfile armedNonVader =
                MoveWeaponHunterPolicy.profile(
                        MoveWeaponHunterPolicy.weaponFacts(
                                List.of("Blaster Rifle")),
                        "Grand Moff Tarkin", 6.0f, true);
        assertFalse(armedNonVader.vader());
        assertFalse(armedNonVader.hasIHaveYouNow());
        assertEquals(8.0f, armedNonVader.effectivePower(), 0.0f);
    }

    @Test
    public void canBeatUsesPositiveCountAndStrictPowerAdvantage() {
        MoveWeaponHunterPolicy.HunterProfile profile = profile(
                "Blaster", "Mara Jade", 8.0f, false);

        assertFalse(profile.canBeat(0, 0.0f));
        assertFalse(profile.canBeat(1, 10.0f));
        assertTrue(profile.canBeat(1, 9.999f));
        assertFalse(profile.canBeat(1, Float.NaN));
    }

    @Test
    public void advantageThresholdsRemainExact() {
        MoveWeaponHunterPolicy.HunterProfile profile = profile(
                "Blaster", "Mara Jade", 8.0f, false);

        assertEquals(60.0f, select(profile,
                target(0, "A", 8.0f, 0, false)).delta(), 0.0f);
        assertEquals(80.0f, select(profile,
                target(0, "A", 7.0f, 0, false)).delta(), 0.0f);
        assertEquals(100.0f, select(profile,
                target(0, "A", 4.0f, 0, false)).delta(), 0.0f);
    }

    @Test
    public void opponentIconsRemainFifteenEach() {
        MoveWeaponHunterPolicy.Evaluation result = select(
                profile("Blaster", "Mara Jade", 8.0f, false),
                target(0, "Docking Bay", 8.0f, 3, false));

        assertEquals(105.0f, result.delta(), 0.0f);
    }

    @Test
    public void vaderLukeAndIhynReasonRemainExact() {
        MoveWeaponHunterPolicy.HunterProfile profile = profile(
                "Vader's Lightsaber", "Darth Vader", 6.0f, true);
        MoveWeaponHunterPolicy.Evaluation result = select(profile,
                target(4, "Carbonite Chamber", 10.0f, 1, true));

        assertTrue(result.applies());
        assertEquals(245.0f, result.delta(), 0.0f);
        assertEquals(4, result.selectedTargetOrdinal());
        assertEquals(13.0f, result.effectivePower(), 0.0f);
        assertTrue(result.foundLuke());
        assertEquals(
                "V29.7 WEAPON HUNTER: Darth Vader + Vader's Lightsaber should CHALLENGE LUKE at Carbonite Chamber! (effective power 13) + IHYN in hand!",
                result.reason());
    }

    @Test
    public void genericReasonRemainsExact() {
        MoveWeaponHunterPolicy.HunterProfile profile = profile(
                "Blaster Rifle", "Mara Jade", 6.0f, false);
        MoveWeaponHunterPolicy.Evaluation result = select(profile,
                target(2, "Cloud City: Downtown Plaza", 5.0f, 0, false));

        assertEquals(80.0f, result.delta(), 0.0f);
        assertEquals(
                "V29.7 WEAPON HUNTER: Mara Jade + Blaster Rifle should attack Cloud City: Downtown Plaza (effective power 8 vs opponents)",
                result.reason());
    }

    @Test
    public void strictBestScoreTieKeepsFirstTarget() {
        MoveWeaponHunterPolicy.HunterProfile profile = profile(
                "Blaster", "Mara Jade", 6.0f, false);
        MoveWeaponHunterPolicy.Evaluation result =
                MoveWeaponHunterPolicy.select(profile, List.of(
                        target(3, "First", 5.0f, 0, false),
                        target(7, "Second", 5.0f, 0, false)));

        assertEquals(3, result.selectedTargetOrdinal());
        assertEquals("First", result.selectedTargetTitle());
    }

    @Test
    public void globalFoundLukeQuirkSurvivesLaterNonLukeWinner() {
        MoveWeaponHunterPolicy.HunterProfile profile = profile(
                "Vader's Lightsaber", "Darth Vader", 6.0f, false);
        MoveWeaponHunterPolicy.Evaluation result =
                MoveWeaponHunterPolicy.select(profile, List.of(
                        target(0, "Luke Site", 9.0f, 0, true),
                        target(1, "Non-Luke Site", 9.0f, 11, false)));

        assertEquals(1, result.selectedTargetOrdinal());
        assertEquals(225.0f, result.delta(), 0.0f);
        assertTrue(result.foundLuke());
        assertEquals(
                "V29.7 WEAPON HUNTER: Darth Vader + Vader's Lightsaber should CHALLENGE LUKE at Non-Luke Site! (effective power 10)",
                result.reason());
    }

    @Test
    public void nullTitleWinningTargetSuppressesOutput() {
        MoveWeaponHunterPolicy.HunterProfile profile = profile(
                "Vader's Lightsaber", "Darth Vader", 6.0f, false);
        MoveWeaponHunterPolicy.Evaluation result =
                MoveWeaponHunterPolicy.select(profile, List.of(
                        target(0, null, 9.0f, 2, true),
                        target(1, "Named", 9.0f, 0, false)));

        assertFalse(result.applies());
        assertEquals(-1, result.selectedTargetOrdinal());
    }

    private static MoveWeaponHunterPolicy.HunterProfile profile(
            String weapon, String character, float power, boolean ihyn) {
        return MoveWeaponHunterPolicy.profile(
                MoveWeaponHunterPolicy.weaponFacts(List.of(weapon)),
                character, power, ihyn);
    }

    private static MoveWeaponHunterPolicy.TargetFact target(
            int ordinal, String title, float opponentPower,
            int icons, boolean luke) {
        return new MoveWeaponHunterPolicy.TargetFact(
                ordinal, title, opponentPower, icons, luke);
    }

    private static MoveWeaponHunterPolicy.Evaluation select(
            MoveWeaponHunterPolicy.HunterProfile profile,
            MoveWeaponHunterPolicy.TargetFact target) {
        return MoveWeaponHunterPolicy.select(profile, List.of(target));
    }
}
