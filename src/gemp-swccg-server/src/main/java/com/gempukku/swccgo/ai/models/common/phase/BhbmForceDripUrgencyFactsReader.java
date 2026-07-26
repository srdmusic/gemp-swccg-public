package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;

import java.util.Collection;

/**
 * Reads one immutable, candidate-simulated BHBM duel-trio snapshot.
 *
 * <p>The three roles follow Card9_151_BACK's distinct source semantics:
 * active Emperor, active Vader, and the current source-modified target with
 * the source's captive, frozen-captive, and targetability overrides. Counts
 * are role booleans, never physical-card totals or time-separated samples.
 */
public final class BhbmForceDripUrgencyFactsReader {
    private static final String BHBM_BLUEPRINT = "9_151";
    public static final String ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA =
            "bhbmActionSourcePermanentCardId";

    public enum CandidateMechanism {
        DEPLOY,
        LANDSPEED,
        OTHER
    }

    private BhbmForceDripUrgencyFactsReader() {
    }

    public static CaptureObjectivePolicy.BhbmForceDripUrgencyFacts read(
            String actionId,
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer analyzer,
            PhysicalCard actionSource,
            PhysicalCard candidate,
            PhysicalCard destination,
            boolean safe,
            CandidateMechanism mechanism) {
        boolean backSideUp = analyzer != null
                && analyzer.isFlipped();
        boolean flipAgeKnown = analyzer != null
                && analyzer.isFlipAgeKnown();
        int flipAge = analyzer != null
                ? analyzer.getTurnsObservedSinceFlip() : 0;
        boolean ownsSource = actionSource != null
                && playerId != null
                && playerId.equals(actionSource.getOwner());
        boolean ownsCandidate = candidate != null
                && playerId != null
                && playerId.equals(candidate.getOwner());

        if (actionId == null || actionId.isBlank()
                || game == null || playerId == null
                || analyzer == null || candidate == null
                || destination == null || mechanism == null
                || !BHBM_BLUEPRINT.equals(
                    normalizeBlueprintId(
                        analyzer.getObjectiveBlueprintId()))
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return facts(
                    safeActionId(actionId), backSideUp,
                    false, flipAgeKnown, flipAge,
                    false, safe, false,
                    ownsSource, ownsCandidate,
                    false, 0, 0, false);
        }

        try {
            GameState gameState = game.getGameState();
            ModifiersQuerying modifiers =
                    game.getModifiersQuerying();
            Collection<PhysicalCard> cards =
                    gameState.getAllPermanentCards();
            PhysicalCard objective =
                    findLiveObjective(
                        game, playerId, cards);
            if (objective == null
                    || cards == null) {
                return facts(
                        actionId, backSideUp,
                        false, flipAgeKnown, flipAge,
                        false, safe, false,
                        ownsSource, ownsCandidate,
                        false, 0, 0, false);
            }

            Filter targetFilter =
                    sourceTargetFilter(
                        gameState, modifiers, objective);
            boolean candidatePresentAtThroneRoom =
                    Filters.Throne_Room.accepts(
                        gameState, modifiers,
                        destination);
            RoleState before = roleStateBefore(
                    game, objective, targetFilter,
                    cards);
            RoleState after = roleStateAfter(
                    game, objective, targetFilter,
                    cards, candidate,
                    destination, before);
            boolean stableBefore = stableBackBefore(
                    game, targetFilter, cards);
            boolean preservesStableBack =
                    stableBefore
                    && switch (mechanism) {
                        case DEPLOY -> true;
                        case LANDSPEED ->
                                stableBackAfterLandspeed(
                                    game, targetFilter,
                                    cards, candidate,
                                    destination);
                        case OTHER -> false;
                    };
            boolean forceDripActive =
                    modifiers.getForceToLoseFromCardLimit(
                        gameState, playerId,
                        objective) > 0.0f;

            return facts(
                    actionId, backSideUp,
                    forceDripActive,
                    flipAgeKnown, flipAge,
                    true, safe,
                    candidatePresentAtThroneRoom,
                    ownsSource, ownsCandidate,
                    stableBefore,
                    before.count(), after.count(),
                    preservesStableBack);
        } catch (Exception ignored) {
            return facts(
                    actionId, backSideUp,
                    false, flipAgeKnown, flipAge,
                    false, safe, false,
                    ownsSource, ownsCandidate,
                    false, 0, 0, false);
        }
    }

    private static CaptureObjectivePolicy.BhbmForceDripUrgencyFacts facts(
            String actionId,
            boolean backSideUp,
            boolean forceDripActive,
            boolean flipAgeKnown,
            int flipAge,
            boolean factsKnown,
            boolean safe,
            boolean candidatePresentAtThroneRoom,
            boolean ownsSource,
            boolean ownsCandidate,
            boolean stableBefore,
            int before,
            int after,
            boolean preservesStableBack) {
        return new CaptureObjectivePolicy.BhbmForceDripUrgencyFacts(
                actionId,
                CaptureObjectivePolicy.ObjectiveKind.BHBM,
                backSideUp,
                forceDripActive,
                flipAgeKnown,
                flipAge,
                factsKnown,
                safe,
                candidatePresentAtThroneRoom,
                ownsSource,
                ownsCandidate,
                stableBefore,
                before,
                after,
                preservesStableBack);
    }

    private static String safeActionId(String actionId) {
        return actionId == null || actionId.isBlank()
                ? "unknown-bhbm-action" : actionId;
    }

    private static PhysicalCard findLiveObjective(
            SwccgGame game,
            String playerId,
            Collection<PhysicalCard> cards) {
        if (cards == null) {
            return null;
        }
        for (PhysicalCard card : cards) {
            if (card == null
                    || !playerId.equals(card.getOwner())
                    || card.getBlueprint() == null
                    || card.getBlueprint().getCardCategory()
                        != CardCategory.OBJECTIVE
                    || !isActive(
                        game, card, false, false)
                    || !card.isFlipped()
                    || !BHBM_BLUEPRINT.equals(
                        normalizeBlueprintId(
                            card.getBlueprintId(true)))) {
                continue;
            }
            return card;
        }
        return null;
    }

    private static Filter sourceTargetFilter(
            GameState gameState,
            ModifiersQuerying modifiers,
            PhysicalCard objective) {
        if (modifiers.hasGameTextModification(
                gameState, objective,
                ModifyGameTextType
                    .BRING_HIM_BEFORE_ME__TARGETS_LEIA_INSTEAD_OF_LUKE)) {
            return Filters.Leia;
        }
        if (modifiers.hasGameTextModification(
                gameState, objective,
                ModifyGameTextType
                    .BRING_HIM_BEFORE_ME__TARGETS_KANAN_INSTEAD_OF_LUKE)) {
            return Filters.Kanan;
        }
        return Filters.Luke;
    }

    private static RoleState roleStateBefore(
            SwccgGame game,
            PhysicalCard objective,
            Filter targetFilter,
            Collection<PhysicalCard> cards) {
        boolean vader = false;
        boolean emperor = false;
        boolean target = false;
        for (PhysicalCard card : cards) {
            if (!vader
                    && activeAtThrone(
                        game, card, Filters.Vader)) {
                vader = true;
            }
            if (!emperor
                    && activeAtThrone(
                        game, card, Filters.Emperor)) {
                emperor = true;
            }
            if (!target
                    && duelTargetAtThrone(
                        game, objective,
                        targetFilter, card)) {
                target = true;
            }
        }
        return new RoleState(vader, emperor, target);
    }

    private static RoleState roleStateAfter(
            SwccgGame game,
            PhysicalCard objective,
            Filter targetFilter,
            Collection<PhysicalCard> cards,
            PhysicalCard candidate,
            PhysicalCard destination,
            RoleState before) {
        GameState gameState = game.getGameState();
        ModifiersQuerying modifiers =
                game.getModifiersQuerying();
        boolean vader = before.vader()
                || Filters.Vader.accepts(
                    gameState, modifiers, candidate);
        boolean emperor = before.emperor()
                || Filters.Emperor.accepts(
                    gameState, modifiers, candidate);
        boolean target = before.target()
                || duelTarget(
                    game, objective,
                    targetFilter, candidate);

        if (!target) {
            for (PhysicalCard card : cards) {
                if (card != null
                        && isActive(
                            game, card, true, false)
                        && card.isCaptive()
                        && sameCard(
                            card.getEscort(), candidate)
                        && duelTarget(
                            game, objective,
                            targetFilter, card)) {
                    target = true;
                    break;
                }
            }
        }

        boolean destinationIsThrone =
                Filters.Throne_Room.accepts(
                    gameState, modifiers,
                    destination);
        return destinationIsThrone
                ? new RoleState(vader, emperor, target)
                : before;
    }

    private static boolean activeAtThrone(
            SwccgGame game,
            PhysicalCard card,
            Filter roleFilter) {
        return isActive(game, card, false, false)
                && roleFilter.accepts(
                    game.getGameState(),
                    game.getModifiersQuerying(), card)
                && atThrone(game, card);
    }

    private static boolean duelTargetAtThrone(
            SwccgGame game,
            PhysicalCard objective,
            Filter targetFilter,
            PhysicalCard card) {
        return isActive(game, card, true, false)
                && duelTarget(
                    game, objective,
                    targetFilter, card)
                && atThrone(game, card);
    }

    private static boolean duelTarget(
            SwccgGame game,
            PhysicalCard objective,
            Filter targetFilter,
            PhysicalCard card) {
        if (card == null) {
            return false;
        }
        GameState gameState = game.getGameState();
        ModifiersQuerying modifiers =
                game.getModifiersQuerying();
        return targetFilter.accepts(
                    gameState, modifiers, card)
                && Filters.not(
                    Filters.frozenCaptive)
                    .accepts(
                        gameState, modifiers, card)
                && Filters.canBeTargetedBy(
                    objective,
                    TargetingReason.TO_BE_DUELED)
                    .accepts(
                        gameState, modifiers, card);
    }

    private static boolean stableBackBefore(
            SwccgGame game,
            Filter targetFilter,
            Collection<PhysicalCard> cards) {
        for (PhysicalCard target : cards) {
            if (!isActive(game, target, true, true)
                    || !targetFilter.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(),
                        target)) {
                continue;
            }
            if (target.isCaptive()
                    || hasVaderPresentWith(
                        game, cards, target,
                        null, null)) {
                return true;
            }
        }
        return false;
    }

    private static boolean stableBackAfterLandspeed(
            SwccgGame game,
            Filter targetFilter,
            Collection<PhysicalCard> cards,
            PhysicalCard candidate,
            PhysicalCard effectiveDestination) {
        for (PhysicalCard target : cards) {
            if (!isActive(game, target, true, true)
                    || !targetFilter.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(),
                        target)) {
                continue;
            }
            if (target.isCaptive()
                    || hasVaderPresentWith(
                        game, cards, target,
                        candidate,
                        effectiveDestination)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasVaderPresentWith(
            SwccgGame game,
            Collection<PhysicalCard> cards,
            PhysicalCard target,
            PhysicalCard movingCandidate,
            PhysicalCard candidateDestination) {
        for (PhysicalCard card : cards) {
            if (!isActive(game, card, false, true)
                    || !Filters.Vader.accepts(
                        game.getGameState(),
                        game.getModifiersQuerying(),
                        card)) {
                continue;
            }
            if (movingCandidate == null
                    && game.getModifiersQuerying()
                        .isPresentWith(
                            game.getGameState(),
                            card, target)) {
                return true;
            }
            if (movingCandidate != null
                    && sameCard(
                        sameCard(card, movingCandidate)
                            ? candidateDestination
                            : presentLocation(game, card),
                        sameCard(target, movingCandidate)
                            ? candidateDestination
                            : presentLocation(game, target))) {
                return true;
            }
        }
        return false;
    }

    private static boolean atThrone(
            SwccgGame game,
            PhysicalCard card) {
        PhysicalCard location =
                presentLocation(game, card);
        return location != null
                && Filters.Throne_Room.accepts(
                    game.getGameState(),
                    game.getModifiersQuerying(),
                    location);
    }

    private static PhysicalCard presentLocation(
            SwccgGame game,
            PhysicalCard card) {
        try {
            return game.getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                        game.getGameState(), card);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isActive(
            SwccgGame game,
            PhysicalCard card,
            boolean includeCaptives,
            boolean includeExcludedFromBattle) {
        if (card == null) {
            return false;
        }
        return game.getGameState()
                .isCardInPlayActive(
                    card,
                    includeExcludedFromBattle,
                    false,
                    includeCaptives,
                    false,
                    false,
                    false,
                    false,
                    false);
    }

    private static boolean sameCard(
            PhysicalCard first,
            PhysicalCard second) {
        if (first == second) {
            return first != null;
        }
        return first != null && second != null
                && first.getPermanentCardId() > 0
                && first.getPermanentCardId()
                    == second.getPermanentCardId();
    }

    private static String normalizeBlueprintId(
            String blueprintId) {
        if (blueprintId == null) {
            return null;
        }
        String normalized = blueprintId.trim();
        if (normalized.endsWith("_BACK")) {
            normalized = normalized.substring(
                    0,
                    normalized.length()
                        - "_BACK".length());
        }
        return normalized;
    }

    private record RoleState(
            boolean vader,
            boolean emperor,
            boolean target) {
        private int count() {
            int count = 0;
            if (vader) count++;
            if (emperor) count++;
            if (target) count++;
            return count;
        }
    }
}
