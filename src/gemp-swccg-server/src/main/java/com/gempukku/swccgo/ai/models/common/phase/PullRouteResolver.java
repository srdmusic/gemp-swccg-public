package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.PullPhysicalCardRef;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Pure PULL owner resolver. It reads no prompt or action text. */
public final class PullRouteResolver {

    private PullRouteResolver() {
    }

    public static PullRoute resolve(PullRouteInput input) {
        Objects.requireNonNull(input, "input");
        DecisionOrigin origin = parseOrigin(input.originValues());
        AwaitingDecisionType decisionType = input.decisionType();
        if (origin == null || decisionType == null
                || !origin.requiredWireTypeName().equals(decisionType.name())) {
            return PullRoute.LEGACY_UNOWNED;
        }

        return switch (origin) {
            case PHASE_ACTION -> validParent(input)
                    ? PullRoute.PULL_PARENT : PullRoute.LEGACY_UNOWNED;
            case PULL_DEPLOY_CHILD -> validPileChild(input, false)
                    ? PullRoute.PULL_DEPLOY_CHILD : PullRoute.LEGACY_UNOWNED;
            case PULL_TAKE_CHILD -> validPileChild(input, false)
                    ? PullRoute.PULL_TAKE_CHILD : PullRoute.LEGACY_UNOWNED;
            case PULL_DESTINATION -> validDestination(input)
                    ? PullRoute.PULL_DESTINATION : PullRoute.LEGACY_UNOWNED;
            case PULL_FAILED_VERIFY -> validPileChild(input, true)
                    ? PullRoute.PULL_FAILED_VERIFY : PullRoute.LEGACY_UNOWNED;
            default -> PullRoute.LEGACY_UNOWNED;
        };
    }

    private static boolean validParent(PullRouteInput input) {
        if (input.decisionType() != AwaitingDecisionType.CARD_ACTION_CHOICE
                || anyPresent(input.minimumValues(), input.maximumValues(),
                        input.parentDecisionIds(), input.parentActionOrdinals(),
                        input.transactionIds(), input.playerIds(), input.sourceZones(),
                        input.sourceZoneOwners(), input.physicalCardIds(),
                        input.physicalPermanentCardIds(), input.selectedCardIds(),
                        input.selectedPermanentCardIds(), input.destinationCardIds(),
                        input.destinationPermanentCardIds(), input.forcedDestinationCardIds(),
                        input.forcedDestinationPermanentCardIds())) {
            return false;
        }
        int size = size(input.actionIds());
        if (size == 0
                || !sameSize(size, input.actionSemantics(), input.sourceCardIds(),
                        input.sourcePermanentCardIds(), input.gameTextActionIds())) {
            return false;
        }

        boolean foundPull = false;
        Set<String> seenActionIds = new HashSet<>();
        for (int i = 0; i < size; i++) {
            if (isBlank(input.actionIds().get(i))
                    || !seenActionIds.add(input.actionIds().get(i))) {
                return false;
            }
            DecisionActionSemantic semantic = DecisionActionSemantic.fromWire(
                    input.actionSemantics().get(i));
            if (semantic == null || !validOptionalGameTextAction(input.gameTextActionIds().get(i))) {
                return false;
            }
            boolean pull = semantic == DecisionActionSemantic.PULL_DEPLOY_FROM_PILE
                    || semantic == DecisionActionSemantic.PULL_TAKE_INTO_HAND_FROM_PILE;
            PullPhysicalCardRef source = parseOptionalCard(
                    input.sourcePermanentCardIds().get(i), input.sourceCardIds().get(i));
            if (source == INVALID_CARD || (pull && source == null)
                    || (pull && isBlank(input.gameTextActionIds().get(i)))) {
                return false;
            }
            foundPull |= pull;
        }
        return foundPull;
    }

    private static boolean validPileChild(PullRouteInput input, boolean failedVerify) {
        if (input.decisionType() != AwaitingDecisionType.ARBITRARY_CARDS
                || !validTransactionIdentity(input)
                || !validRange(input.minimumValues(), input.maximumValues())
                || !validCandidateCards(input.cardIds(), input.physicalPermanentCardIds(),
                        input.physicalCardIds())
                || anyPresent(input.selectedCardIds(), input.selectedPermanentCardIds(),
                        input.destinationCardIds(), input.destinationPermanentCardIds(),
                        input.forcedDestinationCardIds(), input.forcedDestinationPermanentCardIds())) {
            return false;
        }
        return !failedVerify || (isExactInteger(input.minimumValues(), 0)
                && isExactInteger(input.maximumValues(), 0));
    }

    private static boolean validDestination(PullRouteInput input) {
        if (input.decisionType() != AwaitingDecisionType.CARD_SELECTION
                || !validTransactionIdentity(input)
                || !validRange(input.minimumValues(), input.maximumValues())
                || anyPresent(input.physicalCardIds(), input.physicalPermanentCardIds())) {
            return false;
        }

        PullPhysicalCardRef selected = parseRequiredCard(
                input.selectedPermanentCardIds(), input.selectedCardIds());
        List<PullPhysicalCardRef> destinations = parseCards(
                input.destinationPermanentCardIds(), input.destinationCardIds());
        if (selected == null || destinations == null || destinations.isEmpty()
                || input.cardIds() == null || input.cardIds().size() != destinations.size()) {
            return false;
        }
        for (int i = 0; i < destinations.size(); i++) {
            if (!String.valueOf(destinations.get(i).currentCardId()).equals(input.cardIds().get(i))) {
                return false;
            }
        }

        boolean forcedPresent = input.forcedDestinationCardIds() != null
                || input.forcedDestinationPermanentCardIds() != null;
        PullPhysicalCardRef forced = forcedPresent
                ? parseRequiredCard(input.forcedDestinationPermanentCardIds(),
                        input.forcedDestinationCardIds())
                : null;
        if (forcedPresent && forced == null) {
            return false;
        }
        return forced == null;
    }

    private static boolean validTransactionIdentity(PullRouteInput input) {
        return parsePositiveLongSingleton(input.transactionIds()) != null
                && parseNonNegativeSingleton(input.parentDecisionIds()) != null
                && parseNonNegativeSingleton(input.parentActionOrdinals()) != null
                && nonBlankSingleton(input.playerIds()) != null
                && parseRequiredCard(input.sourcePermanentCardIds(), input.sourceCardIds()) != null
                && parseGameTextAction(input.gameTextActionIds()) != null
                && parseZone(input.sourceZones()) != null
                && nonBlankSingleton(input.sourceZoneOwners()) != null;
    }

    private static boolean validCandidateCards(List<String> wireIds,
                                               List<String> permanentIds,
                                               List<String> currentIds) {
        List<PullPhysicalCardRef> cards = parseCards(permanentIds, currentIds);
        if (wireIds == null || cards == null || wireIds.size() != cards.size()) {
            return false;
        }
        Set<String> seenWireIds = new HashSet<>();
        for (String wireId : wireIds) {
            if (isBlank(wireId) || !seenWireIds.add(wireId)) {
                return false;
            }
        }
        return true;
    }

    private static List<PullPhysicalCardRef> parseCards(List<String> permanentIds,
                                                        List<String> currentIds) {
        if (permanentIds == null || currentIds == null || permanentIds.size() != currentIds.size()) {
            return null;
        }
        java.util.ArrayList<PullPhysicalCardRef> cards = new java.util.ArrayList<>(currentIds.size());
        Set<Integer> permanent = new HashSet<>();
        Set<Integer> current = new HashSet<>();
        for (int i = 0; i < currentIds.size(); i++) {
            PullPhysicalCardRef card = parseCard(permanentIds.get(i), currentIds.get(i));
            if (card == null || !permanent.add(card.permanentCardId())
                    || !current.add(card.currentCardId())) {
                return null;
            }
            cards.add(card);
        }
        return cards;
    }

    private static PullPhysicalCardRef parseRequiredCard(List<String> permanentIds,
                                                         List<String> currentIds) {
        return permanentIds != null && permanentIds.size() == 1
                && currentIds != null && currentIds.size() == 1
                ? parseCard(permanentIds.get(0), currentIds.get(0))
                : null;
    }

    private static final PullPhysicalCardRef INVALID_CARD = new PullPhysicalCardRef(0, 0);

    private static PullPhysicalCardRef parseOptionalCard(String permanentId, String currentId) {
        boolean permanentBlank = isBlank(permanentId);
        boolean currentBlank = isBlank(currentId);
        if (permanentBlank && currentBlank) {
            return null;
        }
        if (permanentBlank || currentBlank) {
            return INVALID_CARD;
        }
        PullPhysicalCardRef parsed = parseCard(permanentId, currentId);
        return parsed != null ? parsed : INVALID_CARD;
    }

    private static PullPhysicalCardRef parseCard(String permanentId, String currentId) {
        Integer permanent = parseNonNegative(permanentId);
        Integer current = parseNonNegative(currentId);
        return permanent != null && current != null
                ? new PullPhysicalCardRef(permanent, current) : null;
    }

    private static DecisionOrigin parseOrigin(List<String> values) {
        return values != null && values.size() == 1
                ? DecisionOrigin.fromWire(values.get(0)) : null;
    }

    private static GameTextActionId parseGameTextAction(List<String> values) {
        if (values == null || values.size() != 1 || isBlank(values.get(0))) {
            return null;
        }
        try {
            return GameTextActionId.valueOf(values.get(0));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean validOptionalGameTextAction(String value) {
        if (isBlank(value)) {
            return true;
        }
        try {
            GameTextActionId.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static Zone parseZone(List<String> values) {
        if (values == null || values.size() != 1 || isBlank(values.get(0))) {
            return null;
        }
        try {
            Zone zone = Zone.valueOf(values.get(0));
            return zone.isCardPile() ? zone : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean validRange(List<String> minimum, List<String> maximum) {
        Integer min = parseNonNegativeSingleton(minimum);
        Integer max = parseNonNegativeSingleton(maximum);
        return min != null && max != null && max >= min;
    }

    private static boolean isExactInteger(List<String> values, int expected) {
        Integer parsed = parseNonNegativeSingleton(values);
        return parsed != null && parsed == expected;
    }

    private static Integer parseNonNegativeSingleton(List<String> values) {
        return values != null && values.size() == 1 ? parseNonNegative(values.get(0)) : null;
    }

    private static Long parsePositiveLongSingleton(List<String> values) {
        if (values == null || values.size() != 1 || isBlank(values.get(0))) {
            return null;
        }
        try {
            long parsed = Long.parseLong(values.get(0));
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseNonNegative(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String nonBlankSingleton(List<String> values) {
        return values != null && values.size() == 1 && !isBlank(values.get(0))
                ? values.get(0) : null;
    }

    private static boolean anyPresent(List<String>... values) {
        for (List<String> value : values) {
            if (value != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameSize(int expected, List<String>... values) {
        for (List<String> value : values) {
            if (value == null || value.size() != expected) {
                return false;
            }
        }
        return true;
    }

    private static int size(List<?> values) {
        return values != null ? values.size() : 0;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
