package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MoveHuntTargetPolicyTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";

    @Test
    public void jediTargetBeatsStrongerGenericTarget() {
        Harness harness = new Harness();
        PhysicalCard current = location("Vader's Castle");
        PhysicalCard generic = location("Generic Battle");
        PhysicalCard jediSite = location("Jedi Battle");
        PhysicalCard vader = card(
                PLAYER, CardCategory.CHARACTER, "Darth Vader");
        PhysicalCard weapon = card(
                PLAYER, CardCategory.WEAPON, "Vader's Lightsaber");
        PhysicalCard jedi = card(
                OPPONENT, CardCategory.CHARACTER, "Jedi Knight");
        harness.locations(current, generic, jediSite);
        harness.armed(vader, weapon);
        harness.power(current, 0.0f);
        harness.power(generic, 10.0f);
        harness.power(jediSite, 4.0f);
        when(harness.gameState.getCardsAtLocation(generic))
                .thenReturn(List.of());
        when(harness.gameState.getCardsAtLocation(jediSite))
                .thenReturn(List.of(jedi));

        MoveHuntTargetPolicy.Evaluation result = harness.evaluate(
                current, vader, card -> card == vader,
                location -> location == jediSite);

        assertEquals(MoveHuntTargetPolicy.Branch.JEDI, result.branch());
        assertTrue(result.contribution().applies());
        assertEquals("Jedi Battle", result.targetLocation());
        assertFloat(4.0f, result.targetPower());
        assertFloat(350.0f, result.contribution().delta());
        assertEquals(
                "V35 HUNT JEDI: Armed Vader at Vader's Castle — GO HUNT! Target: Jedi Battle (power 4)",
                result.contribution().reason());
    }

    @Test
    public void genericTargetUsesTwoHundredAndFirstStrictTie() {
        Harness harness = new Harness();
        PhysicalCard current = location("Current");
        PhysicalCard first = location("First");
        PhysicalCard tied = location("Tied");
        PhysicalCard dooku = card(
                PLAYER, CardCategory.CHARACTER, "Count Dooku");
        harness.locations(current, first, tied);
        harness.armed(dooku, card(
                PLAYER, CardCategory.WEAPON, "Lightsaber"));
        harness.power(current, 0.0f);
        harness.power(first, 5.0f);
        harness.power(tied, 5.0f);
        when(harness.gameState.getCardsAtLocation(first))
                .thenReturn(List.of());
        when(harness.gameState.getCardsAtLocation(tied))
                .thenReturn(List.of());

        MoveHuntTargetPolicy.Evaluation result = harness.evaluate(
                current, dooku, card -> card == dooku,
                location -> false);

        assertEquals(MoveHuntTargetPolicy.Branch.GENERIC, result.branch());
        assertEquals("First", result.targetLocation());
        assertFloat(200.0f, result.contribution().delta());
        assertEquals(
                "V35 HUNT DOWN: Armed Vader at Current — GO HUNT! Target: First (power 5)",
                result.contribution().reason());
    }

    @Test
    public void classifierRunsBeforeObjectiveGate() {
        Harness harness = new Harness();
        PhysicalCard current = location("Current");
        PhysicalCard hunter = card(
                PLAYER, CardCategory.CHARACTER, "Dark Hunter");
        List<String> events = new ArrayList<>();

        MoveHuntTargetPolicy.Evaluation result =
                MoveHuntTargetPolicy.evaluate(
                        harness.gameState, harness.game,
                        current, hunter, PLAYER,
                        () -> {
                            events.add("gate");
                            return false;
                        },
                        card -> {
                            events.add("classifier");
                            return true;
                        },
                        title -> false, 350.0f);

        assertEquals(List.of("classifier", "gate"), events);
        assertTrue(result.hunter());
        assertFalse(result.contribution().applies());
    }

    @Test
    public void classifierFailureIsFailOpenAndStillEvaluatesGate() {
        Harness harness = new Harness();
        PhysicalCard current = location("Current");
        PhysicalCard candidate = card(
                PLAYER, CardCategory.CHARACTER, "Dark Candidate");
        AtomicBoolean gateCalled = new AtomicBoolean();

        MoveHuntTargetPolicy.Evaluation result =
                MoveHuntTargetPolicy.evaluate(
                        harness.gameState, harness.game,
                        current, candidate, PLAYER,
                        () -> {
                            gateCalled.set(true);
                            return true;
                        },
                        card -> {
                            throw new RuntimeException("classifier");
                        },
                        title -> false, 350.0f);

        assertTrue(gateCalled.get());
        assertFalse(result.hunter());
        assertFalse(result.contribution().applies());
    }

    @Test
    public void unarmedOrLocallyContestedHunterDoesNotHunt() {
        Harness harness = new Harness();
        PhysicalCard current = location("Current");
        PhysicalCard target = location("Target");
        PhysicalCard vader = card(
                PLAYER, CardCategory.CHARACTER, "Vader");
        harness.locations(current, target);
        harness.power(current, 0.0f);
        harness.power(target, 5.0f);
        when(harness.gameState.getAttachedCards(vader))
                .thenReturn(List.of());

        MoveHuntTargetPolicy.Evaluation unarmed = harness.evaluate(
                current, vader, card -> card == vader,
                location -> false);
        assertFalse(unarmed.armed());
        assertFalse(unarmed.contribution().applies());

        harness.armed(vader, card(
                PLAYER, CardCategory.WEAPON, "Weapon"));
        harness.power(current, 1.0f);
        MoveHuntTargetPolicy.Evaluation contested = harness.evaluate(
                current, vader, card -> card == vader,
                location -> false);
        assertTrue(contested.armed());
        assertFloat(1.0f, contested.opponentPowerAtCurrentLocation());
        assertFalse(contested.contribution().applies());
    }

    @Test
    public void currentPowerReadFailureDefaultsToUncontested() {
        Harness harness = new Harness();
        PhysicalCard current = location("Current");
        PhysicalCard target = location("Target");
        PhysicalCard tyranus = card(
                PLAYER, CardCategory.CHARACTER, "Darth Tyranus");
        harness.locations(current, target);
        harness.armed(tyranus, card(
                PLAYER, CardCategory.WEAPON, "Weapon"));
        when(harness.modifiers.getTotalPowerAtLocation(
                harness.gameState, current, OPPONENT,
                false, false)).thenThrow(new RuntimeException("current"));
        harness.power(target, 3.0f);
        when(harness.gameState.getCardsAtLocation(target))
                .thenReturn(List.of());

        MoveHuntTargetPolicy.Evaluation result = harness.evaluate(
                current, tyranus, card -> card == tyranus,
                location -> false);

        assertTrue(result.contribution().applies());
        assertFloat(0.0f, result.opponentPowerAtCurrentLocation());
    }

    @Test
    public void attachmentReadFailureDefaultsToUnarmed() {
        Harness harness = new Harness();
        PhysicalCard current = location("Current");
        PhysicalCard target = location("Target");
        PhysicalCard vader = card(
                PLAYER, CardCategory.CHARACTER, "Vader");
        harness.locations(current, target);
        harness.power(current, 0.0f);
        when(harness.gameState.getAttachedCards(vader))
                .thenThrow(new RuntimeException("attachments"));

        MoveHuntTargetPolicy.Evaluation result = harness.evaluate(
                current, vader, card -> card == vader,
                location -> false);

        assertFalse(result.armed());
        assertFalse(result.contribution().applies());
        verify(harness.modifiers, never()).getTotalPowerAtLocation(
                harness.gameState, target, OPPONENT,
                false, false);
    }

    @Test
    public void partialTargetScanSurvivesFailureAndStopsLaterReads() {
        Harness harness = new Harness();
        PhysicalCard current = location("Current");
        PhysicalCard first = location("First");
        PhysicalCard broken = location("Broken");
        PhysicalCard later = location("Later");
        PhysicalCard vader = card(
                PLAYER, CardCategory.CHARACTER, "Vader");
        harness.locations(current, first, broken, later);
        harness.armed(vader, card(
                PLAYER, CardCategory.WEAPON, "Weapon"));
        harness.power(current, 0.0f);
        harness.power(first, 4.0f);
        when(harness.gameState.getCardsAtLocation(first))
                .thenReturn(List.of());
        when(harness.modifiers.getTotalPowerAtLocation(
                harness.gameState, broken, OPPONENT,
                false, false)).thenThrow(new RuntimeException("broken"));

        MoveHuntTargetPolicy.Evaluation result = harness.evaluate(
                current, vader, card -> card == vader,
                location -> false);

        assertTrue(result.contribution().applies());
        assertSame(MoveHuntTargetPolicy.Branch.GENERIC, result.branch());
        assertEquals("First", result.targetLocation());
        verify(harness.modifiers, never()).getTotalPowerAtLocation(
                harness.gameState, later, OPPONENT,
                false, false);
    }

    @Test
    public void zeroAndNegativeTargetsAreIgnored() {
        Harness harness = new Harness();
        PhysicalCard current = location("Current");
        PhysicalCard zero = location("Zero");
        PhysicalCard negative = location("Negative");
        PhysicalCard vader = card(
                PLAYER, CardCategory.CHARACTER, "Vader");
        harness.locations(current, zero, negative);
        harness.armed(vader, card(
                PLAYER, CardCategory.WEAPON, "Weapon"));
        harness.power(current, 0.0f);
        harness.power(zero, 0.0f);
        harness.power(negative, -1.0f);

        MoveHuntTargetPolicy.Evaluation result = harness.evaluate(
                current, vader, card -> card == vader,
                location -> false);

        assertFalse(result.contribution().applies());
    }

    @Test
    public void vaderTitleImpostorIsNotAHunter() {
        Harness harness = new Harness();
        PhysicalCard current = location("Current");
        PhysicalCard target = location("Target");
        PhysicalCard broker = card(
                PLAYER, CardCategory.CHARACTER,
                "Lando Calrissian, Vader's Broker");
        harness.locations(current, target);
        harness.armed(broker, card(
                PLAYER, CardCategory.WEAPON, "Weapon"));
        harness.power(current, 0.0f);
        harness.power(target, 5.0f);

        MoveHuntTargetPolicy.Evaluation result = harness.evaluate(
                current, broker, card -> false,
                location -> false);

        assertFalse(result.hunter());
        assertFalse(result.contribution().applies());
    }

    private static PhysicalCard location(String title) {
        return card(null, CardCategory.LOCATION, title);
    }

    private static PhysicalCard card(
            String owner, CardCategory category, String title) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getOwner()).thenReturn(owner);
        when(card.getTitle()).thenReturn(title);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory()).thenReturn(category);
        return card;
    }

    private static void assertFloat(float expected, float actual) {
        assertEquals(Float.floatToIntBits(expected),
                Float.floatToIntBits(actual));
    }

    private static final class Harness {
        private final GameState gameState = mock(GameState.class);
        private final SwccgGame game = mock(SwccgGame.class);
        private final ModifiersQuerying modifiers =
                mock(ModifiersQuerying.class);

        private Harness() {
            when(game.getModifiersQuerying()).thenReturn(modifiers);
            when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
        }

        private void locations(PhysicalCard... locations) {
            when(gameState.getTopLocations())
                    .thenReturn(List.of(locations));
        }

        private void armed(
                PhysicalCard hunter, PhysicalCard weapon) {
            when(gameState.getAttachedCards(hunter))
                    .thenReturn(List.of(weapon));
        }

        private void power(PhysicalCard location, float power) {
            when(modifiers.getTotalPowerAtLocation(
                    gameState, location, OPPONENT,
                    false, false)).thenReturn(power);
        }

        private MoveHuntTargetPolicy.Evaluation evaluate(
                PhysicalCard current, PhysicalCard mover,
                java.util.function.Predicate<PhysicalCard> hunter,
                java.util.function.Predicate<PhysicalCard>
                        blockerLocation) {
            return MoveHuntTargetPolicy.evaluate(
                    gameState, game, current, mover, PLAYER,
                    () -> true, hunter, blockerLocation,
                    350.0f);
        }
    }
}
