package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.DeployDecisionWire;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Verbatim DEPLOY route inputs captured from one engine decision. */
public record DeployRouteInput(
        Phase phase,
        int decisionId,
        AwaitingDecisionType decisionType,
        List<String> originValues,
        List<String> actionIds,
        List<String> actionSemantics,
        List<String> attemptIds,
        List<String> playerIds,
        List<String> sourceCardIds,
        List<String> sourcePermanentCardIds,
        List<String> sourceZones,
        List<String> destinationLegalityKnown,
        List<String> legalDestinations,
        List<String> legalBuddies,
        List<String> selectedBuddy,
        List<String> parentDecisionIds,
        List<String> parentActionOrdinals,
        List<String> cardIds,
        List<String> destinationCardIds,
        List<String> destinationPermanentCardIds,
        List<String> buddyCardIds,
        List<String> buddyPermanentCardIds,
        List<String> selectedBuddyCardIds,
        List<String> selectedBuddyPermanentCardIds,
        List<String> forcedDestination,
        List<String> results,
        List<String> minimumValues,
        List<String> maximumValues,
        List<String> selectableValues) {

    public static DeployRouteInput capture(Phase phase, AwaitingDecision decision) {
        Map<String, String[]> params = decision.getDecisionParameters();
        return new DeployRouteInput(
                phase,
                decision.getAwaitingDecisionId(),
                decision.getDecisionType(),
                values(params, DecisionOrigin.WIRE_PARAMETER),
                values(params, "actionId"),
                values(params, DecisionActionSemantic.WIRE_PARAMETER),
                values(params, DeployDecisionWire.ATTEMPT_ID),
                values(params, DeployDecisionWire.PLAYER_ID),
                values(params, DeployDecisionWire.SOURCE_CARD_ID),
                values(params, DeployDecisionWire.SOURCE_PERMANENT_CARD_ID),
                values(params, DeployDecisionWire.SOURCE_ZONE),
                values(params, DeployDecisionWire.DESTINATION_LEGALITY_KNOWN),
                values(params, DeployDecisionWire.LEGAL_DESTINATIONS),
                values(params, DeployDecisionWire.LEGAL_BUDDIES),
                values(params, DeployDecisionWire.SELECTED_BUDDY),
                values(params, DeployDecisionWire.PARENT_DECISION_ID),
                values(params, DeployDecisionWire.PARENT_ACTION_ORDINAL),
                values(params, "cardId"),
                values(params, DeployDecisionWire.DESTINATION_CARD_ID),
                values(params, DeployDecisionWire.DESTINATION_PERMANENT_CARD_ID),
                values(params, DeployDecisionWire.BUDDY_CARD_ID),
                values(params, DeployDecisionWire.BUDDY_PERMANENT_CARD_ID),
                values(params, DeployDecisionWire.SELECTED_BUDDY_CARD_ID),
                values(params, DeployDecisionWire.SELECTED_BUDDY_PERMANENT_CARD_ID),
                values(params, DeployDecisionWire.FORCED_DESTINATION),
                values(params, "results"),
                values(params, "min"),
                values(params, "max"),
                values(params, "selectable"));
    }

    private static List<String> values(Map<String, String[]> params, String key) {
        if (params == null || !params.containsKey(key)) {
            return null;
        }
        String[] raw = params.get(key);
        return raw != null ? List.copyOf(Arrays.asList(raw)) : null;
    }
}
