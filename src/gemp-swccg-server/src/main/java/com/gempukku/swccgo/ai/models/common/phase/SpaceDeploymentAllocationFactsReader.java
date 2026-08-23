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

/** Engine-backed space-allocation and pilot facts shared by both bots. */
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

    public static boolean isStarDestroyer(
            SwccgGame game, PhysicalCard ship) {
        if (game == null || game.getGameState() == null
                || ship == null) return false;
        try {
            return Filters.Star_Destroyer.accepts(
                    game.getGameState(), game.getModifiersQuerying(), ship);
        } catch (RuntimeException unavailable) {
            return false;
        }
    }

    public static boolean isStormtrooperFamily(
            SwccgGame game, PhysicalCard pilot) {
        if (game == null || game.getGameState() == null
                || pilot == null) return false;
        try {
            return Filters.stormtrooper.accepts(
                    game.getGameState(), game.getModifiersQuerying(), pilot);
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
                    readProspectivePilotModifiers(game, pilot);
            if (modifiers == null) return null;
            return modifiers.stream()
                    .anyMatch(modifier -> modifier
                            instanceof AddsPowerToPilotedBySelfModifier);
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    /** Null means exact intrinsic power cannot be projected safely. */
    public static Float powerAddedIfPiloting(
            SwccgGame game, PhysicalCard pilot, PhysicalCard shipOrVehicle) {
        if (game == null || game.getGameState() == null
                || pilot == null || pilot.getBlueprint() == null
                || shipOrVehicle == null) return null;
        try {
            List<com.gempukku.swccgo.logic.modifiers.Modifier> modifiers =
                    readProspectivePilotModifiers(game, pilot);
            if (modifiers == null) return null;
            float total = 0.0f;
            for (com.gempukku.swccgo.logic.modifiers.Modifier modifier
                    : modifiers) {
                if (modifier instanceof AddsPowerToPilotedBySelfModifier) {
                    Float amount = ((AddsPowerToPilotedBySelfModifier) modifier)
                            .getProspectiveIntrinsicPowerModifier(
                                    game.getGameState(),
                                    game.getModifiersQuerying(),
                                    shipOrVehicle);
                    if (amount == null) return null;
                    total += amount;
                    if (!Float.isFinite(total)) return null;
                }
            }
            return total;
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    private static List<com.gempukku.swccgo.logic.modifiers.Modifier>
            readProspectivePilotModifiers(
                    SwccgGame game, PhysicalCard pilot) {
        List<com.gempukku.swccgo.logic.modifiers.Modifier> alwaysOn =
                pilot.getBlueprint().getAlwaysOnModifiers(game, pilot);
        List<com.gempukku.swccgo.logic.modifiers.Modifier> whileInPlay =
                pilot.getBlueprint().getWhileInPlayModifiers(game, pilot);
        if (alwaysOn == null || whileInPlay == null) return null;
        List<com.gempukku.swccgo.logic.modifiers.Modifier> modifiers =
                new ArrayList<>(alwaysOn.size() + whileInPlay.size());
        modifiers.addAll(alwaysOn);
        modifiers.addAll(whileInPlay);
        return modifiers;
    }

    public static Optional<DeployPilotShipPolicy.ExactPilotAssignmentFacts>
            readExactPilotAssignmentFacts(
                    String actionId, SwccgGame game, PhysicalCard pilot,
                    PhysicalCard shipOrVehicle,
                    boolean specificallyReferencesDestination) {
        if (game == null || game.getGameState() == null
                || pilot == null || pilot.getBlueprint() == null
                || shipOrVehicle == null
                || shipOrVehicle.getBlueprint() == null) {
            return Optional.empty();
        }
        CardCategory destinationCategory =
                shipOrVehicle.getBlueprint().getCardCategory();
        if (pilot.getBlueprint().getCardCategory() != CardCategory.CHARACTER
                || (destinationCategory != CardCategory.STARSHIP
                    && destinationCategory != CardCategory.VEHICLE)) {
            return Optional.empty();
        }
        boolean destinationStarDestroyer;
        boolean stormtrooperFamily;
        boolean matchingPilot;
        boolean destinationNeedsPilot;
        try {
            destinationStarDestroyer = Filters.Star_Destroyer.accepts(
                    game.getGameState(), game.getModifiersQuerying(),
                    shipOrVehicle);
            stormtrooperFamily = Filters.stormtrooper.accepts(
                    game.getGameState(), game.getModifiersQuerying(), pilot);
            matchingPilot = game.getModifiersQuerying().isMatchingPair(
                    game.getGameState(), pilot, shipOrVehicle);
            destinationNeedsPilot = !Filters.piloted.accepts(
                    game.getGameState(), game.getModifiersQuerying(),
                    shipOrVehicle);
        } catch (RuntimeException unavailable) {
            return Optional.empty();
        }
        return Optional.of(
                new DeployPilotShipPolicy.ExactPilotAssignmentFacts(
                        actionId,
                        pilot.getTitle(), shipOrVehicle.getTitle(),
                        destinationCategory == CardCategory.STARSHIP,
                        destinationStarDestroyer,
                        stormtrooperFamily,
                        matchingPilot,
                        specificallyReferencesDestination,
                        destinationNeedsPilot,
                        abilityFourBuddyProgress(
                                game, pilot, shipOrVehicle),
                        powerAddedIfPiloting(game, pilot, shipOrVehicle)));
    }

    /** Null means the exact ability projection could not be read safely. */
    private static Boolean abilityFourBuddyProgress(
            SwccgGame game, PhysicalCard pilot,
            PhysicalCard shipOrVehicle) {
        if (shipOrVehicle.getBlueprint().getCardCategory()
                != CardCategory.STARSHIP || pilot.getOwner() == null) {
            return false;
        }
        try {
            GameState gameState = game.getGameState();
            float pilotAbility = game.getModifiersQuerying()
                    .getAbility(gameState, pilot, true);
            PhysicalCard spaceLocation = resolveSpaceLocation(
                    game, gameState, shipOrVehicle);
            float currentAbility;
            if (spaceLocation != null) {
                currentAbility = game.getModifiersQuerying()
                        .getTotalAbilityAtLocation(
                                gameState, pilot.getOwner(),
                                spaceLocation);
                if (shipOrVehicle.getZone() == null
                        || !shipOrVehicle.getZone().isInPlay()) {
                    currentAbility += game.getModifiersQuerying()
                            .getAbility(gameState, shipOrVehicle, true);
                }
            } else {
                currentAbility = game.getModifiersQuerying()
                        .getAbility(gameState, shipOrVehicle, true);
            }
            if (!Float.isFinite(currentAbility)
                    || !Float.isFinite(pilotAbility)) {
                return null;
            }
            return currentAbility < 4.0f && pilotAbility > 0.0f;
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    public static boolean isPlannerPilotEligible(
            SwccgGame game, PhysicalCard pilot, PhysicalCard ship) {
        Optional<DeployPilotShipPolicy.ExactPilotAssignmentFacts> facts =
                readExactPilotAssignmentFacts(
                        "planner-pilot", game, pilot, ship, false);
        if (facts.isEmpty()) return true;
        return DeployPilotShipPolicy.evaluateExactPilotAssignment(facts.get())
                .operations().stream().noneMatch(operation ->
                        operation.kind()
                                == com.gempukku.swccgo.ai.models.common.policy
                                        .PolicyOperationKind.DEFER);
    }

    public static int plannerPilotQualityTier(
            SwccgGame game, PhysicalCard pilot, PhysicalCard ship) {
        if (!isPlannerPilotEligible(game, pilot, ship)) {
            return Integer.MIN_VALUE;
        }
        return DeployPilotShipPolicy.plannerPilotQualityTier(
                new DeployPilotShipPolicy.PlannerPilotQualityFacts(
                        isMatchingPilot(game, pilot, ship),
                        powerAddedIfPiloting(game, pilot, ship)));
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
