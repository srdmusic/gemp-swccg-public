package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.common.AiCardHelper;
import com.gempukku.swccgo.ai.models.common.strategy.FormationSafety;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Lazy game-object fact collector for the shared CONTROL drain assessment. */
public final class ControlDrainFacts implements ControlDrainAssessment.Facts {

    private final GameState gameState;
    private final SwccgGame game;
    private final String playerId;
    private final String locationCardId;
    private final int turnNumber;
    private final BooleanSupplier battleOrderRules;
    private final BooleanSupplier huntDownV;
    private final BooleanSupplier classicHuntDown;

    public ControlDrainFacts(GameState gameState,
                             SwccgGame game,
                             String playerId,
                             String locationCardId,
                             int turnNumber,
                             BooleanSupplier battleOrderRules,
                             BooleanSupplier huntDownV,
                             BooleanSupplier classicHuntDown) {
        this.gameState = gameState;
        this.game = game;
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.locationCardId = locationCardId;
        this.turnNumber = turnNumber;
        this.battleOrderRules = Objects.requireNonNull(battleOrderRules, "battleOrderRules");
        this.huntDownV = Objects.requireNonNull(huntDownV, "huntDownV");
        this.classicHuntDown = Objects.requireNonNull(
                classicHuntDown, "classicHuntDown");
    }

    @Override
    public boolean classicHuntExecutorHardLoss() {
        if (!classicHuntDown.getAsBoolean() || gameState == null
                || game == null) {
            return false;
        }
        try {
            PhysicalCard location = findLocation();
            return location != null && Filters.Executor_site.accepts(
                    gameState, game.getModifiersQuerying(), location);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ControlDrainAssessment.Primary primary() {
        if (gameState == null || locationCardId == null) {
            return null;
        }
        try {
            PhysicalCard location = findLocation();
            if (location == null || game == null) {
                return null;
            }
            float amount = game.getModifiersQuerying().getForceDrainAmount(
                gameState, location, playerId);
            if (amount <= 0.0f) {
                return new ControlDrainAssessment.Primary(amount, 0.0f, title(location),
                    0, 0, 2);
            }
            float cost = game.getModifiersQuerying().getInitiateForceDrainCost(
                gameState, location, playerId);
            int forcePile = 0;
            int plannedSpend = 0;
            int moveAllowance = 2;
            if (cost > amount && cost - amount < 2.0f) {
                forcePile = gameState.getForcePileSize(playerId);
                List<PhysicalCard> hand = gameState.getHand(playerId);
                if (hand != null) {
                    for (PhysicalCard card : hand) {
                        if (card == null || card.getBlueprint() == null) {
                            continue;
                        }
                        CardCategory category = card.getBlueprint().getCardCategory();
                        if (category == CardCategory.CHARACTER
                                || category == CardCategory.STARSHIP
                                || category == CardCategory.VEHICLE) {
                            if (AiCardHelper.isDeadCard(card, game, playerId)) {
                                continue;
                            }
                            Float deployCost = card.getBlueprint().getDeployCost();
                            if (deployCost != null && deployCost > 0) {
                                plannedSpend += deployCost.intValue();
                            }
                        }
                    }
                }
            }
            return new ControlDrainAssessment.Primary(amount, cost, title(location),
                forcePile, plannedSpend, moveAllowance);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean simpleTricksBlocks() {
        if (gameState == null || locationCardId == null) {
            return false;
        }
        try {
            PhysicalCard location = findLocation();
            if (location == null) {
                return false;
            }
            SwccgCardBlueprint blueprint = location.getBlueprint();
            boolean battleground = false;
            if (game != null) {
                battleground = game.getModifiersQuerying().isBattleground(
                    gameState, location, null);
            } else if (blueprint != null) {
                battleground = blueprint.hasIcon(Icon.DARK_FORCE)
                    && blueprint.hasIcon(Icon.LIGHT_FORCE);
            }
            if (battleground) {
                return false;
            }
            String opponentId = gameState.getOpponent(playerId);
            if (opponentId == null) {
                return false;
            }
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card == null || !opponentId.equals(card.getOwner())
                        || card.getZone() == null || !card.getZone().isInPlay()) {
                    continue;
                }
                if (card.getTitle() != null && card.getTitle().contains("Simple Tricks")) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    @Override
    public ControlDrainAssessment.Economy economy() {
        boolean underBattleOrder = battleOrderRules.getAsBoolean();
        int forceAvailable = exactUsableForce();
        boolean deployable = false;
        int cheapest = Integer.MAX_VALUE;
        if (gameState != null) {
            List<PhysicalCard> hand = gameState.getHand(playerId);
            if (hand != null) {
                for (PhysicalCard card : hand) {
                    try {
                        if (!isPotentialDeployCandidate(card)) {
                            continue;
                        }
                        Integer deployCost = genericDeployCost(card);
                        if (deployCost != null && deployCost <= forceAvailable) {
                            deployable = true;
                            cheapest = Math.min(cheapest, deployCost);
                        }
                    } catch (Exception ignored) {
                        // An unreadable card is not evidence of an affordable deploy.
                    }
                }
            }
        }
        return new ControlDrainAssessment.Economy(
            underBattleOrder, forceAvailable, deployable, cheapest, turnNumber);
    }

    @Override
    public ControlDrainAssessment.DownstreamUses downstreamUses(
            float drainCost) {
        if (gameState == null || game == null) {
            return ControlDrainAssessment.DownstreamUses.unknown();
        }

        int usableForce;
        int reserveDeckSize;
        try {
            usableForce = GameConditions.forceAvailableToUse(game, playerId);
            reserveDeckSize = gameState.getReserveDeckSize(playerId);
        } catch (Exception e) {
            return ControlDrainAssessment.DownstreamUses.unknown();
        }

        boolean complete = true;
        boolean deployWouldBeStranded = false;
        List<PhysicalCard> hand;
        try {
            hand = gameState.getHand(playerId);
        } catch (Exception e) {
            hand = null;
        }
        if (hand == null) {
            complete = false;
        } else {
            for (PhysicalCard card : hand) {
                try {
                    if (!isPotentialDeployCandidate(card)) {
                        continue;
                    }
                    DeployUseAssessment deployUse = assessDeployUse(
                            card, drainCost, usableForce);
                    complete &= deployUse.complete();
                    deployWouldBeStranded |= deployUse.stranded();
                } catch (Exception e) {
                    complete = false;
                }
            }
        }

        boolean paidBattlePresent = false;
        boolean paidBattleWouldBeStranded = false;
        String opponentId;
        try {
            opponentId = gameState.getOpponent(playerId);
        } catch (Exception e) {
            opponentId = null;
        }
        if (opponentId == null) {
            complete = false;
        } else {
            List<PhysicalCard> locations;
            try {
                locations = gameState.getTopLocations();
            } catch (Exception e) {
                locations = null;
                complete = false;
            }
            if (locations == null) {
                complete = false;
                locations = List.of();
            }
            for (PhysicalCard location : locations) {
                if (location == null) {
                    continue;
                }
                try {
                    if (GameConditions.canInitiateBattleAtLocation(
                            playerId, game, location, true, true)) {
                        continue;
                    }
                    if (!GameConditions.canInitiateBattleAtLocation(
                            playerId, game, location, false, true)
                            || FormationSafety.vetoInitiateBattle(
                                    game, gameState, playerId, location)
                                != null) {
                        continue;
                    }

                    float ourPower = game.getModifiersQuerying()
                            .getTotalPowerAtLocation(gameState, location,
                                    playerId, false, false);
                    float theirPower = game.getModifiersQuerying()
                            .getTotalPowerAtLocation(gameState, location,
                                    opponentId, false, false);
                    float ourAbility = game.getModifiersQuerying()
                            .getTotalAbilityAtLocation(
                                    gameState, playerId, location);
                    float theirAbility = game.getModifiersQuerying()
                            .getTotalAbilityAtLocation(
                                    gameState, opponentId, location);
                    float ourWeapons = weaponBonusAt(location, playerId);
                    float theirWeapons = weaponBonusAt(location, opponentId);
                    float effectiveDiff = ourPower - theirPower
                            + 2.5f * (ourAbility - theirAbility)
                            + ourWeapons - theirWeapons;
                    boolean favorable = BattleInitiationPolicy.specificBattle(
                            title(location), ourPower, theirPower,
                            ourAbility, theirAbility, ourWeapons,
                            effectiveDiff, false, false, false,
                            reserveDeckSize >= 3).favorable();
                    if (!favorable) {
                        continue;
                    }

                    float rawCost = game.getModifiersQuerying()
                            .getInitiateBattleCost(
                                    gameState, location, playerId, false);
                    if (!Float.isFinite(rawCost)) {
                        complete = false;
                        continue;
                    }
                    int cost = (int) Math.ceil(Math.max(0.0f, rawCost));
                    if (cost > 0 && cost <= usableForce) {
                        paidBattlePresent = true;
                        if (wouldStrand(
                                drainCost, usableForce, cost)) {
                            paidBattleWouldBeStranded = true;
                        }
                    }
                } catch (Exception e) {
                    complete = false;
                }
            }
        }

        return new ControlDrainAssessment.DownstreamUses(
                complete, usableForce, reserveDeckSize,
                deployWouldBeStranded, paidBattlePresent,
                paidBattleWouldBeStranded);
    }

    @Override
    public boolean battleOrderCostWaived() {
        try {
            PhysicalCard location = findLocation();
            return gameState != null && location != null && game != null
                && game.getModifiersQuerying().getInitiateForceDrainCost(
                    gameState, location, playerId) <= 0.0f;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ControlDrainAssessment.DrainValue battleOrderDrainValue() {
        try {
            PhysicalCard location = findLocation();
            if (gameState == null || location == null || game == null) {
                return null;
            }
            float amount = game.getModifiersQuerying().getForceDrainAmount(
                gameState, location, playerId);
            return new ControlDrainAssessment.DrainValue(amount, title(location));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ControlDrainAssessment.MultiDrain multiDrain() {
        if (gameState == null || locationCardId == null || game == null) {
            return null;
        }
        try {
            int drainCapableSites = 0;
            float thisDrainAmount = 0.0f;
            PhysicalCard thisLocation = findLocation();
            for (PhysicalCard location : gameState.getTopLocations()) {
                if (location == null) {
                    continue;
                }
                try {
                    float power = game.getModifiersQuerying().getTotalPowerAtLocation(
                        gameState, location, playerId, false, false);
                    if (power > 0.0f && game.getModifiersQuerying().getForceDrainAmount(
                            gameState, location, playerId) > 0.0f) {
                        drainCapableSites++;
                    }
                } catch (Exception ignored) {
                }
            }
            if (thisLocation != null) {
                try {
                    thisDrainAmount = game.getModifiersQuerying().getForceDrainAmount(
                        gameState, thisLocation, playerId);
                } catch (Exception ignored) {
                }
            }
            return new ControlDrainAssessment.MultiDrain(
                thisDrainAmount, drainCapableSites, title(thisLocation));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ControlDrainAssessment.HuntDown huntDown() {
        boolean active = huntDownV.getAsBoolean();
        int opponentIcons = 0;
        if (active && gameState != null && locationCardId != null) {
            try {
                PhysicalCard location = findLocation();
                if (location != null && location.getBlueprint() != null) {
                    opponentIcons = location.getBlueprint().getIconCount(Icon.LIGHT_FORCE);
                }
            } catch (Exception ignored) {
            }
        }
        return new ControlDrainAssessment.HuntDown(active, opponentIcons);
    }

    private PhysicalCard findLocation() {
        if (gameState == null || locationCardId == null) {
            return null;
        }
        return gameState.findCardById(Integer.parseInt(locationCardId));
    }

    private int exactUsableForce() {
        if (gameState == null) {
            return 0;
        }
        if (game == null) {
            return gameState.getForcePileSize(playerId);
        }
        try {
            return GameConditions.forceAvailableToUse(game, playerId);
        } catch (Exception e) {
            return gameState.getForcePileSize(playerId);
        }
    }

    private boolean isPotentialDeployCandidate(PhysicalCard card) {
        if (card == null || card.getBlueprint() == null) {
            return false;
        }
        CardCategory category = card.getBlueprint().getCardCategory();
        if (category != CardCategory.CHARACTER
                && category != CardCategory.STARSHIP
                && category != CardCategory.VEHICLE) {
            return false;
        }
        if (game == null) {
            return true;
        }
        return !AiCardHelper.isDeadCard(card, game, playerId)
                && !game.getModifiersQuerying().isPlayingCardProhibited(
                        gameState, card, false)
                && !game.getModifiersQuerying()
                        .isPlayingCardTitleTurnLimitReached(
                                gameState, card)
                && !game.getModifiersQuerying()
                        .isUniquenessOnTableLimitReached(
                                gameState, card);
    }

    private Integer genericDeployCost(PhysicalCard card) {
        // Legacy CONTROL economy only. This overload is not target-specific;
        // downstream stranding uses assessDeployUse instead.
        float cost = game != null
                ? game.getModifiersQuerying().getDeployCost(gameState, card)
                : card.getBlueprint().getDeployCost();
        if (!Float.isFinite(cost)) {
            return null;
        }
        return (int) Math.ceil(Math.max(0.0f, cost));
    }

    private DeployUseAssessment assessDeployUse(
            PhysicalCard card, float drainCost, int usableForce) {
        try {
            boolean hasDeployAction = Filters.deployable(
                    card, null, false, 0.0f).accepts(
                            gameState, game.getModifiersQuerying(), card);

            Set<PhysicalCard> targets = Collections.newSetFromMap(
                    new IdentityHashMap<>());
            Collection<PhysicalCard> locations =
                    gameState.getLocationsInOrder();
            Collection<PhysicalCard> permanents =
                    gameState.getAllPermanentCards();
            if (locations == null || permanents == null) {
                return DeployUseAssessment.unknown();
            }
            targets.addAll(locations);
            targets.addAll(permanents);

            boolean foundExactTarget = false;
            boolean complete = true;
            boolean stranded = false;
            for (PhysicalCard target : targets) {
                if (target == null || target.getZone() == null
                        || !target.getZone().isInPlay()
                        || !Filters.deployableToTarget(
                                card, Filters.sameCardId(target),
                                false, 0.0f).accepts(
                                    gameState,
                                    game.getModifiersQuerying(), card)) {
                    continue;
                }
                foundExactTarget = true;
                Integer cost = exactDeployCostAt(card, target);
                if (cost == null) {
                    complete = false;
                    continue;
                }
                if (wouldStrand(drainCost, usableForce, cost)) {
                    stranded = true;
                }
            }
            if (!foundExactTarget) {
                return hasDeployAction
                        ? DeployUseAssessment.unknown()
                        : DeployUseAssessment.knownNoUse();
            }

            // An unpiloted starship action may have been created only because
            // the engine auto-found a simultaneous pilot in hand. Hull-only
            // target costs cannot prove that the pair survives the drain.
            if (card.getBlueprint().getCardCategory()
                    == CardCategory.STARSHIP
                    && !game.getModifiersQuerying().hasPermanentPilot(
                            gameState, card)) {
                complete = false;
            }
            return new DeployUseAssessment(
                    complete, stranded);
        } catch (Exception e) {
            return DeployUseAssessment.unknown();
        }
    }

    private Integer exactDeployCostAt(
            PhysicalCard card, PhysicalCard target) {
        float cost = game.getModifiersQuerying().getDeployCost(
                gameState, card, card, target,
                false, null, false, 0.0f, null, true);
        if (!Float.isFinite(cost)) {
            return null;
        }
        return (int) Math.ceil(Math.max(0.0f, cost));
    }

    private record DeployUseAssessment(boolean complete, boolean stranded) {
        private static DeployUseAssessment knownNoUse() {
            return new DeployUseAssessment(true, false);
        }

        private static DeployUseAssessment unknown() {
            return new DeployUseAssessment(false, false);
        }
    }

    static boolean wouldStrand(
            float spend, int usableForce, int alternativeCost) {
        return Float.isFinite(spend)
                && spend > 0.0f
                && alternativeCost > 0
                && usableForce >= alternativeCost
                && usableForce - spend < alternativeCost;
    }

    private float weaponBonusAt(
            PhysicalCard location, String ownerId) {
        float bonus = 0.0f;
        List<PhysicalCard> cards = gameState.getCardsAtLocation(location);
        if (cards == null) {
            return bonus;
        }
        for (PhysicalCard card : cards) {
            if (card == null || card.getBlueprint() == null
                    || !ownerId.equals(card.getOwner())
                    || card.getBlueprint().getCardCategory()
                        != CardCategory.CHARACTER) {
                continue;
            }
            bonus += BattleWeaponProfile.assess(
                    game, gameState, card).bonus();
        }
        return bonus;
    }

    private static String title(PhysicalCard card) {
        return card != null && card.getTitle() != null ? card.getTitle() : "?";
    }
}
