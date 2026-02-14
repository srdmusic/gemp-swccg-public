var GempSwccgCommunication = Class.extend({
    url: null,
    apiBase: null,
    failure: null,

    _gameWs: null,
    _gameUpdateCallback: null,
    _gameErrorMap: null,
    _pendingGameUpdate: null,
    _gameChannelNumber: null,
    _gameSnapshotReceived: false,
    _gameWsReconnectTimer: null,
    _gameWsReconnectAttempts: 0,
    _gameWsMaxReconnectAttempts: 6,
    _gameWsBaseDelayMs: 1000,
    _gameWsMaxDelayMs: 15000,
    _gameWsHeartbeatTimer: null,
    _gameWsLastMessageAt: 0,
    _gameWsHeartbeatIntervalMs: 5000,
    _gameWsStaleMs: 20000,

    _chatSockets: null,
    _chatCallbacks: null,
    _chatErrorMaps: null,

    init: function (url, failure) {
        this.url = url;
        this.apiBase = url + "/api";
        this.failure = failure;
        this._chatSockets = {};
        this._chatCallbacks = {};
        this._chatErrorMaps = {};
    },

    getAuthToken: function () {
        try {
            return localStorage.getItem("gemp.jwt");
        } catch (e) {
            return null;
        }
    },

    buildWsBase: function () {
        var protocol = (window.location.protocol === "https:") ? "wss://" : "ws://";
        return protocol + window.location.host + "/gemp-swccg-server/ws";
    },

    errorCheck: function (errorMap) {
        var that = this;
        return function (xhr, status, request) {
            var errorStatus = "" + xhr.status;
            if (errorMap != null && errorMap[errorStatus] != null) {
                errorMap[errorStatus](xhr, status, request);
            }
            else if ("" + xhr.status != "200") {
                that.failure(xhr, status, request);
            }
        };
    },

    startGameSession: function (callback, errorMap) {
        var that = this;
        var gameId = getUrlParam("gameId");
        var participantId = getUrlParam("participantId");
        var token = this.getAuthToken();
        $.ajax({
            type: "GET",
            url: this.apiBase + "/game/" + gameId,
            cache: false,
            dataType: "json",
            data: participantId ? { participantId: participantId } : {},
            xhrFields: { withCredentials: true },
            headers: token ? { Authorization: "Bearer " + token } : {},
            success: function (payload) {
                if (payload && payload.channelNumber != null) {
                    that._gameChannelNumber = payload.channelNumber;
                }
                var xml = that.buildGameXml(payload, "gameState");
                callback(xml);
                that._gameSnapshotReceived = true;
                that.ensureGameSocket();
            },
            error: this.errorCheck(errorMap)
        });
    },

    updateGameState: function (channelNumber, callback, errorMap) {
        this._gameUpdateCallback = callback;
        this._gameErrorMap = errorMap;
        this._gameChannelNumber = channelNumber;
        this.ensureGameSocket();

        if (this._pendingGameUpdate != null) {
            var xml = this.buildGameXml(this._pendingGameUpdate, "update");
            this._pendingGameUpdate = null;
            callback(xml);
        }
    },

    gameDecisionMade: function (decisionId, response, channelNumber, callback, errorMap) {
        var payload = {
            type: "decision",
            decisionId: decisionId,
            decisionValue: response,
            channelNumber: channelNumber
        };
        this.applyAutoPassSettings(payload);
        this.applyParticipantOverride(payload);

        if (this.sendGameMessage(payload, errorMap)) {
            var emptyUpdate = { channelNumber: channelNumber, events: [], clocks: [] };
            callback(this.buildGameXml(emptyUpdate, "update"));
            return;
        }

        var that = this;
        this.postJson(this.apiBase + "/game/" + getUrlParam("gameId") + "/decision", payload,
            function () {
                var emptyUpdate = { channelNumber: channelNumber, events: [], clocks: [] };
                callback(that.buildGameXml(emptyUpdate, "update"));
            },
            errorMap
        );
    },

    getGameCardModifiers: function (cardId, callback, errorMap) {
        var participantId = getUrlParam("participantId");
        var token = this.getAuthToken();
        $.ajax({
            type: "GET",
            url: this.apiBase + "/game/" + getUrlParam("gameId") + "/cardInfo",
            cache: false,
            dataType: "json",
            data: participantId ? { cardId: cardId, participantId: participantId } : { cardId: cardId },
            xhrFields: { withCredentials: true },
            headers: token ? { Authorization: "Bearer " + token } : {},
            success: function (payload) {
                callback(payload.html || "");
            },
            error: this.errorCheck(errorMap)
        });
    },

    concede: function (errorMap) {
        this.postJson(this.apiBase + "/game/" + getUrlParam("gameId") + "/concede", {}, function () {}, errorMap);
    },

    cancel: function (errorMap) {
        this.postJson(this.apiBase + "/game/" + getUrlParam("gameId") + "/cancel", {}, function () {}, errorMap);
    },

    extendGameTimer: function (minutesToExtend, errorMap) {
        this.postJson(this.apiBase + "/game/" + getUrlParam("gameId") + "/extendGameTimer", { minutesToExtend: minutesToExtend }, function () {}, errorMap);
    },

    disableActionTimer: function (errorMap) {
        this.postJson(this.apiBase + "/game/" + getUrlParam("gameId") + "/disableActionTimer", {}, function () {}, errorMap);
    },

    startChat: function (room, callback, errorMap) {
        this._chatCallbacks[room] = callback;
        this._chatErrorMaps[room] = errorMap;
        this.ensureChatSocket(room);
    },

    updateChat: function (room, latestMsgId, callback, errorMap, tryNum, maxTries) {
        this._chatCallbacks[room] = callback;
        this._chatErrorMaps[room] = errorMap;
    },

    sendChatMessage: function (room, messages, errorMap) {
        var socket = this._chatSockets[room];
        if (!socket || socket.readyState !== 1) {
            this.handleChatError(room, "0");
            return;
        }
        socket.send(JSON.stringify({ type: "message", text: messages }));
    },

    getReplay: function (replayId, callback, errorMap) {
        $.ajax({
            type: "GET",
            url: this.url + "/replay/" + replayId,
            cache: false,
            xhrFields: { withCredentials: true },
            success: callback,
            error: this.errorCheck(errorMap),
            dataType: "xml"
        });
    },

    postJson: function (url, payload, callback, errorMap) {
        var token = this.getAuthToken();
        $.ajax({
            type: "POST",
            url: url,
            cache: false,
            data: JSON.stringify(payload || {}),
            contentType: "application/json",
            xhrFields: { withCredentials: true },
            headers: token ? { Authorization: "Bearer " + token } : {},
            success: function () {
                if (callback) {
                    callback();
                }
            },
            error: this.errorCheck(errorMap),
            dataType: "json"
        });
    },

    ensureGameSocket: function () {
        if (this._gameWs && (this._gameWs.readyState === 0 || this._gameWs.readyState === 1)) {
            return;
        }
        var gameId = getUrlParam("gameId");
        if (!gameId) {
            return;
        }

        var wsUrl = this.buildWsBase() + "?channel=game&gameId=" + encodeURIComponent(gameId);
        var token = this.getAuthToken();
        if (token) {
            wsUrl += "&token=" + encodeURIComponent(token);
        }
        var participantId = getUrlParam("participantId");
        if (participantId) {
            wsUrl += "&participantId=" + encodeURIComponent(participantId);
        }
        if (this._gameChannelNumber != null) {
            wsUrl += "&channelNumber=" + encodeURIComponent(this._gameChannelNumber);
        }

        var that = this;
        this._gameWs = new WebSocket(wsUrl);
        this._gameWs.onopen = function () {
            that._gameWsReconnectAttempts = 0;
            that._gameWsLastMessageAt = Date.now();
            that.startGameHeartbeat();
            if (that._gameWsReconnectTimer) {
                clearTimeout(that._gameWsReconnectTimer);
                that._gameWsReconnectTimer = null;
            }
        };
        this._gameWs.onmessage = function (event) {
            that._gameWsLastMessageAt = Date.now();
            that.handleGameMessage(event.data);
        };
        this._gameWs.onerror = function () {
            that.stopGameHeartbeat();
            that.scheduleGameReconnect("0");
        };
        this._gameWs.onclose = function (event) {
            that.stopGameHeartbeat();
            if (that.isAuthClose(event)) {
                that.handleGameError("401");
                return;
            }
            that.scheduleGameReconnect("0");
        };
    },

    scheduleGameReconnect: function (status) {
        if (this._gameWsReconnectAttempts >= this._gameWsMaxReconnectAttempts) {
            this.handleGameError(status || "0");
            return;
        }
        if (this._gameWsReconnectTimer) {
            return;
        }
        if (this._gameWsReconnectAttempts > 0) {
            this._gameChannelNumber = null;
            this._gameSnapshotReceived = false;
            this._pendingGameUpdate = null;
        }
        var exponent = Math.min(this._gameWsReconnectAttempts, 6);
        var delay = Math.min(this._gameWsMaxDelayMs, this._gameWsBaseDelayMs * Math.pow(2, exponent));
        this._gameWsReconnectAttempts += 1;
        var that = this;
        this._gameWsReconnectTimer = setTimeout(function () {
            that._gameWsReconnectTimer = null;
            that.ensureGameSocket();
        }, delay);
    },

    startGameHeartbeat: function () {
        if (this._gameWsHeartbeatTimer) {
            return;
        }
        var that = this;
        this._gameWsHeartbeatTimer = setInterval(function () {
            if (!that._gameWs || that._gameWs.readyState !== 1) {
                return;
            }
            if (!that._gameWsLastMessageAt) {
                that._gameWsLastMessageAt = Date.now();
                return;
            }
            var age = Date.now() - that._gameWsLastMessageAt;
            if (age > that._gameWsStaleMs) {
                try {
                    that._gameWs.close();
                } catch (e) {
                }
            }
        }, this._gameWsHeartbeatIntervalMs);
    },

    stopGameHeartbeat: function () {
        if (this._gameWsHeartbeatTimer) {
            clearInterval(this._gameWsHeartbeatTimer);
            this._gameWsHeartbeatTimer = null;
        }
    },

    isAuthClose: function (event) {
        if (!event) {
            return false;
        }
        var code = event.code;
        if (code === 1008 || code === 4001 || code === 4003 || code === 4401 || code === 4403) {
            return true;
        }
        var reason = (event.reason || "").toLowerCase();
        return reason.indexOf("auth") >= 0 || reason.indexOf("token") >= 0 || reason.indexOf("jwt") >= 0;
    },

    handleGameMessage: function (data) {
        var payload = null;
        try {
            payload = JSON.parse(data);
        } catch (e) {
            return;
        }
        if (!payload || payload.type !== "game") {
            return;
        }
        if (payload.event === "error") {
            this.handleGameError("" + (payload.status || "0"));
            return;
        }
        if (payload.event !== "update" && payload.event !== "snapshot") {
            return;
        }

        if (payload.event === "snapshot") {
            // If we've already applied a snapshot via REST, treat WS snapshot as a delta
            // to avoid losing events that occur between REST snapshot and WS connect.
            var treatAsUpdate = this._gameSnapshotReceived;
            if (payload.channelNumber != null) {
                this._gameChannelNumber = payload.channelNumber;
            }
            var snapshotRoot = treatAsUpdate ? "update" : "gameState";
            if (this._gameUpdateCallback) {
                var snapshotXml = this.buildGameXml(payload, snapshotRoot);
                this._gameUpdateCallback(snapshotXml);
            } else {
                this.queuePendingUpdate(payload);
            }
            if (!treatAsUpdate) {
                this._gameSnapshotReceived = true;
            }
            return;
        }

        var rootName = "update";
        if (this._gameUpdateCallback) {
            var xml = this.buildGameXml(payload, rootName);
            this._gameUpdateCallback(xml);
        } else {
            this.queuePendingUpdate(payload);
        }
    },

    queuePendingUpdate: function (payload) {
        if (!payload) {
            return;
        }
        if (!this._pendingGameUpdate) {
            this._pendingGameUpdate = payload;
            return;
        }

        // Merge events so decisions aren't lost if multiple updates arrive before callbacks are ready.
        if (payload.events && payload.events.length) {
            if (!this._pendingGameUpdate.events) {
                this._pendingGameUpdate.events = [];
            }
            this._pendingGameUpdate.events = this._pendingGameUpdate.events.concat(payload.events);
        }

        if (payload.clocks) {
            this._pendingGameUpdate.clocks = payload.clocks;
        }
        if (payload.channelNumber != null) {
            this._pendingGameUpdate.channelNumber = payload.channelNumber;
        }
    },

    handleGameError: function (status) {
        if (this._gameErrorMap && this._gameErrorMap[status]) {
            this._gameErrorMap[status]();
        } else if (this.failure) {
            this.failure({ status: status }, null, null);
        }
    },

    sendGameMessage: function (payload, errorMap) {
        if (!this._gameWs || this._gameWs.readyState !== 1) {
            return false;
        }
        this._gameWs.send(JSON.stringify(payload));
        return true;
    },

    ensureChatSocket: function (room) {
        if (this._chatSockets[room] && (this._chatSockets[room].readyState === 0 || this._chatSockets[room].readyState === 1)) {
            return;
        }

        var wsUrl = this.buildWsBase() + "?channel=chat&room=" + encodeURIComponent(room);
        var token = this.getAuthToken();
        if (token) {
            wsUrl += "&token=" + encodeURIComponent(token);
        }
        var that = this;
        var socket = new WebSocket(wsUrl);
        this._chatSockets[room] = socket;
        socket.onmessage = function (event) {
            that.handleChatMessage(room, event.data);
        };
        socket.onerror = function () {
            that.handleChatError(room, "0");
        };
        socket.onclose = function () {
            that.handleChatError(room, "0");
        };
    },

    handleChatMessage: function (room, data) {
        var payload = null;
        try {
            payload = JSON.parse(data);
        } catch (e) {
            return;
        }
        if (!payload || payload.type !== "chat") {
            return;
        }
        if (payload.event === "error") {
            this.handleChatError(room, "0");
            return;
        }
        if (payload.event !== "snapshot" && payload.event !== "message") {
            return;
        }

        var messages = [];
        if (payload.messages) {
            messages = payload.messages;
        } else if (payload.message) {
            messages = [payload.message];
        }
        var users = payload.users || [];
        var xml = this.buildChatXml(payload.room || room, messages, users);
        if (this._chatCallbacks[room]) {
            this._chatCallbacks[room](xml);
        }
    },

    handleChatError: function (room, status) {
        var errorMap = this._chatErrorMaps[room];
        if (errorMap && errorMap[status]) {
            errorMap[status]();
        } else if (this.failure) {
            this.failure({ status: status }, null, null);
        }
    },

    applyParticipantOverride: function (payload) {
        var participantId = getUrlParam("participantId");
        if (participantId) {
            payload.participantId = participantId;
        }
    },

    applyAutoPassSettings: function (payload) {
        var autoPass = $.cookie("autoPass");
        if (autoPass === "false") {
            payload.autoPassEnabled = false;
            return;
        }
        var phases = $.cookie("autoPassPhases");
        if (!phases) {
            return;
        }
        var entries = phases.split("0");
        var result = [];
        for (var i = 0; i < entries.length; i++) {
            if (entries[i]) {
                result.push(entries[i]);
            }
        }
        if (result.length > 0) {
            payload.autoPassPhases = result;
        }
    },

    buildGameXml: function (payload, rootName) {
        var doc = document.implementation.createDocument("", "", null);
        var root = doc.createElement(rootName);
        if (payload.channelNumber != null) {
            root.setAttribute("cn", "" + payload.channelNumber);
        }

        var events = payload.events || [];
        for (var i = 0; i < events.length; i++) {
            root.appendChild(this.buildEventXml(doc, events[i]));
        }

        var clocks = payload.clocks || [];
        if (clocks.length > 0) {
            var clocksElem = doc.createElement("clocks");
            for (var j = 0; j < clocks.length; j++) {
                var clock = clocks[j];
                var clockElem = doc.createElement("clock");
                clockElem.setAttribute("participantId", clock.participantId);
                clockElem.appendChild(doc.createTextNode("" + clock.secondsLeft));
                clocksElem.appendChild(clockElem);
            }
            root.appendChild(clocksElem);
        }

        doc.appendChild(root);
        return doc;
    },

    buildEventXml: function (doc, event) {
        var elem = doc.createElement("ge");
        this.setAttr(elem, "type", event.type);
        this.setAttr(elem, "blueprintId", event.blueprintId);
        this.setAttr(elem, "testingText", event.testingText);
        this.setAttr(elem, "backSideTestingText", event.backSideTestingText);
        this.setAttr(elem, "horizontal", event.horizontal);
        this.setAttr(elem, "cardId", event.cardId);
        this.setAttr(elem, "index", event.index);
        this.setAttr(elem, "zoneOwnerId", event.zoneOwnerId);
        this.setAttr(elem, "systemName", event.systemName);
        this.setAttr(elem, "locationIndex", event.locationIndex);
        this.setAttr(elem, "locationIndexes", this.joinValues(event.locationIndexes));
        this.setAttr(elem, "participantId", event.participantId);
        this.setAttr(elem, "allParticipantIds", this.joinValues(event.allParticipantIds));
        this.setAttr(elem, "phase", event.phase);
        this.setAttr(elem, "targetCardId", event.targetCardId);
        this.setAttr(elem, "zone", event.zone);
        this.setAttr(elem, "inverted", event.inverted);
        this.setAttr(elem, "sideways", event.sideways);
        this.setAttr(elem, "frozen", event.frozen);
        this.setAttr(elem, "suspended", event.suspended);
        this.setAttr(elem, "collapsed", event.collapsed);
        this.setAttr(elem, "count", event.count);
        this.setAttr(elem, "destinyText", event.destinyText);
        this.setAttr(elem, "playerAttacking", event.playerAttacking);
        this.setAttr(elem, "playerDefending", event.playerDefending);
        this.setAttr(elem, "otherCardIds", this.joinValues(event.otherCardIds));
        this.setAttr(elem, "otherCardIds2", this.joinValues(event.otherCardIds2));
        this.setAttr(elem, "message", event.message);

        if (event.gameStats) {
            this.applyGameStatsXml(doc, elem, event.gameStats);
        }
        if (event.awaitingDecision) {
            this.applyDecisionXml(doc, elem, event.awaitingDecision);
        }
        return elem;
    },

    applyGameStatsXml: function (doc, elem, stats) {
        this.setAttr(elem, "darkForceGeneration", stats.darkForceGeneration);
        this.setAttr(elem, "lightForceGeneration", stats.lightForceGeneration);
        this.setAttr(elem, "darkBattlePower", stats.darkBattlePower);
        this.setAttr(elem, "lightBattlePower", stats.lightBattlePower);
        this.setAttr(elem, "darkBattleNumDestinyToPower", stats.darkBattleNumDestinyToPower);
        this.setAttr(elem, "lightBattleNumDestinyToPower", stats.lightBattleNumDestinyToPower);
        this.setAttr(elem, "darkBattleNumBattleDestiny", stats.darkBattleNumBattleDestiny);
        this.setAttr(elem, "lightBattleNumBattleDestiny", stats.lightBattleNumBattleDestiny);
        this.setAttr(elem, "darkBattleNumDestinyToAttrition", stats.darkBattleNumDestinyToAttrition);
        this.setAttr(elem, "lightBattleNumDestinyToAttrition", stats.lightBattleNumDestinyToAttrition);
        this.setAttr(elem, "darkBattleDamageRemaining", stats.darkBattleDamageRemaining);
        this.setAttr(elem, "lightBattleDamageRemaining", stats.lightBattleDamageRemaining);
        this.setAttr(elem, "darkBattleAttritionRemaining", stats.darkBattleAttritionRemaining);
        this.setAttr(elem, "lightBattleAttritionRemaining", stats.lightBattleAttritionRemaining);
        this.setAttr(elem, "darkImmuneToRemainingAttrition", stats.darkImmuneToRemainingAttrition);
        this.setAttr(elem, "lightImmuneToRemainingAttrition", stats.lightImmuneToRemainingAttrition);
        this.setAttr(elem, "darkSabaccTotal", stats.darkSabaccTotal);
        this.setAttr(elem, "lightSabaccTotal", stats.lightSabaccTotal);
        this.setAttr(elem, "darkDuelOrLightsaberCombatTotal", stats.darkDuelOrLightsaberCombatTotal);
        this.setAttr(elem, "lightDuelOrLightsaberCombatTotal", stats.lightDuelOrLightsaberCombatTotal);
        this.setAttr(elem, "darkDuelOrLightsaberCombatNumDestiny", stats.darkDuelOrLightsaberCombatNumDestiny);
        this.setAttr(elem, "lightDuelOrLightsaberCombatNumDestiny", stats.lightDuelOrLightsaberCombatNumDestiny);
        this.setAttr(elem, "attackingPowerOrFerocityInAttack", stats.attackingPowerOrFerocityInAttack);
        this.setAttr(elem, "defendingPowerOrFerocityInAttack", stats.defendingPowerOrFerocityInAttack);
        this.setAttr(elem, "attackingNumDestinyInAttack", stats.attackingNumDestinyInAttack);
        this.setAttr(elem, "defendingNumDestinyInAttack", stats.defendingNumDestinyInAttack);
        this.setAttr(elem, "darkRaceTotal", stats.darkRaceTotal);
        this.setAttr(elem, "lightRaceTotal", stats.lightRaceTotal);
        this.setAttr(elem, "darkPoliticsTotal", stats.darkPoliticsTotal);
        this.setAttr(elem, "lightPoliticsTotal", stats.lightPoliticsTotal);

        var playerZones = stats.playerZones || [];
        for (var i = 0; i < playerZones.length; i++) {
            var player = playerZones[i];
            var playerElem = doc.createElement("playerZones");
            playerElem.setAttribute("name", player.name);
            var zones = player.zones || {};
            for (var key in zones) {
                if (zones.hasOwnProperty(key)) {
                    playerElem.setAttribute(key, zones[key]);
                }
            }
            elem.appendChild(playerElem);
        }

        var darkPower = doc.createElement("darkPowerAtLocations");
        var darkEntries = stats.darkPowerAtLocations || [];
        for (var j = 0; j < darkEntries.length; j++) {
            var darkEntry = darkEntries[j];
            darkPower.setAttribute("locationIndex" + darkEntry.locationIndex, darkEntry.value);
        }
        elem.appendChild(darkPower);

        var lightPower = doc.createElement("lightPowerAtLocations");
        var lightEntries = stats.lightPowerAtLocations || [];
        for (var k = 0; k < lightEntries.length; k++) {
            var lightEntry = lightEntries[k];
            lightPower.setAttribute("locationIndex" + lightEntry.locationIndex, lightEntry.value);
        }
        elem.appendChild(lightPower);
    },

    applyDecisionXml: function (doc, elem, decision) {
        this.setAttr(elem, "id", decision.id);
        this.setAttr(elem, "decisionType", decision.decisionType);
        this.setAttr(elem, "text", decision.text);
        var params = decision.parameters || [];
        for (var i = 0; i < params.length; i++) {
            var param = params[i];
            var paramElem = doc.createElement("parameter");
            paramElem.setAttribute("name", param.name);
            paramElem.setAttribute("value", param.value);
            elem.appendChild(paramElem);
        }
    },

    buildChatXml: function (room, messages, users) {
        var doc = document.implementation.createDocument("", "", null);
        var root = doc.createElement("chat");
        root.setAttribute("roomName", room);

        for (var i = 0; i < messages.length; i++) {
            var message = messages[i];
            var msgElem = doc.createElement("message");
            msgElem.setAttribute("msgId", "" + message.id);
            msgElem.setAttribute("from", message.from);
            msgElem.setAttribute("date", "" + message.date);
            msgElem.appendChild(doc.createTextNode(message.text));
            root.appendChild(msgElem);
        }

        for (var j = 0; j < users.length; j++) {
            var userElem = doc.createElement("user");
            userElem.appendChild(doc.createTextNode(users[j]));
            root.appendChild(userElem);
        }

        doc.appendChild(root);
        return doc;
    },

    setAttr: function (elem, name, value) {
        if (value !== undefined && value !== null) {
            elem.setAttribute(name, "" + value);
        }
    },

    joinValues: function (values) {
        if (!values || !values.length) {
            return null;
        }
        return values.join(",");
    }
});
