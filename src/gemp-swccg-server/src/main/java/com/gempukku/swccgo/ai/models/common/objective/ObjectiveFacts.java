package com.gempukku.swccgo.ai.models.common.objective;

import com.gempukku.swccgo.ai.models.common.decision.FactValue;

import java.util.Objects;
import java.util.Set;

/** Immutable objective observations captured once at the mediated decision boundary. */
public record ObjectiveFacts(
        FactValue<Identity> identity,
        FactValue<ProfileResolution> profileResolution,
        FactValue<StrategyFacts> strategy,
        FactValue<TypedBoardFacts> board) {

    public static final String PRODUCER = "objective-facts";

    public ObjectiveFacts {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(profileResolution, "profileResolution");
        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(board, "board");
    }

    public static ObjectiveFacts unknown(String reason) {
        return new ObjectiveFacts(
                unknownValue("physical objective", reason),
                unknownValue("objective profile", reason),
                unknownValue("objective strategy", reason),
                unknownValue("typed objective board scan", reason));
    }

    private static <T> FactValue<T> unknownValue(String provenance, String reason) {
        return FactValue.unknown(PRODUCER, provenance, reason);
    }

    public record Identity(
            int objectivePermanentCardId,
            int objectiveCurrentCardId,
            String canonicalFrontBlueprintId,
            String canonicalBackBlueprintId,
            String currentBlueprintId,
            String oppositeBlueprintId,
            String canonicalFrontTitle,
            String canonicalBackTitle,
            String currentTitle,
            String oppositeTitle,
            String canonicalFrontGameText,
            String canonicalBackGameText,
            String currentGameText,
            String oppositeGameText,
            boolean flipped) {

        public Identity {
            if (objectivePermanentCardId < 0) {
                throw new IllegalArgumentException("objectivePermanentCardId must be >= 0");
            }
            if (objectiveCurrentCardId < 0) {
                throw new IllegalArgumentException("objectiveCurrentCardId must be >= 0");
            }
            requireNonBlank(canonicalFrontBlueprintId, "canonicalFrontBlueprintId");
            requireNonBlank(canonicalBackBlueprintId, "canonicalBackBlueprintId");
            requireNonBlank(currentBlueprintId, "currentBlueprintId");
            requireNonBlank(oppositeBlueprintId, "oppositeBlueprintId");
            requireNonBlank(canonicalFrontTitle, "canonicalFrontTitle");
            requireNonBlank(canonicalBackTitle, "canonicalBackTitle");
            requireNonBlank(currentTitle, "currentTitle");
            requireNonBlank(oppositeTitle, "oppositeTitle");
            canonicalFrontGameText = Objects.requireNonNullElse(canonicalFrontGameText, "");
            canonicalBackGameText = Objects.requireNonNullElse(canonicalBackGameText, "");
            currentGameText = Objects.requireNonNullElse(currentGameText, "");
            oppositeGameText = Objects.requireNonNullElse(oppositeGameText, "");

            if (flipped) {
                requireEqual(canonicalBackBlueprintId, currentBlueprintId,
                        "flipped current blueprint must be canonical back");
                requireEqual(canonicalFrontBlueprintId, oppositeBlueprintId,
                        "flipped opposite blueprint must be canonical front");
                requireEqual(canonicalBackTitle, currentTitle,
                        "flipped current title must be canonical back");
                requireEqual(canonicalFrontTitle, oppositeTitle,
                        "flipped opposite title must be canonical front");
                requireEqual(canonicalBackGameText, currentGameText,
                        "flipped current game text must be canonical back");
                requireEqual(canonicalFrontGameText, oppositeGameText,
                        "flipped opposite game text must be canonical front");
            } else {
                requireEqual(canonicalFrontBlueprintId, currentBlueprintId,
                        "unflipped current blueprint must be canonical front");
                requireEqual(canonicalBackBlueprintId, oppositeBlueprintId,
                        "unflipped opposite blueprint must be canonical back");
                requireEqual(canonicalFrontTitle, currentTitle,
                        "unflipped current title must be canonical front");
                requireEqual(canonicalBackTitle, oppositeTitle,
                        "unflipped opposite title must be canonical back");
                requireEqual(canonicalFrontGameText, currentGameText,
                        "unflipped current game text must be canonical front");
                requireEqual(canonicalBackGameText, oppositeGameText,
                        "unflipped opposite game text must be canonical back");
            }
        }
    }

    public record ProfileResolution(
            MatchKind matchKind,
            String label,
            boolean loaderEnabled,
            boolean hydratedFromJson,
            boolean compiledFallbackUsed) {

        public enum MatchKind {
            BLUEPRINT_ID,
            TITLE_COMPATIBILITY,
            NONE
        }

        public ProfileResolution {
            Objects.requireNonNull(matchKind, "matchKind");
            label = label != null ? label : "";
            if (matchKind != MatchKind.NONE && label.isBlank()) {
                throw new IllegalArgumentException("matched objective profile requires a label");
            }
            if (hydratedFromJson && !loaderEnabled) {
                throw new IllegalArgumentException("JSON hydration requires loaderEnabled");
            }
        }
    }

    public record StrategyFacts(
            ObjectiveKind kind,
            Set<String> flipConditionLocationFragments,
            Set<String> flipBackLocationFragments,
            Set<String> requiredCardsOnTable,
            Set<String> pullableCards,
            StartingRefs startingRefs,
            Set<String> flipCriticalControlCardIds,
            FactValue<String> flipCriticalControlSite,
            FactValue<String> flipCriticalControlCard,
            FactValue<String> flipConditionText,
            FactValue<String> flipBackConditionText,
            boolean requiresOccupy,
            boolean requiresControl,
            boolean flipBackRequiresOccupy,
            boolean flipBackRequiresControl,
            Set<String> strategyCharacterTokens) {

        public StrategyFacts {
            Objects.requireNonNull(kind, "kind");
            flipConditionLocationFragments = Set.copyOf(flipConditionLocationFragments);
            flipBackLocationFragments = Set.copyOf(flipBackLocationFragments);
            requiredCardsOnTable = Set.copyOf(requiredCardsOnTable);
            pullableCards = Set.copyOf(pullableCards);
            Objects.requireNonNull(startingRefs, "startingRefs");
            flipCriticalControlCardIds = Set.copyOf(flipCriticalControlCardIds);
            Objects.requireNonNull(flipCriticalControlSite, "flipCriticalControlSite");
            Objects.requireNonNull(flipCriticalControlCard, "flipCriticalControlCard");
            Objects.requireNonNull(flipConditionText, "flipConditionText");
            Objects.requireNonNull(flipBackConditionText, "flipBackConditionText");
            strategyCharacterTokens = Set.copyOf(strategyCharacterTokens);
        }
    }

    public record ObjectiveKind(
            boolean myLord,
            boolean endorOperations,
            boolean invasion,
            boolean hiddenPath,
            boolean huntDownVirtual,
            boolean wantThatMap) {
    }

    public record StartingRefs(
            Set<String> locationBlueprintIds,
            Set<String> locationTitleFragments,
            Set<String> effectBlueprintIds,
            Set<String> effectTitleFragments,
            Set<String> interruptBlueprintIds,
            Set<String> interruptTitleFragments) {

        public StartingRefs {
            locationBlueprintIds = Set.copyOf(locationBlueprintIds);
            locationTitleFragments = Set.copyOf(locationTitleFragments);
            effectBlueprintIds = Set.copyOf(effectBlueprintIds);
            effectTitleFragments = Set.copyOf(effectTitleFragments);
            interruptBlueprintIds = Set.copyOf(interruptBlueprintIds);
            interruptTitleFragments = Set.copyOf(interruptTitleFragments);
        }
    }

    public record TypedBoardFacts(
            Set<Integer> senatorCardIds,
            Set<Integer> jediSurvivorCardIds,
            Set<Integer> inquisitorCardIds,
            Set<Integer> inquisitorWithHatredCardIds,
            Set<Integer> galacticSenateLocationIds,
            Set<Integer> objectiveRelevantLocationIds,
            Set<Integer> flipBackProtectionLocationIds,
            boolean nonSenateSiteOnTable,
            int hiddenPathNonMapuzoJediSiteCount,
            boolean hiddenPathFlipConditionMet,
            boolean invasionThroneRoomControlledWithNeimoidian,
            boolean invasionNabooSystemControlled,
            boolean controlsFlipCriticalSite) {

        public TypedBoardFacts {
            senatorCardIds = Set.copyOf(senatorCardIds);
            jediSurvivorCardIds = Set.copyOf(jediSurvivorCardIds);
            inquisitorCardIds = Set.copyOf(inquisitorCardIds);
            inquisitorWithHatredCardIds = Set.copyOf(inquisitorWithHatredCardIds);
            galacticSenateLocationIds = Set.copyOf(galacticSenateLocationIds);
            objectiveRelevantLocationIds = Set.copyOf(objectiveRelevantLocationIds);
            flipBackProtectionLocationIds = Set.copyOf(flipBackProtectionLocationIds);
            if (hiddenPathNonMapuzoJediSiteCount < 0) {
                throw new IllegalArgumentException("hiddenPathNonMapuzoJediSiteCount must be >= 0");
            }
            if (hiddenPathFlipConditionMet != (hiddenPathNonMapuzoJediSiteCount >= 2)) {
                throw new IllegalArgumentException("Hidden Path flip truth must match the typed site count");
            }
        }
    }

    private static void requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must be nonblank");
        }
    }

    private static void requireEqual(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new IllegalArgumentException(message + ": expected=" + expected + " actual=" + actual);
        }
    }
}
