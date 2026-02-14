package com.gempukku.swccgo.async.handler.api;

import com.alibaba.fastjson.JSON;
import com.gempukku.swccgo.async.HttpProcessingException;
import com.gempukku.swccgo.async.ResponseWriter;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.Player;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.logic.vo.SwccgDeck;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiDeckRequestHandler extends ApiRequestHandler {
    private final SwccgCardBlueprintLibrary _library;

    public ApiDeckRequestHandler(Map<Type, Object> context) {
        super(context);
        _library = extractObject(context, SwccgCardBlueprintLibrary.class);
    }

    public void handleRequest(String uri, HttpRequest request, Map<Type, Object> context, ResponseWriter responseWriter, String remoteIp) throws Exception {
        if ("/list".equals(uri) && request.method() == HttpMethod.GET) {
            listDecks(request, responseWriter);
        } else if ("/libraryList".equals(uri) && request.method() == HttpMethod.GET) {
            listLibraryDecks(request, responseWriter);
        } else {
            responseWriter.writeError(404);
        }
    }

    private void listDecks(HttpRequest request, ResponseWriter responseWriter) throws Exception {
        Player resourceOwner = getResourceOwnerFromToken(request);
        Map<String, List<String>> decks = getDecksForPlayer(resourceOwner);
        responseWriter.writeJsonResponse(JSON.toJSONString(decks));
    }

    private void listLibraryDecks(HttpRequest request, ResponseWriter responseWriter) throws Exception {
        Player librarian = getLibrarian();
        if (librarian == null)
            throw new HttpProcessingException(401);
        Map<String, List<String>> decks = getDecksForPlayer(librarian);
        responseWriter.writeJsonResponse(JSON.toJSONString(decks));
    }

    private Map<String, List<String>> getDecksForPlayer(Player player) {
        List<String> darkDeckNames = new ArrayList<String>();
        List<String> lightDeckNames = new ArrayList<String>();
        List<String> otherDeckNames = new ArrayList<String>();

        List<String> deckNames = new ArrayList<String>(_deckDao.getPlayerDeckNames(player));
        for (String deckName : deckNames) {
            SwccgDeck deck = _deckDao.getDeckForPlayer(player, deckName);
            if (deck == null)
                continue;
            Side side = deck.getSide(_library);
            if (side == Side.DARK)
                darkDeckNames.add(deckName);
            else if (side == Side.LIGHT)
                lightDeckNames.add(deckName);
            else
                otherDeckNames.add(deckName);
        }

        Collections.sort(darkDeckNames);
        Collections.sort(lightDeckNames);
        Collections.sort(otherDeckNames);

        Map<String, List<String>> response = new LinkedHashMap<String, List<String>>();
        response.put("dark", darkDeckNames);
        response.put("light", lightDeckNames);
        response.put("other", otherDeckNames);
        return response;
    }
}
