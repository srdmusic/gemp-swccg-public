package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MoveSpyFollowPolicyTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";

    @Test
    public void emptySourceAndOccupiedDestinationMeansFollow() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard destination = location("Destination");
        PhysicalCard spy = spyAt(source);
        harness.locations(destination);
        harness.power(source, 0.0f);
        harness.power(destination, 4.0f);

        MoveSpyFollowPolicy.Evaluation result =
                harness.evaluate(spy, "move to destination");

        assertEquals(MoveSpyFollowPolicy.Branch.FOLLOW, result.branch());
        assertTrue(result.contribution().applies());
        assertTrue(result.contribution().claimDoctrineRank());
        assertSame(destination, result.destination());
        assertFloat(500.0f, result.contribution().delta());
        assertEquals(
                "V53 SPY FOLLOW: Opponent moved away — follow them to keep reducing drain!",
                result.contribution().reason());
    }

    @Test
    public void occupiedSourceAndEmptyDestinationMeansStay() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard destination = location("Destination");
        PhysicalCard spy = spyAt(source);
        harness.locations(destination);
        harness.power(source, 2.0f);
        harness.power(destination, 0.0f);

        MoveSpyFollowPolicy.Evaluation result =
                harness.evaluate(spy, "move to destination");

        assertEquals(MoveSpyFollowPolicy.Branch.STAY, result.branch());
        assertFalse(result.contribution().claimDoctrineRank());
        assertFloat(-300.0f, result.contribution().delta());
        assertEquals(
                "V53 SPY STAY: Opponent is HERE — don't leave, keep reducing their drain!",
                result.contribution().reason());
    }

    @Test
    public void historicalRepositionArmRemainsUnreachable() {
        float[] sourcePower = {0.0f, 0.0f, 1.0f, 1.0f};
        float[] destinationPower = {0.0f, 1.0f, 0.0f, 1.0f};
        MoveSpyFollowPolicy.Branch[] expected = {
                MoveSpyFollowPolicy.Branch.NONE,
                MoveSpyFollowPolicy.Branch.FOLLOW,
                MoveSpyFollowPolicy.Branch.STAY,
                MoveSpyFollowPolicy.Branch.NONE};

        for (int i = 0; i < sourcePower.length; i++) {
            Harness harness = new Harness();
            PhysicalCard source = location("Source");
            PhysicalCard destination = location("Destination");
            PhysicalCard spy = spyAt(source);
            harness.locations(destination);
            harness.power(source, sourcePower[i]);
            harness.power(destination, destinationPower[i]);

            assertEquals(expected[i],
                    harness.evaluate(spy, "destination").branch());
        }
    }

    @Test
    public void nullSourceUsesZeroWithoutPowerRead() {
        Harness harness = new Harness();
        PhysicalCard destination = location("Destination");
        PhysicalCard spy = spyAt(null);
        harness.locations(destination);
        harness.power(destination, 3.0f);

        MoveSpyFollowPolicy.Evaluation result =
                harness.evaluate(spy, "destination");

        assertEquals(MoveSpyFollowPolicy.Branch.FOLLOW, result.branch());
        assertFloat(0.0f, result.opponentPowerAtSource());
    }

    @Test
    public void firstTextualDestinationStopsTheScan() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard first = location("First");
        PhysicalCard second = location("Second");
        PhysicalCard spy = spyAt(source);
        harness.locations(first, second);
        harness.power(source, 0.0f);
        harness.power(first, 0.0f);
        harness.power(second, 5.0f);

        MoveSpyFollowPolicy.Evaluation result = harness.evaluate(
                spy, "move through first toward second");

        assertEquals(MoveSpyFollowPolicy.Branch.NONE, result.branch());
        assertSame(first, result.destination());
        verify(harness.modifiers, never()).getTotalPowerAtLocation(
                harness.gameState, second, OPPONENT,
                false, false);
    }

    @Test
    public void nullTitleIsSkippedBeforeTextMatching() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard untitled = location(null);
        PhysicalCard destination = location("Destination");
        PhysicalCard spy = spyAt(source);
        harness.locations(untitled, destination);
        harness.power(source, 0.0f);
        harness.power(destination, 2.0f);

        MoveSpyFollowPolicy.Evaluation result =
                harness.evaluate(spy, "destination");

        assertEquals(MoveSpyFollowPolicy.Branch.FOLLOW, result.branch());
        assertSame(destination, result.destination());
    }

    @Test(expected = RuntimeException.class)
    public void destinationPowerFailurePropagatesToAdapterCatch() {
        Harness harness = new Harness();
        PhysicalCard source = location("Source");
        PhysicalCard destination = location("Destination");
        PhysicalCard spy = spyAt(source);
        harness.locations(destination);
        harness.power(source, 0.0f);
        when(harness.modifiers.getTotalPowerAtLocation(
                harness.gameState, destination, OPPONENT,
                false, false)).thenThrow(new RuntimeException("injected"));

        harness.evaluate(spy, "destination");
    }

    @Test
    public void breakCoverPreservesOpponentOwnAndUnknownArithmetic() {
        assertFloat(30.0f,
                MoveSpyFollowPolicy.breakCover(true, false, false).delta());
        assertFloat(500.0f,
                MoveSpyFollowPolicy.breakCover(false, true, true).delta());
        assertFloat(-500.0f,
                MoveSpyFollowPolicy.breakCover(false, true, false).delta());
        assertFloat(-30.0f,
                MoveSpyFollowPolicy.breakCover(false, false, true).delta());
        assertFloat(30.0f,
                MoveSpyFollowPolicy.breakCover(true, true, true).delta());
    }

    @Test
    public void spyDilutionRequiresFriendlySpyAndRecognizedNonSpyMover() {
        MoveSpyFollowPolicy.Contribution applies =
                MoveSpyFollowPolicy.dilution(true, false, "Sith Temple");

        assertTrue(applies.applies());
        assertFloat(-1500.0f, applies.delta());
        assertFalse(MoveSpyFollowPolicy.dilution(
                true, true, "Sith Temple").applies());
        assertFalse(MoveSpyFollowPolicy.dilution(
                false, false, "Sith Temple").applies());
    }

    private static PhysicalCard location(String title) {
        PhysicalCard location = mock(PhysicalCard.class);
        when(location.getTitle()).thenReturn(title);
        return location;
    }

    private static PhysicalCard spyAt(PhysicalCard source) {
        PhysicalCard spy = mock(PhysicalCard.class);
        when(spy.getAtLocation()).thenReturn(source);
        return spy;
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
            when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
            when(game.getModifiersQuerying()).thenReturn(modifiers);
        }

        private void locations(PhysicalCard... locations) {
            when(gameState.getTopLocations())
                    .thenReturn(List.of(locations));
        }

        private void power(PhysicalCard location, float power) {
            when(modifiers.getTotalPowerAtLocation(
                    gameState, location, OPPONENT,
                    false, false)).thenReturn(power);
        }

        private MoveSpyFollowPolicy.Evaluation evaluate(
                PhysicalCard spy, String actionLower) {
            return MoveSpyFollowPolicy.evaluate(
                    gameState, game, spy, PLAYER, actionLower);
        }
    }
}
