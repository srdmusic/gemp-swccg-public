package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MoveOpportunityPolicyTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";

    @Test
    public void attackWithIconsPreservesScoreReasonAndTarget() {
        PhysicalCard current = location(0, 0);
        PhysicalCard target = location(1, 0);
        GameState gameState = gameState(
                List.of(current, target),
                List.of(List.of(), List.of(powerCard(OPPONENT, 4, false))));

        MoveOpportunityPolicy.AttackAnalysis result =
                MoveOpportunityPolicy.attack(
                        gameState, PLAYER, Side.LIGHT, current, 8);

        assertTrue(result.viable);
        assertTrue(result.hasForcedrainPotential);
        assertSame(target, result.targetLocation);
        assertFloat(92.0f, result.score);
        assertEquals(
                "ATTACK 4 enemies with 8 power (+4 advantage) - deny 1 icon drain!",
                result.reason);
    }

    @Test
    public void attackWithoutIconsPreservesCalculatedAnalysis() {
        PhysicalCard current = location(0, 0);
        PhysicalCard target = location(0, 0);
        GameState gameState = gameState(
                List.of(current, target),
                List.of(List.of(), List.of(powerCard(OPPONENT, 4, false))));

        MoveOpportunityPolicy.AttackAnalysis result =
                MoveOpportunityPolicy.attack(
                        gameState, PLAYER, Side.LIGHT, current, 8);

        assertTrue(result.viable);
        assertFalse(result.hasForcedrainPotential);
        assertSame(target, result.targetLocation);
        assertFloat(77.0f, result.score);
        assertEquals("ATTACK 4 enemies with 8 power (+4 advantage)",
                result.reason);
    }

    @Test
    public void attackPreservesAdvantageBoundary() {
        PhysicalCard current = location(0, 0);
        PhysicalCard target = location(1, 0);
        GameState gameState = gameState(
                List.of(current, target),
                List.of(List.of(), List.of(powerCard(OPPONENT, 4, false))));

        assertNull(MoveOpportunityPolicy.attack(
                gameState, PLAYER, Side.LIGHT, current, 7));
        assertTrue(MoveOpportunityPolicy.attack(
                gameState, PLAYER, Side.LIGHT, current, 8).viable);
    }

    @Test
    public void attackExcludesUndercoverOpponent() {
        PhysicalCard current = location(0, 0);
        PhysicalCard target = location(1, 0);
        GameState gameState = gameState(
                List.of(current, target),
                List.of(List.of(), List.of(powerCard(OPPONENT, 4, true))));

        assertNull(MoveOpportunityPolicy.attack(
                gameState, PLAYER, Side.LIGHT, current, 12));
    }

    @Test
    public void attackKeepsFirstLocationOnEqualScore() {
        PhysicalCard current = location(0, 0);
        PhysicalCard first = location(1, 0);
        PhysicalCard second = location(1, 0);
        GameState gameState = gameState(
                List.of(current, first, second),
                List.of(List.of(),
                        List.of(powerCard(OPPONENT, 4, false)),
                        List.of(powerCard(OPPONENT, 4, false))));

        MoveOpportunityPolicy.AttackAnalysis result =
                MoveOpportunityPolicy.attack(
                        gameState, PLAYER, Side.LIGHT, current, 8);

        assertSame(first, result.targetLocation);
    }

    @Test
    public void attackUsesOppositeSideIcons() {
        PhysicalCard current = location(0, 0);
        PhysicalCard target = location(0, 2);
        GameState gameState = gameState(
                List.of(current, target),
                List.of(List.of(), List.of(powerCard(OPPONENT, 4, false))));

        MoveOpportunityPolicy.AttackAnalysis result =
                MoveOpportunityPolicy.attack(
                        gameState, PLAYER, Side.DARK, current, 8);

        assertFloat(107.0f, result.score);
    }

    @Test
    public void spreadToEmptyLocationPreservesEstablishBoundary() {
        PhysicalCard current = location(0, 0);
        PhysicalCard target = location(1, 0);
        GameState gameState = gameState(
                List.of(current, target),
                List.of(List.of(), List.of(powerCard(PLAYER, 2, false))));

        MoveOpportunityPolicy.SpreadAnalysis result =
                MoveOpportunityPolicy.spread(
                        gameState, PLAYER, Side.LIGHT, current, 10, 0);

        assertTrue(result.viable);
        assertFloat(35.0f, result.score);
        assertEquals(
                "Can establish at empty location - 1 opponent icon(s) = force drain!",
                result.reason);
    }

    @Test
    public void spreadToContestedLocationPreservesMarginBoundary() {
        PhysicalCard current = location(0, 0);
        PhysicalCard target = location(1, 0);
        GameState gameState = gameState(
                List.of(current, target),
                List.of(List.of(), List.of(powerCard(OPPONENT, 2, false))));

        MoveOpportunityPolicy.SpreadAnalysis result =
                MoveOpportunityPolicy.spread(
                        gameState, PLAYER, Side.LIGHT, current, 12, 0);

        assertTrue(result.viable);
        assertFloat(46.0f, result.score);
        assertEquals(
                "Can contest location with 2 enemies - 1 opponent icon(s) = force drain!",
                result.reason);
    }

    @Test
    public void spreadPreservesLowSpareFailure() {
        PhysicalCard current = location(0, 0);
        GameState gameState = gameState(
                List.of(current), List.of(List.of()));

        MoveOpportunityPolicy.SpreadAnalysis result =
                MoveOpportunityPolicy.spread(
                        gameState, PLAYER, Side.LIGHT, current, 7, 0);

        assertFalse(result.viable);
        assertFloat(0.0f, result.score);
        assertEquals("need 6 power to retain control, only have 7",
                result.reason);
    }

    @Test
    public void spreadPreservesNoOpportunityFailure() {
        PhysicalCard current = location(0, 0);
        PhysicalCard target = location(0, 0);
        GameState gameState = gameState(
                List.of(current, target),
                List.of(List.of(), List.of()));

        MoveOpportunityPolicy.SpreadAnalysis result =
                MoveOpportunityPolicy.spread(
                        gameState, PLAYER, Side.LIGHT, current, 10, 0);

        assertFalse(result.viable);
        assertEquals("no good adjacent locations", result.reason);
    }

    @Test
    public void spreadIncludesUndercoverOpponentPower() {
        PhysicalCard current = location(0, 0);
        PhysicalCard target = location(0, 0);
        GameState gameState = gameState(
                List.of(current, target),
                List.of(List.of(), List.of(powerCard(OPPONENT, 2, true))));

        MoveOpportunityPolicy.SpreadAnalysis result =
                MoveOpportunityPolicy.spread(
                        gameState, PLAYER, Side.LIGHT, current, 12, 0);

        assertTrue(result.viable);
        assertEquals("Can contest location with 2 enemies", result.reason);
    }

    @Test
    public void spreadKeepsFirstLocationOnEqualScore() {
        PhysicalCard current = location(0, 0);
        PhysicalCard empty = location(1, 0);
        PhysicalCard contested = location(0, 0);
        GameState gameState = gameState(
                List.of(current, empty, contested),
                List.of(List.of(), List.of(),
                        List.of(powerCard(OPPONENT, 10, false))));

        MoveOpportunityPolicy.SpreadAnalysis result =
                MoveOpportunityPolicy.spread(
                        gameState, PLAYER, Side.LIGHT, current, 20, 0);

        assertFloat(35.0f, result.score);
        assertEquals(
                "Can establish at empty location - 1 opponent icon(s) = force drain!",
                result.reason);
    }

    @Test
    public void attackMatchesLegacyFormulaAcrossBoundaryGrid() {
        int[] sourcePowers = {0, 3, 4, 7, 8, 12};
        int[] friendlyPowers = {0, 2, 6};
        int[] enemyPowers = {0, 2, 4, 8};
        int[] iconCounts = {0, 1, 2};

        for (int sourcePower : sourcePowers) {
            for (int friendlyPower : friendlyPowers) {
                for (int enemyPower : enemyPowers) {
                    for (int icons : iconCounts) {
                        for (boolean undercover : new boolean[]{false, true}) {
                            PhysicalCard current = location(0, 0);
                            PhysicalCard target = location(icons, 0);
                            GameState gameState = gameState(
                                    List.of(current, target),
                                    List.of(List.of(), List.of(
                                            powerCard(PLAYER, friendlyPower, false),
                                            powerCard(OPPONENT, enemyPower, undercover))));

                            MoveOpportunityPolicy.AttackAnalysis actual =
                                    MoveOpportunityPolicy.attack(
                                            gameState, PLAYER, Side.LIGHT,
                                            current, sourcePower);
                            float potentialPower = sourcePower + friendlyPower;
                            boolean viable = !undercover
                                    && enemyPower > 0
                                    && potentialPower - enemyPower >= 4;

                            if (!viable) {
                                assertNull(actual);
                                continue;
                            }

                            float expectedScore = 50.0f;
                            if (potentialPower >= enemyPower * 2) {
                                expectedScore += 25.0f;
                            }
                            expectedScore += icons * 15.0f;
                            expectedScore += enemyPower / 2.0f;
                            String expectedReason = String.format(
                                    "ATTACK %d enemies with %d power (+%d advantage)",
                                    enemyPower, (int) potentialPower,
                                    (int) (potentialPower - enemyPower));
                            if (icons > 0) {
                                expectedReason += " - deny " + icons
                                        + " icon drain!";
                            }

                            assertFloat(expectedScore, actual.score);
                            assertEquals(expectedReason, actual.reason);
                            assertEquals(icons > 0,
                                    actual.hasForcedrainPotential);
                            assertSame(target, actual.targetLocation);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void spreadMatchesLegacyFormulaAcrossBoundaryGrid() {
        int[] sourcePowers = {0, 5, 6, 7, 8, 10, 12, 20};
        int[] sourceEnemyPowers = {0, 2, 8};
        int[] friendlyPowers = {0, 2, 6};
        int[] enemyPowers = {0, 2, 6, 10};
        int[] iconCounts = {0, 1, 2};

        for (int sourcePower : sourcePowers) {
            for (int sourceEnemyPower : sourceEnemyPowers) {
                for (int friendlyPower : friendlyPowers) {
                    for (int enemyPower : enemyPowers) {
                        for (int icons : iconCounts) {
                            PhysicalCard current = location(0, 0);
                            PhysicalCard target = location(icons, 0);
                            GameState gameState = gameState(
                                    List.of(current, target),
                                    List.of(List.of(), List.of(
                                            powerCard(PLAYER, friendlyPower, false),
                                            powerCard(OPPONENT, enemyPower, true))));

                            MoveOpportunityPolicy.SpreadAnalysis actual =
                                    MoveOpportunityPolicy.spread(
                                            gameState, PLAYER, Side.LIGHT, current,
                                            sourcePower, sourceEnemyPower);
                            float retain = Math.max(sourceEnemyPower + 4, 6);
                            float spare = sourcePower - retain;

                            if (spare < 2) {
                                assertFalse(actual.viable);
                                assertEquals(String.format(
                                                "need %d power to retain control, only have %d",
                                                (int) retain, sourcePower),
                                        actual.reason);
                                continue;
                            }

                            float potentialPower = friendlyPower + spare;
                            boolean establishedEmpty = friendlyPower >= 6
                                    && enemyPower == 0;
                            boolean emptyViable = enemyPower == 0
                                    && !establishedEmpty
                                    && potentialPower >= 6;
                            boolean contestViable = enemyPower > 0
                                    && potentialPower >= enemyPower + 4;

                            if (emptyViable) {
                                assertTrue(actual.viable);
                                assertFloat(20.0f + icons * 15.0f,
                                        actual.score);
                                String reason = "Can establish at empty location";
                                if (icons > 0) {
                                    reason += " - " + icons
                                            + " opponent icon(s) = force drain!";
                                }
                                assertEquals(reason, actual.reason);
                            } else if (contestViable) {
                                assertTrue(actual.viable);
                                assertFloat(30.0f + enemyPower / 2.0f
                                                + icons * 15.0f,
                                        actual.score);
                                String reason = "Can contest location with "
                                        + enemyPower + " enemies";
                                if (icons > 0) {
                                    reason += " - " + icons
                                            + " opponent icon(s) = force drain!";
                                }
                                assertEquals(reason, actual.reason);
                            } else {
                                assertFalse(actual.viable);
                                assertEquals("no good adjacent locations",
                                        actual.reason);
                            }
                        }
                    }
                }
            }
        }
    }

    private static GameState gameState(
            List<PhysicalCard> locations,
            List<List<PhysicalCard>> cardsByLocation) {
        GameState gameState = mock(GameState.class);
        when(gameState.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getLocationsInOrder()).thenReturn(locations);
        for (int index = 0; index < locations.size(); index++) {
            when(gameState.getCardsAtLocation(locations.get(index)))
                    .thenReturn(cardsByLocation.get(index));
        }
        return gameState;
    }

    private static PhysicalCard location(int darkIcons, int lightIcons) {
        PhysicalCard location = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(location.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getIconCount(Icon.DARK_FORCE)).thenReturn(darkIcons);
        when(blueprint.getIconCount(Icon.LIGHT_FORCE)).thenReturn(lightIcons);
        return location;
    }

    private static PhysicalCard powerCard(
            String owner, float power, boolean undercover) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getOwner()).thenReturn(owner);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.isUndercover()).thenReturn(undercover);
        when(blueprint.hasPowerAttribute()).thenReturn(true);
        when(blueprint.getPower()).thenReturn(power);
        return card;
    }

    private static void assertFloat(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(actual));
    }
}
