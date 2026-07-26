package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.apache.logging.log4j.LogManager;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ObjectiveAnalyzerFlipAgeObservationTest {

    @Test
    public void observedAgeIsMonotonicThenResetsOnFlipbackAndReflip() {
        ObjectiveAnalyzer analyzer =
                new ObjectiveAnalyzer(LogManager.getLogger(
                        ObjectiveAnalyzerFlipAgeObservationTest.class));

        analyzer.observeFlipState(false, 1);
        assertFalse(analyzer.isFlipAgeKnown());
        assertEquals(0, analyzer.getTurnsObservedSinceFlip());

        analyzer.observeFlipState(true, 3);
        assertTrue(analyzer.isFlipAgeKnown());
        assertEquals(0, analyzer.getTurnsObservedSinceFlip());

        analyzer.observeFlipState(true, 4);
        assertEquals(1, analyzer.getTurnsObservedSinceFlip());
        analyzer.observeFlipState(true, 6);
        assertEquals(3, analyzer.getTurnsObservedSinceFlip());

        analyzer.observeFlipState(true, 5);
        assertEquals(3, analyzer.getTurnsObservedSinceFlip());

        analyzer.observeFlipState(false, 7);
        assertFalse(analyzer.isFlipAgeKnown());
        assertEquals(0, analyzer.getTurnsObservedSinceFlip());

        analyzer.observeFlipState(true, 9);
        assertTrue(analyzer.isFlipAgeKnown());
        assertEquals(0, analyzer.getTurnsObservedSinceFlip());
        analyzer.observeFlipState(true, 10);
        assertEquals(1, analyzer.getTurnsObservedSinceFlip());
    }

    @Test
    public void unknownTurnFailsClosedUntilARealOwnTurnIsObserved() {
        ObjectiveAnalyzer analyzer =
                new ObjectiveAnalyzer(LogManager.getLogger(
                        ObjectiveAnalyzerFlipAgeObservationTest.class));

        analyzer.observeFlipState(true, -1);
        assertTrue(analyzer.isFlipped());
        assertFalse(analyzer.isFlipAgeKnown());
        assertEquals(0, analyzer.getTurnsObservedSinceFlip());

        analyzer.observeFlipState(true, 4);
        assertTrue(analyzer.isFlipAgeKnown());
        assertEquals(0, analyzer.getTurnsObservedSinceFlip());
        analyzer.observeFlipState(true, 5);
        assertEquals(1, analyzer.getTurnsObservedSinceFlip());

        analyzer.reset();
        assertFalse(analyzer.isFlipped());
        assertFalse(analyzer.isFlipAgeKnown());
        assertEquals(0, analyzer.getTurnsObservedSinceFlip());
    }

    @Test
    public void bothFacadesAdvanceOnAnalyzeEarlyOutAndRefreshButReconnectAtZero() {
        SwccgCardBlueprintLibrary cards =
                new SwccgCardBlueprintLibrary();
        SwccgCardBlueprint front =
                cards.getSwccgoCardBlueprint("9_151");
        SwccgCardBlueprint back =
                cards.getSwccgoCardBlueprint("9_151_BACK");
        assertNotNull(front);
        assertNotNull(back);

        PhysicalCard objective = mock(PhysicalCard.class);
        when(objective.getOwner()).thenReturn("dark");
        when(objective.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(objective.getBlueprint()).thenReturn(back);
        when(objective.getOtherSideBlueprint()).thenReturn(front);
        when(objective.getBlueprintId(true)).thenReturn("9_151");
        when(objective.isFlipped()).thenReturn(true);

        AtomicInteger turn = new AtomicInteger(3);
        GameState gameState = mock(GameState.class);
        when(gameState.getAllPermanentCards())
                .thenReturn(List.of(objective));
        when(gameState.getPlayersLatestTurnNumber("dark"))
                .thenAnswer(invocation -> turn.get());
        SwccgGame game = mock(SwccgGame.class);
        when(game.getGameState()).thenReturn(gameState);

        for (ObjectiveAnalyzer analyzer : List.of(
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer(),
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer())) {
            turn.set(3);
            analyzer.analyze(game, "dark", Side.DARK);
            assertEquals(0, analyzer.getTurnsObservedSinceFlip());

            turn.set(4);
            analyzer.analyze(game, "dark", Side.DARK);
            assertEquals(1, analyzer.getTurnsObservedSinceFlip());

            turn.set(6);
            analyzer.refreshFlipStatus(gameState, "dark");
            assertEquals(3, analyzer.getTurnsObservedSinceFlip());

            ObjectiveAnalyzer reconnected =
                    analyzer instanceof
                        com.gempukku.swccgo.ai.models.rando.strategy
                            .ObjectiveAnalyzer
                    ? new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer()
                    : new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer();
            reconnected.analyze(game, "dark", Side.DARK);
            assertTrue(reconnected.isFlipAgeKnown());
            assertEquals(0,
                    reconnected.getTurnsObservedSinceFlip());
        }
    }
}
