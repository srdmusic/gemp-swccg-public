package com.gempukku.swccgo.async.handler.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gempukku.swccgo.async.HttpProcessingException;
import com.gempukku.swccgo.async.ResponseWriter;
import com.gempukku.swccgo.async.auth.JwtService;
import com.gempukku.swccgo.db.LoginInvalidException;
import com.gempukku.swccgo.db.RegisterNotAllowedException;
import com.gempukku.swccgo.game.Player;
import com.mysql.cj.util.StringUtils;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.util.CharsetUtil;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApiAuthRequestHandler extends ApiRequestHandler {
    public ApiAuthRequestHandler(Map<Type, Object> context) {
        super(context);
    }

    public void handleRequest(String uri, HttpRequest request, Map<Type, Object> context, ResponseWriter responseWriter, String remoteIp) throws Exception {
        if ("/login".equals(uri) && request.method() == HttpMethod.POST) {
            login(request, responseWriter, remoteIp);
        } else if ("/register".equals(uri) && request.method() == HttpMethod.POST) {
            register(request, responseWriter, remoteIp);
        } else {
            responseWriter.writeError(404);
        }
    }

    private void login(HttpRequest request, ResponseWriter responseWriter, String remoteIp) throws Exception {
        String login = null;
        String password = null;
        JSONObject json = readJsonBody(request);
        if (json != null) {
            login = json.getString("login");
            password = json.getString("password");
        }

        if (login == null || password == null) {
            String contentType = request.headers().get(io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE);
            if (contentType != null && (contentType.contains("application/x-www-form-urlencoded")
                    || contentType.contains("multipart/form-data"))) {
                HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(request);
                try {
                    if (login == null)
                        login = getFormParameterSafely(postDecoder, "login");
                    if (password == null)
                        password = getFormParameterSafely(postDecoder, "password");
                } finally {
                    postDecoder.destroy();
                }
            }
        }

        if (login == null || password == null)
            throw new HttpProcessingException(400);

        Player player = _playerDao.loginPlayer(login, password);
        if (player == null)
            throw new HttpProcessingException(401);

        if (StringUtils.isNullOrEmpty(player.getPassword()))
            throw new HttpProcessingException(202);

        if (!player.hasType(Player.Type.UNBANNED)) {
            Date bannedUntil = player.getBannedUntil();
            if (bannedUntil == null)
                throw new HttpProcessingException(403);
            if (bannedUntil.after(new Date()))
                throw new HttpProcessingException(409);
        }

        String token = _jwtService.issueToken(player.getName());
        JwtService.JwtToken jwtToken = _jwtService.verifyToken(token);
        long expiresAt = jwtToken != null ? jwtToken.getExpiresAt() : 0L;

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("token", token);
        response.put("tokenType", "Bearer");
        response.put("expiresAt", expiresAt);
        response.put("user", player.GetUserInfo());

        String payload = JSON.toJSONString(response);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json; charset=UTF-8");
        headers.putAll(logUserReturningHeaders(remoteIp, player.getName()));
        responseWriter.writeByteResponse(payload.getBytes(CharsetUtil.UTF_8), headers);
    }

    private void register(HttpRequest request, ResponseWriter responseWriter, String remoteIp) throws Exception {
        String login = null;
        String password = null;
        JSONObject json = readJsonBody(request);
        if (json != null) {
            login = json.getString("login");
            password = json.getString("password");
        }

        if (login == null || password == null) {
            String contentType = request.headers().get(io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE);
            if (contentType != null && (contentType.contains("application/x-www-form-urlencoded")
                    || contentType.contains("multipart/form-data"))) {
                HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(request);
                try {
                    if (login == null)
                        login = getFormParameterSafely(postDecoder, "login");
                    if (password == null)
                        password = getFormParameterSafely(postDecoder, "password");
                } finally {
                    postDecoder.destroy();
                }
            }
        }

        if (login == null || password == null)
            throw new HttpProcessingException(400);

        try {
            if (!_gempSettingDAO.newAccountRegistrationEnabled()) {
                throw new RegisterNotAllowedException();
            }
            if (!_playerDao.registerPlayer(login, password, remoteIp)) {
                throw new HttpProcessingException(403);
            }
        } catch (LoginInvalidException exp) {
            throw new HttpProcessingException(400);
        } catch (RegisterNotAllowedException exp) {
            throw new HttpProcessingException(405);
        }

        Player player = _playerDao.getPlayer(login);
        if (player == null)
            throw new HttpProcessingException(500);

        String token = _jwtService.issueToken(player.getName());
        JwtService.JwtToken jwtToken = _jwtService.verifyToken(token);
        long expiresAt = jwtToken != null ? jwtToken.getExpiresAt() : 0L;

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("token", token);
        response.put("tokenType", "Bearer");
        response.put("expiresAt", expiresAt);
        response.put("user", player.GetUserInfo());

        String payload = JSON.toJSONString(response);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json; charset=UTF-8");
        headers.putAll(logUserReturningHeaders(remoteIp, player.getName()));
        responseWriter.writeByteResponse(payload.getBytes(CharsetUtil.UTF_8), headers);
    }
}
