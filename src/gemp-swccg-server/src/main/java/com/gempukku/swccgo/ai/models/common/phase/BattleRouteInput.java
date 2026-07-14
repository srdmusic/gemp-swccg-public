package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.BattleDecisionWire;
import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Verbatim routing inputs for one possible BATTLE-owned decision. */
public record BattleRouteInput(
        Phase phase,
        int decisionId,
        AwaitingDecisionType decisionType,
        List<String> originValues,
        List<String> actionIds,
        List<String> actionSemantics,
        List<String> cardIds,
        List<String> results,
        List<String> optionalImmuneForfeitValues) {

    public static BattleRouteInput capture(Phase phase, AwaitingDecision decision) {
        Map<String, String[]> params = decision.getDecisionParameters();
        return new BattleRouteInput(
                phase,
                decision.getAwaitingDecisionId(),
                decision.getDecisionType(),
                values(params, DecisionOrigin.WIRE_PARAMETER),
                values(params, "actionId"),
                values(params, DecisionActionSemantic.WIRE_PARAMETER),
                values(params, "cardId"),
                values(params, "results"),
                values(params, BattleDecisionWire.OPTIONAL_IMMUNE_FORFEIT));
    }

    private static List<String> values(Map<String, String[]> params, String key) {
        if (params == null || !params.containsKey(key)) {
            return null;
        }
        String[] values = params.get(key);
        if (values == null) {
            return null;
        }
        return Collections.unmodifiableList(
                new ArrayList<>(Arrays.asList(values)));
    }
}
