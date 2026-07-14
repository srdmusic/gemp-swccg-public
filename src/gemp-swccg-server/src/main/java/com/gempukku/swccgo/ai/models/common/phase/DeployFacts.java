package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.decision.ActionFacts;
import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.decision.FactValue;
import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DeployDecisionWire;
import com.gempukku.swccgo.common.DeployDestinationRef;
import com.gempukku.swccgo.common.DeployPhysicalCardRef;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable physical and response facts for one owned DEPLOY route. */
public record DeployFacts(
        String decisionId,
        int turn,
        String playerId,
        Phase phase,
        DeployRoute route,
        List<ParentCandidate> parentCandidates,
        String attemptId,
        Integer parentDecisionId,
        Integer parentActionOrdinal,
        DeployPhysicalCardRef sourceCard,
        Zone sourceZone,
        List<DeployPhysicalCardRef> orderedDestinationCards,
        List<DeployPhysicalCardRef> orderedBuddyCards,
        List<String> orderedBuddyWireIds,
        DeployPhysicalCardRef selectedBuddy,
        boolean forcedDestination,
        List<String> results,
        FactValue<Float> opponentActiveDrain) {

    private static final String PRODUCER = "deploy-facts";

    public DeployFacts {
        requireNonBlank(decisionId, "decisionId");
        if (turn < 0) {
            throw new IllegalArgumentException("turn must be >= 0");
        }
        requireNonBlank(playerId, "playerId");
        Objects.requireNonNull(route, "route");
        if (route == DeployRoute.LEGACY_UNOWNED) {
            throw new IllegalArgumentException("LEGACY_UNOWNED has no DeployFacts");
        }
        parentCandidates = List.copyOf(parentCandidates);
        orderedDestinationCards = List.copyOf(orderedDestinationCards);
        orderedBuddyCards = List.copyOf(orderedBuddyCards);
        orderedBuddyWireIds = List.copyOf(orderedBuddyWireIds);
        results = List.copyOf(results);
        Objects.requireNonNull(opponentActiveDrain, "opponentActiveDrain");
        validateUniqueDestinations(orderedDestinationCards);
        validateUniqueDestinations(orderedBuddyCards);
        if (selectedBuddy != null && !orderedBuddyCards.contains(selectedBuddy)) {
            throw new IllegalArgumentException("selected buddy is not an offered physical card");
        }

        if (route == DeployRoute.DEPLOY_PARENT) {
            if (parentCandidates.isEmpty() || attemptId != null || sourceCard != null
                    || sourceZone != null || parentDecisionId != null
                    || parentActionOrdinal != null || !orderedDestinationCards.isEmpty()
                    || !orderedBuddyCards.isEmpty() || !orderedBuddyWireIds.isEmpty()
                    || selectedBuddy != null
                    || !results.isEmpty()) {
                throw new IllegalArgumentException("DEPLOY_PARENT carries candidate facts only");
            }
        } else {
            requireNonBlank(attemptId, "attemptId");
            Objects.requireNonNull(parentDecisionId, "parentDecisionId");
            Objects.requireNonNull(parentActionOrdinal, "parentActionOrdinal");
            Objects.requireNonNull(sourceCard, "sourceCard");
            Objects.requireNonNull(sourceZone, "sourceZone");
            if (!parentCandidates.isEmpty()) {
                throw new IllegalArgumentException("DEPLOY child requires one exact transaction");
            }
            if (route == DeployRoute.DEPLOY_BUDDY) {
                if (!orderedDestinationCards.isEmpty() || orderedBuddyCards.isEmpty()
                        || orderedBuddyWireIds.size() != orderedBuddyCards.size()) {
                    throw new IllegalArgumentException(
                            "DEPLOY buddy child requires exact buddies and no destinations");
                }
            } else if (route == DeployRoute.DEPLOY_CONFIRMATION) {
                if (!orderedDestinationCards.isEmpty()) {
                    throw new IllegalArgumentException(
                            "DEPLOY confirmation precedes destination selection");
                }
            } else if (orderedDestinationCards.isEmpty()) {
                throw new IllegalArgumentException(
                        "DEPLOY destination or choice requires exact destinations");
            }
            if ((route == DeployRoute.DEPLOY_V170_UNDERCOVER
                    || route == DeployRoute.DEPLOY_CAPACITY
                    || route == DeployRoute.DEPLOY_CONFIRMATION)
                    && results.size() < 2) {
                throw new IllegalArgumentException("DEPLOY choice requires ordered results");
            }
        }
    }

    public record ParentCandidate(
            int ordinal,
            String actionWireId,
            String attemptId,
            DeployPhysicalCardRef sourceCard,
            Zone sourceZone,
            boolean destinationLegalityKnown,
            List<DeployDestinationRef> orderedDestinations,
            List<DeployPhysicalCardRef> orderedBuddyCandidates,
            DeployPhysicalCardRef selectedBuddy,
            FactValue<Float> deployCost) {

        public ParentCandidate {
            if (ordinal < 0) {
                throw new IllegalArgumentException("ordinal must be >= 0");
            }
            requireNonBlank(actionWireId, "actionWireId");
            requireNonBlank(attemptId, "attemptId");
            Objects.requireNonNull(sourceCard, "sourceCard");
            Objects.requireNonNull(sourceZone, "sourceZone");
            orderedDestinations = List.copyOf(orderedDestinations);
            orderedBuddyCandidates = List.copyOf(orderedBuddyCandidates);
            Objects.requireNonNull(deployCost, "deployCost");
            if (!destinationLegalityKnown && !orderedDestinations.isEmpty()) {
                throw new IllegalArgumentException("unknown legality cannot carry destinations");
            }
            if (selectedBuddy != null && !orderedBuddyCandidates.contains(selectedBuddy)) {
                throw new IllegalArgumentException(
                        "selected buddy must be one of the parent candidates");
            }
        }
    }

    public static FactValue<DeployFacts> parse(DecisionSnapshot snapshot,
                                               DeployRouteInput input,
                                               DeployRoute route,
                                               SwccgGame game) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(route, "route");
        try {
            if (route == DeployRoute.LEGACY_UNOWNED
                    || DeployRouteResolver.resolve(input) != route) {
                return unknown("route is unowned or does not match complete DEPLOY metadata");
            }
            if (!snapshot.decisionFacts().decisionId().equals(String.valueOf(input.decisionId()))
                    || snapshot.decisionFacts().decisionType() != input.decisionType()
                    || !Objects.equals(snapshot.decisionFacts().phase(), input.phase())
                    || !rawMatches(snapshot, input)) {
                return unknown("snapshot and DEPLOY route input do not preserve one raw decision");
            }

            String player = snapshot.decisionFacts().currentPlayer();
            List<ParentCandidate> parentCandidates = List.of();
            String attemptId = null;
            Integer parentDecisionId = null;
            Integer parentActionOrdinal = null;
            DeployPhysicalCardRef sourceCard = null;
            Zone sourceZone = null;
            List<DeployPhysicalCardRef> destinations = List.of();
            List<DeployPhysicalCardRef> buddies = List.of();
            List<String> buddyWireIds = List.of();
            DeployPhysicalCardRef selectedBuddy = null;
            boolean forced = false;
            List<String> results = input.results() != null ? input.results() : List.of();

            if (route == DeployRoute.DEPLOY_PARENT) {
                parentCandidates = parseParentCandidates(snapshot, input, player);
            } else {
                if (!player.equals(single(input.playerIds()))) {
                    return unknown("DEPLOY transaction player does not match current player");
                }
                attemptId = singleRequired(input.attemptIds());
                parentDecisionId = parseInteger(input.parentDecisionIds());
                parentActionOrdinal = parseInteger(input.parentActionOrdinals());
                sourceCard = parseCard(
                        singleRequired(input.sourcePermanentCardIds()),
                        singleRequired(input.sourceCardIds()));
                sourceZone = Zone.valueOf(singleRequired(input.sourceZones()));
                destinations = parseCards(input.destinationPermanentCardIds(),
                        input.destinationCardIds());
                if (route == DeployRoute.DEPLOY_BUDDY) {
                    BuddyCandidates parsedBuddies = parseBuddyCandidates(input);
                    buddies = parsedBuddies.cards();
                    buddyWireIds = parsedBuddies.wireIds();
                } else {
                    buddies = parseCards(input.buddyPermanentCardIds(),
                            input.buddyCardIds());
                }
                if (input.selectedBuddyCardIds() != null
                        || input.selectedBuddyPermanentCardIds() != null) {
                    selectedBuddy = parseCard(
                            singleRequired(input.selectedBuddyPermanentCardIds()),
                            singleRequired(input.selectedBuddyCardIds()));
                }
                forced = Boolean.parseBoolean(singleRequired(input.forcedDestination()));
            }

            FactValue<Float> drain = route == DeployRoute.DEPLOY_V170_UNDERCOVER
                    ? opponentActiveDrain(game, player)
                    : FactValue.unknown(PRODUCER, "opponent active Force drain",
                            "this DEPLOY route does not consume opponent drain");
            return FactValue.known(new DeployFacts(
                    snapshot.decisionFacts().decisionId(),
                    snapshot.decisionFacts().turn(),
                    player,
                    snapshot.decisionFacts().phase(),
                    route,
                    parentCandidates,
                    attemptId,
                    parentDecisionId,
                    parentActionOrdinal,
                    sourceCard,
                    sourceZone,
                    destinations,
                    buddies,
                    buddyWireIds,
                    selectedBuddy,
                    forced,
                    results,
                    drain), PRODUCER, "DeployRouteInput + DecisionSnapshot");
        } catch (RuntimeException e) {
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return unknown("DEPLOY fact parsing failed: " + detail);
        }
    }

    private static List<ParentCandidate> parseParentCandidates(DecisionSnapshot snapshot,
                                                               DeployRouteInput input,
                                                               String player) {
        ArrayList<ParentCandidate> result = new ArrayList<>();
        for (int i = 0; i < input.actionSemantics().size(); i++) {
            if (DecisionActionSemantic.fromWire(input.actionSemantics().get(i))
                    != DecisionActionSemantic.DEPLOY_CARD) {
                continue;
            }
            if (!player.equals(input.playerIds().get(i))) {
                throw new IllegalArgumentException("candidate player does not match current player");
            }
            boolean known = Boolean.parseBoolean(input.destinationLegalityKnown().get(i));
            result.add(new ParentCandidate(
                    i,
                    input.actionIds().get(i),
                    input.attemptIds().get(i),
                    parseCard(input.sourcePermanentCardIds().get(i),
                            input.sourceCardIds().get(i)),
                    Zone.valueOf(input.sourceZones().get(i)),
                    known,
                    known ? parseDestinations(input.legalDestinations().get(i)) : List.of(),
                    parseBuddyRefs(input.legalBuddies().get(i)),
                    parseOptionalBuddy(input.selectedBuddy().get(i)),
                    snapshot.actionFacts().get(i).cost()));
        }
        return List.copyOf(result);
    }

    private static List<DeployDestinationRef> parseDestinations(String encoded) {
        List<DeployRouteResolver.DestinationIdentity> parsed =
                DeployRouteResolver.parseEncodedDestinations(encoded);
        if (parsed == null) {
            throw new IllegalArgumentException("malformed ordered legal destinations");
        }
        ArrayList<DeployDestinationRef> result = new ArrayList<>();
        for (DeployRouteResolver.DestinationIdentity destination : parsed) {
            if (destination instanceof DeployRouteResolver.DestinationIdentity.CardDestination card) {
                result.add(new DeployDestinationRef.Card(new DeployPhysicalCardRef(
                        card.card().permanentId(), card.card().currentId())));
            } else {
                DeployRouteResolver.DestinationIdentity.ZoneDestination zone =
                        (DeployRouteResolver.DestinationIdentity.ZoneDestination) destination;
                result.add(new DeployDestinationRef.ZoneDestination(zone.zone()));
            }
        }
        return List.copyOf(result);
    }

    private static List<DeployPhysicalCardRef> parseCards(List<String> permanent,
                                                          List<String> current) {
        if (permanent == null || current == null || permanent.size() != current.size()) {
            throw new IllegalArgumentException("destination identities are absent or misaligned");
        }
        ArrayList<DeployPhysicalCardRef> result = new ArrayList<>();
        for (int i = 0; i < current.size(); i++) {
            result.add(parseCard(permanent.get(i), current.get(i)));
        }
        return List.copyOf(result);
    }

    private static BuddyCandidates parseBuddyCandidates(DeployRouteInput input) {
        List<DeployPhysicalCardRef> cards = parseCards(
                input.buddyPermanentCardIds(), input.buddyCardIds());
        if (input.decisionType()
                != com.gempukku.swccgo.logic.decisions.AwaitingDecisionType.ARBITRARY_CARDS) {
            return new BuddyCandidates(cards, List.copyOf(input.cardIds()));
        }
        ArrayList<DeployPhysicalCardRef> legalCards = new ArrayList<>();
        ArrayList<String> legalWires = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            if (Boolean.parseBoolean(input.selectableValues().get(i))) {
                legalCards.add(cards.get(i));
                legalWires.add(input.cardIds().get(i));
            }
        }
        return new BuddyCandidates(List.copyOf(legalCards), List.copyOf(legalWires));
    }

    private static List<DeployPhysicalCardRef> parseBuddyRefs(String encoded) {
        List<DeployRouteResolver.CardIdentity> parsed =
                DeployRouteResolver.parseEncodedCards(encoded);
        if (parsed == null) {
            throw new IllegalArgumentException("malformed ordered legal buddies");
        }
        return parsed.stream()
                .map(card -> new DeployPhysicalCardRef(
                        card.permanentId(), card.currentId()))
                .toList();
    }

    private static DeployPhysicalCardRef parseOptionalBuddy(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        DeployRouteResolver.CardIdentity parsed =
                DeployRouteResolver.parseEncodedCard(encoded);
        if (parsed == null) {
            throw new IllegalArgumentException("malformed selected buddy");
        }
        return new DeployPhysicalCardRef(parsed.permanentId(), parsed.currentId());
    }

    private record BuddyCandidates(
            List<DeployPhysicalCardRef> cards, List<String> wireIds) {
    }

    private static DeployPhysicalCardRef parseCard(String permanent, String current) {
        return new DeployPhysicalCardRef(Integer.parseInt(permanent), Integer.parseInt(current));
    }

    private static FactValue<Float> opponentActiveDrain(SwccgGame game, String playerId) {
        if (game == null || game.getGameState() == null) {
            return FactValue.unknown(PRODUCER, "live game + top locations",
                    "game state is unavailable");
        }
        try {
            GameState gameState = game.getGameState();
            String opponent = gameState.getOpponent(playerId);
            if (opponent == null || opponent.isBlank()) {
                return FactValue.unknown(PRODUCER, "GameState opponent",
                        "opponent identity is unavailable");
            }
            float total = 0f;
            for (PhysicalCard location : gameState.getTopLocations()) {
                boolean opponentPresent = false;
                for (PhysicalCard card : gameState.getCardsAtLocation(location)) {
                    if (card != null && opponent.equals(card.getOwner())) {
                        opponentPresent = true;
                        break;
                    }
                }
                if (opponentPresent) {
                    total += game.getModifiersQuerying()
                            .getForceDrainAmount(gameState, location, opponent);
                }
            }
            return FactValue.known(total, PRODUCER,
                    "bound PlayCharacterAction + live opponent occupied locations");
        } catch (RuntimeException e) {
            return FactValue.unknown(PRODUCER, "live opponent active drain scan",
                    "drain scan failed: " + e.getClass().getSimpleName());
        }
    }

    private static boolean rawMatches(DecisionSnapshot snapshot, DeployRouteInput input) {
        DecisionSnapshot.RawDecision raw = snapshot.rawDecision();
        return Objects.equals(raw.values(com.gempukku.swccgo.common.DecisionOrigin.WIRE_PARAMETER),
                    input.originValues())
                && Objects.equals(raw.values("actionId"), input.actionIds())
                && Objects.equals(raw.values(DecisionActionSemantic.WIRE_PARAMETER),
                    input.actionSemantics())
                && Objects.equals(raw.values(DeployDecisionWire.ATTEMPT_ID), input.attemptIds())
                && Objects.equals(raw.values(DeployDecisionWire.PLAYER_ID), input.playerIds())
                && Objects.equals(raw.values(DeployDecisionWire.SOURCE_CARD_ID), input.sourceCardIds())
                && Objects.equals(raw.values(DeployDecisionWire.SOURCE_PERMANENT_CARD_ID),
                    input.sourcePermanentCardIds())
                && Objects.equals(raw.values(DeployDecisionWire.SOURCE_ZONE), input.sourceZones())
                && Objects.equals(raw.values(DeployDecisionWire.DESTINATION_LEGALITY_KNOWN),
                    input.destinationLegalityKnown())
                && Objects.equals(raw.values(DeployDecisionWire.LEGAL_DESTINATIONS),
                    input.legalDestinations())
                && Objects.equals(raw.values(DeployDecisionWire.LEGAL_BUDDIES),
                    input.legalBuddies())
                && Objects.equals(raw.values(DeployDecisionWire.SELECTED_BUDDY),
                    input.selectedBuddy())
                && Objects.equals(raw.values(DeployDecisionWire.PARENT_DECISION_ID),
                    input.parentDecisionIds())
                && Objects.equals(raw.values(DeployDecisionWire.PARENT_ACTION_ORDINAL),
                    input.parentActionOrdinals())
                && Objects.equals(raw.values("cardId"), input.cardIds())
                && Objects.equals(raw.values(DeployDecisionWire.DESTINATION_CARD_ID),
                    input.destinationCardIds())
                && Objects.equals(raw.values(DeployDecisionWire.DESTINATION_PERMANENT_CARD_ID),
                    input.destinationPermanentCardIds())
                && Objects.equals(raw.values(DeployDecisionWire.BUDDY_CARD_ID),
                    input.buddyCardIds())
                && Objects.equals(raw.values(DeployDecisionWire.BUDDY_PERMANENT_CARD_ID),
                    input.buddyPermanentCardIds())
                && Objects.equals(raw.values(DeployDecisionWire.SELECTED_BUDDY_CARD_ID),
                    input.selectedBuddyCardIds())
                && Objects.equals(raw.values(DeployDecisionWire.SELECTED_BUDDY_PERMANENT_CARD_ID),
                    input.selectedBuddyPermanentCardIds())
                && Objects.equals(raw.values(DeployDecisionWire.FORCED_DESTINATION),
                    input.forcedDestination())
                && Objects.equals(raw.values("results"), input.results())
                && Objects.equals(raw.values("min"), input.minimumValues())
                && Objects.equals(raw.values("max"), input.maximumValues())
                && Objects.equals(raw.values("selectable"), input.selectableValues());
    }

    private static String single(List<String> values) {
        return values != null && values.size() == 1 ? values.get(0) : null;
    }

    private static String singleRequired(List<String> values) {
        String value = single(values);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("required DEPLOY singleton is absent");
        }
        return value;
    }

    private static Integer parseInteger(List<String> values) {
        return Integer.parseInt(singleRequired(values));
    }

    private static FactValue<DeployFacts> unknown(String reason) {
        return FactValue.unknown(PRODUCER, "DEPLOY route metadata", reason);
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be nonblank");
        }
    }

    private static void validateUniqueDestinations(List<DeployPhysicalCardRef> destinations) {
        Set<Integer> permanent = new HashSet<>();
        Set<Integer> current = new HashSet<>();
        for (DeployPhysicalCardRef destination : destinations) {
            if (!permanent.add(destination.permanentCardId())
                    || !current.add(destination.currentCardId())) {
                throw new IllegalArgumentException("destination physical identities must be unique");
            }
        }
    }
}
