var GempSwccgCommunication = Class.extend({
    url:null,
    apiBase:null,
    failure:null,
    _gameWs:null,
    _gameUpdateCallback:null,
    _gameErrorMap:null,
    _pendingGameUpdate:null,
    _gameChannelNumber:null,
    _gameSnapshotReceived:null,
    _gameWsReconnectTimer:null,
    _gameWsReconnectAttempts:null,
    _gameWsMaxReconnectAttempts:null,
    _gameWsBaseDelayMs:null,
    _gameWsMaxDelayMs:null,
    _gameWsEnabled:null,
    _gameWsDisabled:null,
    _chatSockets:null,
    _chatCallbacks:null,
    _chatErrorMaps:null,
    _chatReconnectTimers:null,
    _chatReconnectAttempts:null,
    _chatUsers:null,
    _chatWsEnabled:null,
    _hallSocket:null,
    _hallCallback:null,
    _hallErrorMap:null,
    _hallReconnectTimer:null,
    _hallReconnectAttempts:null,
    _hallWsEnabled:null,
    _hallWsDisabled:null,
    _hallPendingUpdates:null,
    _hallState:null,
    _hallChannelNumber:null,
    _hallKnownQueues:null,
    _hallKnownTournaments:null,
    _hallKnownTables:null,

    init:function (url, failure) {
        this.url = url;
        this.apiBase = url + "/api";
        this.failure = failure;
        this._gameWs = null;
        this._gameUpdateCallback = null;
        this._gameErrorMap = null;
        this._pendingGameUpdate = null;
        this._gameChannelNumber = null;
        this._gameSnapshotReceived = false;
        this._gameWsReconnectTimer = null;
        this._gameWsReconnectAttempts = 0;
        this._gameWsMaxReconnectAttempts = 6;
        this._gameWsBaseDelayMs = 1000;
        this._gameWsMaxDelayMs = 15000;
        this._gameWsEnabled = false;
        this._gameWsDisabled = false;
        this._chatSockets = {};
        this._chatCallbacks = {};
        this._chatErrorMaps = {};
        this._chatReconnectTimers = {};
        this._chatReconnectAttempts = {};
        this._chatUsers = {};
        this._chatWsEnabled = {};
        this._hallSocket = null;
        this._hallCallback = null;
        this._hallErrorMap = null;
        this._hallReconnectTimer = null;
        this._hallReconnectAttempts = 0;
        this._hallWsEnabled = false;
        this._hallWsDisabled = false;
        this._hallPendingUpdates = [];
        this._hallState = {};
        this._hallChannelNumber = null;
        this._hallKnownQueues = {};
        this._hallKnownTournaments = {};
        this._hallKnownTables = {};
    },

    errorCheck:function (errorMap) {
        var that = this;
        return function (xhr, status, request) {
            var errorStatus = "" + xhr.status;
            if (errorMap != null && errorMap[errorStatus] != null)
                errorMap[errorStatus](xhr, status, request);
            else if (""+xhr.status != "200")
                that.failure(xhr, status, request);
        };
    },

    logout:function (callback, errorMap) {
        var that = this;
        var authHeaders = this.buildAuthHeaders();
        $.ajax({
            type:"POST",
            url:this.apiBase + "/auth/logout",
            cache:false,
            async:false,
            headers:authHeaders,
            xhrFields:{ withCredentials:true },
            success:function (payload, status, request) {
                that.clearAuthToken();
                if (callback != null)
                    callback(payload, status, request);
            },
            error:function (xhr, status, request) {
                that.clearAuthToken();
                that.errorCheck(errorMap)(xhr, status, request);
            },
            dataType:"json"
        });
    },

    getDelivery:function (callback) {
        $.ajax({
            type:"GET",
            url:this.url + "/delivery",
            cache:false,
            data:{
                participantId:getUrlParam("participantId") },
            success:callback,
            error:null,
            dataType:"xml"
        });
    },

    deliveryCheck:function (callback) {
        var that = this;
        return function (xml, status, request) {
            var delivery = request.getResponseHeader("Delivery-Service-Package");
            if (delivery == "true" && window.deliveryService != null)
                that.getDelivery(window.deliveryService);
            callback(xml);
        };
    },
    
    deliveryCheckStatus:function (callback) {
        var that = this;
        return function (xml, status, request) {
            var delivery = request.getResponseHeader("Delivery-Service-Package");
            if (delivery == "true" && window.deliveryService != null)
                that.getDelivery(window.deliveryService);
            callback(xml, request.status);
        };
    },

    getGameHistory:function (start, count, callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/gameHistory",
            cache:false,
            data:{
                start:start,
                count:count,
                participantId:getUrlParam("participantId") },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },

    getStats:function (startDay, length, callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/stats",
            cache:false,
            data:{
                startDay:startDay,
                length:length,
                participantId:getUrlParam("participantId") },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },

    getPlayerStats:function (callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/playerStats",
            cache:false,
            data:{
                participantId:getUrlParam("participantId") },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },

    getCollectionStats:function (callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/playerCollectionStats",
            cache:false,
            data:{
                participantId:getUrlParam("participantId") },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    getLiveTournaments:function (callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/tournament",
            cache:false,
            data:{
                participantId:getUrlParam("participantId") },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },

    getHistoryTournaments:function (callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/tournament/history",
            cache:false,
            data:{
                participantId:getUrlParam("participantId") },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },

    getTournament:function (tournamentId, callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/tournament/" + tournamentId,
            cache:false,
            data:{
                participantId:getUrlParam("participantId") },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },

    getLeagues:function (callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/league",
            cache:false,
            data:{
                participantId:getUrlParam("participantId") },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },

    getLeague:function (type, callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/league/" + type,
            cache:false,
            data:{
                participantId:getUrlParam("participantId") },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },

    joinLeague:function (code, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/league/" + code,
            cache:false,
            data:{
                participantId:getUrlParam("participantId") },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    getReplay:function (replayId, callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/replay/" + replayId,
            cache:false,
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    startGameSession:function (callback, errorMap) {
        var that = this;
        this._gameErrorMap = errorMap;
        this.startGameSessionHttp(function (xml) {
            that.captureGameChannelFromXml(xml);
            that._gameSnapshotReceived = true;
            callback(xml);
            if (that.supportsWebSockets() && !that._gameWsDisabled) {
                that._gameWsEnabled = true;
                that.ensureGameSocket();
            } else {
                that._gameWsEnabled = false;
            }
        }, errorMap);
    },
    startGameSessionHttp:function (callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/game/" + getUrlParam("gameId"),
            cache:false,
            data:{ participantId:getUrlParam("participantId") },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    updateGameState:function (channelNumber, callback, errorMap) {
        this._gameErrorMap = errorMap;
        if (channelNumber != null && channelNumber !== "")
            this._gameChannelNumber = channelNumber;

        if (this.supportsWebSockets() && !this._gameWsDisabled) {
            this._gameWsEnabled = true;
            this._gameUpdateCallback = callback;
            this.flushPendingGameUpdate();
            this.ensureGameSocket();
            return;
        }

        this._gameWsEnabled = false;
        this.updateGameStateHttp(channelNumber, callback, errorMap);
    },
    updateGameStateHttp:function (channelNumber, callback, errorMap) {
        var that = this;
        $.ajax({
            type:"POST",
            url:this.url + "/game/" + getUrlParam("gameId"),
            cache:false,
            data:{
                channelNumber:channelNumber,
                participantId:getUrlParam("participantId") },
            success:this.deliveryCheck(function (xml) {
                that.captureGameChannelFromXml(xml);
                callback(xml);
            }),
            timeout: 20000,
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    getGameCardModifiers:function (cardId, callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/game/" + getUrlParam("gameId") + "/cardInfo",
            cache:false,
            data:{
                cardId:cardId,
                participantId:getUrlParam("participantId") },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    gameDecisionMade:function (decisionId, response, channelNumber, callback, errorMap) {
        this._gameErrorMap = errorMap;
        if (channelNumber != null && channelNumber !== "")
            this._gameChannelNumber = channelNumber;

        if (this.supportsWebSockets() && !this._gameWsDisabled) {
            this._gameWsEnabled = true;
            this.ensureGameSocket();

            if (this.sendGameDecisionWs(decisionId, response, channelNumber)) {
                callback(this.buildGameXml({
                    channelNumber:channelNumber,
                    events:[],
                    clocks:[]
                }, "update"));
                return;
            }
        }

        this.gameDecisionMadeHttp(decisionId, response, channelNumber, callback, errorMap);
    },
    gameDecisionMadeHttp:function (decisionId, response, channelNumber, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/game/" + getUrlParam("gameId"),
            cache:false,
            data:{
                channelNumber:channelNumber,
                participantId:getUrlParam("participantId"),
                decisionId:decisionId,
                decisionValue:response },
            success:this.deliveryCheck(callback),
            timeout: 20000,
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    captureGameChannelFromXml:function (xml) {
        if (xml == null || xml.documentElement == null)
            return;
        var root = xml.documentElement;
        if (root.tagName == "gameState" || root.tagName == "update") {
            var channelNumber = root.getAttribute("cn");
            if (channelNumber != null && channelNumber !== "")
                this._gameChannelNumber = channelNumber;
        }
    },
    concede:function (errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/game/" + getUrlParam("gameId") + "/concede",
            cache:false,
            data:{
                participantId:getUrlParam("participantId")},
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    cancel:function (errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/game/" + getUrlParam("gameId") + "/cancel",
            cache:false,
            data:{
                participantId:getUrlParam("participantId")},
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    extendGameTimer:function (minutesToExtend, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/game/" + getUrlParam("gameId") + "/extendGameTimer",
            cache:false,
            data:{
                participantId:getUrlParam("participantId"),
                minutesToExtend:minutesToExtend},
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    disableActionTimer:function (errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/game/" + getUrlParam("gameId") + "/disableActionTimer",
            cache:false,
            data:{
                participantId:getUrlParam("participantId")},
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    getDecks:function (callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/deck/list",
            cache:false,
            data:{
                participantId:getUrlParam("participantId")},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    getPrettyDeckLink: function(deckName) {
        var cacheBreaker = Math.round(Math.random()*1000000);
        return this.url + "/deck?deckName="+deckName+"&cacheBreaker=" + cacheBreaker + "&pretty=true";
    },
    getDeck:function (deckName, callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/deck",
            cache:false,
            data:{
                participantId:getUrlParam("participantId"),
                deckName:deckName },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    getLibraryDecks:function (callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/deck/libraryList",
            cache:false,
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    getLibraryDeck:function (deckName, callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/deck/library",
            cache:false,
            data:{
                deckName:deckName },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    getCollectionTypes:function (callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/collection",
            cache:false,
            data:{
                participantId:getUrlParam("participantId")},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    getMerchant:function (filter, ownedCompareSelect, ownedMin, start, count, callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/merchant",
            cache:false,
            data:{
                participantId:getUrlParam("participantId"),
                filter:filter,
                ownedCompareSelect:ownedCompareSelect,
                ownedMin:ownedMin,
                start:start,
                count:count},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    buyItem:function (blueprintId, price, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/merchant/buy",
            cache:false,
            data:{
                participantId:getUrlParam("participantId"),
                blueprintId:blueprintId,
                price:price},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    sellItem:function (blueprintId, price, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/merchant/sell",
            cache:false,
            data:{
                participantId:getUrlParam("participantId"),
                blueprintId:blueprintId,
                price:price},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    sellAll:function (blueprintId, price, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/merchant/sellAll",
            cache:false,
            data:{
                participantId:getUrlParam("participantId"),
                blueprintId:blueprintId,
                price:price},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    tradeInFoil:function (blueprintId, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/merchant/tradeFoil",
            cache:false,
            data:{
                participantId:getUrlParam("participantId"),
                blueprintId:blueprintId},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    getCollection:function (collectionType, filter, start, count, callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/collection/" + collectionType,
            cache:false,
            data:{
                participantId:getUrlParam("participantId"),
                filter:filter,
                start:start,
                count:count},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    openPack:function (collectionType, pack, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/collection/" + collectionType,
            cache:false,
            data:{
                participantId:getUrlParam("participantId"),
                pack:pack},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    openSelectionPack:function (collectionType, pack, selection, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/collection/" + collectionType,
            cache:false,
            data:{
                participantId:getUrlParam("participantId"),
                pack:pack,
                selection:selection},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    saveDeck:function (deckName, contents, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/deck",
            cache:false,
            async:false,
            data:{
                participantId:getUrlParam("participantId"),
                deckName:deckName,
                deckContents:contents},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    renameDeck:function (oldDeckName, deckName, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/deck/rename",
            cache:false,
            data:{
                participantId:getUrlParam("participantId"),
                oldDeckName:oldDeckName,
                deckName:deckName},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    deleteDeck:function (deckName, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/deck/delete",
            cache:false,
            data:{
                participantId:getUrlParam("participantId"),
                deckName:deckName},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    getDeckStats:function (contents, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/deck/stats",
            cache:false,
            data:{
                participantId:getUrlParam("participantId"),
                deckContents:contents},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    // Charlie Code
    loadShields:function (shieldUrl, callback, errorMap) {
        $.ajax({
            type:"GET",
            url: shieldUrl,
            cache:false,
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
       });
    },
    startChat:function (room, callback, errorMap) {
        this._chatCallbacks[room] = callback;
        this._chatErrorMaps[room] = errorMap;

        if (this.supportsWebSockets()) {
            this._chatWsEnabled[room] = true;
            this.ensureChatSocket(room);
            return;
        }

        this._chatWsEnabled[room] = false;
        this.startChatHttp(room, callback, errorMap);
    },
    startChatHttp:function (room, callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/chat/" + room,
            cache:false,
            data:{
                participantId:getUrlParam("participantId")},
            success:callback,
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    chatErrorCheckWithRetry:function (room, latestMsgId, callback, errorMap, tryNum, maxTries) {
        var that = this;
        return function (xhr, status, request) {
            var errorStatus = "" + xhr.status;
            if (errorStatus == "0") {
                // Try again
                setTimeout(function() {
                    that.updateChat(room, latestMsgId, callback, errorMap, tryNum + 1, maxTries);
                });
                return;
            }
            if (errorMap != null && errorMap[errorStatus] != null)
                errorMap[errorStatus](xhr, status, request);
            else if (errorStatus != "200")
                that.failure(xhr, status, request);
        };
    },
    updateChat:function (room, latestMsgId, callback, errorMap, tryNum, maxTries) {
        this._chatCallbacks[room] = callback;
        this._chatErrorMaps[room] = errorMap;

        if (this._chatWsEnabled[room]) {
            this.ensureChatSocket(room);
            return;
        }

        this.updateChatHttp(room, latestMsgId, callback, errorMap, tryNum, maxTries);
    },
    updateChatHttp:function (room, latestMsgId, callback, errorMap, tryNum, maxTries) {
        $.ajax({
            type:"POST",
            url:this.url + "/chat/" + room,
            cache:false,
            async:true,
            data:{
                participantId:getUrlParam("participantId"),
                latestMsgIdRcvd:latestMsgId},
            success:callback,
            timeout: 5000,
            error:this.chatErrorCheckWithRetry(room, latestMsgId, callback, errorMap, tryNum, maxTries),
            dataType:"xml"
        });
    },
    sendChatMessage:function (room, messages, errorMap) {
        if (this._chatWsEnabled[room]) {
            var socket = this._chatSockets[room];
            if (socket != null && socket.readyState === 1) {
                socket.send(JSON.stringify({ type:"message", text:messages }));
                return;
            }
            this.ensureChatSocket(room);
        }

        this.sendChatMessageHttp(room, messages, errorMap);
    },
    sendChatMessageHttp:function (room, messages, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/chat/" + room,
            cache:false,
            async:false,
            data:{
                participantId:getUrlParam("participantId"),
                message:messages},
            traditional:true,
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    supportsWebSockets:function () {
        return window != null && typeof window.WebSocket != "undefined";
    },
    getAuthToken:function () {
        if (typeof localStorage == "undefined")
            return null;
        try {
            return localStorage.getItem("gemp.jwt");
        } catch (e) {
            return null;
        }
    },
    storeAuthToken:function (token) {
        if (token == null || token === "" || typeof localStorage == "undefined")
            return;
        try {
            localStorage.setItem("gemp.jwt", token);
        } catch (e) {
        }
    },
    clearAuthToken:function () {
        if (typeof localStorage == "undefined")
            return;
        try {
            localStorage.removeItem("gemp.jwt");
        } catch (e) {
        }
    },
    buildAuthHeaders:function () {
        var token = this.getAuthToken();
        if (token != null && token !== "")
            return { Authorization:"Bearer " + token };
        return {};
    },
    parseJsonSafely:function (payload) {
        if (payload == null || payload === "")
            return null;
        if (typeof payload === "object")
            return payload;
        try {
            return JSON.parse(payload);
        } catch (e) {
            return null;
        }
    },
    buildWsBase:function () {
        var protocol = (window.location.protocol === "https:") ? "wss://" : "ws://";
        return protocol + window.location.host + "/gemp-swccg-server/ws";
    },
    flushPendingGameUpdate:function () {
        if (this._gameUpdateCallback == null || this._pendingGameUpdate == null)
            return;

        var callback = this._gameUpdateCallback;
        this._gameUpdateCallback = null;
        callback(this.buildGameXml(this._pendingGameUpdate, "update"));
        this._pendingGameUpdate = null;
    },
    ensureGameSocket:function () {
        if (!this._gameWsEnabled)
            return;

        if (this._gameWs != null && (this._gameWs.readyState === 0 || this._gameWs.readyState === 1))
            return;

        var gameId = getUrlParam("gameId");
        if (gameId == null || gameId === "")
            return;

        var wsUrl = this.buildWsBase() + "?channel=game&gameId=" + encodeURIComponent(gameId);
        var token = this.getAuthToken();
        if (token != null && token !== "")
            wsUrl += "&token=" + encodeURIComponent(token);

        var participantId = getUrlParam("participantId");
        if (participantId != null && participantId !== "")
            wsUrl += "&participantId=" + encodeURIComponent(participantId);

        if (this._gameChannelNumber != null && this._gameChannelNumber !== "")
            wsUrl += "&channelNumber=" + encodeURIComponent(this._gameChannelNumber);

        var that = this;
        this._gameWs = new WebSocket(wsUrl);

        this._gameWs.onopen = function () {
            that._gameWsReconnectAttempts = 0;
            if (that._gameWsReconnectTimer != null) {
                clearTimeout(that._gameWsReconnectTimer);
                that._gameWsReconnectTimer = null;
            }
        };

        this._gameWs.onmessage = function (event) {
            that.handleGameWsMessage(event.data);
        };

        this._gameWs.onerror = function () {
            // Reconnect/fallback is handled from onclose.
        };

        this._gameWs.onclose = function (event) {
            that._gameWs = null;
            if (!that._gameWsEnabled)
                return;

            if (that.isAuthClose(event)) {
                that.handleGameError("401");
                return;
            }
            that.scheduleGameReconnect();
        };
    },
    scheduleGameReconnect:function () {
        if (!this._gameWsEnabled)
            return;
        if (this._gameWsReconnectTimer != null)
            return;

        if (this._gameWsReconnectAttempts >= this._gameWsMaxReconnectAttempts) {
            this.enableGamePollingFallback();
            return;
        }

        var delay = Math.min(this._gameWsMaxDelayMs, this._gameWsBaseDelayMs * Math.pow(2, this._gameWsReconnectAttempts));
        this._gameWsReconnectAttempts += 1;

        var that = this;
        this._gameWsReconnectTimer = setTimeout(function () {
            that._gameWsReconnectTimer = null;
            that.ensureGameSocket();
        }, delay);
    },
    enableGamePollingFallback:function () {
        this._gameWsEnabled = false;
        this._gameWsDisabled = true;

        if (this._gameWsReconnectTimer != null) {
            clearTimeout(this._gameWsReconnectTimer);
            this._gameWsReconnectTimer = null;
        }

        if (this._gameWs != null) {
            try {
                this._gameWs.close();
            } catch (e) {
            }
        }
        this._gameWs = null;

        if (this._gameUpdateCallback != null) {
            var callback = this._gameUpdateCallback;
            this._gameUpdateCallback = null;
            this.updateGameStateHttp(this._gameChannelNumber, callback, this._gameErrorMap);
        }
    },
    handleGameWsMessage:function (data) {
        var payload = null;
        try {
            payload = JSON.parse(data);
        } catch (e) {
            return;
        }

        if (payload == null || payload.type !== "game")
            return;

        if (payload.event === "error") {
            this.handleGameError("" + (payload.status || "0"));
            return;
        }
        if (payload.event === "ack")
            return;
        if (payload.event !== "snapshot" && payload.event !== "update")
            return;

        if (payload.channelNumber != null)
            this._gameChannelNumber = payload.channelNumber;

        var rootName = (payload.event === "snapshot" && !this._gameSnapshotReceived) ? "gameState" : "update";
        if (payload.event === "snapshot")
            this._gameSnapshotReceived = true;

        var xml = this.buildGameXml(payload, rootName);
        if (this._gameUpdateCallback != null) {
            var callback = this._gameUpdateCallback;
            this._gameUpdateCallback = null;
            callback(xml);
            return;
        }

        this.queuePendingGameUpdate(payload);
    },
    queuePendingGameUpdate:function (payload) {
        if (payload == null)
            return;

        if (this._pendingGameUpdate == null) {
            this._pendingGameUpdate = {
                channelNumber:payload.channelNumber,
                events:(payload.events || []).slice(0),
                clocks:(payload.clocks || []).slice(0)
            };
            return;
        }

        if (payload.events != null && payload.events.length > 0)
            this._pendingGameUpdate.events = this._pendingGameUpdate.events.concat(payload.events);
        if (payload.clocks != null)
            this._pendingGameUpdate.clocks = payload.clocks.slice(0);
        if (payload.channelNumber != null)
            this._pendingGameUpdate.channelNumber = payload.channelNumber;
    },
    handleGameError:function (status) {
        if (this._gameWs != null) {
            try {
                this._gameWs.close();
            } catch (e) {
            }
        }
        this._gameWs = null;
        this._gameWsEnabled = false;

        if (this._gameErrorMap != null && this._gameErrorMap[status] != null)
            this._gameErrorMap[status]();
        else if (this.failure != null)
            this.failure({ status:status }, null, null);
    },
    sendGameDecisionWs:function (decisionId, response, channelNumber) {
        if (this._gameWs == null || this._gameWs.readyState !== 1)
            return false;

        var payload = {
            type:"decision",
            decisionId:decisionId,
            decisionValue:response,
            channelNumber:channelNumber
        };
        if (payload.channelNumber == null || payload.channelNumber === "")
            payload.channelNumber = this._gameChannelNumber;

        this.applyAutoPassSettings(payload);
        this.applyParticipantOverride(payload);
        this._gameWs.send(JSON.stringify(payload));
        return true;
    },
    applyParticipantOverride:function (payload) {
        var participantId = getUrlParam("participantId");
        if (participantId != null && participantId !== "")
            payload.participantId = participantId;
    },
    applyAutoPassSettings:function (payload) {
        if (typeof $ == "undefined" || typeof $.cookie != "function")
            return;

        var autoPass = $.cookie("autoPass");
        if (autoPass === "false") {
            payload.autoPassEnabled = false;
            return;
        }

        var phases = $.cookie("autoPassPhases");
        if (phases == null || phases === "")
            return;

        var entries = phases.split("0");
        var result = [];
        for (var i = 0; i < entries.length; i++) {
            if (entries[i] != null && entries[i] !== "")
                result.push(entries[i]);
        }
        if (result.length > 0)
            payload.autoPassPhases = result;
    },
    buildGameXml:function (payload, rootName) {
        var doc = document.implementation.createDocument("", "", null);
        var root = doc.createElement(rootName || "update");

        if (payload != null && payload.channelNumber != null)
            root.setAttribute("cn", "" + payload.channelNumber);

        var events = (payload != null && payload.events != null) ? payload.events : [];
        for (var i = 0; i < events.length; i++) {
            root.appendChild(this.buildGameEventXml(doc, events[i]));
        }

        var clocks = (payload != null && payload.clocks != null) ? payload.clocks : [];
        if (clocks.length > 0) {
            var clocksElem = doc.createElement("clocks");
            for (var j = 0; j < clocks.length; j++) {
                var clock = clocks[j];
                if (clock == null)
                    continue;
                var clockElem = doc.createElement("clock");
                this.setAttr(clockElem, "participantId", clock.participantId);
                clockElem.appendChild(doc.createTextNode("" + clock.secondsLeft));
                clocksElem.appendChild(clockElem);
            }
            root.appendChild(clocksElem);
        }

        doc.appendChild(root);
        return doc;
    },
    buildGameEventXml:function (doc, event) {
        var elem = doc.createElement("ge");
        if (event == null)
            return elem;

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

        if (event.gameStats != null)
            this.applyGameStatsXml(doc, elem, event.gameStats);
        if (event.awaitingDecision != null)
            this.applyDecisionXml(doc, elem, event.awaitingDecision);

        return elem;
    },
    applyGameStatsXml:function (doc, elem, stats) {
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
            this.setAttr(playerElem, "name", player.name);
            var zones = player.zones || {};
            for (var key in zones) {
                if (zones.hasOwnProperty(key))
                    this.setAttr(playerElem, key, zones[key]);
            }
            elem.appendChild(playerElem);
        }

        var darkPower = doc.createElement("darkPowerAtLocations");
        var darkEntries = stats.darkPowerAtLocations || [];
        for (var j = 0; j < darkEntries.length; j++) {
            var darkEntry = darkEntries[j];
            if (darkEntry != null)
                this.setAttr(darkPower, "locationIndex" + darkEntry.locationIndex, darkEntry.value);
        }
        elem.appendChild(darkPower);

        var lightPower = doc.createElement("lightPowerAtLocations");
        var lightEntries = stats.lightPowerAtLocations || [];
        for (var k = 0; k < lightEntries.length; k++) {
            var lightEntry = lightEntries[k];
            if (lightEntry != null)
                this.setAttr(lightPower, "locationIndex" + lightEntry.locationIndex, lightEntry.value);
        }
        elem.appendChild(lightPower);
    },
    applyDecisionXml:function (doc, elem, decision) {
        this.setAttr(elem, "id", decision.id);
        this.setAttr(elem, "decisionType", decision.decisionType);
        this.setAttr(elem, "text", decision.text);

        var params = decision.parameters || [];
        for (var i = 0; i < params.length; i++) {
            var param = params[i];
            if (param == null)
                continue;
            var paramElem = doc.createElement("parameter");
            this.setAttr(paramElem, "name", param.name);
            this.setAttr(paramElem, "value", param.value);
            elem.appendChild(paramElem);
        }
    },
    setAttr:function (elem, name, value) {
        if (elem != null && name != null && value != null)
            elem.setAttribute(name, "" + value);
    },
    joinValues:function (values) {
        if (values == null || values.length === 0)
            return null;
        return values.join(",");
    },
    ensureChatSocket:function (room) {
        if (!this._chatWsEnabled[room])
            return;

        var existing = this._chatSockets[room];
        if (existing != null && (existing.readyState === 0 || existing.readyState === 1))
            return;

        var wsUrl = this.buildWsBase() + "?channel=chat&room=" + encodeURIComponent(room);
        var token = this.getAuthToken();
        if (token != null && token != "")
            wsUrl += "&token=" + encodeURIComponent(token);

        var that = this;
        var socket = new WebSocket(wsUrl);
        this._chatSockets[room] = socket;

        socket.onopen = function () {
            that._chatReconnectAttempts[room] = 0;
            if (that._chatReconnectTimers[room] != null) {
                clearTimeout(that._chatReconnectTimers[room]);
                that._chatReconnectTimers[room] = null;
            }
        };

        socket.onmessage = function (event) {
            that.handleChatMessage(room, event.data);
        };

        socket.onerror = function () {
            // onclose manages reconnect/fallback behavior.
        };

        socket.onclose = function (event) {
            that._chatSockets[room] = null;
            if (!that._chatWsEnabled[room])
                return;

            if (that.isAuthClose(event)) {
                that.enableChatPollingFallback(room);
                return;
            }
            that.scheduleChatReconnect(room);
        };
    },
    scheduleChatReconnect:function (room) {
        if (!this._chatWsEnabled[room])
            return;

        var reconnectAttempts = this._chatReconnectAttempts[room] || 0;
        if (reconnectAttempts >= 6) {
            this.enableChatPollingFallback(room);
            return;
        }
        if (this._chatReconnectTimers[room] != null)
            return;

        var delay = Math.min(15000, 1000 * Math.pow(2, reconnectAttempts));
        this._chatReconnectAttempts[room] = reconnectAttempts + 1;

        var that = this;
        this._chatReconnectTimers[room] = setTimeout(function () {
            that._chatReconnectTimers[room] = null;
            that.ensureChatSocket(room);
        }, delay);
    },
    enableChatPollingFallback:function (room) {
        this._chatWsEnabled[room] = false;

        var socket = this._chatSockets[room];
        if (socket != null) {
            try {
                socket.close();
            } catch (e) {
            }
        }
        this._chatSockets[room] = null;

        if (this._chatReconnectTimers[room] != null) {
            clearTimeout(this._chatReconnectTimers[room]);
            this._chatReconnectTimers[room] = null;
        }

        var callback = this._chatCallbacks[room];
        if (callback != null) {
            this.startChatHttp(room, callback, this._chatErrorMaps[room]);
        }
    },
    isAuthClose:function (event) {
        if (event == null)
            return false;
        var code = event.code;
        if (code === 1008 || code === 4001 || code === 4003 || code === 4401 || code === 4403)
            return true;

        var reason = (event.reason || "").toLowerCase();
        return reason.indexOf("auth") > -1 || reason.indexOf("token") > -1 || reason.indexOf("jwt") > -1;
    },
    handleChatMessage:function (room, data) {
        var payload = null;
        try {
            payload = JSON.parse(data);
        } catch (e) {
            return;
        }

        if (payload == null || payload.type !== "chat")
            return;

        if (payload.event === "error") {
            this.handleChatError(room, "0");
            return;
        }

        var messages = [];
        var users = this._chatUsers[room] || [];

        if (payload.event === "snapshot") {
            messages = payload.messages || [];
            users = payload.users || [];
            this._chatUsers[room] = users;
        } else if (payload.event === "message") {
            if (payload.message != null)
                messages = [payload.message];
            if (payload.users != null) {
                users = payload.users;
                this._chatUsers[room] = users;
            }
        } else if (payload.event === "users") {
            users = payload.users || [];
            this._chatUsers[room] = users;
        } else {
            return;
        }

        var callback = this._chatCallbacks[room];
        if (callback != null) {
            callback(this.buildChatXml(payload.room || room, this.normalizeChatMessages(messages), users));
        }
    },
    normalizeChatMessages:function (messages) {
        var result = [];
        if (messages == null)
            return result;

        for (var i = 0; i < messages.length; i++) {
            var message = messages[i];
            if (message == null)
                continue;
            if (typeof message === "string") {
                result.push({
                    id: (new Date()).getTime() + i,
                    from: "System",
                    text: message,
                    date: (new Date()).getTime()
                });
            } else {
                result.push({
                    id: message.id,
                    from: message.from,
                    text: message.text,
                    date: message.date
                });
            }
        }

        return result;
    },
    buildChatXml:function (room, messages, users) {
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
    handleChatError:function (room, status) {
        var errorMap = this._chatErrorMaps[room];
        if (errorMap != null && errorMap[status] != null)
            errorMap[status]();
        else if (this.failure != null)
            this.failure({ status:status }, null, null);
    },
    getHall:function (callback, errorMap) {
        var that = this;
        $.ajax({
            type:"GET",
            url:this.url + "/hall",
            cache:false,
            data:{
                participantId:getUrlParam("participantId")},
            success:this.deliveryCheck(function (xml) {
                that.captureHallStateFromXml(xml, true);
                callback(xml);
            }),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    updateHall:function (callback, channelNumber, errorMap) {
        this._hallErrorMap = errorMap;
        this._hallChannelNumber = channelNumber;

        if (this.supportsWebSockets() && !this._hallWsDisabled) {
            this._hallWsEnabled = true;
            this._hallCallback = callback;
            this.flushPendingHallUpdate();
            this.ensureHallSocket();
            return;
        }

        this._hallWsEnabled = false;
        this.updateHallHttp(callback, channelNumber, errorMap);
    },
    updateHallHttp:function (callback, channelNumber, errorMap) {
        var that = this;
        $.ajax({
            type:"POST",
            url:this.url + "/hall/update",
            cache:false,
            data:{
                channelNumber:channelNumber,
                participantId:getUrlParam("participantId") },
            success:this.deliveryCheck(function (xml) {
                that.captureHallStateFromXml(xml, false);
                callback(xml);
            }),
            timeout: 20000,
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    flushPendingHallUpdate:function () {
        if (this._hallCallback == null)
            return;
        if (this._hallPendingUpdates == null || this._hallPendingUpdates.length === 0)
            return;

        var callback = this._hallCallback;
        this._hallCallback = null;
        callback(this._hallPendingUpdates.shift());
    },
    ensureHallSocket:function () {
        if (!this._hallWsEnabled)
            return;

        if (this._hallSocket != null && (this._hallSocket.readyState === 0 || this._hallSocket.readyState === 1))
            return;

        var wsUrl = this.buildWsBase() + "?channel=hall";
        var token = this.getAuthToken();
        if (token != null && token !== "")
            wsUrl += "&token=" + encodeURIComponent(token);

        var that = this;
        this._hallSocket = new WebSocket(wsUrl);

        this._hallSocket.onopen = function () {
            that._hallReconnectAttempts = 0;
            if (that._hallReconnectTimer != null) {
                clearTimeout(that._hallReconnectTimer);
                that._hallReconnectTimer = null;
            }
        };

        this._hallSocket.onmessage = function (event) {
            that.handleHallWsMessage(event.data);
        };

        this._hallSocket.onerror = function () {
            // onclose handles reconnect and fallback.
        };

        this._hallSocket.onclose = function (event) {
            that._hallSocket = null;
            if (!that._hallWsEnabled)
                return;

            if (that.isAuthClose(event)) {
                that.handleHallError("401");
                return;
            }
            that.scheduleHallReconnect();
        };
    },
    scheduleHallReconnect:function () {
        if (!this._hallWsEnabled)
            return;

        if (this._hallReconnectAttempts >= 3) {
            this.enableHallPollingFallback();
            return;
        }
        if (this._hallReconnectTimer != null)
            return;

        var delay = Math.min(4000, 500 * Math.pow(2, this._hallReconnectAttempts));
        this._hallReconnectAttempts += 1;

        var that = this;
        this._hallReconnectTimer = setTimeout(function () {
            that._hallReconnectTimer = null;
            that.ensureHallSocket();
        }, delay);
    },
    enableHallPollingFallback:function () {
        this._hallWsEnabled = false;
        this._hallWsDisabled = true;

        if (this._hallReconnectTimer != null) {
            clearTimeout(this._hallReconnectTimer);
            this._hallReconnectTimer = null;
        }

        if (this._hallSocket != null) {
            try {
                this._hallSocket.close();
            } catch (e) {
            }
        }
        this._hallSocket = null;

        if (this._hallCallback != null) {
            var callback = this._hallCallback;
            this._hallCallback = null;
            this.updateHallHttp(callback, this._hallChannelNumber, this._hallErrorMap);
        }
    },
    handleHallWsMessage:function (data) {
        var payload = null;
        try {
            payload = JSON.parse(data);
        } catch (e) {
            return;
        }

        if (payload == null || payload.type !== "hall")
            return;

        var xml = this.buildHallXmlFromWs(payload);
        if (xml == null)
            return;

        if (this._hallCallback != null) {
            var callback = this._hallCallback;
            this._hallCallback = null;
            callback(xml);
            return;
        }

        this._hallPendingUpdates.push(xml);
        if (this._hallPendingUpdates.length > 200) {
            this._hallPendingUpdates.shift();
        }
    },
    handleHallError:function (status) {
        if (this._hallErrorMap != null && this._hallErrorMap[status] != null)
            this._hallErrorMap[status]();
        else if (this.failure != null)
            this.failure({ status:status }, null, null);
    },
    captureHallStateFromXml:function (xml, resetKnown) {
        if (xml == null || xml.documentElement == null)
            return;

        var root = xml.documentElement;
        if (root.tagName !== "hall")
            return;

        if (resetKnown) {
            this._hallKnownQueues = {};
            this._hallKnownTournaments = {};
            this._hallKnownTables = {};
        }

        this.captureHallAttribute(root, "channelNumber");
        this.captureHallAttribute(root, "currency");
        this.captureHallAttribute(root, "privateGamesEnabledBoolean");
        this.captureHallAttribute(root, "aiTablesEnabledBoolean");
        this.captureHallAttribute(root, "motd");
        this.captureHallAttribute(root, "serverTime");

        this.captureHallKnownEntities(root, "queue", this._hallKnownQueues);
        this.captureHallKnownEntities(root, "tournament", this._hallKnownTournaments);
        this.captureHallKnownEntities(root, "table", this._hallKnownTables);
    },
    captureHallAttribute:function (root, name) {
        if (root == null || name == null)
            return;
        var value = root.getAttribute(name);
        if (value != null && value !== "") {
            this._hallState[name] = value;
        }
    },
    captureHallKnownEntities:function (root, tagName, targetMap) {
        if (root == null || tagName == null || targetMap == null)
            return;

        var entities = root.getElementsByTagName(tagName);
        for (var i = 0; i < entities.length; i++) {
            var entity = entities[i];
            var id = entity.getAttribute("id");
            if (id == null || id === "")
                continue;
            var action = entity.getAttribute("action");
            if (action === "remove")
                delete targetMap[id];
            else
                targetMap[id] = true;
        }
    },
    buildHallXmlFromWs:function (payload) {
        if (payload == null || payload.event == null)
            return null;

        if (payload.channelNumber != null)
            this._hallState.channelNumber = "" + payload.channelNumber;
        if (payload.motd != null)
            this._hallState.motd = payload.motd;
        if (payload.serverTime != null)
            this._hallState.serverTime = payload.serverTime;

        var doc = document.implementation.createDocument("", "", null);
        var hall = doc.createElement("hall");

        if (this._hallState.channelNumber != null)
            hall.setAttribute("channelNumber", this._hallState.channelNumber);
        if (this._hallState.currency != null)
            hall.setAttribute("currency", this._hallState.currency);
        if (this._hallState.privateGamesEnabledBoolean != null)
            hall.setAttribute("privateGamesEnabledBoolean", this._hallState.privateGamesEnabledBoolean);
        if (this._hallState.aiTablesEnabledBoolean != null)
            hall.setAttribute("aiTablesEnabledBoolean", this._hallState.aiTablesEnabledBoolean);
        if (this._hallState.motd != null)
            hall.setAttribute("motd", this._hallState.motd);
        if (this._hallState.serverTime != null)
            hall.setAttribute("serverTime", this._hallState.serverTime);

        var event = payload.event;
        if (event === "newPlayerGame") {
            var newGame = doc.createElement("newGame");
            if (payload.gameId != null)
                newGame.setAttribute("id", payload.gameId);
            hall.appendChild(newGame);
        } else if (event === "addTournamentQueue" || event === "updateTournamentQueue" || event === "removeTournamentQueue") {
            hall.appendChild(this.buildHallEntityElement(doc, "queue", payload.id, event, payload.props, this._hallKnownQueues));
        } else if (event === "addTournament" || event === "updateTournament" || event === "removeTournament") {
            hall.appendChild(this.buildHallEntityElement(doc, "tournament", payload.id, event, payload.props, this._hallKnownTournaments));
        } else if (event === "addTable" || event === "updateTable" || event === "removeTable") {
            hall.appendChild(this.buildHallEntityElement(doc, "table", payload.id, event, payload.props, this._hallKnownTables));
        } else if (event !== "channelNumber" && event !== "motd" && event !== "serverTime") {
            return null;
        }

        doc.appendChild(hall);
        return doc;
    },
    buildHallEntityElement:function (doc, tagName, id, event, props, knownMap) {
        var elem = doc.createElement(tagName);
        if (id != null)
            elem.setAttribute("id", id);

        var action = this.resolveHallAction(event, id, knownMap);
        elem.setAttribute("action", action);

        if (props != null && action !== "remove") {
            for (var key in props) {
                if (props.hasOwnProperty(key) && props[key] != null) {
                    elem.setAttribute(key, "" + props[key]);
                }
            }
        }

        return elem;
    },
    resolveHallAction:function (event, id, knownMap) {
        if (event == null)
            return "update";
        if (event.indexOf("remove") === 0) {
            if (knownMap != null && id != null)
                delete knownMap[id];
            return "remove";
        }

        var known = knownMap != null && id != null && knownMap[id] === true;
        if (knownMap != null && id != null)
            knownMap[id] = true;

        if (!known)
            return "add";
        return "update";
    },
    joinQueue:function (queueId, deckName, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/hall/queue/" + queueId,
            cache:false,
            data:{
                deckName:deckName,
                sampleDeck:sampleDeck,
                participantId:getUrlParam("participantId")},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    leaveQueue:function (queueId, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/hall/queue/" + queueId + "/leave",
            cache:false,
            data:{
                participantId:getUrlParam("participantId")},
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    dropFromTournament:function(tournamentId, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/hall/tournament/" + tournamentId + "/leave",
            cache:false,
            data:{
                participantId:getUrlParam("participantId")},
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    joinTable:function (tableId, deckName, sampleDeck, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/hall/" + tableId,
            cache:false,
            data:{
                deckName:deckName,
                sampleDeck:sampleDeck,
                participantId:getUrlParam("participantId")},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    leaveTable:function (tableId, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/hall/"+tableId+"/leave",
            cache:false,
            data:{
                participantId:getUrlParam("participantId")},
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    createTable:function (format, deckName, sampleDeck, tableDesc, isPrivate, playVsAi, aiSkill, aiDeckName, aiDeckSample, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/hall",
            cache:false,
            data:{
                format:format,
                deckName:deckName,
                sampleDeck:sampleDeck,
                tableDesc:tableDesc,
                isPrivate:isPrivate,
                playVsAi:playVsAi,
                aiSkill:aiSkill,
                aiDeckName:aiDeckName,
                aiDeckSample:aiDeckSample,
                participantId:getUrlParam("participantId")},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    setShutdownMode:function (enabled, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/shutdown",
            cache:false,
            data:{
                enabled:enabled
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    clearServerCache:function (callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/clearcache",
            cache:false,
            data:{},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    
    setPrivateMode:function (enabled, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/settings/privategames",
            cache:false,
            data:{
                enabled:enabled
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },

    setAiTablesEnabled:function (enabled, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/settings/aitables",
            cache:false,
            data:{
                enabled:enabled
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    
    setNewAccountRegistration:function (enabled, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/settings/newaccounts",
            cache:false,
            data:{
                enabled:enabled
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    
    setInGameStatTracking:function (enabled, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/settings/stattracking",
            cache:false,
            data:{
                enabled:enabled
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    
    purgeInGameStatisticsListeners:function (callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/settings/purgestattrackers",
            cache:false,
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    
    setBonusAbilities:function (enabled, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/settings/bonusabilities",
            cache:false,
            data:{
                enabled:enabled
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    
    getMOTD:function (callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/admin/motd/get",
            cache:false,
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"json"
        });
    },
    
    setMOTD:function (motd, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/motd/update",
            cache:false,
            data:{
                motd:motd
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    addItems:function (collectionType, product, players, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/collections/additems",
            cache:false,
            data:{
                collectionType:collectionType,
                product:product,
                players:players
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    
    addItemsToAllPlayers:function (collectionType, reason, product, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/collections/additemstoall",
            cache:false,
            data:{
                collectionType:collectionType,
                reason:reason,
                product:product
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    addCurrency:function (players, currencyAmount, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/collections/addcurrency",
            cache:false,
            data:{
                players:players,
                currencyAmount:currencyAmount
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    resetUserPassword:function (login, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/user/passwordreset",
            cache:false,
            data:{
                login:login
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    
    permabanUser:function (login, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/user/ban/permanent",
            cache:false,
            data:{
                login:login
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    
    tempbanUser:function (login, duration, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/user/ban/temporary",
            cache:false,
            data:{
                login:login,
                duration:duration
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    
    unbanUser:function (login, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/user/ban/acquit",
            cache:false,
            data:{
                login:login
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    
    susUserSearch:function (login, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/users/detailedsearch",
            cache:false,
            data:{
                login:login
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    
    banMultiple:function (logins, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/users/ban/permanent",
            cache:false,
            data:{
                logins:logins
            },
            traditional: true,
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    
    deactivateMultiple:function (logins, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/users/deactivate",
            cache:false,
            data:{
                logins:logins
            },
            traditional: true,
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    
    
    showUsersWithFlag:function (flag, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/users/findwithflag",
            cache:false,
            data:{
                flag:flag
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    
    showPlaytesters:function (callback, errorMap) {
      this.showUsersWithFlag("PLAYTESTER", callback, errorMap)  
    },
    
    showCommentators:function (callback, errorMap) {
      this.showUsersWithFlag("COMMENTATOR", callback, errorMap)  
    },
    
    addFlagToUser:function (login, flag, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/user/addflag",
            cache:false,
            data:{
                login:login,
                flag:flag
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    
    addPlaytesterToUser:function (login, callback, errorMap) {
      this.addFlagToUser(login, "PLAYTESTER", callback, errorMap)  
    },
    
    addCommentatorToUser:function (login, callback, errorMap) {
      this.addFlagToUser(login, "COMMENTATOR", callback, errorMap)  
    },
    
    removeFlagFromUsers:function (logins, flag, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/users/removeflag",
            cache:false,
            data:{
                logins:logins,
                flag:flag
            },
            traditional:true,
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    
    removeFlagFromUser:function (logins, flag, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/users/removeflag",
            cache:false,
            data:{
                logins:logins,
                flag:flag
            },
            traditional:false,
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    
    removePlaytesterFromUsers:function (logins, callback, errorMap) {
      this.removeFlagFromUsers(logins, "PLAYTESTER", callback, errorMap)  
    },
    
    removeCommentatorFromUsers:function (logins, callback, errorMap) {
      this.removeFlagFromUsers(logins, "COMMENTATOR", callback, errorMap)  
    },
    
    reactivateUser:function (logins, callback, errorMap) {
      this.removeFlagFromUser(logins, "DEACTIVATED", callback, errorMap)  
    },
    
    
    
    previewSealedLeague:function (name, cost, start, format, serieDuration, maxMatches, 
                              allowTimeExtensions, allowSpectators, showPlayerNames, 
                              invitationOnly, registrationInfo, decisionTimeoutSeconds, 
                              timePerPlayerMinutes,
                              callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/league/sealed/preview",
            cache:false,
            data:{
                name:name,
                cost:cost,
                start:start,
                format:format,
                serieDuration:serieDuration,
                maxMatches:maxMatches,
                allowTimeExtensions:allowTimeExtensions,
                allowSpectators:allowSpectators,
                showPlayerNames:showPlayerNames,
                invitationOnly:invitationOnly,
                registrationInfo:registrationInfo,
                decisionTimeoutSeconds:decisionTimeoutSeconds,
                timePerPlayerMinutes:timePerPlayerMinutes
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    
    addSealedLeague:function (name, cost, start, format, serieDuration, maxMatches, 
                              allowTimeExtensions, allowSpectators, showPlayerNames, 
                              invitationOnly, registrationInfo, decisionTimeoutSeconds, 
                              timePerPlayerMinutes,
                              callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/league/sealed/create",
            cache:false,
            data:{
                name:name,
                cost:cost,
                start:start,
                format:format,
                serieDuration:serieDuration,
                maxMatches:maxMatches,
                allowTimeExtensions:allowTimeExtensions,
                allowSpectators:allowSpectators,
                showPlayerNames:showPlayerNames,
                invitationOnly:invitationOnly,
                registrationInfo:registrationInfo,
                decisionTimeoutSeconds:decisionTimeoutSeconds,
                timePerPlayerMinutes:timePerPlayerMinutes
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },


    previewSoloDraftLeague:function (name, cost, start, format, serieDuration, maxMatches,
                              allowTimeExtensions, allowSpectators, showPlayerNames,
                              invitationOnly, registrationInfo, decisionTimeoutSeconds,
                              timePerPlayerMinutes,
                              callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/league/solodraft/preview",
            cache:false,
            data:{
                name:name,
                cost:cost,
                start:start,
                format:format,
                serieDuration:serieDuration,
                maxMatches:maxMatches,
                allowTimeExtensions:allowTimeExtensions,
                allowSpectators:allowSpectators,
                showPlayerNames:showPlayerNames,
                invitationOnly:invitationOnly,
                registrationInfo:registrationInfo,
                decisionTimeoutSeconds:decisionTimeoutSeconds,
                timePerPlayerMinutes:timePerPlayerMinutes
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },

    addSoloDraftLeague:function (name, cost, start, format, serieDuration, maxMatches,
                              allowTimeExtensions, allowSpectators, showPlayerNames,
                              invitationOnly, registrationInfo, decisionTimeoutSeconds,
                              timePerPlayerMinutes,
                              callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/league/solodraft/create",
            cache:false,
            data:{
                name:name,
                cost:cost,
                start:start,
                format:format,
                serieDuration:serieDuration,
                maxMatches:maxMatches,
                allowTimeExtensions:allowTimeExtensions,
                allowSpectators:allowSpectators,
                showPlayerNames:showPlayerNames,
                invitationOnly:invitationOnly,
                registrationInfo:registrationInfo,
                decisionTimeoutSeconds:decisionTimeoutSeconds,
                timePerPlayerMinutes:timePerPlayerMinutes
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },


    previewConstructedLeague:function (name, cost, start, collectionType,  
                              allowTimeExtensions, allowSpectators, showPlayerNames, 
                              invitationOnly, registrationInfo, decisionTimeoutSeconds, 
                              timePerPlayerMinutes, formats, serieDurations, maxMatches,
                              callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/league/constructed/preview",
            cache:false,
            traditional: true,
            data:{
                name:name,
                cost:cost,
                start:start,
                collectionType:collectionType,
                allowTimeExtensions:allowTimeExtensions,
                allowSpectators:allowSpectators,
                showPlayerNames:showPlayerNames,
                invitationOnly:invitationOnly,
                registrationInfo:registrationInfo,
                decisionTimeoutSeconds:decisionTimeoutSeconds,
                timePerPlayerMinutes:timePerPlayerMinutes,
                formats:formats,
                serieDurations:serieDurations,
                maxMatches:maxMatches
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    
    addConstructedLeague:function (name, cost, start, collectionType,  
                              allowTimeExtensions, allowSpectators, showPlayerNames, 
                              invitationOnly, registrationInfo, decisionTimeoutSeconds, 
                              timePerPlayerMinutes, formats, serieDurations, maxMatches,
                              callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/league/constructed/create",
            cache:false,
            traditional: true,
            data:{
                name:name,
                cost:cost,
                start:start,
                collectionType:collectionType,
                allowTimeExtensions:allowTimeExtensions,
                allowSpectators:allowSpectators,
                showPlayerNames:showPlayerNames,
                invitationOnly:invitationOnly,
                registrationInfo:registrationInfo,
                decisionTimeoutSeconds:decisionTimeoutSeconds,
                timePerPlayerMinutes:timePerPlayerMinutes,
                formats:formats,
                serieDurations:serieDurations,
                maxMatches:maxMatches
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    
    addPlayersToLeague:function (leagueType, players, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/league/addplayers",
            cache:false,
            traditional: true,
            data:{
                leagueType:leagueType,
                players:players
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    
    leagueDeckCheck:function (leagueId, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/league/deckcheck",
            cache:false,
            data:{
                leagueId:leagueId
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    
    addTables:function (name, tournament, format, timer, playerones, playertwos, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/admin/addTables",
            cache:false,
            data:{
                name:name,
                tournament:tournament,
                format:format,
                timer:timer,
                playerones:playerones,
                playertwos:playertwos
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    //NEVER EVER EVER use this for actual authentication
    // This is strictly to simplify things like auto-hiding
    // of the admin panel.  If you actually need functionality
    // gated behind authorization, it goes on the server
    // and not in here.
    
    getPlayerInfo:function (callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/playerStats/playerInfo",
            cache:false,
            data:{
                participantId:getUrlParam("participantId")
            },
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"json"
        });
    },
    
    getFormat:function (formatCode, callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/hall/format/" + formatCode,
            cache:false,
            data:{
                participantId:getUrlParam("participantId")},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    getStatus:function (callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/",
            cache:false,
            data:{
                participantId:getUrlParam("participantId")},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    login:function (login, password, callback, errorMap) {
        var that = this;
        $.ajax({
            type:"POST",
            url:this.apiBase + "/auth/login",
            cache:false,
            async:false,
            data:{
                login:login,
                password:password
            },
            xhrFields:{ withCredentials:true },
            success:function (payload, status, request) {
                var statusCode = "" + request.status;
                var authPayload = that.parseJsonSafely(payload);
                if (statusCode == "200" && authPayload != null && authPayload.token != null) {
                    that.storeAuthToken(authPayload.token);
                    callback(authPayload, request.status);
                    return;
                }
                that.clearAuthToken();
                callback(authPayload, request.status);
            },
            error:function (xhr, status, request) {
                that.clearAuthToken();
                that.errorCheck(errorMap)(xhr, status, request);
            },
            dataType:"text"
        });
    },
    register:function (login, password, callback, errorMap) {
        var that = this;
        $.ajax({
            type:"POST",
            url:this.apiBase + "/auth/register",
            cache:false,
            data:{
                login:login,
                password:password
            },
            xhrFields:{ withCredentials:true },
            success:function (payload, status, request) {
                var statusCode = "" + request.status;
                var authPayload = that.parseJsonSafely(payload);
                if (statusCode == "200" && authPayload != null && authPayload.token != null) {
                    that.storeAuthToken(authPayload.token);
                    callback(authPayload, request.status);
                    return;
                }
                that.clearAuthToken();
                callback(authPayload, request.status);
            },
            error:function (xhr, status, request) {
                that.clearAuthToken();
                that.errorCheck(errorMap)(xhr, status, request);
            },
            dataType:"text"
        });
    },
    getRegistrationForm:function (callback, errorMap) {
        $.ajax({
            type:"POST",
            url:"/gemp-swccg/includes/registrationForm.html",
            cache:false,
            async:false,
            data:{
                participantId:getUrlParam("participantId")},
            success:this.deliveryCheck(callback),
            error:this.errorCheck(errorMap),
            dataType:"html"
        });
    },
    getDraft:function (leagueType, callback, errorMap) {
        $.ajax({
            type:"GET",
            url:this.url + "/soloDraft/"+leagueType,
            cache:false,
            data:{
                participantId:getUrlParam("participantId")},
            success:callback,
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    },
    makeDraftPick:function (leagueType, choiceId, callback, errorMap) {
        $.ajax({
            type:"POST",
            url:this.url + "/soloDraft/"+leagueType,
            cache:false,
            data:{
                choiceId:choiceId,
                participantId:getUrlParam("participantId")},
            success:callback,
            error:this.errorCheck(errorMap),
            dataType:"xml"
        });
    }
});
