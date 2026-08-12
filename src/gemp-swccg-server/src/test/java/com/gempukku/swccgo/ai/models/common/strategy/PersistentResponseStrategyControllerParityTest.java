package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.PersistentResponsePolicy;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.ForceDrainState;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersistentResponseStrategyControllerParityTest {
    private static final String BOT = "bot";
    private static final String OPPONENT = "opponent";

    @Test
    public void bothControllersExposeAndResetTheSamePerGameLedger() {
        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .StrategyController();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .StrategyController();
        MutableGameView game = new MutableGameView();

        completeTurn(rando, chosen, game, 12, 5);
        completeTurn(rando, chosen, game, 13, 4);

        PersistentResponsePolicy.DrainHistory randoThreat = rando
                .getPersistentResponseSnapshot()
                .repeatedThreatAt(117).orElseThrow();
        PersistentResponsePolicy.DrainHistory chosenThreat = chosen
                .getPersistentResponseSnapshot()
                .repeatedThreatAt(117).orElseThrow();
        assertEquals(9, randoThreat.projectedTwoTurnDamage());
        assertEquals(randoThreat, chosenThreat);

        rando.reset();
        chosen.reset();
        assertTrue(rando.getPersistentResponseSnapshot().histories().isEmpty());
        assertTrue(chosen.getPersistentResponseSnapshot().histories().isEmpty());
    }

    private void completeTurn(
            com.gempukku.swccgo.ai.models.rando.strategy.StrategyController rando,
            com.gempukku.swccgo.ai.models.chosenone.strategy.StrategyController chosen,
            MutableGameView game, int turn, int finalPaid) {
        ForceDrainState drain = drain(finalPaid);
        rando.observePersistentResponse(game.set(drain, OPPONENT, turn), BOT);
        chosen.observePersistentResponse(game.set(drain, OPPONENT, turn), BOT);
        game.drain.set(null);
        rando.observePersistentResponse(game.set(null, OPPONENT, turn), BOT);
        chosen.observePersistentResponse(game.set(null, OPPONENT, turn), BOT);
        rando.observePersistentResponse(game.set(null, BOT, turn), BOT);
        chosen.observePersistentResponse(game.set(null, BOT, turn), BOT);
    }

    private ForceDrainState drain(int finalPaid) {
        PhysicalCard location = mock(PhysicalCard.class);
        when(location.getPermanentCardId()).thenReturn(117);
        when(location.getTitle()).thenReturn("Carbonite Chamber");
        ForceDrainState drain = mock(ForceDrainState.class);
        when(drain.getPlayerId()).thenReturn(OPPONENT);
        when(drain.getLocation()).thenReturn(location);
        when(drain.getForcePaid()).thenReturn(finalPaid);
        return drain;
    }

    private static final class MutableGameView {
        private final GameState gameState = mock(GameState.class);
        private final AtomicReference<ForceDrainState> drain =
                new AtomicReference<>();
        private final AtomicReference<String> currentPlayer =
                new AtomicReference<>(BOT);
        private final AtomicInteger opponentTurn = new AtomicInteger();

        private MutableGameView() {
            when(gameState.getOpponent(BOT)).thenReturn(OPPONENT);
            when(gameState.getForceDrainState())
                    .thenAnswer(ignored -> drain.get());
            when(gameState.getCurrentPlayerId())
                    .thenAnswer(ignored -> currentPlayer.get());
            when(gameState.getPlayersLatestTurnNumber(OPPONENT))
                    .thenAnswer(ignored -> opponentTurn.get());
        }

        private GameState set(ForceDrainState activeDrain,
                              String currentPlayerId, int turn) {
            drain.set(activeDrain);
            currentPlayer.set(currentPlayerId);
            opponentTurn.set(turn);
            return gameState;
        }
    }
}
