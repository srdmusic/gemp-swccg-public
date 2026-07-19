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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MoveObjectiveConsolidationPolicyTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";

    @Test
    public void lonePreFlipUsesStrictBestAllyAndTripleBonus() {
        Harness harness = new Harness();
        PhysicalCard current = location("Current");
        PhysicalCard first = location("First Ally");
        PhysicalCard tied = location("Tied Ally");
        harness.cardsAtCurrent(current, powerCard(PLAYER, "Solo", 2.0f));
        harness.locations(current, first, tied);
        harness.power(current, OPPONENT, 7.0f);
        harness.power(first, PLAYER, 5.0f);
        harness.power(tied, PLAYER, 5.0f);

        MoveObjectiveConsolidationPolicy.Evaluation result =
                harness.preFlip(current);

        assertEquals(
                MoveObjectiveConsolidationPolicy.Branch
                        .PRE_FLIP_LONE_OUTGUNNED,
                result.branch());
        assertEquals("First Ally", result.bestAllyLocation());
        assertFloat(160.0f, result.contribution().delta());
        assertTrue(result.contribution().claimDoctrineRank());
        assertEquals(
                "V22.5 PRE-FLIP: LONE & OUTGUNNED (2 vs 7) - move to join allies at First Ally",
                result.contribution().reason());
    }

    @Test
    public void lonePreFlipKeepsScoreWhenNoAllyLocationExists() {
        Harness harness = new Harness();
        PhysicalCard current = location("Current");
        harness.cardsAtCurrent(current, powerCard(PLAYER, "Solo", 3.0f));
        harness.locations(current);
        harness.power(current, OPPONENT, 7.0f);

        MoveObjectiveConsolidationPolicy.Evaluation result =
                harness.preFlip(current);

        assertFloat(100.0f, result.contribution().delta());
        assertEquals(null, result.bestAllyLocation());
        assertEquals(
                "V22.5 PRE-FLIP: LONE & OUTGUNNED (3 vs 7) - move to join allies",
                result.contribution().reason());
    }

    @Test
    public void smallGroupPreFlipUsesPowerBearingCardsAndSixty() {
        Harness harness = new Harness();
        PhysicalCard current = location("Current");
        harness.cardsAtCurrent(current,
                powerCard(PLAYER, "Character", 3.0f),
                powerCard(
                        PLAYER, "Power-Bearing Ship", 3.0f,
                        CardCategory.STARSHIP));
        harness.power(current, OPPONENT, 10.0f);

        MoveObjectiveConsolidationPolicy.Evaluation result =
                harness.preFlip(current);

        assertEquals(2, result.ownPowerCardCount());
        assertEquals(
                MoveObjectiveConsolidationPolicy.Branch
                        .PRE_FLIP_SMALL_GROUP_OUTGUNNED,
                result.branch());
        assertFloat(60.0f, result.contribution().delta());
        assertFalse(result.contribution().claimDoctrineRank());
        assertEquals(
                "V22.5 PRE-FLIP: Outgunned at Current (6 vs 10)",
                result.contribution().reason());
    }

    @Test
    public void preFlipThresholdsRemainStrict() {
        Harness harness = new Harness();
        PhysicalCard current = location("Current");
        harness.cardsAtCurrent(current,
                powerCard(PLAYER, "One", 3.0f),
                powerCard(PLAYER, "Two", 3.0f));
        harness.power(current, OPPONENT, 9.0f);
        assertFalse(harness.preFlip(current).contribution().applies());

        harness.power(current, OPPONENT, 9.1f);
        assertEquals(
                MoveObjectiveConsolidationPolicy.Branch
                        .PRE_FLIP_SMALL_GROUP_OUTGUNNED,
                harness.preFlip(current).branch());
    }

    @Test
    public void preFlipPartialAllyScanKeepsEarlierCandidate() {
        Harness harness = new Harness();
        PhysicalCard current = location("Current");
        PhysicalCard first = location("First");
        PhysicalCard broken = location("Broken");
        PhysicalCard later = location("Later");
        harness.cardsAtCurrent(current, powerCard(PLAYER, "Solo", 2.0f));
        harness.locations(current, first, broken, later);
        harness.power(current, OPPONENT, 7.0f);
        harness.power(first, PLAYER, 4.0f);
        when(harness.modifiers.getTotalPowerAtLocation(
                harness.gameState, broken, PLAYER,
                false, false)).thenThrow(new RuntimeException("broken"));

        MoveObjectiveConsolidationPolicy.Evaluation result =
                harness.preFlip(current);

        assertEquals("First", result.bestAllyLocation());
        verify(harness.modifiers, never()).getTotalPowerAtLocation(
                harness.gameState, later, PLAYER,
                false, false);
    }

    @Test
    public void strongPostFlipProtectionLocationCanMove() {
        Harness harness = new Harness();
        PhysicalCard current = location("Protected");
        harness.cardsAtCurrent(current,
                powerCard(PLAYER, "One", 5.0f),
                powerCard(PLAYER, "Two", 5.0f),
                powerCard(PLAYER, "Three", 3.0f));
        harness.locations(current);
        harness.power(current, OPPONENT, 0.0f);
        harness.power(current, PLAYER, 13.0f);

        MoveObjectiveConsolidationPolicy.Evaluation result =
                harness.postFlip(current, title -> true);

        assertEquals(
                MoveObjectiveConsolidationPolicy.Branch
                        .POST_FLIP_STRONG_CAN_MOVE,
                result.branch());
        assertFloat(-30.0f, result.contribution().delta());
        assertEquals(
                "V22.2 POST-FLIP: Strong at protection loc - can move",
                result.contribution().reason());
    }

    @Test
    public void postFlipStayThresholdsRemainStrict() {
        float[] totals = {15.0f, 15.1f, 25.0f, 25.1f};
        float[] expected = {-80.0f, -120.0f, -120.0f, -160.0f};
        for (int i = 0; i < totals.length; i++) {
            Harness harness = new Harness();
            PhysicalCard current = location("Protected");
            harness.cardsAtCurrent(
                    current, powerCard(PLAYER, "Defender", 4.0f));
            harness.locations(current);
            harness.power(current, OPPONENT, totals[i]);
            harness.power(current, PLAYER, 4.0f);

            MoveObjectiveConsolidationPolicy.Evaluation result =
                    harness.postFlip(current, title -> true);

            assertEquals(
                    MoveObjectiveConsolidationPolicy.Branch.POST_FLIP_STAY,
                    result.branch());
            assertFloat(expected[i], result.contribution().delta());
            assertEquals(
                    "V22.2 POST-FLIP: STAY at protection location! Opponent power="
                            + (int) totals[i],
                    result.contribution().reason());
        }
    }

    @Test
    public void postFlipLoneReinforcementThresholdsRemainStrict() {
        float[] deficits = {4.0f, 4.1f, 8.0f, 8.1f};
        float[] expected = {80.0f, 120.0f, 120.0f, 160.0f};
        for (int i = 0; i < deficits.length; i++) {
            Harness harness = new Harness();
            PhysicalCard current = location("Current");
            PhysicalCard protectedSite = location("Protected");
            harness.cardsAtCurrent(
                    current, powerCard(PLAYER, "Solo", 3.0f));
            harness.locations(current, protectedSite);
            harness.power(current, OPPONENT, 0.0f);
            harness.power(protectedSite, OPPONENT, deficits[i]);
            harness.power(protectedSite, PLAYER, 4.0f);

            MoveObjectiveConsolidationPolicy.Evaluation result =
                    harness.postFlip(
                            current, "Protected"::equals);

            assertEquals(
                    MoveObjectiveConsolidationPolicy.Branch
                            .POST_FLIP_LONE_REINFORCE,
                    result.branch());
            assertFloat(expected[i], result.contribution().delta());
            assertTrue(result.contribution().claimDoctrineRank());
            assertEquals(
                    "V22.2 POST-FLIP: Lone char should reinforce Protected",
                    result.contribution().reason());
        }
    }

    @Test
    public void postFlipNonLoneSevereDeficitUsesSixty() {
        Harness harness = new Harness();
        PhysicalCard current = location("Current");
        PhysicalCard protectedSite = location("Protected");
        harness.cardsAtCurrent(current,
                powerCard(PLAYER, "One", 3.0f),
                powerCard(PLAYER, "Two", 3.0f));
        harness.locations(current, protectedSite);
        harness.power(current, OPPONENT, 0.0f);
        harness.power(protectedSite, OPPONENT, 7.0f);
        harness.power(protectedSite, PLAYER, 4.0f);

        MoveObjectiveConsolidationPolicy.Evaluation result =
                harness.postFlip(current, "Protected"::equals);

        assertEquals(
                MoveObjectiveConsolidationPolicy.Branch
                        .POST_FLIP_SEVERE_REINFORCE,
                result.branch());
        assertFloat(60.0f, result.contribution().delta());
        assertFalse(result.contribution().claimDoctrineRank());
    }

    @Test
    public void postFlipFailuresAreObservedInOrderAndKeepPartialFacts() {
        Harness harness = new Harness();
        PhysicalCard current = location("Current");
        PhysicalCard first = location("Protected First");
        PhysicalCard broken = location("Protected Broken");
        PhysicalCard later = location("Protected Later");
        harness.cardsAtCurrent(current, powerCard(PLAYER, "Solo", 2.0f));
        harness.locations(current, first, broken, later);
        harness.power(current, OPPONENT, 2.0f);
        harness.power(first, OPPONENT, 5.0f);
        harness.power(first, PLAYER, 1.0f);
        when(harness.modifiers.getTotalPowerAtLocation(
                harness.gameState, broken, OPPONENT,
                false, false)).thenThrow(new RuntimeException("opponent"));
        when(harness.modifiers.getTotalPowerAtLocation(
                harness.gameState, broken, PLAYER,
                false, false)).thenThrow(new RuntimeException("protection"));
        List<String> events = new ArrayList<>();

        MoveObjectiveConsolidationPolicy.Evaluation result =
                MoveObjectiveConsolidationPolicy.postFlip(
                        harness.gameState, harness.game,
                        current, PLAYER,
                        title -> title.startsWith("Protected"),
                        e -> events.add("opponent:" + e.getMessage()),
                        e -> events.add("protection:" + e.getMessage()));

        assertEquals(
                List.of("opponent:opponent", "protection:protection"),
                events);
        assertFloat(7.0f, result.opponentTotalPower());
        assertFloat(8.0f, result.worstProtectionDeficit());
        assertEquals("Protected First",
                result.weakestProtectionLocation());
        assertFloat(120.0f, result.contribution().delta());
        verify(harness.modifiers, never()).getTotalPowerAtLocation(
                harness.gameState, later, PLAYER,
                false, false);
    }

    private static PhysicalCard location(String title) {
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getTitle()).thenReturn(title);
        return card;
    }

    private static PhysicalCard powerCard(
            String owner, String title, float power) {
        return powerCard(
                owner, title, power, CardCategory.CHARACTER);
    }

    private static PhysicalCard powerCard(
            String owner, String title, float power,
            CardCategory category) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getOwner()).thenReturn(owner);
        when(card.getTitle()).thenReturn(title);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(blueprint.hasPowerAttribute()).thenReturn(true);
        when(blueprint.getPower()).thenReturn(power);
        when(blueprint.getCardCategory())
                .thenReturn(category);
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

        private void cardsAtCurrent(
                PhysicalCard location, PhysicalCard... cards) {
            when(gameState.getCardsAtLocation(location))
                    .thenReturn(List.of(cards));
        }

        private void locations(PhysicalCard... locations) {
            when(gameState.getLocationsInOrder())
                    .thenReturn(List.of(locations));
        }

        private void power(
                PhysicalCard location, String player, float power) {
            when(modifiers.getTotalPowerAtLocation(
                    gameState, location, player,
                    false, false)).thenReturn(power);
        }

        private MoveObjectiveConsolidationPolicy.Evaluation preFlip(
                PhysicalCard current) {
            return MoveObjectiveConsolidationPolicy.preFlip(
                    gameState, game, current, PLAYER);
        }

        private MoveObjectiveConsolidationPolicy.Evaluation postFlip(
                PhysicalCard current,
                java.util.function.Predicate<String> protection) {
            return MoveObjectiveConsolidationPolicy.postFlip(
                    gameState, game, current, PLAYER, protection,
                    ignored -> { }, ignored -> { });
        }
    }
}
