package com.gempukku.swccgo.game;

import com.gempukku.swccgo.ai.AiRegistry;
import com.gempukku.swccgo.ai.SwccgAiController;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import com.gempukku.swccgo.logic.timing.DefaultUserFeedback;
import com.gempukku.swccgo.logic.vo.SwccgDeck;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SwccgGameMediatorAllAiDriveTest {
    private static final String GAME_ID = "all-ai-game";
    private static final String LIGHT_ID = "~The_Chosen_One";
    private static final String DARK_ID = "~Rando_Cal";
    private static final String THIRD_ID = "~Unexpected_Third_AI";

    @After
    public void clearRegistry() {
        AiRegistry.unregisterGame(GAME_ID);
    }

    @Test
    public void allAiPathDrivesMoreThanLegacyFiftyDecisionLimitIteratively() throws Exception {
        Fixture fixture = new Fixture(120);
        SwccgAiController lightAi = mock(SwccgAiController.class);
        SwccgAiController darkAi = mock(SwccgAiController.class);
        when(lightAi.decide(anyString(), any(AwaitingDecision.class), any(GameState.class))).thenReturn("");
        when(darkAi.decide(anyString(), any(AwaitingDecision.class), any(GameState.class))).thenReturn("");
        AiRegistry.register(GAME_ID, LIGHT_ID, lightAi);
        AiRegistry.register(GAME_ID, DARK_ID, darkAi);

        fixture.mediator.startGame();

        assertEquals(0, fixture.remaining.get());
        verify(fixture.game, times(120)).carryOutPendingActionsUntilDecisionNeeded();
        verify(lightAi, times(60)).decide(LIGHT_ID, fixture.decision, fixture.gameState);
        verify(darkAi, times(60)).decide(DARK_ID, fixture.decision, fixture.gameState);
        verify(fixture.game, never()).abortGame();
    }

    @Test
    public void mixedHumanAiPathStillStopsAtHumanDecision() throws Exception {
        Fixture fixture = new Fixture(2);
        SwccgAiController lightAi = mock(SwccgAiController.class);
        when(lightAi.decide(anyString(), any(AwaitingDecision.class), any(GameState.class))).thenReturn("");
        AiRegistry.register(GAME_ID, LIGHT_ID, lightAi);

        fixture.mediator.startGame();

        assertEquals("the registered AI advances once, then the human remains pending", 1,
                fixture.remaining.get());
        verify(lightAi).decide(LIGHT_ID, fixture.decision, fixture.gameState);
        verify(fixture.game).carryOutPendingActionsUntilDecisionNeeded();
        verify(fixture.game, never()).abortGame();
    }

    @Test
    public void iterativePathRequiresExactlyTwoAiParticipants() throws Exception {
        Fixture fixture = new Fixture(51, true);
        SwccgAiController lightAi = mock(SwccgAiController.class);
        SwccgAiController darkAi = mock(SwccgAiController.class);
        SwccgAiController thirdAi = mock(SwccgAiController.class);
        when(lightAi.decide(anyString(), any(AwaitingDecision.class), any(GameState.class))).thenReturn("");
        when(darkAi.decide(anyString(), any(AwaitingDecision.class), any(GameState.class))).thenReturn("");
        AiRegistry.register(GAME_ID, LIGHT_ID, lightAi);
        AiRegistry.register(GAME_ID, DARK_ID, darkAi);
        AiRegistry.register(GAME_ID, THIRD_ID, thirdAi);

        fixture.mediator.startGame();

        assertEquals("a non-two-player game must stay on the guarded legacy path", 1,
                fixture.remaining.get());
        verify(fixture.game, times(50)).carryOutPendingActionsUntilDecisionNeeded();
        verify(fixture.game, never()).abortGame();
    }

    @Test
    public void repeatedAwaitingDecisionObjectMayStillRepresentProgress() throws Exception {
        Fixture fixture = new Fixture(2);
        SwccgAiController lightAi = mock(SwccgAiController.class);
        SwccgAiController darkAi = mock(SwccgAiController.class);
        when(lightAi.decide(anyString(), any(AwaitingDecision.class), any(GameState.class))).thenReturn("");
        when(darkAi.decide(anyString(), any(AwaitingDecision.class), any(GameState.class))).thenReturn("");
        AiRegistry.register(GAME_ID, LIGHT_ID, lightAi);
        AiRegistry.register(GAME_ID, DARK_ID, darkAi);
        doAnswer(invocation -> {
            fixture.remaining.decrementAndGet();
            return null;
        }).when(fixture.game).carryOutPendingActionsUntilDecisionNeeded();

        fixture.mediator.startGame();

        assertEquals(0, fixture.remaining.get());
        verify(lightAi, times(2)).decide(LIGHT_ID, fixture.decision, fixture.gameState);
        verify(fixture.game, never()).abortGame();
    }

    @Test
    public void checkedInvalidAllAiResponseIsVisibleAndAborts() throws Exception {
        Fixture fixture = new Fixture(1);
        SwccgAiController lightAi = mock(SwccgAiController.class);
        SwccgAiController darkAi = mock(SwccgAiController.class);
        when(lightAi.decide(anyString(), any(AwaitingDecision.class), any(GameState.class))).thenReturn("invalid");
        when(darkAi.decide(anyString(), any(AwaitingDecision.class), any(GameState.class))).thenReturn("");
        doThrow(new DecisionResultInvalidException("test rejection"))
                .when(fixture.decision).decisionMade("invalid");
        AiRegistry.register(GAME_ID, LIGHT_ID, lightAi);
        AiRegistry.register(GAME_ID, DARK_ID, darkAi);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                fixture.mediator::startGame);

        assertTrue(failure.getMessage().contains("engine rejected"));
        verify(fixture.gameState).sendMessage(contains("engine rejected"));
        verify(fixture.game).abortGame();
        verify(fixture.game, never()).carryOutPendingActionsUntilDecisionNeeded();
    }

    @Test
    public void runtimeAllAiFailureIsVisibleAndAborts() throws Exception {
        Fixture fixture = new Fixture(1);
        SwccgAiController lightAi = mock(SwccgAiController.class);
        SwccgAiController darkAi = mock(SwccgAiController.class);
        when(lightAi.decide(anyString(), any(AwaitingDecision.class), any(GameState.class)))
                .thenThrow(new IllegalArgumentException("test fault"));
        when(darkAi.decide(anyString(), any(AwaitingDecision.class), any(GameState.class))).thenReturn("");
        AiRegistry.register(GAME_ID, LIGHT_ID, lightAi);
        AiRegistry.register(GAME_ID, DARK_ID, darkAi);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                fixture.mediator::startGame);

        assertTrue(failure.getMessage().contains("runtime failure"));
        verify(fixture.gameState).sendMessage(contains("runtime failure"));
        verify(fixture.game).abortGame();
    }

    @Test
    public void runtimeWhileDiscoveringPendingDecisionIsVisibleAndAborts() throws Exception {
        Fixture fixture = new Fixture(1);
        SwccgAiController lightAi = mock(SwccgAiController.class);
        SwccgAiController darkAi = mock(SwccgAiController.class);
        AiRegistry.register(GAME_ID, LIGHT_ID, lightAi);
        AiRegistry.register(GAME_ID, DARK_ID, darkAi);
        when(fixture.feedback.getUsersPendingDecision())
                .thenThrow(new IllegalStateException("pending lookup fault"));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                fixture.mediator::startGame);

        assertTrue(failure.getMessage().contains("runtime failure"));
        verify(fixture.gameState).sendMessage(contains("runtime failure"));
        verify(fixture.game).abortGame();
    }

    @Test
    public void unfinishedAllAiGameWithoutPendingDecisionIsVisibleAndAborts() throws Exception {
        Fixture fixture = new Fixture(0);
        SwccgAiController lightAi = mock(SwccgAiController.class);
        SwccgAiController darkAi = mock(SwccgAiController.class);
        AiRegistry.register(GAME_ID, LIGHT_ID, lightAi);
        AiRegistry.register(GAME_ID, DARK_ID, darkAi);
        when(fixture.game.isFinished()).thenReturn(false);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                fixture.mediator::startGame);

        assertTrue(failure.getMessage().contains("no AI decision is pending"));
        verify(fixture.gameState).sendMessage(contains("no AI decision is pending"));
        verify(fixture.game).abortGame();
    }

    @Test
    public void vanishedAllAiControllerIsVisibleAndAborts() throws Exception {
        Fixture fixture = new Fixture(1);
        SwccgAiController lightAi = mock(SwccgAiController.class);
        SwccgAiController darkAi = mock(SwccgAiController.class);
        AiRegistry.register(GAME_ID, LIGHT_ID, lightAi);
        AiRegistry.register(GAME_ID, DARK_ID, darkAi);
        when(fixture.feedback.getAwaitingDecision(LIGHT_ID)).thenAnswer(invocation -> {
            AiRegistry.unregisterGame(GAME_ID);
            return fixture.decision;
        });

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                fixture.mediator::startGame);

        assertTrue(failure.getMessage().contains("no registered AI controller"));
        verify(fixture.gameState).sendMessage(contains("no registered AI controller"));
        verify(fixture.game).abortGame();
    }

    @Test
    public void allAiGuardExhaustionIsVisibleAndAbortsInsteadOfReturningPending() throws Exception {
        Fixture fixture = new Fixture(10001);
        SwccgAiController lightAi = mock(SwccgAiController.class);
        SwccgAiController darkAi = mock(SwccgAiController.class);
        when(lightAi.decide(anyString(), any(AwaitingDecision.class), any(GameState.class))).thenReturn("");
        when(darkAi.decide(anyString(), any(AwaitingDecision.class), any(GameState.class))).thenReturn("");
        AiRegistry.register(GAME_ID, LIGHT_ID, lightAi);
        AiRegistry.register(GAME_ID, DARK_ID, darkAi);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                fixture.mediator::startGame);

        assertTrue(failure.getMessage().contains("guard exhausted"));
        assertEquals(1, fixture.remaining.get());
        verify(fixture.gameState).sendMessage(contains("guard exhausted"));
        verify(fixture.game).abortGame();
        verify(fixture.game, times(10000)).carryOutPendingActionsUntilDecisionNeeded();
    }

    @Test
    public void abortFailureIsAttachedToTheVisibleAllAiFailure() throws Exception {
        Fixture fixture = new Fixture(0);
        SwccgAiController lightAi = mock(SwccgAiController.class);
        SwccgAiController darkAi = mock(SwccgAiController.class);
        IllegalStateException abortFailure = new IllegalStateException("abort fault");
        AiRegistry.register(GAME_ID, LIGHT_ID, lightAi);
        AiRegistry.register(GAME_ID, DARK_ID, darkAi);
        when(fixture.game.isFinished()).thenReturn(false);
        doThrow(abortFailure).when(fixture.game).abortGame();

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                fixture.mediator::startGame);

        assertEquals(1, failure.getSuppressed().length);
        assertSame(abortFailure, failure.getSuppressed()[0]);
    }

    private static final class Fixture {
        private final AtomicInteger remaining;
        private final AtomicReference<String> currentPlayer = new AtomicReference<>(LIGHT_ID);
        private final AwaitingDecision decision = mock(AwaitingDecision.class);
        private final GameState gameState = mock(GameState.class);
        private final SwccgGame game = mock(SwccgGame.class);
        private final DefaultUserFeedback feedback = mock(DefaultUserFeedback.class);
        private final SwccgGameMediator mediator;

        private Fixture(int decisions) throws Exception {
            this(decisions, false);
        }

        private Fixture(int decisions, boolean includeThirdParticipant) throws Exception {
            remaining = new AtomicInteger(decisions);
            SwccgDeck lightDeck = new SwccgDeck("Chosen Light");
            SwccgDeck darkDeck = new SwccgDeck("Rando Dark");
            SwccgDeck thirdDeck = new SwccgDeck("Unexpected Third");
            SwccgGameParticipant[] participants = includeThirdParticipant
                    ? new SwccgGameParticipant[]{
                            new SwccgGameParticipant(LIGHT_ID, lightDeck),
                            new SwccgGameParticipant(DARK_ID, darkDeck),
                            new SwccgGameParticipant(THIRD_ID, thirdDeck)
                    }
                    : new SwccgGameParticipant[]{
                            new SwccgGameParticipant(LIGHT_ID, lightDeck),
                            new SwccgGameParticipant(DARK_ID, darkDeck)
                    };
            mediator = new SwccgGameMediator(GAME_ID, mock(SwccgFormat.class), null,
                    participants, mock(SwccgCardBlueprintLibrary.class), 3600,
                    true, true, true, false, 300, false, false);
            setField(mediator, "_swccgoGame", game);
            setField(mediator, "_userFeedback", feedback);

            when(game.getGameState()).thenReturn(gameState);
            when(game.isFinished()).thenAnswer(invocation -> remaining.get() == 0);
            when(feedback.getUsersPendingDecision()).thenAnswer(invocation -> pendingUsers());
            when(feedback.getAwaitingDecision(anyString())).thenAnswer(invocation -> {
                String requested = invocation.getArgument(0);
                return remaining.get() > 0 && currentPlayer.get().equals(requested) ? decision : null;
            });
            doAnswer(invocation -> {
                if (remaining.decrementAndGet() > 0) {
                    currentPlayer.set(currentPlayer.get().equals(LIGHT_ID) ? DARK_ID : LIGHT_ID);
                }
                return null;
            }).when(game).carryOutPendingActionsUntilDecisionNeeded();
        }

        private Set<String> pendingUsers() {
            return remaining.get() > 0
                    ? Collections.singleton(currentPlayer.get())
                    : Collections.emptySet();
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
