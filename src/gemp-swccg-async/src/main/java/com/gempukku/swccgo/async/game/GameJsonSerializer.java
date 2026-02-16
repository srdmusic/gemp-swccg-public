package com.gempukku.swccgo.async.game;

import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.state.GameEvent;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.timing.GameStats;
import com.gempukku.swccgo.logic.timing.GuiUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GameJsonSerializer {
    // Keep WS payloads close to legacy XML attribute names so the client adapter stays trivial.
    public Map<String, Object> buildPayload(GameJsonVisitor visitor) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("channelNumber", visitor.getChannelNumber());
        payload.put("events", serializeEvents(visitor.getEvents()));
        payload.put("clocks", serializeClocks(visitor.getClocks()));
        return payload;
    }

    public List<Map<String, Object>> serializeEvents(List<GameEvent> events) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (GameEvent event : events) {
            result.add(serializeEvent(event));
        }
        return result;
    }

    public List<Map<String, Object>> serializeClocks(Map<String, Integer> clocks) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        Map<String, Integer> sorted = new TreeMap<String, Integer>(clocks);
        for (Map.Entry<String, Integer> entry : sorted.entrySet()) {
            Map<String, Object> clock = new LinkedHashMap<String, Object>();
            clock.put("participantId", entry.getKey());
            clock.put("secondsLeft", entry.getValue());
            result.add(clock);
        }
        return result;
    }

    private Map<String, Object> serializeEvent(GameEvent gameEvent) {
        Map<String, Object> event = new LinkedHashMap<String, Object>();
        event.put("type", gameEvent.getType().name());
        if (gameEvent.getBlueprintId() != null) {
            event.put("blueprintId", gameEvent.getBlueprintId());
        }
        if (gameEvent.getTestingText() != null) {
            event.put("testingText", gameEvent.getTestingText());
        }
        if (gameEvent.getBackSideTestingText() != null) {
            event.put("backSideTestingText", gameEvent.getBackSideTestingText());
        }
        if (gameEvent.getHorizontal() != null) {
            event.put("horizontal", gameEvent.getHorizontal());
        }
        if (gameEvent.getCardId() != null) {
            event.put("cardId", gameEvent.getCardId());
        }
        if (gameEvent.getIndex() != null) {
            event.put("index", gameEvent.getIndex());
        }
        if (gameEvent.getZoneOwnerId() != null) {
            event.put("zoneOwnerId", gameEvent.getZoneOwnerId());
        }
        if (gameEvent.getSystemName() != null) {
            event.put("systemName", gameEvent.getSystemName());
        }
        if (gameEvent.getLocationIndex() != null) {
            event.put("locationIndex", gameEvent.getLocationIndex());
        }
        if (gameEvent.getLocationIndexes() != null) {
            event.put("locationIndexes", new ArrayList<Integer>(gameEvent.getLocationIndexes()));
        }
        if (gameEvent.getParticipantId() != null) {
            event.put("participantId", gameEvent.getParticipantId());
        }
        if (gameEvent.getAllParticipantIds() != null) {
            event.put("allParticipantIds", new ArrayList<String>(gameEvent.getAllParticipantIds()));
        }
        if (gameEvent.getPhase() != null) {
            event.put("phase", gameEvent.getPhase());
        }
        if (gameEvent.getTargetCardId() != null) {
            event.put("targetCardId", gameEvent.getTargetCardId());
        }
        if (gameEvent.getZone() != null) {
            event.put("zone", gameEvent.getZone().name());
        }
        if (gameEvent.isInverted() != null) {
            event.put("inverted", gameEvent.isInverted());
        }
        if (gameEvent.isSideways() != null) {
            event.put("sideways", gameEvent.isSideways());
        }
        if (gameEvent.isFrozen() != null) {
            event.put("frozen", gameEvent.isFrozen());
        }
        if (gameEvent.isSuspendedOrTurnedOff() != null) {
            event.put("suspended", gameEvent.isSuspendedOrTurnedOff());
        }
        if (gameEvent.isCollapsed() != null) {
            event.put("collapsed", gameEvent.isCollapsed());
        }
        if (gameEvent.getCount() != null) {
            event.put("count", gameEvent.getCount());
        }
        if (gameEvent.getDestinyText() != null) {
            event.put("destinyText", gameEvent.getDestinyText());
        }
        if (gameEvent.getPlayerAttacking() != null) {
            event.put("playerAttacking", gameEvent.getPlayerAttacking());
        }
        if (gameEvent.getPlayerDefending() != null) {
            event.put("playerDefending", gameEvent.getPlayerDefending());
        }
        if (gameEvent.getOtherCardIds() != null) {
            event.put("otherCardIds", toIntegerList(gameEvent.getOtherCardIds()));
        }
        if (gameEvent.getOtherCardIds2() != null) {
            event.put("otherCardIds2", toIntegerList(gameEvent.getOtherCardIds2()));
        }
        if (gameEvent.getMessage() != null) {
            event.put("message", gameEvent.getMessage());
        }
        if (gameEvent.getGameStats() != null) {
            event.put("gameStats", serializeGameStats(gameEvent.getGameStats()));
        }
        if (gameEvent.getAwaitingDecision() != null) {
            event.put("awaitingDecision", serializeDecision(gameEvent.getAwaitingDecision()));
        }
        return event;
    }

    private Map<String, Object> serializeGameStats(GameStats gameStats) {
        Map<String, Object> stats = new LinkedHashMap<String, Object>();
        stats.put("darkForceGeneration", GuiUtils.formatAsString(gameStats.getDarkForceGeneration(), true));
        stats.put("lightForceGeneration", GuiUtils.formatAsString(gameStats.getLightForceGeneration(), true));
        stats.put("darkBattlePower", GuiUtils.formatAsString(gameStats.getDarkBattlePower(), true));
        stats.put("lightBattlePower", GuiUtils.formatAsString(gameStats.getLightBattlePower(), true));
        stats.put("darkBattleNumDestinyToPower", gameStats.getDarkBattleNumDestinyToPower());
        stats.put("lightBattleNumDestinyToPower", gameStats.getLightBattleNumDestinyToPower());
        stats.put("darkBattleNumBattleDestiny", gameStats.getDarkBattleNumBattleDestiny());
        stats.put("lightBattleNumBattleDestiny", gameStats.getLightBattleNumBattleDestiny());
        stats.put("darkBattleNumDestinyToAttrition", gameStats.getDarkBattleNumDestinyToAttrition());
        stats.put("lightBattleNumDestinyToAttrition", gameStats.getLightBattleNumDestinyToAttrition());
        stats.put("darkBattleDamageRemaining", GuiUtils.formatAsString(gameStats.getDarkBattleDamageRemaining(), true));
        stats.put("lightBattleDamageRemaining", GuiUtils.formatAsString(gameStats.getLightBattleDamageRemaining(), true));
        stats.put("darkBattleAttritionRemaining", GuiUtils.formatAsString(gameStats.getDarkBattleAttritionRemaining(), true));
        stats.put("lightBattleAttritionRemaining", GuiUtils.formatAsString(gameStats.getLightBattleAttritionRemaining(), true));
        stats.put("darkImmuneToRemainingAttrition", gameStats.isDarkImmuneToRemainingAttrition());
        stats.put("lightImmuneToRemainingAttrition", gameStats.isLightImmuneToRemainingAttrition());
        stats.put("darkSabaccTotal", GuiUtils.formatAsString(gameStats.getDarkSabaccTotal(), true));
        stats.put("lightSabaccTotal", GuiUtils.formatAsString(gameStats.getLightSabaccTotal(), true));
        stats.put("darkDuelOrLightsaberCombatTotal", GuiUtils.formatAsString(gameStats.getDarkDuelOrLightsaberCombatTotal(), true));
        stats.put("lightDuelOrLightsaberCombatTotal", GuiUtils.formatAsString(gameStats.getLightDuelOrLightsaberCombatTotal(), true));
        stats.put("darkDuelOrLightsaberCombatNumDestiny", gameStats.getDarkDuelOrLightsaberCombatNumDestiny());
        stats.put("lightDuelOrLightsaberCombatNumDestiny", gameStats.getLightDuelOrLightsaberCombatNumDestiny());
        stats.put("attackingPowerOrFerocityInAttack", GuiUtils.formatAsString(gameStats.getAttackingPowerOrFerocityInAttack(), true));
        stats.put("defendingPowerOrFerocityInAttack", GuiUtils.formatAsString(gameStats.getDefendingPowerOrFerocityInAttack(), true));
        stats.put("attackingNumDestinyInAttack", gameStats.getAttackingNumDestinyInAttack());
        stats.put("defendingNumDestinyInAttack", gameStats.getDefendingNumDestinyInAttack());
        stats.put("darkRaceTotal", GuiUtils.formatAsString(gameStats.getDarkRaceTotal(), true));
        stats.put("lightRaceTotal", GuiUtils.formatAsString(gameStats.getLightRaceTotal(), true));
        stats.put("darkPoliticsTotal", GuiUtils.formatAsString(gameStats.getDarkPoliticsTotal(), true));
        stats.put("lightPoliticsTotal", GuiUtils.formatAsString(gameStats.getLightPoliticsTotal(), true));

        List<Map<String, Object>> playerZones = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Map<Zone, Integer>> playerZoneSizes : gameStats.getZoneSizes().entrySet()) {
            Map<String, Object> player = new LinkedHashMap<String, Object>();
            player.put("name", playerZoneSizes.getKey());
            Map<String, Integer> zones = new LinkedHashMap<String, Integer>();
            for (Map.Entry<Zone, Integer> zoneSizes : playerZoneSizes.getValue().entrySet()) {
                zones.put(zoneSizes.getKey().name(), zoneSizes.getValue());
            }
            player.put("zones", zones);
            playerZones.add(player);
        }
        stats.put("playerZones", playerZones);

        stats.put("darkPowerAtLocations", serializePowerAtLocations(gameStats.getDarkPowerAtLocations()));
        stats.put("lightPowerAtLocations", serializePowerAtLocations(gameStats.getLightPowerAtLocations()));
        return stats;
    }

    private List<Map<String, Object>> serializePowerAtLocations(Map<Integer, Float> powerAtLocations) {
        List<Map<String, Object>> entries = new ArrayList<Map<String, Object>>();
        for (Map.Entry<Integer, Float> entry : powerAtLocations.entrySet()) {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("locationIndex", entry.getKey());
            payload.put("value", GuiUtils.formatAsString(entry.getValue(), true));
            entries.add(payload);
        }
        return entries;
    }

    private Map<String, Object> serializeDecision(AwaitingDecision decision) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("id", decision.getAwaitingDecisionId());
        result.put("decisionType", decision.getDecisionType().name());
        if (decision.getText() != null) {
            result.put("text", decision.getText());
        }
        List<Map<String, String>> parameters = new ArrayList<Map<String, String>>();
        for (Map.Entry<String, String[]> paramEntry : decision.getDecisionParameters().entrySet()) {
            for (String value : paramEntry.getValue()) {
                Map<String, String> param = new LinkedHashMap<String, String>();
                param.put("name", paramEntry.getKey());
                param.put("value", value);
                parameters.add(param);
            }
        }
        result.put("parameters", parameters);
        return result;
    }

    private List<Integer> toIntegerList(int[] values) {
        List<Integer> list = new ArrayList<Integer>();
        for (int value : values) {
            list.add(value);
        }
        return list;
    }
}
