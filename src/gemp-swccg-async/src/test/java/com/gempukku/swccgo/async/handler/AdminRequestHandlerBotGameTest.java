package com.gempukku.swccgo.async.handler;

import com.gempukku.swccgo.async.HttpProcessingException;
import com.gempukku.swccgo.async.ResponseWriter;
import com.gempukku.swccgo.db.PlayerDAO;
import com.gempukku.swccgo.game.Player;
import com.gempukku.swccgo.hall.HallException;
import com.gempukku.swccgo.hall.HallServer;
import com.gempukku.swccgo.service.LoggedUserHolder;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AdminRequestHandlerBotGameTest {
    private static final String GAME_ID = "bot-game-id";
    private static final String VALID_FORM = "format=open&lightSkill=CHOSENONE&lightDeck=Chosen+Light"
            + "&darkSkill=RANDO&darkDeck=Rando+Dark&deckOwner=deck-owner";

    private Map<Type, Object> context;
    private PlayerDAO playerDao;
    private HallServer hallServer;
    private LoggedUserHolder loggedUserHolder;
    private ResponseWriter responseWriter;
    private AdminRequestHandler handler;
    private Player deckOwner;
    private String adminSession;
    private String userSession;

    @Before
    public void setUp() {
        context = new HashMap<>();
        playerDao = mock(PlayerDAO.class);
        hallServer = mock(HallServer.class);
        loggedUserHolder = new LoggedUserHolder();
        responseWriter = mock(ResponseWriter.class);
        context.put(PlayerDAO.class, playerDao);
        context.put(HallServer.class, hallServer);
        context.put(LoggedUserHolder.class, loggedUserHolder);

        Player admin = player("admin", "a");
        Player user = player("user", "");
        deckOwner = player("deck-owner", "");
        adminSession = loggedUserHolder.logUser("admin");
        userSession = loggedUserHolder.logUser("user");
        when(playerDao.getPlayer("admin")).thenReturn(admin);
        when(playerDao.getPlayer("user")).thenReturn(user);
        when(playerDao.getPlayer("deck-owner")).thenReturn(deckOwner);
        handler = new AdminRequestHandler(context);
    }

    @Test
    public void missingSessionIsUnauthorized() throws Exception {
        HttpProcessingException failure = assertThrows(HttpProcessingException.class,
                () -> invoke(VALID_FORM, null));

        assertEquals(401, failure.getStatus());
        verify(hallServer, never()).createChosenOneVsRandoGame(
                "open", "Chosen Light", "Rando Dark", deckOwner);
    }

    @Test
    public void nonAdminIsForbidden() throws Exception {
        HttpProcessingException failure = assertThrows(HttpProcessingException.class,
                () -> invoke(VALID_FORM, userSession));

        assertEquals(403, failure.getStatus());
        verify(hallServer, never()).createChosenOneVsRandoGame(
                "open", "Chosen Light", "Rando Dark", deckOwner);
    }

    @Test
    public void missingRequiredParameterIsBadRequest() throws Exception {
        String missingDeckOwner = "format=open&lightSkill=CHOSENONE&lightDeck=Chosen+Light"
                + "&darkSkill=RANDO&darkDeck=Rando+Dark";

        HttpProcessingException failure = assertThrows(HttpProcessingException.class,
                () -> invoke(missingDeckOwner, adminSession));

        assertEquals(400, failure.getStatus());
        verify(hallServer, never()).createChosenOneVsRandoGame(
                "open", "Chosen Light", "Rando Dark", deckOwner);
    }

    @Test
    public void lightSkillMustBeExactChosenOne() throws Exception {
        String wrongSkill = VALID_FORM.replace("lightSkill=CHOSENONE", "lightSkill=RANDO");

        HttpProcessingException failure = assertThrows(HttpProcessingException.class,
                () -> invoke(wrongSkill, adminSession));

        assertEquals(400, failure.getStatus());
        verify(hallServer, never()).createChosenOneVsRandoGame(
                "open", "Chosen Light", "Rando Dark", deckOwner);
    }

    @Test
    public void darkSkillMustBeExactRando() throws Exception {
        String wrongSkill = VALID_FORM.replace("darkSkill=RANDO", "darkSkill=CHOSENONE");

        HttpProcessingException failure = assertThrows(HttpProcessingException.class,
                () -> invoke(wrongSkill, adminSession));

        assertEquals(400, failure.getStatus());
        verify(hallServer, never()).createChosenOneVsRandoGame(
                "open", "Chosen Light", "Rando Dark", deckOwner);
    }

    @Test
    public void missingDeckOwnerIsNotFound() throws Exception {
        when(playerDao.getPlayer("deck-owner")).thenReturn(null);

        HttpProcessingException failure = assertThrows(HttpProcessingException.class,
                () -> invoke(VALID_FORM, adminSession));

        assertEquals(404, failure.getStatus());
        verify(hallServer, never()).createChosenOneVsRandoGame(
                "open", "Chosen Light", "Rando Dark", null);
    }

    @Test
    public void invalidBotGameInputIsBadRequest() throws Exception {
        when(hallServer.createChosenOneVsRandoGame(
                "open", "Chosen Light", "Rando Dark", deckOwner))
                .thenThrow(new HallServer.BotGameInputException("invalid deck"));

        HttpProcessingException failure = assertThrows(HttpProcessingException.class,
                () -> invoke(VALID_FORM, adminSession));

        assertEquals(400, failure.getStatus());
    }

    @Test
    public void operationalConflictIsConflict() throws Exception {
        when(hallServer.createChosenOneVsRandoGame(
                "open", "Chosen Light", "Rando Dark", deckOwner))
                .thenThrow(new HallException("another game is active"));

        HttpProcessingException failure = assertThrows(HttpProcessingException.class,
                () -> invoke(VALID_FORM, adminSession));

        assertEquals(409, failure.getStatus());
    }

    @Test
    public void validAdminRequestStartsOnlyExactMatchup() throws Exception {
        when(hallServer.createChosenOneVsRandoGame(
                "open", "Chosen Light", "Rando Dark", deckOwner)).thenReturn(GAME_ID);

        invoke(VALID_FORM, adminSession);

        verify(hallServer).createChosenOneVsRandoGame(
                "open", "Chosen Light", "Rando Dark", deckOwner);
        verify(responseWriter).writeHtmlResponse("OK gameId=" + GAME_ID);
    }

    @Test
    public void botGameRouteIsPostOnly() throws Exception {
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/admin/botgame");
        try {
            handler.handleRequest("/botgame", request, context, responseWriter, "127.0.0.1");
        } finally {
            request.release();
        }

        verify(responseWriter).writeError(404);
        verify(hallServer, never()).createChosenOneVsRandoGame(
                "open", "Chosen Light", "Rando Dark", deckOwner);
    }

    private void invoke(String form, String sessionId) throws Exception {
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                HttpMethod.POST, "/admin/botgame", Unpooled.copiedBuffer(form, CharsetUtil.UTF_8));
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);
        HttpUtil.setContentLength(request, request.content().readableBytes());
        if (sessionId != null) {
            request.headers().set(HttpHeaderNames.COOKIE, "loggedUser=" + sessionId);
        }
        try {
            handler.handleRequest("/botgame", request, context, responseWriter, "127.0.0.1");
        } finally {
            request.release();
        }
    }

    private static Player player(String name, String type) {
        return new Player(1, name, "password", type, null, (Date) null,
                "127.0.0.1", "127.0.0.1");
    }
}
