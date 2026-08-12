package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SetupPolicyTest {
    @Test
    public void setupTurnIncludesTurnZeroAndNegativeBootstrap() {
        assertTrue(SetupPolicy.isSetupTurn(-1));
        assertTrue(SetupPolicy.isSetupTurn(0));
        assertFalse(SetupPolicy.isSetupTurn(1));
    }

    @Test
    public void earlyBanIsDecisionTerminalAndPreservesCandidateOrder() {
        SetupPolicy.EarlyBanEvaluation result =
                SetupPolicy.earlyStartingEffectBan(List.of(
                        new SetupPolicy.StartingCandidate("7", "Safe Effect"),
                        new SetupPolicy.StartingCandidate("8", "Tentacle"),
                        new SetupPolicy.StartingCandidate("temp0", null)));

        assertTrue(result.terminalDecision());
        assertEquals(List.of("7", "8", "temp0"), result.candidates().stream()
                .map(SetupPolicy.EarlyBanCandidate::actionId).toList());
        assertEquals(100.0f, result.candidates().get(0).score(), 0.0f);
        assertEquals(-500.0f, result.candidates().get(1).score(), 0.0f);
        assertEquals(100.0f, result.candidates().get(2).score(), 0.0f);
    }

    @Test
    public void earlyBanWithoutMatchDoesNotOwnDecision() {
        SetupPolicy.EarlyBanEvaluation result =
                SetupPolicy.earlyStartingEffectBan(List.of(
                        new SetupPolicy.StartingCandidate("7", "Safe Effect")));

        assertFalse(result.terminalDecision());
        assertTrue(result.candidates().isEmpty());
    }

    @Test
    public void startingInterruptPreservesExclusiveScoreLadder() {
        assertContribution(SetupPolicy.startingInterrupt(
                        "The Force Is Strong In My Family"),
                SetupPolicy.Branch.INTERRUPT_EPIC, 1500.0f);
        assertContribution(SetupPolicy.startingInterrupt(
                        "Starting: deploy three Effects"),
                SetupPolicy.Branch.INTERRUPT_THREE_EFFECTS, 300.0f);
        assertContribution(SetupPolicy.startingInterrupt(
                        "Starting: deploy two Effects"),
                SetupPolicy.Branch.INTERRUPT_TWO_EFFECTS, 250.0f);
        assertContribution(SetupPolicy.startingInterrupt(
                        "Starting: deploy an Effect"),
                SetupPolicy.Branch.INTERRUPT_EFFECTS, 200.0f);
        assertContribution(SetupPolicy.startingInterrupt(
                        "Starting: deploy a location"),
                SetupPolicy.Branch.INTERRUPT_DEPLOY, 100.0f);
        assertContribution(SetupPolicy.startingInterrupt("Draw a card"),
                SetupPolicy.Branch.INTERRUPT_GENERIC, 0.0f);
    }

    @Test
    public void startingLocationTextStacksInLegacyOrder() {
        List<SetupPolicy.Contribution> result =
                SetupPolicy.startingLocationText(
                        "Funeral Pyre",
                        "Reserve deck, force generation, and Epic text");

        assertBranches(result,
                SetupPolicy.Branch.LOCATION_RESERVE,
                SetupPolicy.Branch.LOCATION_FORCE_GENERATION,
                SetupPolicy.Branch.LOCATION_EPIC,
                SetupPolicy.Branch.LOCATION_FUNERAL_PYRE);
        assertDeltas(result, 75.0f, 25.0f, 1000.0f, 1000.0f);
    }

    @Test
    public void cloudCityBothIconsUsesInteriorArm() {
        assertContribution(SetupPolicy.startingLocationCloudCity(
                        "Cloud City: Mixed Site", true, true),
                SetupPolicy.Branch.LOCATION_CC_INTERIOR, -500.0f);
    }

    @Test
    public void battlegroundDetectionPreservesPrecedenceAndSithArms() {
        SetupPolicy.BattlegroundEvaluation battleground =
                SetupPolicy.startingLocationBattleground(
                        "Dooku's Site", "This is a battleground", true, true);
        assertTrue(battleground.battleground());
        assertEquals("game text contains 'battleground'",
                battleground.battlegroundReason());
        assertContribution(battleground.contribution(),
                SetupPolicy.Branch.LOCATION_BATTLEGROUND, 300.0f);

        assertContribution(SetupPolicy.startingLocationSith(
                        "Dooku's Site", true, true),
                SetupPolicy.Branch.LOCATION_SITH_NON_PALACE, 600.0f);
        assertContribution(SetupPolicy.startingLocationSith(
                        "Theed Palace", true, true),
                SetupPolicy.Branch.LOCATION_SITH_PALACE, -350.0f);
        assertContribution(SetupPolicy.startingLocationSith(
                        "Guest Quarters", false, true),
                SetupPolicy.Branch.LOCATION_SITH_NON_BATTLEGROUND, -300.0f);
        assertNull(SetupPolicy.startingLocationSith(
                "Guest Quarters", false, false));
    }

    @Test
    public void startingEffectBanIsCandidateTerminal() {
        SetupPolicy.StartingEffectEvaluation result =
                SetupPolicy.startingEffectBan("No Escape (V)");

        assertTrue(result.terminalCandidate());
        assertBranches(result.contributions(), SetupPolicy.Branch.EFFECT_BANNED);
        assertDeltas(result.contributions(), -600.0f);
    }

    @Test
    public void startingEffectIdentityPreservesAdditiveOrder() {
        List<SetupPolicy.Contribution> result =
                SetupPolicy.startingEffectIdentity(
                        "Endor Shield: You'll Be Dead", true, 2, true);

        assertBranches(result,
                SetupPolicy.Branch.EFFECT_DUPLICATE,
                SetupPolicy.Branch.EFFECT_PREFERRED,
                SetupPolicy.Branch.EFFECT_SHADOW_COLLECTIVE,
                SetupPolicy.Branch.EFFECT_IWTM);
        assertDeltas(result, -300.0f, 200.0f, 500.0f, 1000.0f);
    }

    @Test
    public void startingEffectTextPreservesThreeIndependentV126Arms() {
        List<SetupPolicy.Contribution> result =
                SetupPolicy.startingEffectText(
                        "First Strike",
                        "Initiate battles for free. Force generation is +1."
                                + " [Episode I] Dark Jedi.",
                        true);

        assertBranches(result,
                SetupPolicy.Branch.EFFECT_BATTLE_STARTER,
                SetupPolicy.Branch.EFFECT_FORCE_GENERATION,
                SetupPolicy.Branch.EFFECT_ROTS_SYNERGY);
        assertDeltas(result, 500.0f, 400.0f, 600.0f);
    }

    @Test
    public void startingEffectDeckStacksSkywalkerBeforeHuntDownPenalty() {
        List<SetupPolicy.Contribution> result =
                SetupPolicy.startingEffectDeck("A Cunning Warrior", true);

        assertBranches(result,
                SetupPolicy.Branch.EFFECT_SKYWALKER,
                SetupPolicy.Branch.EFFECT_HUNT_DOWN_OTHER);
        assertDeltas(result, 1000.0f, -300.0f);
        assertContribution(SetupPolicy.startingEffectDeck(
                        "I Am Your Father", true).get(0),
                SetupPolicy.Branch.EFFECT_HUNT_DOWN_REQUIRED, 500.0f);
        assertTrue("Legacy Hunt Down must bypass the Vader-deck whitelist",
                SetupPolicy.startingEffectDeck(
                        "A Sith's Plans", false).isEmpty());
    }

    @Test
    public void objectiveStartingEffectPreservesFirstMatchesAndAdditiveOrder() {
        List<SetupPolicy.Contribution> result =
                SetupPolicy.startingEffectObjective(
                        "Deploy a Hoth location from Reserve Deck."
                                + " It adds one to Force generation and deploys Vader."
                                + " This Epic text also mentions Tatooine.",
                        true,
                        List.of("hoth", "tatooine"),
                        List.of("vader", "luke"));

        assertBranches(result,
                SetupPolicy.Branch.EFFECT_OBJECTIVE_LOCATION,
                SetupPolicy.Branch.EFFECT_OBJECTIVE_REQUIRED_CARD,
                SetupPolicy.Branch.EFFECT_LOCATION_PULL,
                SetupPolicy.Branch.EFFECT_RESERVE_ACCESS,
                SetupPolicy.Branch.EFFECT_FORCE_ECONOMY,
                SetupPolicy.Branch.EFFECT_EPIC);
        assertDeltas(result, 250.0f, 200.0f, 100.0f, 50.0f, 25.0f, 1500.0f);
    }

    @Test
    public void reserveStartingEffectsKeepV80UngatedAndShadowTurnGated() {
        assertBranches(SetupPolicy.reserveStartingEffect(
                        "A Good Friend", false),
                SetupPolicy.Branch.RESERVE_SKYWALKER);
        assertTrue(SetupPolicy.reserveStartingEffect(
                "You'll Be Dead", false).isEmpty());
        assertBranches(SetupPolicy.reserveStartingEffect(
                        "You'll Be Dead", true),
                SetupPolicy.Branch.RESERVE_SHADOW_COLLECTIVE);
    }

    @Test
    public void sagaScoringAndDirectSelectionUseSameDeckMapping() {
        SetupPolicy.SagaEvaluation score = SetupPolicy.sagaChoice(
                "Rey Saga", "You Have That Power, Too");
        assertTrue(score.sagaChoice());
        assertContribution(score.contribution(),
                SetupPolicy.Branch.SAGA_CORRECT, 1000.0f);

        SetupPolicy.SagaEvaluation wrong = SetupPolicy.sagaChoice(
                "Luke Saga", "My Father Has It");
        assertContribution(wrong.contribution(),
                SetupPolicy.Branch.SAGA_WRONG, -500.0f);

        String[] shuffled = {
                "You Have That Power, Too", "I Have It", "My Father Has It"};
        SetupPolicy.SagaSelection rey =
                SetupPolicy.chooseSaga("Rey Saga", shuffled);
        assertTrue(rey.sagaChoice());
        assertEquals(0, rey.index());
        assertEquals(1, SetupPolicy.chooseSaga("Unknown", shuffled).index());
        assertEquals(-1, SetupPolicy.chooseSaga(
                "Unknown", new String[]{"My Father Has It"}).index());
        assertFalse(SetupPolicy.chooseSaga(
                "Luke Saga", new String[]{"No", "Yes"}).sagaChoice());
    }

    // V61 amended 2026-07-27: persona counts are the primary saga signal.
    @Test
    public void sagaPersonaCountsBeatDeckNameAndDefault() {
        String[] canonical = {
                "My Father Has It", "I Have It", "You Have That Power, Too"};

        // Replay rgfogqxrh4uat4bo: 4x Rey / 3x Luke / 2x Anakin. The deck
        // name is null AND would have said Luke — counts must win.
        SetupPolicy.SagaSelection rey = SetupPolicy.chooseSaga(
                null, canonical, 3, 2, 4);
        assertEquals(2, rey.index());
        assertTrue(rey.reason().contains("counts L=3 A=2 R=4"));

        // A name that contradicts the counts loses to the counts.
        assertEquals(2, SetupPolicy.chooseSaga(
                "Skywalker Saga - Luke", canonical, 3, 2, 4).index());

        // Unique Luke majority picks Luke; unique Anakin picks Anakin.
        assertEquals(1, SetupPolicy.chooseSaga(
                null, canonical, 4, 1, 2).index());
        assertEquals(0, SetupPolicy.chooseSaga(
                null, canonical, 1, 4, 2).index());

        // Tie between personas falls through to the name chain.
        assertEquals(2, SetupPolicy.chooseSaga(
                "Rey Saga", canonical, 3, 0, 3).index());

        // All-zero counts preserve the original name chain and default.
        assertEquals(1, SetupPolicy.chooseSaga(
                "Skywalker Saga - Luke", canonical, 0, 0, 0).index());
        assertEquals(1, SetupPolicy.chooseSaga(
                null, canonical, 0, 0, 0).index());

        // Winning persona's option missing from the menu → fall through
        // (Rey majority, but only Anakin/Luke offered → name/default chain).
        assertEquals(1, SetupPolicy.chooseSaga(
                null, new String[]{"My Father Has It", "I Have It"},
                0, 0, 4).index());

        // Non-saga menus stay untouched regardless of counts.
        assertFalse(SetupPolicy.chooseSaga(
                null, new String[]{"No", "Yes"}, 0, 0, 4).sagaChoice());
    }

    // V61 starting-location signal 2026-07-27: the marker location decides
    // before counts, name, and default.
    @Test
    public void sagaStartingLocationBeatsCountsNameAndDefault() {
        String[] canonical = {
                "My Father Has It", "I Have It", "You Have That Power, Too"};

        // The failing 05:29 game: 3x Luke / 3x Rey tie, null name, but the
        // deck started at Ajan Kloss: Training Course → Rey.
        SetupPolicy.SagaSelection rey = SetupPolicy.chooseSaga(
                null, canonical, 3, 2, 3,
                SetupPolicy.SagaStartingLocation.REY_LOCATION);
        assertEquals(2, rey.index());
        assertTrue(rey.reason().contains("Ajan Kloss"));

        // The location outranks contradicting counts AND a contradicting name.
        assertEquals(2, SetupPolicy.chooseSaga(
                "Skywalker Saga - Luke", canonical, 4, 0, 1,
                SetupPolicy.SagaStartingLocation.REY_LOCATION).index());
        assertEquals(1, SetupPolicy.chooseSaga(
                "Rey Saga", canonical, 0, 0, 4,
                SetupPolicy.SagaStartingLocation.LUKE_LOCATION).index());

        // Any other own starting location is the Anakin build.
        assertEquals(0, SetupPolicy.chooseSaga(
                null, canonical, 0, 0, 4,
                SetupPolicy.SagaStartingLocation.OTHER_LOCATION).index());

        // UNKNOWN board falls back to the count law.
        assertEquals(2, SetupPolicy.chooseSaga(
                null, canonical, 3, 2, 4,
                SetupPolicy.SagaStartingLocation.UNKNOWN).index());

        // A pinned option missing from the menu falls through safely.
        assertEquals(1, SetupPolicy.chooseSaga(
                null, new String[]{"My Father Has It", "I Have It"},
                0, 0, 0,
                SetupPolicy.SagaStartingLocation.REY_LOCATION).index());

        // Non-saga menus stay untouched regardless of the location.
        assertFalse(SetupPolicy.chooseSaga(
                null, new String[]{"No", "Yes"}, 0, 0, 0,
                SetupPolicy.SagaStartingLocation.REY_LOCATION).sagaChoice());
    }

    private static void assertContribution(
            SetupPolicy.Contribution contribution,
            SetupPolicy.Branch branch,
            float delta) {
        assertEquals(branch, contribution.branch());
        assertEquals(delta, contribution.delta(), 0.0f);
    }

    private static void assertBranches(
            List<SetupPolicy.Contribution> contributions,
            SetupPolicy.Branch... expected) {
        assertEquals(List.of(expected), contributions.stream()
                .map(SetupPolicy.Contribution::branch).toList());
    }

    private static void assertDeltas(
            List<SetupPolicy.Contribution> contributions,
            float... expected) {
        assertEquals(expected.length, contributions.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], contributions.get(i).delta(), 0.0f);
        }
    }
}
