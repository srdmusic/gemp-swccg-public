package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.TraceSnapshots;
import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PullDecisionWire;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PullTestFixtures {

    static final String PLAYER = "darkPlayer";
    static final int DECISION_ID = 41;
    static final long TRANSACTION_ID = 7001L;
    static final int PARENT_DECISION_ID = 37;
    static final int PARENT_ORDINAL = 1;
    static final GameTextActionId GAME_TEXT_ACTION =
            GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD;

    private PullTestFixtures() {
    }

    static Map<String, String[]> parent(DecisionActionSemantic semantic) {
        Map<String, String[]> params = new LinkedHashMap<>();
        put(params, DecisionOrigin.WIRE_PARAMETER, DecisionOrigin.PHASE_ACTION.name());
        put(params, "actionId", "0", "1");
        put(params, "cardId", "501", "502");
        put(params, DecisionActionSemantic.WIRE_PARAMETER,
                DecisionActionSemantic.UNKNOWN.name(), semantic.name());
        put(params, PullDecisionWire.SOURCE_CARD_ID, "", "902");
        put(params, PullDecisionWire.SOURCE_PERMANENT_CARD_ID, "", "9002");
        put(params, PullDecisionWire.GAME_TEXT_ACTION_ID, "", GAME_TEXT_ACTION.name());
        put(params, "noPass", "false");
        return params;
    }

    static Map<String, String[]> child(DecisionOrigin origin) {
        Map<String, String[]> params = transaction(origin, AwaitingDecisionType.ARBITRARY_CARDS);
        put(params, "cardId", "temp0", "temp1", "temp2");
        put(params, "blueprintId", "7_1", "7_1", "9_2");
        put(params, "min", "0");
        put(params, "max", "2");
        put(params, "selectable", "true", "true", "true");
        put(params, "preselected", "false", "false", "false");
        put(params, "returnAnyChange", "false");
        put(params, PullDecisionWire.PHYSICAL_CARD_ID, "101", "102", "103");
        put(params, PullDecisionWire.PHYSICAL_PERMANENT_CARD_ID, "1001", "1002", "1003");
        return params;
    }

    static Map<String, String[]> destination(boolean oneCandidate) {
        Map<String, String[]> params = transaction(
                DecisionOrigin.PULL_DESTINATION, AwaitingDecisionType.CARD_SELECTION);
        put(params, "min", "0");
        put(params, "max", "1");
        put(params, PullDecisionWire.SELECTED_CARD_ID, "102");
        put(params, PullDecisionWire.SELECTED_PERMANENT_CARD_ID, "1002");
        if (oneCandidate) {
            put(params, "cardId", "301");
            put(params, PullDecisionWire.DESTINATION_CARD_ID, "301");
            put(params, PullDecisionWire.DESTINATION_PERMANENT_CARD_ID, "3001");
        } else {
            put(params, "cardId", "301", "302");
            put(params, PullDecisionWire.DESTINATION_CARD_ID, "301", "302");
            put(params, PullDecisionWire.DESTINATION_PERMANENT_CARD_ID, "3001", "3002");
        }
        return params;
    }

    static Map<String, String[]> failedVerify() {
        Map<String, String[]> params = child(DecisionOrigin.PULL_FAILED_VERIFY);
        put(params, "min", "0");
        put(params, "max", "0");
        put(params, "selectable", "false", "false", "false");
        return params;
    }

    static PullRouteInput input(AwaitingDecisionType type, Map<String, String[]> params) {
        return PullRouteInput.capture(decision(type, "ignored prompt text", params));
    }

    static AwaitingDecision decision(AwaitingDecisionType type,
                                     String text,
                                     Map<String, String[]> params) {
        return new AwaitingDecision() {
            @Override
            public int getAwaitingDecisionId() {
                return DECISION_ID;
            }

            @Override
            public String getText() {
                return text;
            }

            @Override
            public AwaitingDecisionType getDecisionType() {
                return type;
            }

            @Override
            public Map<String, String[]> getDecisionParameters() {
                return params;
            }

            @Override
            public void decisionMade(String result) throws DecisionResultInvalidException {
            }
        };
    }

    static DecisionSnapshot snapshot(AwaitingDecisionType type,
                                     String text,
                                     Map<String, String[]> params) {
        TraceSnapshots.Input input = new TraceSnapshots.Input();
        input.decisionId = String.valueOf(DECISION_ID);
        input.decisionTypeName = type.name();
        input.decisionText = text;
        input.phase = Phase.DEPLOY;
        input.turn = 5;
        input.currentPlayer = PLAYER;
        input.side = Side.DARK;
        input.noPassParam = booleanValue(params.get("noPass"));
        input.minParam = integerValue(params.get("min"));
        input.maxParam = integerValue(params.get("max"));
        input.actionIds = list(params.get("actionId"));
        input.cardIds = list(params.get("cardId"));
        input.blueprintIds = list(params.get("blueprintId"));
        input.selectable = booleanList(params.get("selectable"));
        input.rawParameters = params;
        TraceSnapshots.Result result = TraceSnapshots.build(input);
        if (result.snapshot() == null) {
            throw new AssertionError("snapshot failed: " + result.issues());
        }
        return result.snapshot();
    }

    static PullFacts facts(AwaitingDecisionType type,
                           String text,
                           Map<String, String[]> params,
                           PullRoute route) {
        PullRouteInput input = input(type, params);
        return PullFacts.parse(snapshot(type, text, params), input, route).value();
    }

    static void put(Map<String, String[]> params, String key, String... values) {
        params.put(key, values);
    }

    private static Map<String, String[]> transaction(DecisionOrigin origin,
                                                     AwaitingDecisionType type) {
        Map<String, String[]> params = new LinkedHashMap<>();
        put(params, DecisionOrigin.WIRE_PARAMETER, origin.name());
        put(params, PullDecisionWire.TRANSACTION_ID, String.valueOf(TRANSACTION_ID));
        put(params, PullDecisionWire.PARENT_DECISION_ID, String.valueOf(PARENT_DECISION_ID));
        put(params, PullDecisionWire.PARENT_ACTION_ORDINAL, String.valueOf(PARENT_ORDINAL));
        put(params, PullDecisionWire.PLAYER_ID, PLAYER);
        put(params, PullDecisionWire.SOURCE_CARD_ID, "902");
        put(params, PullDecisionWire.SOURCE_PERMANENT_CARD_ID, "9002");
        put(params, PullDecisionWire.GAME_TEXT_ACTION_ID, GAME_TEXT_ACTION.name());
        put(params, PullDecisionWire.SOURCE_ZONE, Zone.RESERVE_DECK.name());
        put(params, PullDecisionWire.SOURCE_ZONE_OWNER, PLAYER);
        return params;
    }

    private static List<String> list(String[] values) {
        return values != null ? new ArrayList<>(Arrays.asList(values)) : null;
    }

    private static List<Boolean> booleanList(String[] values) {
        if (values == null) {
            return null;
        }
        List<Boolean> result = new ArrayList<>(values.length);
        for (String value : values) {
            result.add(Boolean.valueOf(value));
        }
        return result;
    }

    private static Boolean booleanValue(String[] values) {
        return values != null && values.length == 1 ? Boolean.valueOf(values[0]) : null;
    }

    private static Integer integerValue(String[] values) {
        return values != null && values.length == 1 ? Integer.valueOf(values[0]) : null;
    }
}
