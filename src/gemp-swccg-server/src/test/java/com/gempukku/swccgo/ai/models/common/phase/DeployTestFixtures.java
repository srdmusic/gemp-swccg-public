package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.TraceSnapshots;
import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.DeployDecisionWire;
import com.gempukku.swccgo.common.Phase;
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

final class DeployTestFixtures {
    static final String PLAYER = "darkPlayer";
    static final int DECISION_ID = 51;
    static final int PARENT_DECISION_ID = 51;
    static final int PARENT_ORDINAL = 1;
    static final String ATTEMPT = "DEPLOY-test-opaque-1";

    private DeployTestFixtures() {
    }

    static Map<String, String[]> parent(boolean forcedDestination,
                                        boolean selectedBuddy) {
        Map<String, String[]> params = new LinkedHashMap<>();
        put(params, DecisionOrigin.WIRE_PARAMETER, DecisionOrigin.PHASE_ACTION.name());
        put(params, "actionId", "0", "1");
        put(params, "cardId", "501", "502");
        put(params, "blueprintId", "1_1", "2_2");
        put(params, DecisionActionSemantic.WIRE_PARAMETER,
                DecisionActionSemantic.UNKNOWN.name(),
                DecisionActionSemantic.DEPLOY_CARD.name());
        put(params, DeployDecisionWire.ATTEMPT_ID, "", ATTEMPT);
        put(params, DeployDecisionWire.PLAYER_ID, "", PLAYER);
        put(params, DeployDecisionWire.SOURCE_CARD_ID, "", "102");
        put(params, DeployDecisionWire.SOURCE_PERMANENT_CARD_ID, "", "1002");
        put(params, DeployDecisionWire.SOURCE_ZONE, "", Zone.HAND.name());
        put(params, DeployDecisionWire.DESTINATION_LEGALITY_KNOWN, "", "true");
        put(params, DeployDecisionWire.LEGAL_DESTINATIONS, "",
                forcedDestination ? "CARD:3001:301" : "CARD:3001:301,CARD:3002:302");
        put(params, DeployDecisionWire.LEGAL_BUDDIES, "", "4001:401,4002:402");
        put(params, DeployDecisionWire.SELECTED_BUDDY, "",
                selectedBuddy ? "4002:402" : "");
        put(params, "noPass", "false");
        return params;
    }

    static Map<String, String[]> destination(boolean forced) {
        Map<String, String[]> params = child(
                DecisionOrigin.DEPLOY_DESTINATION, true, forced);
        put(params, "cardId", "301", "302");
        put(params, DeployDecisionWire.DESTINATION_CARD_ID, "301", "302");
        put(params, DeployDecisionWire.DESTINATION_PERMANENT_CARD_ID, "3001", "3002");
        put(params, "min", "0");
        put(params, "max", "1");
        return params;
    }

    static Map<String, String[]> buddy(AwaitingDecisionType type) {
        DecisionOrigin origin = type == AwaitingDecisionType.ARBITRARY_CARDS
                ? DecisionOrigin.DEPLOY_BUDDY_ARBITRARY
                : DecisionOrigin.DEPLOY_BUDDY;
        Map<String, String[]> params = child(origin, false, false);
        put(params, "min", "0");
        put(params, "max", "1");
        if (type == AwaitingDecisionType.ARBITRARY_CARDS) {
            put(params, "cardId", "temp0", "temp1", "temp2");
            put(params, DeployDecisionWire.BUDDY_CARD_ID, "400", "401", "402");
            put(params, DeployDecisionWire.BUDDY_PERMANENT_CARD_ID,
                    "4000", "4001", "4002");
            put(params, "selectable", "false", "true", "true");
            put(params, "preselected", "false", "false", "false");
            put(params, "returnAnyChange", "false");
        } else {
            put(params, "cardId", "401", "402");
            put(params, DeployDecisionWire.BUDDY_CARD_ID, "401", "402");
            put(params, DeployDecisionWire.BUDDY_PERMANENT_CARD_ID, "4001", "4002");
        }
        return params;
    }

    static Map<String, String[]> choice(DecisionOrigin origin, boolean forced) {
        Map<String, String[]> params = child(origin, true, forced);
        put(params, DeployDecisionWire.DESTINATION_CARD_ID, "301");
        put(params, DeployDecisionWire.DESTINATION_PERMANENT_CARD_ID, "3001");
        put(params, "results", "Yes", "No");
        return params;
    }

    static Map<String, String[]> confirmation() {
        Map<String, String[]> params = child(
                DecisionOrigin.DEPLOY_CONFIRMATION, false, false);
        put(params, "results", "Yes", "No");
        return params;
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

    static DeployRouteInput input(AwaitingDecisionType type,
                                  Map<String, String[]> params) {
        return DeployRouteInput.capture(
                Phase.DEPLOY, decision(type, "irrelevant prompt", params));
    }

    static DecisionSnapshot snapshot(AwaitingDecisionType type,
                                     Map<String, String[]> params) {
        TraceSnapshots.Input input = new TraceSnapshots.Input();
        input.decisionId = String.valueOf(DECISION_ID);
        input.decisionTypeName = type.name();
        input.decisionText = "irrelevant prompt";
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

    static DeployFacts facts(AwaitingDecisionType type,
                             Map<String, String[]> params,
                             DeployRoute route) {
        DeployRouteInput input = input(type, params);
        return DeployFacts.parse(snapshot(type, params), input, route, null).value();
    }

    static void put(Map<String, String[]> params, String key, String... values) {
        params.put(key, values);
    }

    private static Map<String, String[]> child(DecisionOrigin origin,
                                               boolean destinationsKnown,
                                               boolean forced) {
        Map<String, String[]> params = new LinkedHashMap<>();
        put(params, DecisionOrigin.WIRE_PARAMETER, origin.name());
        put(params, DeployDecisionWire.ATTEMPT_ID, ATTEMPT);
        put(params, DeployDecisionWire.PARENT_DECISION_ID,
                String.valueOf(PARENT_DECISION_ID));
        put(params, DeployDecisionWire.PARENT_ACTION_ORDINAL,
                String.valueOf(PARENT_ORDINAL));
        put(params, DeployDecisionWire.PLAYER_ID, PLAYER);
        put(params, DeployDecisionWire.SOURCE_CARD_ID, "102");
        put(params, DeployDecisionWire.SOURCE_PERMANENT_CARD_ID, "1002");
        put(params, DeployDecisionWire.SOURCE_ZONE, Zone.HAND.name());
        put(params, DeployDecisionWire.DESTINATION_LEGALITY_KNOWN,
                String.valueOf(destinationsKnown));
        put(params, DeployDecisionWire.DESTINATION_CARD_ID);
        put(params, DeployDecisionWire.DESTINATION_PERMANENT_CARD_ID);
        put(params, DeployDecisionWire.BUDDY_CARD_ID);
        put(params, DeployDecisionWire.BUDDY_PERMANENT_CARD_ID);
        put(params, DeployDecisionWire.FORCED_DESTINATION, String.valueOf(forced));
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
        return values != null && values.length == 1
                ? Boolean.valueOf(values[0]) : null;
    }

    private static Integer integerValue(String[] values) {
        return values != null && values.length == 1
                ? Integer.valueOf(values[0]) : null;
    }
}
