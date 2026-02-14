package com.gempukku.swccgo.chat;

import com.gempukku.swccgo.PrivateInformationException;
import com.gempukku.swccgo.SubscriptionExpiredException;
import com.gempukku.swccgo.game.ChatCommunicationChannel;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ChatRoomMediator {
    private Logger _logger;
    private ChatRoom _chatRoom;

    private Map<String, ChatCommunicationChannel> _listeners = new HashMap<String, ChatCommunicationChannel>();
    private Map<String, Long> _lastSeen = new HashMap<String, Long>();
    private Map<String, Long> _lastActive = new HashMap<String, Long>();

    private final int _channelInactivityTimeoutPeriod;
    private final int _userIdleTimeoutPeriod;
    private boolean _privateRoom;
    private Set<String> _allowedPlayers;
    private boolean _allowSpectatorsToChat;
    private boolean _playtesting;

    private ReadWriteLock _lock = new ReentrantReadWriteLock();

    private Map<String, ChatCommandCallback> _chatCommandCallbacks = new HashMap<String, ChatCommandCallback>();

    public ChatRoomMediator(String roomName, boolean muteJoinPartMessages, int secondsTimeoutPeriod, int secondsIdlePeriod, boolean privateRoom, Set<String> allowedPlayers, boolean allowSpectatorsToChat, boolean playtesting) {
        _logger = LogManager.getLogger("chat."+roomName);
        _privateRoom = privateRoom;
        _allowedPlayers = allowedPlayers;
        _allowSpectatorsToChat = allowSpectatorsToChat;
        _playtesting = playtesting;
        _channelInactivityTimeoutPeriod = 1000 * secondsTimeoutPeriod;
        _userIdleTimeoutPeriod = Math.max(0, secondsIdlePeriod) * 1000;
        _chatRoom = new ChatRoom(muteJoinPartMessages);
    }

    public void addChatCommandCallback(String command, ChatCommandCallback callback) {
        _chatCommandCallbacks.put(command.toLowerCase(), callback);
    }

    public List<ChatMessage> joinUser(String playerId, boolean admin, boolean playtester) throws PrivateInformationException {
        return joinUser(playerId, admin, playtester, new ChatCommunicationChannel());
    }

    public List<ChatMessage> joinUser(String playerId, boolean admin, boolean playtester, ChatCommunicationChannel listener) throws PrivateInformationException {
        _lock.writeLock().lock();
        try {
            if(_allowedPlayers != null && !_allowedPlayers.contains(playerId) && _privateRoom)
                throw new PrivateInformationException();
            if(_allowedPlayers != null && !_allowedPlayers.contains(playerId) && _playtesting && !admin && !playtester)
                throw new PrivateInformationException();

            _listeners.put(playerId, listener);
            recordPresence(playerId, true);
            _chatRoom.joinChatRoom(playerId, _allowedPlayers != null && !_allowedPlayers.contains(playerId) && !_allowSpectatorsToChat, listener);
            return listener.consumeMessages(0);
        } finally {
            _lock.writeLock().unlock();
        }
    }

    public ChatCommunicationChannel getChatRoomListener(String playerId) throws SubscriptionExpiredException {
        _lock.readLock().lock();
        try {
            ChatCommunicationChannel gatheringChatRoomListener = _listeners.get(playerId);
            if (gatheringChatRoomListener == null)
                throw new SubscriptionExpiredException();
            return gatheringChatRoomListener;
        } finally {
            _lock.readLock().unlock();
        }
    }

    public void partUser(String playerId) {
        _lock.writeLock().lock();
        try {
            _chatRoom.partChatRoom(playerId);
            _listeners.remove(playerId);
            _lastSeen.remove(playerId);
            _lastActive.remove(playerId);
        } finally {
            _lock.writeLock().unlock();
        }
    }

    public void sendMessage(String playerId, String message, boolean admin) throws PrivateInformationException, ChatCommandErrorException {
        if (processIfKnownCommand(playerId, message, admin))
            return;

        _lock.writeLock().lock();
        try {
            if (admin || _allowedPlayers == null || _allowedPlayers.contains(playerId) || _allowSpectatorsToChat) {
                recordPresence(playerId, false);
                _logger.trace(playerId + ": " + message);
                _chatRoom.postMessage(playerId, message);
            }
            else if (_privateRoom) {
                throw new PrivateInformationException();
            }
        } finally {
            _lock.writeLock().unlock();
        }
    }

    private boolean processIfKnownCommand(String playerId, String message, boolean admin) throws ChatCommandErrorException {
        if (message.startsWith("/")) {
            // Maybe it's a known command
            String commandString = message.substring(1);
            int spaceIndex = commandString.indexOf(" ");
            String commandName;
            String commandParameters="";
            if (spaceIndex>-1) {
                commandName = commandString.substring(0, spaceIndex);
                commandParameters = commandString.substring(spaceIndex+1);
            } else {
                commandName = commandString;
            }
            final ChatCommandCallback callbackForCommand = _chatCommandCallbacks.get(commandName.toLowerCase());
            if (callbackForCommand != null) {
                callbackForCommand.commandReceived(playerId, commandParameters, admin);
                return true;
            }
        }
        return false;
    }

    public void cleanup() {
        _lock.writeLock().lock();
        try {
            long currentTime = System.currentTimeMillis();
            Map<String, ChatCommunicationChannel> copy = new HashMap<String, ChatCommunicationChannel>(_listeners);
            for (Map.Entry<String, ChatCommunicationChannel> playerListener : copy.entrySet()) {
                String playerId = playerListener.getKey();
                ChatCommunicationChannel listener = playerListener.getValue();
                if (currentTime > (listener.getLastAccessed() + _channelInactivityTimeoutPeriod)) {
                    _chatRoom.partChatRoom(playerId);
                    _listeners.remove(playerId);
                    _lastSeen.remove(playerId);
                    _lastActive.remove(playerId);
                }
            }
        } finally {
            _lock.writeLock().unlock();
        }
    }

    public Collection<String> getUsersInRoom() {
        _lock.readLock().lock();
        try {
            return _chatRoom.getUsersInRoom();
        } finally {
            _lock.readLock().unlock();
        }
    }

    public void markSeen(String playerId) {
        _lock.writeLock().lock();
        try {
            recordPresence(playerId, true);
        } finally {
            _lock.writeLock().unlock();
        }
    }

    public void markActive(String playerId) {
        _lock.writeLock().lock();
        try {
            recordPresence(playerId, false);
        } finally {
            _lock.writeLock().unlock();
        }
    }

    public Map<String, String> getUserStatusMap() {
        _lock.readLock().lock();
        try {
            Map<String, String> statuses = new HashMap<String, String>();
            long now = System.currentTimeMillis();
            for (String user : _chatRoom.getUsersInRoom()) {
                String status = "online";
                if (_userIdleTimeoutPeriod > 0) {
                    Long lastActive = _lastActive.get(user);
                    if (lastActive != null && now - lastActive > _userIdleTimeoutPeriod) {
                        status = "away";
                    }
                }
                statuses.put(user, status);
            }
            return statuses;
        } finally {
            _lock.readLock().unlock();
        }
    }

    private void recordPresence(String playerId, boolean passive) {
        long now = System.currentTimeMillis();
        _lastSeen.put(playerId, now);
        if (!passive) {
            _lastActive.put(playerId, now);
        } else if (!_lastActive.containsKey(playerId)) {
            _lastActive.put(playerId, now);
        }
    }
}
