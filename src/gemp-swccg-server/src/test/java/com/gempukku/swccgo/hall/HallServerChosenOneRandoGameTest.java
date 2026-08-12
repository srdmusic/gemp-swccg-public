package com.gempukku.swccgo.hall;

import com.gempukku.swccgo.ai.AiRegistry;
import com.gempukku.swccgo.ai.models.chosenone.TheChosenOneAi;
import com.gempukku.swccgo.ai.models.rando.RandoCalAi;
import com.gempukku.swccgo.bot.BotStatsGameResultListener;
import com.gempukku.swccgo.chat.ChatServer;
import com.gempukku.swccgo.chat.ChatRoomMediator;
import com.gempukku.swccgo.collection.CollectionsManager;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.db.BotStatsDAO;
import com.gempukku.swccgo.db.GempSettingDAO;
import com.gempukku.swccgo.db.IpBanDAO;
import com.gempukku.swccgo.db.PlayerDAO;
import com.gempukku.swccgo.game.Player;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.SwccgFormat;
import com.gempukku.swccgo.game.SwccgGameMediator;
import com.gempukku.swccgo.game.SwccgGameParticipant;
import com.gempukku.swccgo.game.SwccgoServer;
import com.gempukku.swccgo.game.formats.SwccgoFormatLibrary;
import com.gempukku.swccgo.league.LeagueService;
import com.gempukku.swccgo.logic.timing.GameResultListener;
import com.gempukku.swccgo.logic.vo.SwccgDeck;
import com.gempukku.swccgo.service.AdminService;
import com.gempukku.swccgo.tournament.PairingMechanismRegistry;
import com.gempukku.swccgo.tournament.TournamentPrizeSchemeRegistry;
import com.gempukku.swccgo.tournament.TournamentService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HallServerChosenOneRandoGameTest {
    private static final String GAME_ID = "bot-game-id";
    private static final String LIGHT_ID = "~The_Chosen_One";
    private static final String DARK_ID = "~Rando_Cal";

    private SwccgoServer swccgoServer;
    private SwccgGameMediator mediator;
    private HallServer hall;
    private Player deckOwner;
    private SwccgCardBlueprintLibrary library;
    private SwccgoFormatLibrary formatLibrary;
    private SwccgFormat format;
    private SwccgDeck lightDeck;
    private SwccgDeck darkDeck;
    private List<GameResultListener> listeners;

    @Before
    public void setUp() throws Exception {
        swccgoServer = mock(SwccgoServer.class);
        mediator = mock(SwccgGameMediator.class);
        listeners = new ArrayList<>();

        ChatServer chatServer = mock(ChatServer.class);
        ChatRoomMediator hallChat = mock(ChatRoomMediator.class);
        LeagueService leagueService = mock(LeagueService.class);
        TournamentService tournamentService = mock(TournamentService.class);
        library = mock(SwccgCardBlueprintLibrary.class);
        formatLibrary = mock(SwccgoFormatLibrary.class);
        CollectionsManager collectionsManager = mock(CollectionsManager.class);
        PlayerDAO playerDao = mock(PlayerDAO.class);
        IpBanDAO ipBanDao = mock(IpBanDAO.class);
        GempSettingDAO settingDao = mock(GempSettingDAO.class);
        BotStatsDAO botStatsDao = mock(BotStatsDAO.class);
        AdminService adminService = mock(AdminService.class);
        TournamentPrizeSchemeRegistry prizeRegistry = mock(TournamentPrizeSchemeRegistry.class);
        PairingMechanismRegistry pairingRegistry = mock(PairingMechanismRegistry.class);
        format = mock(SwccgFormat.class);

        when(chatServer.createChatRoom(anyString(), anyBoolean(), anyInt(), isNull(),
                anyBoolean(), anyBoolean())).thenReturn(hallChat);

        when(settingDao.aiTablesEnabled()).thenReturn(true);
        when(formatLibrary.getHallFormats()).thenReturn(Collections.singletonMap("open", format));
        when(format.getName()).thenReturn("Open");
        when(format.isPlaytesting()).thenReturn(false);

        SwccgCardBlueprint lightBlueprint = mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint darkBlueprint = mock(SwccgCardBlueprint.class);
        when(lightBlueprint.getSide()).thenReturn(Side.LIGHT);
        when(darkBlueprint.getSide()).thenReturn(Side.DARK);
        when(library.getSwccgoCardBlueprint("light-card")).thenReturn(lightBlueprint);
        when(library.getSwccgoCardBlueprint("dark-card")).thenReturn(darkBlueprint);
        when(library.getBaseBlueprintId("light-card")).thenReturn("light-card");
        when(library.getBaseBlueprintId("dark-card")).thenReturn("dark-card");

        lightDeck = new SwccgDeck("Chosen Light");
        lightDeck.addCard("light-card");
        darkDeck = new SwccgDeck("Rando Dark");
        darkDeck.addCard("dark-card");
        deckOwner = mock(Player.class);
        when(deckOwner.getName()).thenReturn("deck-owner");
        when(swccgoServer.getParticipantDeck(deckOwner, "Chosen Light")).thenReturn(lightDeck);
        when(swccgoServer.getParticipantDeck(deckOwner, "Rando Dark")).thenReturn(darkDeck);
        when(swccgoServer.createNewGame(any(), isNull(), isNull(), any(),
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(mediator);
        when(swccgoServer.getGameChatRoom(GAME_ID)).thenReturn(null);
        when(mediator.getGameId()).thenReturn(GAME_ID);
        doAnswer(invocation -> {
            listeners.add(invocation.getArgument(0));
            return null;
        }).when(mediator).addGameResultListener(any(GameResultListener.class));

        hall = new HallServer(swccgoServer, chatServer, leagueService, tournamentService,
                library, formatLibrary, collectionsManager, playerDao, ipBanDao, settingDao,
                botStatsDao, adminService, prizeRegistry, pairingRegistry);
        setField(hall, "_operational", true);
    }

    @After
    public void clearRegistry() {
        AiRegistry.unregisterGame(GAME_ID);
    }

    @Test
    public void exactControllersArePublishedUnlockedAndRecordedBeforeSynchronousStart() throws Exception {
        when(mediator.isFinished()).thenReturn(true);
        doAnswer(invocation -> {
            assertEquals(1, hall.getTablesCount());
            assertTrue(listeners.size() >= 2);
            for (GameResultListener listener : listeners) {
                assertFalse(listener instanceof BotStatsGameResultListener);
            }
            assertTrue(AiRegistry.get(GAME_ID, LIGHT_ID) instanceof TheChosenOneAi);
            assertTrue(AiRegistry.get(GAME_ID, DARK_ID) instanceof RandoCalAi);
            ReentrantReadWriteLock lock = (ReentrantReadWriteLock) field(hall, "_hallDataAccessLock");
            assertTrue("synchronous game must start after releasing the Hall write lock",
                    !lock.isWriteLockedByCurrentThread());
            for (GameResultListener listener : new ArrayList<>(listeners)) {
                listener.gameFinished(LIGHT_ID, "test", Collections.singletonMap(DARK_ID, "test"),
                        "Light", "Dark");
            }
            return null;
        }).when(mediator).startGame();

        String gameId = hall.createChosenOneVsRandoGame("open", "Chosen Light", "Rando Dark", deckOwner);

        assertEquals(GAME_ID, gameId);
        assertEquals(0, hall.getTablesCount());
        assertEquals(null, AiRegistry.get(GAME_ID, LIGHT_ID));
        assertEquals(null, AiRegistry.get(GAME_ID, DARK_ID));

        ArgumentCaptor<SwccgGameParticipant[]> participants = ArgumentCaptor.forClass(SwccgGameParticipant[].class);
        verify(swccgoServer).createNewGame(eq(format), isNull(), isNull(), participants.capture(),
                eq(true), eq(true), eq(true), eq(true), eq(true), eq(false),
                eq(300), eq(60), eq(false), eq(false), eq(false));
        assertEquals(LIGHT_ID, participants.getValue()[0].getPlayerId());
        assertEquals(2, participants.getValue().length);
        assertEquals(Side.LIGHT, participants.getValue()[0].getDeck().getSide(
                (SwccgCardBlueprintLibrary) field(hall, "_library")));
        assertEquals(DARK_ID, participants.getValue()[1].getPlayerId());
        assertEquals(Side.DARK, participants.getValue()[1].getDeck().getSide(
                (SwccgCardBlueprintLibrary) field(hall, "_library")));
        verify(format).validateDeck(lightDeck);
        verify(format).validateDeck(darkDeck);
        verify(mediator, never()).addGameStateListener(anyString(), any());
    }

    @Test
    public void unfinishedSynchronousStartFailsAndCleansPublishedState() throws Exception {
        when(mediator.isFinished()).thenReturn(false);

        HallException failure = assertThrows(HallException.class,
                () -> hall.createChosenOneVsRandoGame("open", "Chosen Light", "Rando Dark", deckOwner));

        assertTrue(failure.getMessage().contains("did not finish"));
        verify(mediator).abortGame(anyString());
        assertEquals(0, hall.getTablesCount());
        assertEquals(null, AiRegistry.get(GAME_ID, LIGHT_ID));
        assertEquals(null, AiRegistry.get(GAME_ID, DARK_ID));
    }

    @Test
    public void startFailureAbortsAndCleansPublishedState() throws Exception {
        doThrow(new IllegalStateException("test start failure")).when(mediator).startGame();

        HallException failure = assertThrows(HallException.class,
                () -> hall.createChosenOneVsRandoGame("open", "Chosen Light", "Rando Dark", deckOwner));

        assertTrue(failure.getMessage().contains("test start failure"));
        verify(mediator).abortGame(anyString());
        assertEquals(0, hall.getTablesCount());
        assertEquals(null, AiRegistry.get(GAME_ID, LIGHT_ID));
        assertEquals(null, AiRegistry.get(GAME_ID, DARK_ID));
    }

    @Test
    public void alreadyAbortedStartFailureIsNotAbortedTwice() throws Exception {
        when(mediator.isFinished()).thenReturn(true);
        doThrow(new IllegalStateException("all-AI path already aborted")).when(mediator).startGame();

        HallException failure = assertThrows(HallException.class,
                () -> hall.createChosenOneVsRandoGame("open", "Chosen Light", "Rando Dark", deckOwner));

        assertTrue(failure.getMessage().contains("already aborted"));
        verify(mediator, never()).abortGame(anyString());
        assertEquals(0, hall.getTablesCount());
        assertEquals(null, AiRegistry.get(GAME_ID, LIGHT_ID));
        assertEquals(null, AiRegistry.get(GAME_ID, DARK_ID));
    }

    @Test
    public void unsupportedFormatIsRejectedBeforeCreatingGame() throws Exception {
        HallServer.BotGameInputException failure = assertThrows(HallServer.BotGameInputException.class,
                () -> hall.createChosenOneVsRandoGame("unsupported", "Chosen Light", "Rando Dark", deckOwner));

        assertTrue(failure.getMessage().contains("not supported"));
        verifyNoGameCreated();
    }

    @Test
    public void missingDeckIsRejectedBeforeCreatingGame() throws Exception {
        HallServer.BotGameInputException failure = assertThrows(HallServer.BotGameInputException.class,
                () -> hall.createChosenOneVsRandoGame("open", "Missing", "Rando Dark", deckOwner));

        assertTrue(failure.getMessage().contains("don't have a deck"));
        verifyNoGameCreated();
    }

    @Test
    public void wrongSideDeckIsRejectedBeforeCreatingGame() throws Exception {
        when(swccgoServer.getParticipantDeck(deckOwner, "Wrong Light")).thenReturn(darkDeck);

        HallServer.BotGameInputException failure = assertThrows(HallServer.BotGameInputException.class,
                () -> hall.createChosenOneVsRandoGame("open", "Wrong Light", "Rando Dark", deckOwner));

        assertTrue(failure.getMessage().contains("not a Light Side deck"));
        verifyNoGameCreated();
    }

    @Test
    public void wrongDarkSideDeckIsRejectedBeforeCreatingGame() throws Exception {
        when(swccgoServer.getParticipantDeck(deckOwner, "Wrong Dark")).thenReturn(lightDeck);

        HallServer.BotGameInputException failure = assertThrows(HallServer.BotGameInputException.class,
                () -> hall.createChosenOneVsRandoGame("open", "Chosen Light", "Wrong Dark", deckOwner));

        assertTrue(failure.getMessage().contains("not a Dark Side deck"));
        verifyNoGameCreated();
    }

    @Test
    public void disabledAiTablesRejectBotGame() throws Exception {
        setField(hall, "_aiTablesEnabled", false);

        HallException failure = assertThrows(HallException.class,
                () -> hall.createChosenOneVsRandoGame("open", "Chosen Light", "Rando Dark", deckOwner));

        assertTrue(failure.getMessage().contains("disabled"));
        verifyNoGameCreated();
    }

    @Test
    public void shutdownRejectsBotGame() throws Exception {
        setField(hall, "_shutdown", true);

        HallException failure = assertThrows(HallException.class,
                () -> hall.createChosenOneVsRandoGame("open", "Chosen Light", "Rando Dark", deckOwner));

        assertTrue(failure.getMessage().contains("shutdown"));
        verifyNoGameCreated();
    }

    @Test
    public void nonOperationalHallRejectsBotGame() throws Exception {
        setField(hall, "_operational", false);

        HallException failure = assertThrows(HallException.class,
                () -> hall.createChosenOneVsRandoGame("open", "Chosen Light", "Rando Dark", deckOwner));

        assertTrue(failure.getMessage().contains("operational"));
        verifyNoGameCreated();
    }

    @Test
    public void awaitingTableRejectsBotGame() throws Exception {
        Map<String, AwaitingTable> awaitingTables = awaitingTables();
        awaitingTables.put("waiting", new AwaitingTable(format, null, null, null, "waiting", false));

        HallException failure = assertThrows(HallException.class,
                () -> hall.createChosenOneVsRandoGame("open", "Chosen Light", "Rando Dark", deckOwner));

        assertTrue(failure.getMessage().contains("awaiting players"));
        verifyNoGameCreated();
    }

    @Test
    public void runningTableRejectsBotGame() throws Exception {
        SwccgGameMediator activeGame = mock(SwccgGameMediator.class);
        when(activeGame.isFinished()).thenReturn(false);
        runningTables().put("active", new RunningTable(activeGame, "Open", null, "active", null, null));

        HallException failure = assertThrows(HallException.class,
                () -> hall.createChosenOneVsRandoGame("open", "Chosen Light", "Rando Dark", deckOwner));

        assertTrue(failure.getMessage().contains("already active"));
        verifyNoGameCreated();
    }

    private void verifyNoGameCreated() {
        verify(swccgoServer, never()).createNewGame(any(), isNull(), isNull(), any(),
                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                anyBoolean(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    @SuppressWarnings("unchecked")
    private Map<String, AwaitingTable> awaitingTables() throws Exception {
        return (Map<String, AwaitingTable>) field(hall, "_awaitingTables");
    }

    @SuppressWarnings("unchecked")
    private Map<String, RunningTable> runningTables() throws Exception {
        return (Map<String, RunningTable>) field(hall, "_runningTables");
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
