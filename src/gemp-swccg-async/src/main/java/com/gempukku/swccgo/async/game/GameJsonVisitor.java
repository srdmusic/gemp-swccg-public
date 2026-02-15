package com.gempukku.swccgo.async.game;

import com.gempukku.swccgo.game.ParticipantCommunicationVisitor;
import com.gempukku.swccgo.game.state.GameEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameJsonVisitor implements ParticipantCommunicationVisitor {
    private int _channelNumber = -1;
    private final List<GameEvent> _events = new ArrayList<GameEvent>();
    private Map<String, Integer> _clocks = new LinkedHashMap<String, Integer>();

    @Override
    public void visitChannelNumber(int channelNumber) {
        _channelNumber = channelNumber;
    }

    @Override
    public void visitGameEvent(GameEvent gameEvent) {
        _events.add(gameEvent);
    }

    @Override
    public void visitClock(Map<String, Integer> secondsLeft) {
        _clocks = new LinkedHashMap<String, Integer>(secondsLeft);
    }

    public int getChannelNumber() {
        return _channelNumber;
    }

    public List<GameEvent> getEvents() {
        return _events;
    }

    public Map<String, Integer> getClocks() {
        return _clocks;
    }
}
