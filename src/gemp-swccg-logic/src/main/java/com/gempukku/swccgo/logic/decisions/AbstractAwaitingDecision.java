package com.gempukku.swccgo.logic.decisions;

import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.DeployActionMetadata;
import com.gempukku.swccgo.common.DeployDecisionWire;
import com.gempukku.swccgo.common.DeployDestinationRef;
import com.gempukku.swccgo.common.DeployPhysicalCardRef;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * The abstract class that defines the based implementation for a decision a player need to make.
 */
public abstract class AbstractAwaitingDecision implements AwaitingDecision {
    private final int _id;
    private final String _text;
    private final AwaitingDecisionType _decisionType;
    private final Map<String, String[]> _params = new HashMap<>();

    /**
     * Creates an awaiting decision of the specified type, and with the specified id and text.
     * @param id the id
     * @param text the text to show the player making the decision
     * @param decisionType the decision type
     */
    public AbstractAwaitingDecision(int id, String text, AwaitingDecisionType decisionType) {
        _id = id;
        _text = text;
        _decisionType = decisionType;
    }

    /**
     * Sets the specified parameter to the specified string value.
     * @param name the parameter name
     * @param value the value
     */
    protected void setParam(String name, String value) {
        _params.put(name, new String[] {value});
    }

    /**
     * Sets the specified parameter to the specified string array value.
     * @param name the parameter name
     * @param value the value
     */
    protected void setParam(String name, String[] value) {
        _params.put(name, value);
    }

    // ═══ ACTIVATE/CONTROL Option 2 decision-origin seam (2026-07-13) ═══
    // Packet: Handoffs/CODEX_ACTIVATE_CONTROL_PHASE_PACKET_2026-07-13.md §1.
    // The minimum stamp helper: it stamps the engine-owned DecisionOrigin as the
    // single "decisionOrigin" wire parameter. protected + final so only decision
    // creation sites (including anonymous subclasses via instance-initializer
    // blocks) stamp an origin, and no arbitrary public parameter mutation is
    // exposed. Nothing in a live decide() path reads this in the shadow phase.
    /**
     * Stamps the engine-owned decision origin onto this decision.
     * @param origin the origin of this decision
     */
    protected final void setDecisionOrigin(DecisionOrigin origin) {
        setParam(DecisionOrigin.WIRE_PARAMETER, origin.name());
    }

    /** Stamps exact DEPLOY transaction identity at an engine decision boundary. */
    protected final void setDeployTransactionMetadata(Action action,
                                                      DecisionOrigin origin,
                                                      Collection<PhysicalCard> destinations,
                                                      boolean forcedDestination) {
        if (!hasDeployMetadata(action)) {
            return;
        }

        DeployActionMetadata prior = action.getDeployActionMetadata();
        List<DeployDestinationRef> destinationRefs = List.copyOf(destinations).stream()
                .map(card -> (DeployDestinationRef) new DeployDestinationRef.Card(
                        new DeployPhysicalCardRef(
                                card.getPermanentCardId(), card.getCardId())))
                .toList();
        DeployActionMetadata metadata = prior.withDestinations(destinationRefs);
        if (prior.forcedDestination()
                && prior.orderedLegalDestinations().equals(destinationRefs)) {
            metadata = metadata.withForcedDestination(true);
        }
        action.setDeployActionMetadata(metadata);
        writeDeployTransactionMetadata(
                action, origin, forcedDestination || prior.forcedDestination());
    }

    /** Stamps exact candidate identities for a simultaneous deploy buddy choice. */
    protected final void setDeployBuddyMetadata(Action action,
                                                Collection<PhysicalCard> buddies) {
        if (!hasDeployMetadata(action)) {
            return;
        }
        List<DeployPhysicalCardRef> buddyRefs = List.copyOf(buddies).stream()
                .map(card -> new DeployPhysicalCardRef(
                        card.getPermanentCardId(), card.getCardId()))
                .toList();
        action.setDeployActionMetadata(
                action.getDeployActionMetadata().withBuddyCandidates(buddyRefs));
        DecisionOrigin origin = getDecisionType() == AwaitingDecisionType.ARBITRARY_CARDS
                ? DecisionOrigin.DEPLOY_BUDDY_ARBITRARY
                : DecisionOrigin.DEPLOY_BUDDY;
        writeDeployTransactionMetadata(action, origin, false);
    }

    /** Stamps an exact deploy confirmation that precedes destination selection. */
    protected final void setDeployConfirmationMetadata(Action action) {
        if (hasDeployMetadata(action)) {
            writeDeployTransactionMetadata(
                    action, DecisionOrigin.DEPLOY_CONFIRMATION, false);
        }
    }

    private static boolean hasDeployMetadata(Action action) {
        return action != null
                && action.getDecisionActionSemantic() == DecisionActionSemantic.DEPLOY_CARD
                && action.getDeployAttemptId() != null
                && action.getDeployActionMetadata() != null;
    }

    @Override
    public final void refreshDeployTransactionMetadata(Action action) {
        String[] rawOrigins = _params.get(DecisionOrigin.WIRE_PARAMETER);
        if (rawOrigins == null || rawOrigins.length != 1) {
            return;
        }
        DecisionOrigin origin = DecisionOrigin.fromWire(rawOrigins[0]);
        if (origin == null || !origin.name().startsWith("DEPLOY_")
                || !hasDeployMetadata(action)) {
            return;
        }
        String[] rawForced = _params.get(DeployDecisionWire.FORCED_DESTINATION);
        boolean forced = rawForced != null && rawForced.length == 1
                && Boolean.parseBoolean(rawForced[0]);
        writeDeployTransactionMetadata(action, origin, forced);
    }

    private void writeDeployTransactionMetadata(Action action,
                                                DecisionOrigin origin,
                                                boolean forcedDestination) {
        DeployActionMetadata metadata = action.getDeployActionMetadata();

        setDecisionOrigin(origin);
        setParam(DeployDecisionWire.ATTEMPT_ID, action.getDeployAttemptId());
        if (action.getAcceptedDecisionId() != null
                && action.getAcceptedDecisionOrdinal() != null) {
            setParam(DeployDecisionWire.PARENT_DECISION_ID,
                    String.valueOf(action.getAcceptedDecisionId()));
            setParam(DeployDecisionWire.PARENT_ACTION_ORDINAL,
                    String.valueOf(action.getAcceptedDecisionOrdinal()));
        }
        if (action.getPerformingPlayer() != null) {
            setParam(DeployDecisionWire.PLAYER_ID, action.getPerformingPlayer());
        }
        setParam(DeployDecisionWire.SOURCE_CARD_ID,
                String.valueOf(metadata.sourceCard().currentCardId()));
        setParam(DeployDecisionWire.SOURCE_PERMANENT_CARD_ID,
                String.valueOf(metadata.sourceCard().permanentCardId()));
        if (metadata.sourceZone() != null) {
            setParam(DeployDecisionWire.SOURCE_ZONE, metadata.sourceZone().name());
        }
        setParam(DeployDecisionWire.DESTINATION_LEGALITY_KNOWN,
                String.valueOf(metadata.destinationLegalityKnown()));
        setParam(DeployDecisionWire.DESTINATION_CARD_ID,
                metadata.orderedLegalDestinations().stream()
                        .filter(DeployDestinationRef.Card.class::isInstance)
                        .map(DeployDestinationRef.Card.class::cast)
                        .map(card -> String.valueOf(card.card().currentCardId()))
                        .toArray(String[]::new));
        setParam(DeployDecisionWire.DESTINATION_PERMANENT_CARD_ID,
                metadata.orderedLegalDestinations().stream()
                        .filter(DeployDestinationRef.Card.class::isInstance)
                        .map(DeployDestinationRef.Card.class::cast)
                        .map(card -> String.valueOf(card.card().permanentCardId()))
                        .toArray(String[]::new));
        setParam(DeployDecisionWire.BUDDY_CARD_ID,
                metadata.orderedLegalBuddies().stream()
                        .map(card -> String.valueOf(card.currentCardId()))
                        .toArray(String[]::new));
        setParam(DeployDecisionWire.BUDDY_PERMANENT_CARD_ID,
                metadata.orderedLegalBuddies().stream()
                        .map(card -> String.valueOf(card.permanentCardId()))
                        .toArray(String[]::new));
        if (metadata.selectedBuddy() != null) {
            setParam(DeployDecisionWire.SELECTED_BUDDY_CARD_ID,
                    String.valueOf(metadata.selectedBuddy().currentCardId()));
            setParam(DeployDecisionWire.SELECTED_BUDDY_PERMANENT_CARD_ID,
                    String.valueOf(metadata.selectedBuddy().permanentCardId()));
        }
        setParam(DeployDecisionWire.FORCED_DESTINATION,
                String.valueOf(forcedDestination));
    }

    @Override
    public int getAwaitingDecisionId() {
        return _id;
    }

    @Override
    public String getText() {
        return _text;
    }

    @Override
    public AwaitingDecisionType getDecisionType() {
        return _decisionType;
    }

    @Override
    public Map<String, String[]> getDecisionParameters() {
        return _params;
    }
}
