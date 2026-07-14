package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Immutable raw decision fields used by the pure DRAW route resolver. */
public record DrawRouteInput(
        Phase phase,
        DecisionOrigin origin,
        AwaitingDecisionType decisionType,
        Boolean yourTurn,
        List<String> actionIds,
        List<String> cardIds,
        List<String> blueprintIds,
        List<String> actionTexts,
        List<String> testingTexts,
        List<String> backSideTestingTexts,
        List<String> horizontals,
        List<String> actionSemantics) {

    public DrawRouteInput {
        actionIds = immutableNullable(actionIds);
        cardIds = immutableNullable(cardIds);
        blueprintIds = immutableNullable(blueprintIds);
        actionTexts = immutableNullable(actionTexts);
        testingTexts = immutableNullable(testingTexts);
        backSideTestingTexts = immutableNullable(backSideTestingTexts);
        horizontals = immutableNullable(horizontals);
        actionSemantics = immutableNullable(actionSemantics);
    }

    /** Capture only raw decision parameters. No game or strategy reads occur here. */
    public static DrawRouteInput capture(Phase phase, AwaitingDecision decision) {
        Map<String, String[]> params = decision != null ? decision.getDecisionParameters() : null;
        return new DrawRouteInput(
                phase,
                DecisionOrigin.fromWire(single(params, DecisionOrigin.WIRE_PARAMETER)),
                decision != null ? decision.getDecisionType() : null,
                parseBoolean(single(params, "yourTurn")),
                values(params, "actionId"),
                values(params, "cardId"),
                values(params, "blueprintId"),
                values(params, "actionText"),
                values(params, "testingText"),
                values(params, "backSideTestingText"),
                values(params, "horizontal"),
                values(params, DecisionActionSemantic.WIRE_PARAMETER));
    }

    private static String single(Map<String, String[]> params, String key) {
        if (params == null) {
            return null;
        }
        String[] raw = params.get(key);
        return raw != null && raw.length == 1 ? raw[0] : null;
    }

    private static Boolean parseBoolean(String raw) {
        if ("true".equalsIgnoreCase(raw)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(raw)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static List<String> values(Map<String, String[]> params, String key) {
        if (params == null || !params.containsKey(key)) {
            return null;
        }
        String[] raw = params.get(key);
        if (raw == null) {
            return Collections.emptyList();
        }
        List<String> copy = new ArrayList<>(raw.length);
        Collections.addAll(copy, raw);
        return copy;
    }

    private static List<String> immutableNullable(List<String> values) {
        return values == null
                ? null
                : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
