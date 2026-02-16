package com.gempukku.swccgo.async.ws;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gempukku.polling.WaitingRequest;
import com.gempukku.swccgo.PrivateInformationException;
import com.gempukku.swccgo.SubscriptionConflictException;
import com.gempukku.swccgo.SubscriptionExpiredException;
import com.gempukku.swccgo.async.game.GameJsonSerializer;
import com.gempukku.swccgo.async.game.GameJsonVisitor;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.db.PlayerDAO;
import com.gempukku.swccgo.game.Player;
import com.gempukku.swccgo.game.SwccgGameMediator;
import com.gempukku.swccgo.game.SwccgoServer;
import com.gempukku.swccgo.game.state.GameCommunicationChannel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameWebSocketSession implements WebSocketSession {
    private final ChannelHandlerContext _ctx;
    private final SwccgoServer _swccgoServer;
    private final PlayerDAO _playerDao;
    private final Player _viewer;
    private final String _gameId;
    private final String _participantId;
    private final Integer _requestedChannelNumber;
    private final GameJsonSerializer _serializer = new GameJsonSerializer();
    private final Set<Phase> _autoPassDefault = new HashSet<Phase>();
    private final AtomicBoolean _closed = new AtomicBoolean(false);
    private final AtomicBoolean _sending = new AtomicBoolean(false);
    private final Object _sendLock = new Object();
    private final long _tokenExpiresAtMs;

    private SwccgGameMediator _gameMediator;
    private GameCommunicationChannel _gameChannel;
    private int _channelNumber = -1;
    private Player _resourceOwner;
    private ScheduledFuture<?> _tickTask;
    private ScheduledFuture<?> _expiryTimer;
    private final WaitingRequest _waitingRequest = new GameWaitingRequest();

    public GameWebSocketSession(ChannelHandlerContext ctx, SwccgoServer swccgoServer, PlayerDAO playerDao, Player viewer, String gameId, String participantId, Integer channelNumber, long tokenExpiresAt) {
        _ctx = ctx;
        _swccgoServer = swccgoServer;
        _playerDao = playerDao;
        _viewer = viewer;
        _gameId = gameId;
        _participantId = participantId;
        _requestedChannelNumber = channelNumber;
        _tokenExpiresAtMs = tokenExpiresAt > 0 ? tokenExpiresAt * 1000L : 0L;

        _autoPassDefault.add(Phase.ACTIVATE);
        _autoPassDefault.add(Phase.CONTROL);
        _autoPassDefault.add(Phase.DEPLOY);
        _autoPassDefault.add(Phase.BATTLE);
        _autoPassDefault.add(Phase.MOVE);
        _autoPassDefault.add(Phase.DRAW);
    }

    @Override
    public void onOpen() {
        _gameMediator = _swccgoServer.getGameById(_gameId);
        if (_gameMediator == null) {
            sendError("Game not found.", 404);
            _ctx.close();
            return;
        }

        _resourceOwner = resolveResourceOwner();
        if (_resourceOwner == null) {
            sendError("Player not found.", 401);
            _ctx.close();
            return;
        }

        GameJsonVisitor visitor = new GameJsonVisitor();
        boolean needsSignup = _requestedChannelNumber == null;
        if (!needsSignup) {
            try {
                _channelNumber = _requestedChannelNumber.intValue();
                _gameChannel = _gameMediator.getCommunicationChannel(_resourceOwner, _channelNumber);
            } catch (PrivateInformationException exp) {
                sendError("Access denied.", 403);
                _ctx.close();
                return;
            } catch (SubscriptionConflictException exp) {
                sendError("Channel conflict.", 409);
                _ctx.close();
                return;
            } catch (SubscriptionExpiredException exp) {
                needsSignup = true;
            }
        }

        if (needsSignup) {
            try {
                _gameMediator.signupUserForGame(_resourceOwner, visitor);
            } catch (PrivateInformationException exp) {
                sendError("Access denied.", 403);
                _ctx.close();
                return;
            }
            _channelNumber = visitor.getChannelNumber();
            try {
                _gameChannel = _gameMediator.getCommunicationChannel(_resourceOwner, _channelNumber);
            } catch (PrivateInformationException exp) {
                sendError("Access denied.", 403);
                _ctx.close();
                return;
            } catch (SubscriptionConflictException exp) {
                sendError("Channel conflict.", 409);
                _ctx.close();
                return;
            } catch (SubscriptionExpiredException exp) {
                sendError("Subscription expired.", 410);
                _ctx.close();
                return;
            }
        } else {
            _gameMediator.processVisitor(_gameChannel, _channelNumber, _resourceOwner.getName(), visitor);
        }

        sendSnapshot(visitor);
        registerForUpdates();
        startTicker();
        scheduleTokenExpiry();
    }

    @Override
    public void onClose() {
        if (_closed.compareAndSet(false, true)) {
            stopTicker();
            stopExpiryTimer();
            if (_gameChannel != null) {
                _gameChannel.unregisterRequest(_waitingRequest);
            }
        }
    }

    @Override
    public void onTextMessage(String message) {
        JSONObject payload;
        try {
            payload = JSON.parseObject(message);
        } catch (Exception exp) {
            return;
        }
        if (payload == null) {
            return;
        }

        String type = payload.getString("type");
        if (type == null) {
            type = payload.getString("action");
        }
        if (type == null) {
            return;
        }

        switch (type) {
            case "decision":
                handleDecision(payload);
                break;
            case "autopass":
                handleAutoPass(payload);
                break;
            case "concede":
                handleConcede();
                break;
            case "cancel":
                handleCancel();
                break;
            case "extendGameTimer":
                handleExtendGameTimer(payload);
                break;
            case "disableActionTimer":
                handleDisableActionTimer();
                break;
            default:
                break;
        }
    }

    private Player resolveResourceOwner() {
        if (_participantId != null && !_participantId.isEmpty() && _viewer.hasType(Player.Type.ADMIN)) {
            return _playerDao.getPlayer(_participantId);
        }
        return _viewer;
    }

    private void handleDecision(JSONObject payload) {
        Integer decisionId = payload.getInteger("decisionId");
        String decisionValue = payload.getString("decisionValue");
        Integer channelNumber = payload.getInteger("channelNumber");

        if (decisionId == null) {
            sendError("Missing decisionId.", 400);
            return;
        }
        if (decisionValue == null) {
            sendError("Missing decisionValue.", 400);
            return;
        }

        int channel = channelNumber != null ? channelNumber : _channelNumber;
        if (channel < 0) {
            sendError("Missing channelNumber.", 400);
            return;
        }

        Set<Phase> autoPassPhases = resolveAutoPassPhases(payload);
        _gameMediator.setPlayerAutoPassSettings(_resourceOwner.getName(), autoPassPhases);

        try {
            _gameMediator.playerAnswered(_resourceOwner, channel, decisionId, decisionValue);
        } catch (SubscriptionConflictException exp) {
            sendError("Channel conflict.", 409);
            return;
        } catch (SubscriptionExpiredException exp) {
            sendError("Subscription expired.", 410);
            return;
        }

        sendAck("decision");
        requestUpdate();
    }

    private void handleAutoPass(JSONObject payload) {
        Set<Phase> autoPassPhases = resolveAutoPassPhases(payload);
        _gameMediator.setPlayerAutoPassSettings(_resourceOwner.getName(), autoPassPhases);
        sendAck("autopass");
    }

    private void handleConcede() {
        _gameMediator.concede(_resourceOwner);
        sendAck("concede");
    }

    private void handleCancel() {
        _gameMediator.cancel(_resourceOwner);
        sendAck("cancel");
    }

    private void handleExtendGameTimer(JSONObject payload) {
        Integer minutes = payload.getInteger("minutesToExtend");
        if (minutes == null) {
            sendError("Missing minutesToExtend.", 400);
            return;
        }
        _gameMediator.extendGameTimer(_resourceOwner, minutes);
        sendAck("extendGameTimer");
    }

    private void handleDisableActionTimer() {
        _gameMediator.disableActionTimer(_resourceOwner);
        sendAck("disableActionTimer");
    }

    private Set<Phase> resolveAutoPassPhases(JSONObject payload) {
        if (payload == null) {
            return _autoPassDefault;
        }

        Boolean enabled = payload.getBoolean("autoPassEnabled");
        if (enabled != null && !enabled) {
            return Collections.emptySet();
        }

        Object rawPhases = payload.get("autoPassPhases");
        if (rawPhases instanceof Iterable) {
            Set<Phase> phases = new HashSet<Phase>();
            for (Object phaseObj : (Iterable<?>) rawPhases) {
                if (phaseObj == null) {
                    continue;
                }
                try {
                    phases.add(Phase.valueOf(phaseObj.toString()));
                } catch (IllegalArgumentException ignored) {
                    sendError("Invalid autoPassPhases.", 400);
                    return _autoPassDefault;
                }
            }
            return phases;
        }

        return _autoPassDefault;
    }

    private void sendSnapshot(GameJsonVisitor visitor) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("eventType", "gameState");
        payload.put("gameId", _gameId);
        payload.putAll(_serializer.buildPayload(visitor));
        sendEvent("snapshot", payload);
    }

    private void requestUpdate() {
        if (_closed.get() || _gameMediator == null || _gameChannel == null) {
            return;
        }
        if (!_sending.compareAndSet(false, true)) {
            return;
        }
        try {
            _gameChannel.unregisterRequest(_waitingRequest);
            GameJsonVisitor visitor = new GameJsonVisitor();
            _gameMediator.processVisitor(_gameChannel, _channelNumber, _resourceOwner.getName(), visitor);
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("gameId", _gameId);
            payload.putAll(_serializer.buildPayload(visitor));
            sendEvent("update", payload);
        } catch (Exception exp) {
            sendError("Failed to process game update.", 500);
        } finally {
            _sending.set(false);
            registerForUpdates();
        }
    }

    private void registerForUpdates() {
        if (_closed.get() || _gameChannel == null) {
            return;
        }
        if (_gameChannel.registerRequest(_waitingRequest)) {
            requestUpdate();
        }
    }

    private void startTicker() {
        stopTicker();
        // Safety net: poll the game channel periodically so clients still progress
        // if a channel wake-up is missed by infrastructure/proxies.
        _tickTask = _ctx.executor().scheduleAtFixedRate(() -> {
            if (_closed.get()) {
                return;
            }
            requestUpdate();
        }, 2, 2, TimeUnit.SECONDS);
    }

    private void stopTicker() {
        if (_tickTask != null) {
            _tickTask.cancel(false);
            _tickTask = null;
        }
    }

    private void scheduleTokenExpiry() {
        if (_tokenExpiresAtMs <= 0) {
            return;
        }
        long delay = _tokenExpiresAtMs - System.currentTimeMillis();
        if (delay <= 0) {
            closeForTokenExpiry();
            return;
        }
        _expiryTimer = _ctx.executor().schedule(this::closeForTokenExpiry, delay, TimeUnit.MILLISECONDS);
    }

    private void stopExpiryTimer() {
        if (_expiryTimer != null) {
            _expiryTimer.cancel(false);
            _expiryTimer = null;
        }
    }

    private void closeForTokenExpiry() {
        if (_closed.get()) {
            return;
        }
        onClose();
        _ctx.writeAndFlush(new CloseWebSocketFrame(4401, "token expired"))
                .addListener(ChannelFutureListener.CLOSE);
    }

    private void sendAck(String action) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("action", action);
        sendEvent("ack", payload);
    }

    private void sendError(String message, Integer status) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("message", message);
        if (status != null) {
            payload.put("status", status);
        }
        sendEvent("error", payload);
    }

    private void sendEvent(String event, Map<String, Object> payload) {
        Map<String, Object> message = new LinkedHashMap<String, Object>();
        message.put("type", "game");
        message.put("event", event);
        if (_gameId != null) {
            message.put("gameId", _gameId);
        }
        if (_viewer != null) {
            message.put("viewerId", _viewer.getName());
        }
        if (_resourceOwner != null) {
            message.put("recipientId", _resourceOwner.getName());
        }
        if (_channelNumber >= 0) {
            message.put("channelNumber", _channelNumber);
        }
        if (payload != null) {
            message.putAll(payload);
        }
        sendJson(message);
    }

    private void sendJson(Map<String, Object> message) {
        final String json = JSON.toJSONString(message);
        synchronized (_sendLock) {
            _ctx.executor().execute(() -> {
                if (_ctx.channel().isActive()) {
                    _ctx.writeAndFlush(new TextWebSocketFrame(json));
                }
            });
        }
    }

    private class GameWaitingRequest implements WaitingRequest {
        @Override
        public void processRequest() {
            requestUpdate();
        }
    }
}
