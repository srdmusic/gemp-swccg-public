package com.gempukku.swccgo.async.handler.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gempukku.swccgo.async.HttpProcessingException;
import com.gempukku.swccgo.async.auth.JwtService;
import com.gempukku.swccgo.async.handler.SwccgoServerRequestHandler;
import com.gempukku.swccgo.game.Player;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

import java.lang.reflect.Type;
import java.util.Map;

public class ApiRequestHandler extends SwccgoServerRequestHandler {
    protected final JwtService _jwtService = JwtService.getInstance();

    public ApiRequestHandler(Map<Type, Object> context) {
        super(context);
    }

    protected Player getResourceOwnerFromToken(HttpRequest request) throws HttpProcessingException {
        String token = getTokenFromRequest(request);
        if (token == null)
            throw new HttpProcessingException(401);

        JwtService.JwtToken jwtToken = _jwtService.verifyToken(token);
        if (jwtToken == null)
            throw new HttpProcessingException(401);

        Player resourceOwner = _playerDao.getPlayer(jwtToken.getSubject());
        if (resourceOwner == null)
            throw new HttpProcessingException(401);

        return resourceOwner;
    }

    protected String getTokenFromRequest(HttpRequest request) {
        String authHeader = request.headers().get(HttpHeaderNames.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer "))
            return authHeader.substring("Bearer ".length()).trim();

        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        return getQueryParameterSafely(decoder, "token");
    }

    protected JSONObject readJsonBody(HttpRequest request) throws HttpProcessingException {
        if (!(request instanceof FullHttpRequest))
            return null;
        String body = ((FullHttpRequest) request).content().toString(CharsetUtil.UTF_8);
        if (body == null || body.trim().isEmpty())
            return null;
        try {
            return JSON.parseObject(body);
        } catch (Exception e) {
            throw new HttpProcessingException(400);
        }
    }
}
