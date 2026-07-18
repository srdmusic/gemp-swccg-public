package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BattleDecisionPolicyTest {

    @Test
    public void battleTacticsPreserveBaseScoreAndContributionOrder() {
        BattleDecisionPolicy.Context context = context(
            null, null, List.of("fire", "draw", "ignore"),
            List.of("Fire weapon at unique character", "Draw battle destiny", "Use an interrupt"),
            List.of("", "", ""), new AtomicInteger());

        List<BattleDecisionPolicy.ScoredAction> results = BattleDecisionPolicy.evaluate(context);

        assertEquals(2, results.size());
        assertEquals("fire", results.get(0).actionId());
        assertEquals(100.0f, results.get(0).baseScore(), 0.0f);
        assertEquals(List.of("Fire weapon", "Target character", "Target unique card", "Fire weapons during battle"),
            results.get(0).contributions().stream().map(BattleDecisionPolicy.Contribution::reason).toList());
        assertEquals(List.of(40.0f, 10.0f, 20.0f, 50.0f),
            results.get(0).contributions().stream().map(BattleDecisionPolicy.Contribution::delta).toList());
        assertEquals(List.of("Draw battle destiny"),
            results.get(1).contributions().stream().map(BattleDecisionPolicy.Contribution::reason).toList());
        assertEquals(30.0f, results.get(1).contributions().get(0).delta(), 0.0f);
    }

    @Test
    public void v76PredictorRunsExactlyOncePerCandidate() {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard location = mock(PhysicalCard.class);
        AtomicInteger predictions = new AtomicInteger();

        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getOpponent("bot")).thenReturn("opponent");
        when(gameState.getTopLocations()).thenReturn(List.of(location));
        when(gameState.getCardsAtLocation(location)).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        when(location.getCardId()).thenReturn(7);
        when(location.getTitle()).thenReturn("Test Site");
        when(modifiers.getTotalPowerAtLocation(any(), any(), anyString(), anyBoolean(), anyBoolean()))
            .thenAnswer(invocation -> "bot".equals(invocation.getArgument(2)) ? 10.0f : 8.0f);
        when(modifiers.getTotalAbilityAtLocation(any(), anyString(), any()))
            .thenReturn(4.0f);

        BattleDecisionPolicy.Context context = context(
            gameState, game, List.of("battle"), List.of("Initiate battle"),
            List.of("7"), predictions);

        List<BattleDecisionPolicy.ScoredAction> results = BattleDecisionPolicy.evaluate(context);

        assertEquals(1, results.size());
        assertEquals(1, predictions.get());
        assertFalse(results.get(0).contributions().isEmpty());
    }

    @Test
    public void routingMatchesBattleDecisionContract() {
        AtomicInteger predictions = new AtomicInteger();
        assertTrue(BattleDecisionPolicy.canEvaluate(context(
            null, null, List.of("0"), List.of("Initiate battle"), List.of(""), predictions)));

        BattleDecisionPolicy.Context nonBattle = context(
            null, null, List.of("0"), List.of("Use an interrupt"), List.of(""), predictions);
        assertFalse(BattleDecisionPolicy.canEvaluate(new DelegatingContext(nonBattle) {
            @Override
            public Phase getPhase() {
                return Phase.DEPLOY;
            }

            @Override
            public String getDecisionText() {
                return "Choose an action";
            }
        }));
    }

    private static BattleDecisionPolicy.Context context(
            GameState gameState, SwccgGame game, List<String> actionIds,
            List<String> actionTexts, List<String> cardIds, AtomicInteger predictions) {
        return new BattleDecisionPolicy.Context() {
            @Override public String getDecisionType() { return "CARD_ACTION_CHOICE"; }
            @Override public Phase getPhase() { return Phase.BATTLE; }
            @Override public String getDecisionText() { return "Choose battle action"; }
            @Override public List<String> getActionIds() { return actionIds; }
            @Override public List<String> getActionTexts() { return actionTexts; }
            @Override public List<String> getCardIds() { return cardIds; }
            @Override public GameState getGameState() { return gameState; }
            @Override public SwccgGame getGame() { return game; }
            @Override public String getPlayerId() { return "bot"; }
            @Override public int getReserveDeckSize() { return 20; }
            @Override public int getLifeForce() { return 20; }
            @Override public int getForcePileSize() { return 5; }
            @Override public int getHandSize() { return 5; }
            @Override public ObjectiveAnalyzer getObjectiveAnalyzer() { return null; }
            @Override public float getVaderExpendabilityFactor() { return 0.3f; }
            @Override public int getCriticalLifeForce() { return 6; }
            @Override
            public BattleDecisionPolicy.Prediction predictBattle(
                    int myPower, int myDestinyDraws, int opponentPower, int opponentDestinyDraws) {
                predictions.incrementAndGet();
                return new BattleDecisionPolicy.Prediction(0.75f, 3.0f, 1.0f);
            }
            @Override public Logger getLogger() { return LogManager.getLogger(BattleDecisionPolicyTest.class); }
        };
    }

    private abstract static class DelegatingContext implements BattleDecisionPolicy.Context {
        private final BattleDecisionPolicy.Context delegate;

        private DelegatingContext(BattleDecisionPolicy.Context delegate) {
            this.delegate = delegate;
        }

        @Override public String getDecisionType() { return delegate.getDecisionType(); }
        @Override public Phase getPhase() { return delegate.getPhase(); }
        @Override public String getDecisionText() { return delegate.getDecisionText(); }
        @Override public List<String> getActionIds() { return delegate.getActionIds(); }
        @Override public List<String> getActionTexts() { return delegate.getActionTexts(); }
        @Override public List<String> getCardIds() { return delegate.getCardIds(); }
        @Override public GameState getGameState() { return delegate.getGameState(); }
        @Override public SwccgGame getGame() { return delegate.getGame(); }
        @Override public String getPlayerId() { return delegate.getPlayerId(); }
        @Override public int getReserveDeckSize() { return delegate.getReserveDeckSize(); }
        @Override public int getLifeForce() { return delegate.getLifeForce(); }
        @Override public int getForcePileSize() { return delegate.getForcePileSize(); }
        @Override public int getHandSize() { return delegate.getHandSize(); }
        @Override public ObjectiveAnalyzer getObjectiveAnalyzer() { return delegate.getObjectiveAnalyzer(); }
        @Override public float getVaderExpendabilityFactor() { return delegate.getVaderExpendabilityFactor(); }
        @Override public int getCriticalLifeForce() { return delegate.getCriticalLifeForce(); }
        @Override public BattleDecisionPolicy.Prediction predictBattle(int a, int b, int c, int d) {
            return delegate.predictBattle(a, b, c, d);
        }
        @Override public Logger getLogger() { return delegate.getLogger(); }
    }
}
