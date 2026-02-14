package com.gempukku.swccgo.async.handler.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gempukku.swccgo.async.HttpProcessingException;
import com.gempukku.swccgo.async.ResponseWriter;
import com.gempukku.swccgo.db.GempSettingDAO;
import com.gempukku.swccgo.game.Player;
import com.gempukku.swccgo.hall.HallServer;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApiAdminRequestHandler extends ApiRequestHandler {
    private final HallServer _hallServer;
    private final GempSettingDAO _gempSettingDAO;

    public ApiAdminRequestHandler(Map<Type, Object> context) {
        super(context);
        _hallServer = extractObject(context, HallServer.class);
        _gempSettingDAO = extractObject(context, GempSettingDAO.class);
    }

    public void handleRequest(String uri, HttpRequest request, Map<Type, Object> context, ResponseWriter responseWriter, String remoteIp) throws Exception {
        if ("/status".equals(uri) && request.method() == HttpMethod.GET) {
            getStatus(request, responseWriter);
        } else if ("/motd".equals(uri) && request.method() == HttpMethod.GET) {
            getMotd(request, responseWriter);
        } else if ("/motd".equals(uri) && request.method() == HttpMethod.POST) {
            setMotd(request, responseWriter);
        } else if ("/shutdown".equals(uri) && request.method() == HttpMethod.POST) {
            setShutdown(request, responseWriter);
        } else if ("/privateGames".equals(uri) && request.method() == HttpMethod.POST) {
            setPrivateGames(request, responseWriter);
        } else if ("/bonusAbilities".equals(uri) && request.method() == HttpMethod.POST) {
            setBonusAbilities(request, responseWriter);
        } else if ("/inGameStats".equals(uri) && request.method() == HttpMethod.POST) {
            setInGameStats(request, responseWriter);
        } else if ("/newAccountRegistration".equals(uri) && request.method() == HttpMethod.POST) {
            setNewAccountRegistration(request, responseWriter);
        } else if ("/purgeInGameStats".equals(uri) && request.method() == HttpMethod.POST) {
            purgeInGameStats(request, responseWriter);
        } else {
            responseWriter.writeError(404);
        }
    }

    private void getStatus(HttpRequest request, ResponseWriter responseWriter) throws Exception {
        requireAdmin(request);
        Map<String, Object> status = new LinkedHashMap<String, Object>();
        status.put("motd", _hallServer.getMOTD());
        status.put("shutdown", _hallServer.isShutdown());
        status.put("privateGamesEnabled", _hallServer.privateGamesAllowed());
        status.put("bonusAbilitiesEnabled", _hallServer.bonusAbilitiesEnabled());
        status.put("inGameStatisticsEnabled", _hallServer.inGameStatisticsEnabled());
        status.put("newAccountRegistrationEnabled", _gempSettingDAO.newAccountRegistrationEnabled());
        responseWriter.writeJsonResponse(JSON.toJSONString(status));
    }

    private void getMotd(HttpRequest request, ResponseWriter responseWriter) throws Exception {
        requireAdmin(request);
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("motd", _hallServer.getMOTD());
        responseWriter.writeJsonResponse(JSON.toJSONString(response));
    }

    private void setMotd(HttpRequest request, ResponseWriter responseWriter) throws Exception {
        requireAdmin(request);
        JSONObject body = readJsonBody(request);
        String motd = body != null ? body.getString("motd") : null;
        if (motd == null)
            throw new HttpProcessingException(400);
        _hallServer.setMOTD(motd);
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("motd", motd);
        responseWriter.writeJsonResponse(JSON.toJSONString(response));
    }

    private void setShutdown(HttpRequest request, ResponseWriter responseWriter) throws Exception {
        requireAdmin(request);
        boolean enabled = readEnabledFlag(request);
        if (enabled)
            _hallServer.setShutdown();
        else
            _hallServer.setOperational();
        writeBooleanResponse(responseWriter, "shutdown", enabled);
    }

    private void setPrivateGames(HttpRequest request, ResponseWriter responseWriter) throws Exception {
        requireAdmin(request);
        boolean enabled = readEnabledFlag(request);
        _hallServer.setPrivateGames(enabled);
        writeBooleanResponse(responseWriter, "privateGamesEnabled", enabled);
    }

    private void setBonusAbilities(HttpRequest request, ResponseWriter responseWriter) throws Exception {
        requireAdmin(request);
        boolean enabled = readEnabledFlag(request);
        _hallServer.setBonusAbilities(enabled);
        writeBooleanResponse(responseWriter, "bonusAbilitiesEnabled", enabled);
    }

    private void setInGameStats(HttpRequest request, ResponseWriter responseWriter) throws Exception {
        requireAdmin(request);
        boolean enabled = readEnabledFlag(request);
        _hallServer.setInGameStatisticsEnabled(enabled);
        writeBooleanResponse(responseWriter, "inGameStatisticsEnabled", enabled);
    }

    private void setNewAccountRegistration(HttpRequest request, ResponseWriter responseWriter) throws Exception {
        requireAdmin(request);
        boolean enabled = readEnabledFlag(request);
        _gempSettingDAO.setNewAccountRegistrationEnabled(enabled);
        writeBooleanResponse(responseWriter, "newAccountRegistrationEnabled", enabled);
    }

    private void purgeInGameStats(HttpRequest request, ResponseWriter responseWriter) throws Exception {
        requireAdmin(request);
        int removed = _hallServer.removeInGameStatisticsListeners();
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("removed", removed);
        responseWriter.writeJsonResponse(JSON.toJSONString(response));
    }

    private void requireAdmin(HttpRequest request) throws HttpProcessingException {
        Player player = getResourceOwnerFromToken(request);
        if (!player.hasType(Player.Type.ADMIN))
            throw new HttpProcessingException(403);
    }

    private boolean readEnabledFlag(HttpRequest request) throws HttpProcessingException {
        JSONObject body = readJsonBody(request);
        if (body == null || !body.containsKey("enabled"))
            throw new HttpProcessingException(400);
        return body.getBooleanValue("enabled");
    }

    private void writeBooleanResponse(ResponseWriter responseWriter, String key, boolean value) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put(key, value);
        responseWriter.writeJsonResponse(JSON.toJSONString(response));
    }
}
