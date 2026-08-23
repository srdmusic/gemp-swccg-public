package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Engine-backed V298 fact adapter shared by Rando and Chosen One. */
public final class SpaceDeploymentAllocationFactsReader {
    private SpaceDeploymentAllocationFactsReader() {
    }

    public static Optional<SpaceDeploymentAllocationPolicy.Evaluation>
            evaluateParent(
                    String actionId,
                    SwccgGame game,
                    String playerId,
                    PhysicalCard deployingCard,
                    String actionText,
                    String plannedTargetId,
                    boolean typedSpaceObjectiveNeed,
                    boolean repilotPlan,
                    int lifeForce,
                    int criticalLifeForce) {
        if (game == null || game.getGameState() == null
                || playerId == null || deployingCard == null
                || deployingCard.getBlueprint() == null) {
            return Optional.empty();
        }
        GameState gameState = game.getGameState();
        CardCategory category =
                deployingCard.getBlueprint().getCardCategory();
        String actionLower = actionText != null
                ? actionText.toLowerCase(Locale.ROOT) : "";
        boolean starshipRoute = category == CardCategory.STARSHIP;
        boolean explicitAboardCharacter = category == CardCategory.CHARACTER
                && (actionLower.contains(" aboard")
                    || actionLower.contains(" as pilot")
                    || actionLower.contains(" pilot aboard"));
        if (!starshipRoute && !explicitAboardCharacter) {
            return Optional.empty();
        }

        PhysicalCard destination = findById(gameState, plannedTargetId);
        if (destination == null) {
            destination = findAssetNamedInText(gameState, actionLower);
        }
        List<PhysicalCard> locations = new ArrayList<>();
        PhysicalCard exactLocation = resolveSpaceLocation(
                game, gameState, destination);
        if (exactLocation != null) {
            locations.add(exactLocation);
        } else {
            for (PhysicalCard location : gameState.getLocationsInOrder()) {
                if (isSpaceLocation(location)
                        && (starshipRoute
                            || actionLower.contains(location.getTitle()
                                    .toLowerCase(Locale.ROOT)))) {
                    locations.add(location);
                }
            }
        }
        if (locations.isEmpty()) return Optional.empty();

        boolean orphanRepilot = repilotPlan;
        if (!orphanRepilot && explicitAboardCharacter
                && deployingCard.getBlueprint().hasIcon(Icon.PILOT)
                && destination != null) {
            try {
                orphanRepilot = !Filters.piloted.accepts(
                        gameState, game.getModifiersQuerying(), destination);
            } catch (RuntimeException ignored) {
                orphanRepilot = false;
            }
        }

        SpaceDeploymentAllocationPolicy.Evaluation deferred = null;
        for (PhysicalCard location : locations) {
            Optional<SpaceDeploymentAllocationPolicy.Evaluation> evaluation =
                    evaluateAtLocation(
                            actionId, game, playerId, deployingCard,
                            location, typedSpaceObjectiveNeed,
                            orphanRepilot, lifeForce, criticalLifeForce);
            if (evaluation.isEmpty()) return Optional.empty();
            if (!SpaceDeploymentAllocationPolicy.isDeferred(
                    evaluation.get())) {
                return evaluation;
            }
            if (deferred == null) deferred = evaluation.get();
        }
        return Optional.ofNullable(deferred);
    }

    public static Optional<SpaceDeploymentAllocationPolicy.Evaluation>
            evaluateDestination(
                    String actionId,
                    SwccgGame game,
                    String playerId,
                    PhysicalCard deployingCard,
                    SwccgCardBlueprint deployingBlueprint,
                    PhysicalCard destination,
                    boolean destinationNeedsPilot,
                    boolean typedSpaceObjectiveNeed,
                    int lifeForce,
                    int criticalLifeForce) {
        if (game == null || game.getGameState() == null
                || playerId == null || deployingBlueprint == null
                || destination == null) {
            return Optional.empty();
        }
        CardCategory category = deployingBlueprint.getCardCategory();
        if (category != CardCategory.CHARACTER
                && category != CardCategory.STARSHIP
                && category != CardCategory.VEHICLE) {
            return Optional.empty();
        }
        PhysicalCard location = resolveSpaceLocation(
                game, game.getGameState(), destination);
        if (location == null) return Optional.empty();
        boolean orphanRepilot = category == CardCategory.CHARACTER
                && deployingBlueprint.hasIcon(Icon.PILOT)
                && destinationNeedsPilot;
        return evaluateAtLocation(
                actionId, game, playerId, deployingCard,
                deployingBlueprint, location, typedSpaceObjectiveNeed,
                orphanRepilot, lifeForce, criticalLifeForce);
    }

    public static PhysicalCard resolveSpaceLocation(
            SwccgGame game, GameState gameState, PhysicalCard destination) {
        if (game == null || gameState == null || destination == null
                || destination.getBlueprint() == null) {
            return null;
        }
        PhysicalCard location = destination;
        CardCategory category = destination.getBlueprint().getCardCategory();
        if (category == CardCategory.STARSHIP
                || category == CardCategory.VEHICLE) {
            try {
                location = game.getModifiersQuerying()
                        .getLocationThatCardIsAt(gameState, destination);
            } catch (RuntimeException unavailable) {
                return null;
            }
        }
        return isSpaceLocation(location) ? location : null;
    }

    public static PhysicalCard findSimultaneousShip(
            GameState gameState, String shipName) {
        if (gameState == null || shipName == null) return null;
        String offered = normalizeTitle(shipName);
        PhysicalCard best = null;
        int bestLength = -1;
        for (PhysicalCard card : gameState.getAllPermanentCards()) {
            if (card == null || card.getBlueprint() == null
                    || card.getTitle() == null) continue;
            CardCategory category = card.getBlueprint().getCardCategory();
            if (category != CardCategory.STARSHIP
                    && category != CardCategory.VEHICLE) continue;
            String candidate = normalizeTitle(card.getTitle());
            if ((offered.contains(candidate) || candidate.contains(offered))
                    && candidate.length() > bestLength) {
                best = card;
                bestLength = candidate.length();
            }
        }
        return best;
    }

    public static boolean isMatchingPilot(
            SwccgGame game, PhysicalCard pilot, PhysicalCard ship) {
        if (game == null || game.getGameState() == null
                || pilot == null || ship == null) return false;
        try {
            return game.getModifiersQuerying().isMatchingPair(
                    game.getGameState(), pilot, ship);
        } catch (RuntimeException unavailable) {
            return false;
        }
    }

    /** Null means the card-source modifier list could not be read. */
    public static Boolean readsAddsPowerWhenPiloting(
            SwccgGame game, PhysicalCard pilot) {
        if (game == null || pilot == null
                || pilot.getBlueprint() == null) return null;
        try {
            List<com.gempukku.swccgo.logic.modifiers.Modifier> modifiers =
                    pilot.getBlueprint()
                            .getWhileInPlayModifiers(game, pilot);
            if (modifiers == null) return null;
            return modifiers.stream()
                    .anyMatch(modifier -> modifier
                            instanceof AddsPowerToPilotedBySelfModifier);
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    public static int plannerPilotQualityTier(
            SwccgGame game, PhysicalCard pilot, PhysicalCard ship) {
        return DeployPilotShipPolicy.plannerPilotQualityTier(
                new DeployPilotShipPolicy.PlannerPilotQualityFacts(
                        isMatchingPilot(game, pilot, ship),
                        Boolean.TRUE.equals(
                                readsAddsPowerWhenPiloting(game, pilot))));
    }

    private static Optional<SpaceDeploymentAllocationPolicy.Evaluation>
            evaluateAtLocation(
                    String actionId,
                    SwccgGame game,
                    String playerId,
                    PhysicalCard deployingCard,
                    PhysicalCard location,
                    boolean typedSpaceObjectiveNeed,
                    boolean orphanRepilot,
                    int lifeForce,
                    int criticalLifeForce) {
        return evaluateAtLocation(
                actionId, game, playerId, deployingCard,
                deployingCard.getBlueprint(), location,
                typedSpaceObjectiveNeed, orphanRepilot,
                lifeForce, criticalLifeForce);
    }

    private static Optional<SpaceDeploymentAllocationPolicy.Evaluation>
            evaluateAtLocation(
                    String actionId,
                    SwccgGame game,
                    String playerId,
                    PhysicalCard deployingCard,
                    SwccgCardBlueprint deployingBlueprint,
                    PhysicalCard location,
                    boolean typedSpaceObjectiveNeed,
                    boolean orphanRepilot,
                    int lifeForce,
                    int criticalLifeForce) {
        try {
            GameState gameState = game.getGameState();
            String opponentId = gameState.getOpponent(playerId);
            float currentAbility = game.getModifiersQuerying()
                    .getTotalAbilityAtLocation(
                            gameState, playerId, location);
            float candidateAbility = 0.0f;
            if (deployingCard != null) {
                candidateAbility = game.getModifiersQuerying()
                        .getAbility(gameState, deployingCard, true);
            } else if (deployingBlueprint.hasAbilityAttribute()
                    && deployingBlueprint.getAbility() != null) {
                candidateAbility = deployingBlueprint.getAbility();
            }
            boolean opponentPressure = false;
            if (opponentId != null) {
                for (PhysicalCard card :
                        gameState.getCardsAtLocation(location)) {
                    if (card != null && opponentId.equals(card.getOwner())) {
                        opponentPressure = true;
                        break;
                    }
                }
            }
            float ourPower = game.getModifiersQuerying()
                    .getTotalPowerAtLocation(
                            gameState, location, playerId, false, false);
            float opponentPower = opponentId != null
                    ? game.getModifiersQuerying().getTotalPowerAtLocation(
                            gameState, location, opponentId,
                            false, false)
                    : 0.0f;
            float candidatePower = deployingBlueprint.getCardCategory()
                            == CardCategory.STARSHIP
                    && deployingBlueprint.hasPowerAttribute()
                    && deployingBlueprint.getPower() != null
                    ? deployingBlueprint.getPower() : 0.0f;
            boolean favorableBattle = opponentPressure
                    && opponentPower > 0.0f
                    && ourPower + candidatePower >= opponentPower;
            return Optional.of(SpaceDeploymentAllocationPolicy.evaluate(
                    new SpaceDeploymentAllocationPolicy.Facts(
                            actionId, true, currentAbility,
                            currentAbility + candidateAbility,
                            opponentPressure, favorableBattle,
                            typedSpaceObjectiveNeed, orphanRepilot,
                            opponentPressure
                                    && lifeForce <= criticalLifeForce)));
        } catch (RuntimeException unavailable) {
            return Optional.empty();
        }
    }

    private static boolean isSpaceLocation(PhysicalCard location) {
        if (location == null || location.getBlueprint() == null) return false;
        CardSubtype subtype = location.getBlueprint().getCardSubtype();
        return subtype == CardSubtype.SYSTEM || subtype == CardSubtype.SECTOR;
    }

    private static PhysicalCard findById(
            GameState gameState, String cardId) {
        if (cardId == null) return null;
        try {
            return gameState.findCardById(Integer.parseInt(cardId));
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    private static PhysicalCard findAssetNamedInText(
            GameState gameState, String actionLower) {
        for (PhysicalCard card : gameState.getAllPermanentCards()) {
            if (card == null || card.getTitle() == null
                    || card.getBlueprint() == null) continue;
            CardCategory category = card.getBlueprint().getCardCategory();
            if ((category == CardCategory.STARSHIP
                    || category == CardCategory.VEHICLE)
                    && actionLower.contains(
                            card.getTitle().toLowerCase(Locale.ROOT))) {
                return card;
            }
        }
        return null;
    }

    private static String normalizeTitle(String title) {
        return title == null ? "" : title.toLowerCase(Locale.ROOT)
                .replace("•", "")
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
