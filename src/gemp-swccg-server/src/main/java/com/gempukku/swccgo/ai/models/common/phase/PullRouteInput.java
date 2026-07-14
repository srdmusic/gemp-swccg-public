package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.PullDecisionWire;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Verbatim PULL routing fields, including absent versus present-empty arrays. */
public record PullRouteInput(
        int decisionId,
        AwaitingDecisionType decisionType,
        List<String> originValues,
        List<String> actionSemantics,
        List<String> actionIds,
        List<String> cardIds,
        List<String> minimumValues,
        List<String> maximumValues,
        List<String> parentDecisionIds,
        List<String> parentActionOrdinals,
        List<String> transactionIds,
        List<String> playerIds,
        List<String> sourceCardIds,
        List<String> sourcePermanentCardIds,
        List<String> gameTextActionIds,
        List<String> sourceZones,
        List<String> sourceZoneOwners,
        List<String> physicalCardIds,
        List<String> physicalPermanentCardIds,
        List<String> selectedCardIds,
        List<String> selectedPermanentCardIds,
        List<String> destinationCardIds,
        List<String> destinationPermanentCardIds,
        List<String> forcedDestinationCardIds,
        List<String> forcedDestinationPermanentCardIds) {

    public PullRouteInput {
        originValues = immutableNullable(originValues);
        actionSemantics = immutableNullable(actionSemantics);
        actionIds = immutableNullable(actionIds);
        cardIds = immutableNullable(cardIds);
        minimumValues = immutableNullable(minimumValues);
        maximumValues = immutableNullable(maximumValues);
        parentDecisionIds = immutableNullable(parentDecisionIds);
        parentActionOrdinals = immutableNullable(parentActionOrdinals);
        transactionIds = immutableNullable(transactionIds);
        playerIds = immutableNullable(playerIds);
        sourceCardIds = immutableNullable(sourceCardIds);
        sourcePermanentCardIds = immutableNullable(sourcePermanentCardIds);
        gameTextActionIds = immutableNullable(gameTextActionIds);
        sourceZones = immutableNullable(sourceZones);
        sourceZoneOwners = immutableNullable(sourceZoneOwners);
        physicalCardIds = immutableNullable(physicalCardIds);
        physicalPermanentCardIds = immutableNullable(physicalPermanentCardIds);
        selectedCardIds = immutableNullable(selectedCardIds);
        selectedPermanentCardIds = immutableNullable(selectedPermanentCardIds);
        destinationCardIds = immutableNullable(destinationCardIds);
        destinationPermanentCardIds = immutableNullable(destinationPermanentCardIds);
        forcedDestinationCardIds = immutableNullable(forcedDestinationCardIds);
        forcedDestinationPermanentCardIds = immutableNullable(forcedDestinationPermanentCardIds);
    }

    /** Captures only raw decision fields. Prompt text is deliberately excluded. */
    public static PullRouteInput capture(AwaitingDecision decision) {
        Objects.requireNonNull(decision, "decision");
        Map<String, String[]> params = decision.getDecisionParameters();
        return new PullRouteInput(
                decision.getAwaitingDecisionId(),
                decision.getDecisionType(),
                values(params, DecisionOrigin.WIRE_PARAMETER),
                values(params, DecisionActionSemantic.WIRE_PARAMETER),
                values(params, "actionId"),
                values(params, "cardId"),
                values(params, "min"),
                values(params, "max"),
                values(params, PullDecisionWire.PARENT_DECISION_ID),
                values(params, PullDecisionWire.PARENT_ACTION_ORDINAL),
                values(params, PullDecisionWire.TRANSACTION_ID),
                values(params, PullDecisionWire.PLAYER_ID),
                values(params, PullDecisionWire.SOURCE_CARD_ID),
                values(params, PullDecisionWire.SOURCE_PERMANENT_CARD_ID),
                values(params, PullDecisionWire.GAME_TEXT_ACTION_ID),
                values(params, PullDecisionWire.SOURCE_ZONE),
                values(params, PullDecisionWire.SOURCE_ZONE_OWNER),
                values(params, PullDecisionWire.PHYSICAL_CARD_ID),
                values(params, PullDecisionWire.PHYSICAL_PERMANENT_CARD_ID),
                values(params, PullDecisionWire.SELECTED_CARD_ID),
                values(params, PullDecisionWire.SELECTED_PERMANENT_CARD_ID),
                values(params, PullDecisionWire.DESTINATION_CARD_ID),
                values(params, PullDecisionWire.DESTINATION_PERMANENT_CARD_ID),
                values(params, PullDecisionWire.FORCED_DESTINATION_CARD_ID),
                values(params, PullDecisionWire.FORCED_DESTINATION_PERMANENT_CARD_ID));
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
