package com.gempukku.swccgo.async.handler.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gempukku.swccgo.async.ResponseWriter;
import com.gempukku.swccgo.collection.CollectionsManager;
import com.gempukku.swccgo.db.vo.League;
import com.gempukku.swccgo.game.Player;
import com.gempukku.swccgo.game.SwccgFormat;
import com.gempukku.swccgo.game.formats.SwccgoFormatLibrary;
import com.gempukku.swccgo.hall.HallChannelVisitor;
import com.gempukku.swccgo.hall.HallCommunicationChannel;
import com.gempukku.swccgo.hall.HallException;
import com.gempukku.swccgo.hall.HallServer;
import com.gempukku.swccgo.league.LeagueSeriesData;
import com.gempukku.swccgo.league.LeagueService;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiHallRequestHandler extends ApiRequestHandler {
    private final HallServer _hallServer;
    private final CollectionsManager _collectionsManager;
    private final SwccgoFormatLibrary _formatLibrary;
    private final LeagueService _leagueService;

    public ApiHallRequestHandler(Map<Type, Object> context) {
        super(context);
        _hallServer = extractObject(context, HallServer.class);
        _collectionsManager = extractObject(context, CollectionsManager.class);
        _formatLibrary = extractObject(context, SwccgoFormatLibrary.class);
        _leagueService = extractObject(context, LeagueService.class);
    }

    public void handleRequest(String uri, HttpRequest request, Map<Type, Object> context, ResponseWriter responseWriter, String remoteIp) throws Exception {
        if (("".equals(uri) || "/".equals(uri)) && request.method() == HttpMethod.GET) {
            getHallSnapshot(request, responseWriter);
        } else if ("/table".equals(uri) && request.method() == HttpMethod.POST) {
            createTable(request, responseWriter);
        } else if (uri.startsWith("/table/") && request.method() == HttpMethod.POST) {
            handleTableAction(uri, request, responseWriter);
        } else if (uri.startsWith("/queue/") && request.method() == HttpMethod.POST) {
            handleQueueAction(uri, request, responseWriter);
        } else if (uri.startsWith("/tournament/") && request.method() == HttpMethod.POST) {
            handleTournamentAction(uri, request, responseWriter);
        } else {
            responseWriter.writeError(404);
        }
    }

    private void getHallSnapshot(HttpRequest request, ResponseWriter responseWriter) throws Exception {
        Player resourceOwner = getResourceOwnerFromToken(request);
        processLoginReward(resourceOwner.getName());

        HallCommunicationChannel channel = new HallCommunicationChannel(0);
        HallSnapshotVisitor visitor = new HallSnapshotVisitor();
        channel.processCommunicationChannel(_hallServer, resourceOwner, visitor);
        Map<String, Object> snapshot = visitor.toSnapshot();
        snapshot.put("currency", _collectionsManager.getPlayerCollection(resourceOwner, "permanent").getCurrency());
        snapshot.put("privateGamesEnabled", _hallServer.privateGamesAllowed());
        snapshot.put("formats", buildFormats(resourceOwner));

        responseWriter.writeJsonResponse(JSON.toJSONString(snapshot));
    }

    private void handleTableAction(String uri, HttpRequest request, ResponseWriter responseWriter) throws Exception {
        String rest = uri.substring("/table/".length());
        if (rest.endsWith("/join")) {
            String tableId = rest.substring(0, rest.length() - "/join".length());
            joinTable(request, responseWriter, tableId);
        } else if (rest.endsWith("/leave")) {
            String tableId = rest.substring(0, rest.length() - "/leave".length());
            leaveTable(request, responseWriter, tableId);
        } else {
            responseWriter.writeError(404);
        }
    }

    private void handleQueueAction(String uri, HttpRequest request, ResponseWriter responseWriter) throws Exception {
        String rest = uri.substring("/queue/".length());
        if (rest.endsWith("/join")) {
            String queueId = rest.substring(0, rest.length() - "/join".length());
            joinQueue(request, responseWriter, queueId);
        } else if (rest.endsWith("/leave")) {
            String queueId = rest.substring(0, rest.length() - "/leave".length());
            leaveQueue(request, responseWriter, queueId);
        } else {
            responseWriter.writeError(404);
        }
    }

    private void handleTournamentAction(String uri, HttpRequest request, ResponseWriter responseWriter) throws Exception {
        String rest = uri.substring("/tournament/".length());
        if (rest.endsWith("/leave")) {
            String tournamentId = rest.substring(0, rest.length() - "/leave".length());
            dropFromTournament(request, responseWriter, tournamentId);
        } else {
            responseWriter.writeError(404);
        }
    }

    private void createTable(HttpRequest request, ResponseWriter responseWriter) throws Exception {
        Player resourceOwner = getResourceOwnerFromToken(request);
        JSONObject body = requireJsonBody(request);

        String format = body.getString("format");
        String deckName = body.getString("deckName");
        String tableDesc = body.getString("tableDesc");
        boolean sampleDeck = body.getBooleanValue("sampleDeck");
        boolean isPrivate = body.getBooleanValue("isPrivate");
        boolean playVsAi = body.getBooleanValue("playVsAi");
        String aiSkill = body.getString("aiSkill");
        String aiDeckName = body.getString("aiDeckName");

        if (format == null || deckName == null)
            throw new com.gempukku.swccgo.async.HttpProcessingException(400, "Missing format or deckName");

        if (isPrivate && !_hallServer.privateGamesAllowed())
            throw new com.gempukku.swccgo.async.HttpProcessingException(400, "Private games are currently disabled");
        if (isPrivate && (tableDesc == null || tableDesc.isEmpty()))
            throw new com.gempukku.swccgo.async.HttpProcessingException(400, "Private games must include an opponent description");

        Player librarian = sampleDeck ? getLibrarian() : null;
        try {
            _hallServer.createNewTable(format, resourceOwner, deckName, sampleDeck,
                    tableDesc, isPrivate, librarian, playVsAi, aiSkill, aiDeckName);
            writeOk(responseWriter);
        } catch (HallException e) {
            throw new com.gempukku.swccgo.async.HttpProcessingException(400, e.getMessage());
        }
    }

    private void joinTable(HttpRequest request, ResponseWriter responseWriter, String tableId) throws Exception {
        Player resourceOwner = getResourceOwnerFromToken(request);
        JSONObject body = requireJsonBody(request);
        String deckName = body.getString("deckName");
        boolean sampleDeck = body.getBooleanValue("sampleDeck");
        if (deckName == null)
            throw new com.gempukku.swccgo.async.HttpProcessingException(400, "Missing deckName");
        Player librarian = sampleDeck ? getLibrarian() : null;
        try {
            _hallServer.joinTableAsPlayer(tableId, resourceOwner, deckName, sampleDeck, librarian);
            writeOk(responseWriter);
        } catch (HallException e) {
            throw new com.gempukku.swccgo.async.HttpProcessingException(400, e.getMessage());
        }
    }

    private void leaveTable(HttpRequest request, ResponseWriter responseWriter, String tableId) throws Exception {
        Player resourceOwner = getResourceOwnerFromToken(request);
        _hallServer.leaveAwaitingTable(resourceOwner, tableId);
        writeOk(responseWriter);
    }

    private void joinQueue(HttpRequest request, ResponseWriter responseWriter, String queueId) throws Exception {
        Player resourceOwner = getResourceOwnerFromToken(request);
        JSONObject body = requireJsonBody(request);
        String deckName = body.getString("deckName");
        boolean sampleDeck = body.getBooleanValue("sampleDeck");
        if (deckName == null)
            throw new com.gempukku.swccgo.async.HttpProcessingException(400, "Missing deckName");
        Player librarian = sampleDeck ? getLibrarian() : null;
        try {
            _hallServer.joinQueue(queueId, resourceOwner, deckName, sampleDeck, librarian);
            writeOk(responseWriter);
        } catch (HallException e) {
            throw new com.gempukku.swccgo.async.HttpProcessingException(400, e.getMessage());
        }
    }

    private void leaveQueue(HttpRequest request, ResponseWriter responseWriter, String queueId) throws Exception {
        Player resourceOwner = getResourceOwnerFromToken(request);
        _hallServer.leaveQueue(queueId, resourceOwner);
        writeOk(responseWriter);
    }

    private void dropFromTournament(HttpRequest request, ResponseWriter responseWriter, String tournamentId) throws Exception {
        Player resourceOwner = getResourceOwnerFromToken(request);
        _hallServer.dropFromTournament(tournamentId, resourceOwner);
        writeOk(responseWriter);
    }

    private JSONObject requireJsonBody(HttpRequest request) throws com.gempukku.swccgo.async.HttpProcessingException {
        JSONObject body = readJsonBody(request);
        if (body == null)
            throw new com.gempukku.swccgo.async.HttpProcessingException(400, "Missing JSON body");
        return body;
    }

    private void writeOk(ResponseWriter responseWriter) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("status", "ok");
        responseWriter.writeJsonResponse(JSON.toJSONString(response));
    }

    private List<Map<String, String>> buildFormats(Player resourceOwner) {
        List<Map<String, String>> formats = new ArrayList<Map<String, String>>();
        for (Map.Entry<String, SwccgFormat> format : _formatLibrary.getHallFormats().entrySet()) {
            if (format.getValue().isPlaytesting()
                    && !(resourceOwner.hasType(Player.Type.ADMIN)
                    || resourceOwner.hasType(Player.Type.PLAYTESTER))) {
                continue;
            }
            Map<String, String> entry = new LinkedHashMap<String, String>();
            entry.put("type", format.getKey());
            entry.put("name", format.getValue().getName());
            formats.add(entry);
        }
        for (League league : _leagueService.getActiveLeagues()) {
            LeagueSeriesData currentLeagueSeries = _leagueService.getCurrentLeagueSeries(league);
            if (currentLeagueSeries != null && _leagueService.isPlayerInLeague(league, resourceOwner)) {
                Map<String, String> entry = new LinkedHashMap<String, String>();
                entry.put("type", league.getType());
                entry.put("name", league.getName());
                formats.add(entry);
            }
        }
        return formats;
    }

    private static class HallSnapshotVisitor implements HallChannelVisitor {
        private Integer _channelNumber;
        private String _motd;
        private String _serverTime;
        private final List<String> _newPlayerGames = new ArrayList<String>();
        private final List<Map<String, Object>> _tables = new ArrayList<Map<String, Object>>();
        private final List<Map<String, Object>> _tournamentQueues = new ArrayList<Map<String, Object>>();
        private final List<Map<String, Object>> _tournaments = new ArrayList<Map<String, Object>>();

        @Override
        public void channelNumber(int channelNumber) {
            _channelNumber = channelNumber;
        }

        @Override
        public void motdChanged(String motd) {
            _motd = motd;
        }

        @Override
        public void serverTime(String serverTime) {
            _serverTime = serverTime;
        }

        @Override
        public void newPlayerGame(String gameId) {
            _newPlayerGames.add(gameId);
        }

        @Override
        public void addTournamentQueue(String queueId, Map<String, String> props) {
            _tournamentQueues.add(buildEntry(queueId, props));
        }

        @Override
        public void updateTournamentQueue(String queueId, Map<String, String> props) {
            _tournamentQueues.add(buildEntry(queueId, props));
        }

        @Override
        public void removeTournamentQueue(String queueId) {
        }

        @Override
        public void addTournament(String tournamentId, Map<String, String> props) {
            _tournaments.add(buildEntry(tournamentId, props));
        }

        @Override
        public void updateTournament(String tournamentId, Map<String, String> props) {
            _tournaments.add(buildEntry(tournamentId, props));
        }

        @Override
        public void removeTournament(String tournamentId) {
        }

        @Override
        public void addTable(String tableId, Map<String, String> props) {
            _tables.add(buildEntry(tableId, props));
        }

        @Override
        public void updateTable(String tableId, Map<String, String> props) {
            _tables.add(buildEntry(tableId, props));
        }

        @Override
        public void removeTable(String tableId) {
        }

        private Map<String, Object> buildEntry(String id, Map<String, String> props) {
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("id", id);
            entry.put("props", props);
            return entry;
        }

        public Map<String, Object> toSnapshot() {
            Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
            if (_channelNumber != null)
                snapshot.put("channelNumber", _channelNumber);
            if (_motd != null)
                snapshot.put("motd", _motd);
            if (_serverTime != null)
                snapshot.put("serverTime", _serverTime);
            snapshot.put("tables", _tables);
            snapshot.put("tournamentQueues", _tournamentQueues);
            snapshot.put("tournaments", _tournaments);
            snapshot.put("newPlayerGames", _newPlayerGames);
            return snapshot;
        }
    }
}
