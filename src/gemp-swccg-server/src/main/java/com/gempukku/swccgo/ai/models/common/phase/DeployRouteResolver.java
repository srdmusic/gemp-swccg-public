package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Pure DEPLOY resolver. Prompt text and phase alone never establish ownership. */
public final class DeployRouteResolver {
    private DeployRouteResolver() {
    }

    public static DeployRoute resolve(DeployRouteInput input) {
        Objects.requireNonNull(input, "input");
        DecisionOrigin origin = singletonOrigin(input.originValues());
        if (origin == null || input.decisionType() == null
                || !origin.requiredWireTypeName().equals(input.decisionType().name())) {
            return DeployRoute.LEGACY_UNOWNED;
        }
        return switch (origin) {
            case PHASE_ACTION -> validParent(input)
                    ? DeployRoute.DEPLOY_PARENT : DeployRoute.LEGACY_UNOWNED;
            case DEPLOY_DESTINATION -> validChild(input, false)
                    ? DeployRoute.DEPLOY_DESTINATION : DeployRoute.LEGACY_UNOWNED;
            case DEPLOY_BUDDY, DEPLOY_BUDDY_ARBITRARY -> validBuddy(input)
                    ? DeployRoute.DEPLOY_BUDDY : DeployRoute.LEGACY_UNOWNED;
            case DEPLOY_V170_UNDERCOVER -> validUndercoverChoice(input)
                    ? DeployRoute.DEPLOY_V170_UNDERCOVER : DeployRoute.LEGACY_UNOWNED;
            case DEPLOY_CAPACITY -> validChoice(input)
                    ? DeployRoute.DEPLOY_CAPACITY : DeployRoute.LEGACY_UNOWNED;
            case DEPLOY_CONFIRMATION -> validConfirmation(input)
                    ? DeployRoute.DEPLOY_CONFIRMATION : DeployRoute.LEGACY_UNOWNED;
            default -> DeployRoute.LEGACY_UNOWNED;
        };
    }

    private static boolean validParent(DeployRouteInput input) {
        if (input.phase() != Phase.DEPLOY
                || input.decisionType() != AwaitingDecisionType.CARD_ACTION_CHOICE
                || anyPresent(input.parentDecisionIds(), input.parentActionOrdinals(),
                        input.destinationCardIds(), input.destinationPermanentCardIds(),
                        input.buddyCardIds(), input.buddyPermanentCardIds(),
                        input.selectedBuddyCardIds(), input.selectedBuddyPermanentCardIds(),
                        input.forcedDestination(), input.results(), input.selectableValues())) {
            return false;
        }
        int size = size(input.actionIds());
        if (size == 0 || !sameSize(size, input.actionSemantics(), input.attemptIds(),
                input.playerIds(), input.sourceCardIds(), input.sourcePermanentCardIds(),
                input.sourceZones(), input.destinationLegalityKnown(),
                input.legalDestinations(), input.legalBuddies(),
                input.selectedBuddy())) {
            return false;
        }

        boolean foundDeploy = false;
        Set<String> actionIds = new HashSet<>();
        Set<String> attempts = new HashSet<>();
        for (int i = 0; i < size; i++) {
            if (blank(input.actionIds().get(i)) || !actionIds.add(input.actionIds().get(i))) {
                return false;
            }
            DecisionActionSemantic semantic = DecisionActionSemantic.fromWire(
                    input.actionSemantics().get(i));
            if (semantic == null) {
                return false;
            }
            if (semantic != DecisionActionSemantic.DEPLOY_CARD) {
                if (!allBlankAt(i, input.attemptIds(), input.playerIds(),
                        input.sourceCardIds(), input.sourcePermanentCardIds(),
                        input.sourceZones(), input.destinationLegalityKnown(),
                        input.legalDestinations(), input.legalBuddies(),
                        input.selectedBuddy())) {
                    return false;
                }
                continue;
            }
            foundDeploy = true;
            String attempt = input.attemptIds().get(i);
            if (blank(attempt) || !attempts.add(attempt)
                    || blank(input.playerIds().get(i))
                    || parseCard(input.sourcePermanentCardIds().get(i),
                            input.sourceCardIds().get(i)) == null
                    || parseZone(input.sourceZones().get(i)) == null) {
                return false;
            }
            Boolean known = parseBoolean(input.destinationLegalityKnown().get(i));
            if (known == null || (!known && !blank(input.legalDestinations().get(i)))) {
                return false;
            }
            if (known && parseEncodedDestinations(input.legalDestinations().get(i)) == null) {
                return false;
            }
            List<CardIdentity> buddies = parseEncodedCards(input.legalBuddies().get(i));
            String selectedBuddyRaw = input.selectedBuddy().get(i);
            CardIdentity selectedBuddy = parseEncodedCard(selectedBuddyRaw);
            if (buddies == null
                    || (!blank(selectedBuddyRaw) && selectedBuddy == null)
                    || (selectedBuddy != null && !buddies.contains(selectedBuddy))) {
                return false;
            }
        }
        return foundDeploy;
    }

    private static boolean validChild(DeployRouteInput input, boolean requireSingletonDestination) {
        if (input.decisionType() != AwaitingDecisionType.CARD_SELECTION
                || !validIdentity(input)
                || !isBooleanSingleton(input.destinationLegalityKnown(), true)
                || !isBooleanSingleton(input.forcedDestination(), false)
                || input.cardIds() == null || input.cardIds().isEmpty()
                || !sameSize(input.cardIds().size(), input.destinationCardIds(),
                        input.destinationPermanentCardIds())
                || !validBuddyMetadata(input)
                || !validRange(input.minimumValues(), input.maximumValues())) {
            return false;
        }
        if (requireSingletonDestination && input.cardIds().size() != 1) {
            return false;
        }
        for (int i = 0; i < input.cardIds().size(); i++) {
            CardIdentity card = parseCard(input.destinationPermanentCardIds().get(i),
                    input.destinationCardIds().get(i));
            if (card == null || !String.valueOf(card.currentId()).equals(input.cardIds().get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean validChoice(DeployRouteInput input) {
        return input.decisionType() == AwaitingDecisionType.MULTIPLE_CHOICE
                && validIdentity(input)
                && validBuddyMetadata(input)
                && isBooleanSingleton(input.destinationLegalityKnown(), true)
                && isBooleanSingleton(input.forcedDestination())
                && input.destinationCardIds() != null
                && input.destinationCardIds().size() == 1
                && input.destinationPermanentCardIds() != null
                && input.destinationPermanentCardIds().size() == 1
                && parseCard(input.destinationPermanentCardIds().get(0),
                        input.destinationCardIds().get(0)) != null
                && input.results() != null && input.results().size() >= 2;
    }

    private static boolean validBuddy(DeployRouteInput input) {
        boolean arbitrary = input.decisionType() == AwaitingDecisionType.ARBITRARY_CARDS;
        if ((!arbitrary && input.decisionType() != AwaitingDecisionType.CARD_SELECTION)
                || !validIdentity(input)
                || !validBuddyMetadata(input)
                || !isBooleanSingleton(input.destinationLegalityKnown(), false)
                || !isBooleanSingleton(input.forcedDestination(), false)
                || input.cardIds() == null || input.cardIds().isEmpty()
                || !sameSize(input.cardIds().size(), input.buddyCardIds(),
                        input.buddyPermanentCardIds())
                || !empty(input.destinationCardIds())
                || !empty(input.destinationPermanentCardIds())
                || !validRange(input.minimumValues(), input.maximumValues())) {
            return false;
        }
        if (arbitrary && !sameSize(input.cardIds().size(), input.selectableValues())) {
            return false;
        }
        for (int i = 0; i < input.cardIds().size(); i++) {
            CardIdentity card = parseCard(input.buddyPermanentCardIds().get(i),
                    input.buddyCardIds().get(i));
            if (card == null) {
                return false;
            }
            if (arbitrary) {
                Boolean selectable = parseBoolean(input.selectableValues().get(i));
                if (selectable == null || !input.cardIds().get(i).equals("temp" + i)) {
                    return false;
                }
            } else if (!String.valueOf(card.currentId()).equals(input.cardIds().get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean validConfirmation(DeployRouteInput input) {
        return input.decisionType() == AwaitingDecisionType.MULTIPLE_CHOICE
                && validIdentity(input)
                && validBuddyMetadata(input)
                && isBooleanSingleton(input.destinationLegalityKnown(), false)
                && isBooleanSingleton(input.forcedDestination(), false)
                && empty(input.destinationCardIds())
                && empty(input.destinationPermanentCardIds())
                && input.results() != null && input.results().size() >= 2;
    }

    private static boolean validUndercoverChoice(DeployRouteInput input) {
        return validChoice(input)
                && uniqueResult(input.results(), "yes") >= 0
                && uniqueResult(input.results(), "no") >= 0;
    }

    private static int uniqueResult(List<String> results, String expected) {
        int found = -1;
        for (int i = 0; i < results.size(); i++) {
            String value = results.get(i);
            if (value != null && expected.equalsIgnoreCase(value.trim())) {
                if (found >= 0) {
                    return -1;
                }
                found = i;
            }
        }
        return found;
    }

    private static boolean validIdentity(DeployRouteInput input) {
        return nonBlankSingleton(input.attemptIds()) != null
                && nonBlankSingleton(input.playerIds()) != null
                && parseNonNegativeSingleton(input.parentDecisionIds()) != null
                && parseNonNegativeSingleton(input.parentActionOrdinals()) != null
                && parseRequiredCard(input.sourcePermanentCardIds(), input.sourceCardIds()) != null
                && parseZoneSingleton(input.sourceZones()) != null;
    }

    private static boolean validBuddyMetadata(DeployRouteInput input) {
        if (input.buddyCardIds() == null || input.buddyPermanentCardIds() == null
                || input.buddyCardIds().size() != input.buddyPermanentCardIds().size()) {
            return false;
        }
        Set<CardIdentity> candidates = new HashSet<>();
        for (int i = 0; i < input.buddyCardIds().size(); i++) {
            CardIdentity candidate = parseCard(
                    input.buddyPermanentCardIds().get(i), input.buddyCardIds().get(i));
            if (candidate == null || !candidates.add(candidate)) {
                return false;
            }
        }
        boolean selectedAbsent = input.selectedBuddyCardIds() == null
                && input.selectedBuddyPermanentCardIds() == null;
        if (selectedAbsent) {
            return true;
        }
        CardIdentity selected = parseRequiredCard(
                input.selectedBuddyPermanentCardIds(), input.selectedBuddyCardIds());
        return selected != null && candidates.contains(selected);
    }

    static List<DestinationIdentity> parseEncodedDestinations(String value) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return List.of();
        }
        java.util.ArrayList<DestinationIdentity> result = new java.util.ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String token : value.split(",", -1)) {
            if (!seen.add(token)) {
                return null;
            }
            String[] parts = token.split(":", -1);
            if (parts.length == 3 && "CARD".equals(parts[0])) {
                CardIdentity card = parseCard(parts[1], parts[2]);
                if (card == null) {
                    return null;
                }
                result.add(new DestinationIdentity.CardDestination(card));
            } else if (parts.length == 2 && "ZONE".equals(parts[0])) {
                Zone zone = parseZone(parts[1]);
                if (zone == null) {
                    return null;
                }
                result.add(new DestinationIdentity.ZoneDestination(zone));
            } else {
                return null;
            }
        }
        return List.copyOf(result);
    }

    static List<CardIdentity> parseEncodedCards(String value) {
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return List.of();
        }
        java.util.ArrayList<CardIdentity> result = new java.util.ArrayList<>();
        Set<CardIdentity> seen = new HashSet<>();
        for (String token : value.split(",", -1)) {
            CardIdentity card = parseEncodedCard(token);
            if (card == null || !seen.add(card)) {
                return null;
            }
            result.add(card);
        }
        return List.copyOf(result);
    }

    static CardIdentity parseEncodedCard(String value) {
        if (blank(value)) {
            return null;
        }
        String[] parts = value.split(":", -1);
        return parts.length == 2 ? parseCard(parts[0], parts[1]) : null;
    }

    static CardIdentity parseCard(String permanent, String current) {
        Integer permanentId = parseNonNegative(permanent);
        Integer currentId = parseNonNegative(current);
        return permanentId != null && currentId != null
                ? new CardIdentity(permanentId, currentId) : null;
    }

    static Zone parseZone(String value) {
        if (blank(value)) {
            return null;
        }
        try {
            return Zone.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static CardIdentity parseRequiredCard(List<String> permanent,
                                                  List<String> current) {
        return permanent != null && permanent.size() == 1
                && current != null && current.size() == 1
                ? parseCard(permanent.get(0), current.get(0)) : null;
    }

    private static Zone parseZoneSingleton(List<String> values) {
        return values != null && values.size() == 1 ? parseZone(values.get(0)) : null;
    }

    private static boolean validRange(List<String> minimum, List<String> maximum) {
        Integer min = parseNonNegativeSingleton(minimum);
        Integer max = parseNonNegativeSingleton(maximum);
        return min != null && max != null && max >= min;
    }

    private static Integer parseNonNegativeSingleton(List<String> values) {
        return values != null && values.size() == 1 ? parseNonNegative(values.get(0)) : null;
    }

    private static Integer parseNonNegative(String value) {
        if (blank(value)) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean parseBoolean(String value) {
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        return null;
    }

    private static boolean isBooleanSingleton(List<String> values, boolean expected) {
        return values != null && values.size() == 1
                && Boolean.valueOf(expected).equals(parseBoolean(values.get(0)));
    }

    private static boolean isBooleanSingleton(List<String> values) {
        return values != null && values.size() == 1
                && parseBoolean(values.get(0)) != null;
    }

    private static DecisionOrigin singletonOrigin(List<String> values) {
        return values != null && values.size() == 1
                ? DecisionOrigin.fromWire(values.get(0)) : null;
    }

    private static String nonBlankSingleton(List<String> values) {
        return values != null && values.size() == 1 && !blank(values.get(0))
                ? values.get(0) : null;
    }

    @SafeVarargs
    private static boolean anyPresent(List<String>... values) {
        for (List<String> value : values) {
            if (value != null) {
                return true;
            }
        }
        return false;
    }

    @SafeVarargs
    private static boolean sameSize(int size, List<String>... values) {
        for (List<String> value : values) {
            if (value == null || value.size() != size) {
                return false;
            }
        }
        return true;
    }

    @SafeVarargs
    private static boolean allBlankAt(int index, List<String>... values) {
        for (List<String> value : values) {
            if (!blank(value.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static int size(List<String> values) {
        return values != null ? values.size() : 0;
    }

    private static boolean empty(List<String> values) {
        return values != null && values.isEmpty();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record CardIdentity(int permanentId, int currentId) {
    }

    public sealed interface DestinationIdentity {
        record CardDestination(CardIdentity card) implements DestinationIdentity {
        }

        record ZoneDestination(Zone zone) implements DestinationIdentity {
        }
    }
}
