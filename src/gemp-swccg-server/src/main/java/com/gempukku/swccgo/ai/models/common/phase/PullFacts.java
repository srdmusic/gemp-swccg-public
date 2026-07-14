package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.decision.FactValue;
import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PullDecisionWire;
import com.gempukku.swccgo.common.PullPhysicalCardRef;
import com.gempukku.swccgo.common.Zone;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable, stable-identity facts for one owned PULL transaction stage. */
public record PullFacts(
        String decisionId,
        int turn,
        String playerId,
        Phase phase,
        PullRoute route,
        Long transactionId,
        ParentIdentity parentIdentity,
        List<ParentCandidate> parentCandidates,
        FactValue<PullPhysicalCardRef> sourceCard,
        FactValue<GameTextActionId> gameTextActionId,
        FactValue<Zone> sourceZone,
        FactValue<String> sourceZoneOwner,
        List<PullPhysicalCardRef> candidateCards,
        PullPhysicalCardRef selectedChild,
        List<PullPhysicalCardRef> orderedDestinations,
        PullPhysicalCardRef forcedDestination,
        /** The engine does not expose the exact Java filter yet. This must stay UNKNOWN. */
        FactValue<String> sourceFilter) {

    private static final String PRODUCER = "pull-facts";

    public PullFacts {
        requireNonBlank(decisionId, "decisionId");
        if (turn < 0) {
            throw new IllegalArgumentException("turn must be >= 0");
        }
        requireNonBlank(playerId, "playerId");
        Objects.requireNonNull(route, "route");
        if (route == PullRoute.LEGACY_UNOWNED) {
            throw new IllegalArgumentException("LEGACY_UNOWNED has no owned PullFacts");
        }
        Objects.requireNonNull(parentIdentity, "parentIdentity");
        parentCandidates = List.copyOf(parentCandidates);
        Objects.requireNonNull(sourceCard, "sourceCard");
        Objects.requireNonNull(gameTextActionId, "gameTextActionId");
        Objects.requireNonNull(sourceZone, "sourceZone");
        Objects.requireNonNull(sourceZoneOwner, "sourceZoneOwner");
        candidateCards = List.copyOf(candidateCards);
        orderedDestinations = List.copyOf(orderedDestinations);
        Objects.requireNonNull(sourceFilter, "sourceFilter");
        if (!sourceFilter.isUnknown()) {
            throw new IllegalArgumentException("sourceFilter must remain explicitly UNKNOWN");
        }
        validateUniqueCards(candidateCards, "candidateCards");
        validateUniqueCards(orderedDestinations, "orderedDestinations");

        if (route == PullRoute.PULL_PARENT) {
            if (transactionId != null || parentIdentity.acceptedActionOrdinal() != null
                    || parentCandidates.isEmpty() || sourceCard.isKnown()
                    || gameTextActionId.isKnown() || sourceZone.isKnown()
                    || sourceZoneOwner.isKnown() || !candidateCards.isEmpty()
                    || selectedChild != null || !orderedDestinations.isEmpty()
                    || forcedDestination != null) {
                throw new IllegalArgumentException("PULL_PARENT facts must remain pre-acceptance");
            }
            validateParentCandidates(parentCandidates);
        } else {
            if (transactionId == null || transactionId <= 0) {
                throw new IllegalArgumentException("owned child facts require transactionId > 0");
            }
            if (parentIdentity.acceptedActionOrdinal() == null || !parentCandidates.isEmpty()
                    || sourceCard.isUnknown() || gameTextActionId.isUnknown()
                    || sourceZone.isUnknown() || sourceZoneOwner.isUnknown()) {
                throw new IllegalArgumentException("owned child facts require complete parent/source metadata");
            }
            if (!sourceZone.value().isCardPile() || sourceZoneOwner.value().isBlank()) {
                throw new IllegalArgumentException("owned child facts require a card-pile zone and owner");
            }

            switch (route) {
                case PULL_DEPLOY_CHILD, PULL_TAKE_CHILD, PULL_FAILED_VERIFY -> {
                    if (selectedChild != null || !orderedDestinations.isEmpty()
                            || forcedDestination != null) {
                        throw new IllegalArgumentException(route + " cannot carry destination outcome facts");
                    }
                }
                case PULL_DESTINATION -> {
                if (selectedChild == null || orderedDestinations.isEmpty()
                        || !candidateCards.equals(orderedDestinations)) {
                    throw new IllegalArgumentException(
                            "PULL_DESTINATION requires selected child and aligned ordered destinations");
                }
                if (forcedDestination != null) {
                    throw new IllegalArgumentException(
                            "a prompted destination cannot carry auto-selection evidence");
                }
                }
                default -> throw new IllegalArgumentException("unsupported owned route " + route);
            }
        }
    }

    public record ParentIdentity(int decisionId, Integer acceptedActionOrdinal) {
        public ParentIdentity {
            if (decisionId < 0) {
                throw new IllegalArgumentException("parent decisionId must be >= 0");
            }
            if (acceptedActionOrdinal != null && acceptedActionOrdinal < 0) {
                throw new IllegalArgumentException("acceptedActionOrdinal must be >= 0");
            }
        }
    }

    /** One typed PULL parent candidate, retained at its original decision ordinal. */
    public record ParentCandidate(
            int ordinal,
            String actionWireId,
            DecisionActionSemantic semantic,
            PullPhysicalCardRef sourceCard,
            GameTextActionId gameTextActionId) {

        public ParentCandidate {
            if (ordinal < 0) {
                throw new IllegalArgumentException("ordinal must be >= 0");
            }
            requireNonBlank(actionWireId, "actionWireId");
            if (semantic != DecisionActionSemantic.PULL_DEPLOY_FROM_PILE
                    && semantic != DecisionActionSemantic.PULL_TAKE_INTO_HAND_FROM_PILE) {
                throw new IllegalArgumentException("parent candidate requires a PULL semantic");
            }
            Objects.requireNonNull(sourceCard, "sourceCard");
            Objects.requireNonNull(gameTextActionId, "gameTextActionId");
        }
    }

    /**
     * Parses one owned route from the same raw decision used by the snapshot. Any
     * absent permanent identity or transaction metadata returns UNKNOWN.
     */
    public static FactValue<PullFacts> parse(DecisionSnapshot snapshot,
                                             PullRouteInput input,
                                             PullRoute route) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(route, "route");
        try {
            if (route == PullRoute.LEGACY_UNOWNED || PullRouteResolver.resolve(input) != route) {
                return unknownFacts("route is unowned or does not match complete PULL metadata");
            }
            if (snapshot.decisionFacts().decisionType() != input.decisionType()
                    || !snapshot.decisionFacts().decisionId().equals(String.valueOf(input.decisionId()))
                    || !rawMatches(snapshot, input)) {
                return unknownFacts("snapshot and captured PULL decision do not preserve the same raw order");
            }

            String playerId = snapshot.decisionFacts().currentPlayer();
            if (route != PullRoute.PULL_PARENT
                    && !playerId.equals(single(input.playerIds()))) {
                return unknownFacts("transaction player metadata does not match the decision player");
            }

            ParentIdentity parentIdentity;
            Long transactionId;
            List<ParentCandidate> parentCandidates;
            FactValue<PullPhysicalCardRef> sourceCard;
            FactValue<GameTextActionId> gameTextActionId;
            FactValue<Zone> sourceZone;
            FactValue<String> sourceZoneOwner;

            if (route == PullRoute.PULL_PARENT) {
                parentIdentity = new ParentIdentity(input.decisionId(), null);
                transactionId = null;
                parentCandidates = parseParentCandidates(input);
                sourceCard = unknown(PullDecisionWire.SOURCE_PERMANENT_CARD_ID,
                        "parent source is candidate-specific until acceptance");
                gameTextActionId = unknown(PullDecisionWire.GAME_TEXT_ACTION_ID,
                        "parent game-text action is candidate-specific until acceptance");
                sourceZone = unknown(PullDecisionWire.SOURCE_ZONE,
                        "parent search zone is emitted by the child decision");
                sourceZoneOwner = unknown(PullDecisionWire.SOURCE_ZONE_OWNER,
                        "parent search owner is emitted by the child decision");
            } else {
                parentIdentity = new ParentIdentity(
                        parseInteger(input.parentDecisionIds()),
                        parseInteger(input.parentActionOrdinals()));
                transactionId = parseLong(input.transactionIds());
                parentCandidates = List.of();
                sourceCard = known(parseCard(input.sourcePermanentCardIds(), input.sourceCardIds()),
                        PullDecisionWire.SOURCE_PERMANENT_CARD_ID);
                gameTextActionId = known(parseGameTextAction(input.gameTextActionIds()),
                        PullDecisionWire.GAME_TEXT_ACTION_ID);
                sourceZone = known(parseZone(input.sourceZones()), PullDecisionWire.SOURCE_ZONE);
                sourceZoneOwner = known(singleRequired(input.sourceZoneOwners()),
                        PullDecisionWire.SOURCE_ZONE_OWNER);
            }

            List<PullPhysicalCardRef> candidates = switch (route) {
                case PULL_DEPLOY_CHILD, PULL_TAKE_CHILD, PULL_FAILED_VERIFY ->
                        parseCards(input.physicalPermanentCardIds(), input.physicalCardIds());
                case PULL_DESTINATION ->
                        parseCards(input.destinationPermanentCardIds(), input.destinationCardIds());
                default -> List.of();
            };
            PullPhysicalCardRef selected = route == PullRoute.PULL_DESTINATION
                    ? parseCard(input.selectedPermanentCardIds(), input.selectedCardIds()) : null;
            List<PullPhysicalCardRef> destinations = route == PullRoute.PULL_DESTINATION
                    ? candidates : List.of();
            PullPhysicalCardRef forced = route == PullRoute.PULL_DESTINATION
                    && input.forcedDestinationCardIds() != null
                    ? parseCard(input.forcedDestinationPermanentCardIds(),
                            input.forcedDestinationCardIds())
                    : null;

            PullFacts facts = new PullFacts(
                    snapshot.decisionFacts().decisionId(),
                    snapshot.decisionFacts().turn(),
                    playerId,
                    snapshot.decisionFacts().phase(),
                    route,
                    transactionId,
                    parentIdentity,
                    parentCandidates,
                    sourceCard,
                    gameTextActionId,
                    sourceZone,
                    sourceZoneOwner,
                    candidates,
                    selected,
                    destinations,
                    forced,
                    FactValue.unknown(PRODUCER, "engine search filter",
                            "exact Java source filter is not present on PullDecisionWire"));
            return FactValue.known(facts, PRODUCER, "PullRouteInput + DecisionSnapshot");
        } catch (RuntimeException e) {
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return unknownFacts("PULL fact parsing failed: " + detail);
        }
    }

    private static List<ParentCandidate> parseParentCandidates(PullRouteInput input) {
        java.util.ArrayList<ParentCandidate> candidates = new java.util.ArrayList<>();
        for (int i = 0; i < input.actionSemantics().size(); i++) {
            DecisionActionSemantic semantic = DecisionActionSemantic.fromWire(
                    input.actionSemantics().get(i));
            if (semantic == DecisionActionSemantic.PULL_DEPLOY_FROM_PILE
                    || semantic == DecisionActionSemantic.PULL_TAKE_INTO_HAND_FROM_PILE) {
                candidates.add(new ParentCandidate(
                        i,
                        input.actionIds().get(i),
                        semantic,
                        parseCard(input.sourcePermanentCardIds().get(i),
                                input.sourceCardIds().get(i)),
                        parseGameTextAction(input.gameTextActionIds().get(i))));
            }
        }
        return candidates;
    }

    private static void validateParentCandidates(List<ParentCandidate> candidates) {
        int previousOrdinal = -1;
        Set<String> actionIds = new HashSet<>();
        for (ParentCandidate candidate : candidates) {
            if (candidate.ordinal() <= previousOrdinal || !actionIds.add(candidate.actionWireId())) {
                throw new IllegalArgumentException("parent candidates must preserve unique original order");
            }
            previousOrdinal = candidate.ordinal();
        }
    }

    private static void validateUniqueCards(List<PullPhysicalCardRef> cards, String name) {
        Set<Integer> permanentIds = new HashSet<>();
        Set<Integer> currentIds = new HashSet<>();
        for (PullPhysicalCardRef card : cards) {
            if (!permanentIds.add(card.permanentCardId())
                    || !currentIds.add(card.currentCardId())) {
                throw new IllegalArgumentException(name + " must contain unique physical cards");
            }
        }
    }

    private static boolean rawMatches(DecisionSnapshot snapshot, PullRouteInput input) {
        DecisionSnapshot.RawDecision raw = snapshot.rawDecision();
        return matches(raw, DecisionOrigin.WIRE_PARAMETER, input.originValues())
                && matches(raw, DecisionActionSemantic.WIRE_PARAMETER, input.actionSemantics())
                && matches(raw, "actionId", input.actionIds())
                && matches(raw, "cardId", input.cardIds())
                && matches(raw, "min", input.minimumValues())
                && matches(raw, "max", input.maximumValues())
                && matches(raw, PullDecisionWire.PARENT_DECISION_ID, input.parentDecisionIds())
                && matches(raw, PullDecisionWire.PARENT_ACTION_ORDINAL, input.parentActionOrdinals())
                && matches(raw, PullDecisionWire.TRANSACTION_ID, input.transactionIds())
                && matches(raw, PullDecisionWire.PLAYER_ID, input.playerIds())
                && matches(raw, PullDecisionWire.SOURCE_CARD_ID, input.sourceCardIds())
                && matches(raw, PullDecisionWire.SOURCE_PERMANENT_CARD_ID,
                        input.sourcePermanentCardIds())
                && matches(raw, PullDecisionWire.GAME_TEXT_ACTION_ID, input.gameTextActionIds())
                && matches(raw, PullDecisionWire.SOURCE_ZONE, input.sourceZones())
                && matches(raw, PullDecisionWire.SOURCE_ZONE_OWNER, input.sourceZoneOwners())
                && matches(raw, PullDecisionWire.PHYSICAL_CARD_ID, input.physicalCardIds())
                && matches(raw, PullDecisionWire.PHYSICAL_PERMANENT_CARD_ID,
                        input.physicalPermanentCardIds())
                && matches(raw, PullDecisionWire.SELECTED_CARD_ID, input.selectedCardIds())
                && matches(raw, PullDecisionWire.SELECTED_PERMANENT_CARD_ID,
                        input.selectedPermanentCardIds())
                && matches(raw, PullDecisionWire.DESTINATION_CARD_ID, input.destinationCardIds())
                && matches(raw, PullDecisionWire.DESTINATION_PERMANENT_CARD_ID,
                        input.destinationPermanentCardIds())
                && matches(raw, PullDecisionWire.FORCED_DESTINATION_CARD_ID,
                        input.forcedDestinationCardIds())
                && matches(raw, PullDecisionWire.FORCED_DESTINATION_PERMANENT_CARD_ID,
                        input.forcedDestinationPermanentCardIds());
    }

    private static boolean matches(DecisionSnapshot.RawDecision raw, String key,
                                   List<String> captured) {
        return Objects.equals(raw.values(key), captured);
    }

    private static List<PullPhysicalCardRef> parseCards(List<String> permanentIds,
                                                        List<String> currentIds) {
        if (permanentIds == null || currentIds == null || permanentIds.size() != currentIds.size()) {
            throw new IllegalArgumentException("permanent/current candidate identity arrays are not aligned");
        }
        java.util.ArrayList<PullPhysicalCardRef> cards = new java.util.ArrayList<>(currentIds.size());
        for (int i = 0; i < currentIds.size(); i++) {
            cards.add(parseCard(permanentIds.get(i), currentIds.get(i)));
        }
        return cards;
    }

    private static PullPhysicalCardRef parseCard(List<String> permanentIds,
                                                 List<String> currentIds) {
        return parseCard(singleRequired(permanentIds), singleRequired(currentIds));
    }

    private static PullPhysicalCardRef parseCard(String permanentId, String currentId) {
        return new PullPhysicalCardRef(parseNonNegative(permanentId), parseNonNegative(currentId));
    }

    private static GameTextActionId parseGameTextAction(List<String> values) {
        return parseGameTextAction(singleRequired(values));
    }

    private static GameTextActionId parseGameTextAction(String value) {
        return GameTextActionId.valueOf(value);
    }

    private static Zone parseZone(List<String> values) {
        return Zone.valueOf(singleRequired(values));
    }

    private static int parseInteger(List<String> values) {
        return parseNonNegative(singleRequired(values));
    }

    private static long parseLong(List<String> values) {
        long parsed = Long.parseLong(singleRequired(values));
        if (parsed <= 0) {
            throw new IllegalArgumentException("transactionId must be > 0");
        }
        return parsed;
    }

    private static int parseNonNegative(String value) {
        int parsed = Integer.parseInt(value);
        if (parsed < 0) {
            throw new IllegalArgumentException("physical and decision ids must be >= 0");
        }
        return parsed;
    }

    private static String single(List<String> values) {
        return values != null && values.size() == 1 ? values.get(0) : null;
    }

    private static String singleRequired(List<String> values) {
        String value = single(values);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("required singleton wire value is absent or blank");
        }
        return value;
    }

    private static <T> FactValue<T> known(T value, String provenance) {
        return FactValue.known(value, PRODUCER, provenance);
    }

    private static <T> FactValue<T> unknown(String provenance, String reason) {
        return FactValue.unknown(PRODUCER, provenance, reason);
    }

    private static FactValue<PullFacts> unknownFacts(String reason) {
        return FactValue.unknown(PRODUCER, "PullRouteInput + DecisionSnapshot", reason);
    }

    private static void requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must be nonblank");
        }
    }
}
