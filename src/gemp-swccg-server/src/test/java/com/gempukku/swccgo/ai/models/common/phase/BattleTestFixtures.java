package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.TraceSnapshots;
import com.gempukku.swccgo.common.BattleDecisionWire;
import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BattleTestFixtures {
    static final String PLAYER = "dark-player";
    static final int DECISION_ID = 71;

    private BattleTestFixtures() {
    }

    static Map<String, String[]> initiation() {
        Map<String, String[]> params = params(DecisionOrigin.PHASE_ACTION);
        put(params, "actionId", "0", "1", "2");
        put(params, "actionText", "Pass", "Initiate battle", "Use game text");
        put(params, "cardId", "100", "301", "302");
        put(params, DecisionActionSemantic.WIRE_PARAMETER,
                DecisionActionSemantic.UNKNOWN.name(),
                DecisionActionSemantic.BATTLE_INITIATE.name(),
                DecisionActionSemantic.UNKNOWN.name());
        put(params, "noPass", "false");
        return params;
    }

    static Map<String, String[]> fire() {
        Map<String, String[]> params = params(DecisionOrigin.BATTLE_ACTION);
        put(params, "actionId", "0", "1");
        put(params, "actionText", "Fire weapon", "Use battle action");
        put(params, "cardId", "401", "402");
        put(params, DecisionActionSemantic.WIRE_PARAMETER,
                DecisionActionSemantic.BATTLE_FIRE.name(),
                DecisionActionSemantic.UNKNOWN.name());
        put(params, "noPass", "false");
        return params;
    }

    static Map<String, String[]> tactic() {
        Map<String, String[]> params = fire();
        put(params, DecisionActionSemantic.WIRE_PARAMETER,
                DecisionActionSemantic.UNKNOWN.name(),
                DecisionActionSemantic.UNKNOWN.name());
        return params;
    }

    static Map<String, String[]> forfeit(boolean optional) {
        Map<String, String[]> params = params(DecisionOrigin.BATTLE_FORFEIT);
        put(params, "cardId", "501", "502");
        put(params, "min", optional ? "0" : "1");
        put(params, "max", "1");
        put(params, BattleDecisionWire.OPTIONAL_IMMUNE_FORFEIT,
                String.valueOf(optional));
        return params;
    }

    static Map<String, String[]> power() {
        Map<String, String[]> params = params(DecisionOrigin.BATTLE_POWER);
        put(params, "results", "Yes", "No");
        return params;
    }

    static Map<String, String[]> destinySelection() {
        Map<String, String[]> params = params(DecisionOrigin.BATTLE_DESTINY_SELECTION);
        put(params, "cardId", "temp0", "temp1", "temp2");
        put(params, "selectable", "true", "true", "true");
        put(params, "preselected", "false", "false", "false");
        put(params, "returnAnyChange", "false");
        put(params, "min", "1");
        put(params, "max", "2");
        return params;
    }

    static Map<String, String[]> actionChoice() {
        Map<String, String[]> params = new LinkedHashMap<>();
        put(params, "actionId", "0", "1");
        put(params, "noPass", "false");
        return params;
    }

    static Map<String, String[]> integer() {
        Map<String, String[]> params = new LinkedHashMap<>();
        put(params, "min", "1");
        put(params, "max", "5");
        put(params, "default", "3");
        return params;
    }

    static Map<String, String[]> empty() {
        return new LinkedHashMap<>();
    }

    static AwaitingDecision decision(AwaitingDecisionType type,
                                     Map<String, String[]> params) {
        return new AwaitingDecision() {
            @Override
            public int getAwaitingDecisionId() {
                return DECISION_ID;
            }

            @Override
            public String getText() {
                return "prompt text cannot establish BATTLE ownership";
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

    static BattleRouteInput input(AwaitingDecisionType type,
                                  Map<String, String[]> params) {
        return BattleRouteInput.capture(
                Phase.BATTLE, decision(type, params));
    }

    static DecisionSnapshot snapshot(AwaitingDecisionType type,
                                     Map<String, String[]> params) {
        TraceSnapshots.Input input = new TraceSnapshots.Input();
        input.producerId = "battle-fixture";
        input.decisionId = String.valueOf(DECISION_ID);
        input.decisionTypeName = type.name();
        input.decisionText = "prompt text cannot establish BATTLE ownership";
        input.phase = Phase.BATTLE;
        input.turn = 8;
        input.currentPlayer = PLAYER;
        input.side = Side.DARK;
        input.noPassParam = booleanValue(params.get("noPass"));
        input.minParam = integerValue(params.get("min"));
        input.maxParam = integerValue(params.get("max"));
        input.actionIds = list(params.get("actionId"));
        input.actionTexts = list(params.get("actionText"));
        input.cardIds = list(params.get("cardId"));
        input.blueprintIds = list(params.get("blueprintId"));
        input.multipleChoiceResults = list(params.get("results"));
        input.selectable = booleanList(params.get("selectable"));
        input.rawParameters = params;
        TraceSnapshots.Result result = TraceSnapshots.build(input);
        if (result.snapshot() == null) {
            throw new AssertionError("snapshot failed: " + result.issues());
        }
        return result.snapshot();
    }

    static BattleFacts facts(AwaitingDecisionType type,
                             Map<String, String[]> params,
                             BattleWindowRoute route) {
        return BattleFacts.parse(
                snapshot(type, params), input(type, params), route).value();
    }

    static void put(Map<String, String[]> params, String key, String... values) {
        params.put(key, values);
    }

    private static Map<String, String[]> params(DecisionOrigin origin) {
        Map<String, String[]> params = new LinkedHashMap<>();
        put(params, DecisionOrigin.WIRE_PARAMETER, origin.name());
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
