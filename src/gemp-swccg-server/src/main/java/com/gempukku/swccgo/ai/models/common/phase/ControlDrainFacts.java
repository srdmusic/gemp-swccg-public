package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.common.AiCardHelper;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.List;
import java.util.Objects;
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

    public ControlDrainFacts(GameState gameState,
                             SwccgGame game,
                             String playerId,
                             String locationCardId,
                             int turnNumber,
                             BooleanSupplier battleOrderRules,
                             BooleanSupplier huntDownV) {
        this.gameState = gameState;
        this.game = game;
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.locationCardId = locationCardId;
        this.turnNumber = turnNumber;
        this.battleOrderRules = Objects.requireNonNull(battleOrderRules, "battleOrderRules");
        this.huntDownV = Objects.requireNonNull(huntDownV, "huntDownV");
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
        int forceAvailable = gameState != null ? gameState.getForcePileSize(playerId) : 0;
        boolean deployable = false;
        int cheapest = Integer.MAX_VALUE;
        if (gameState != null) {
            List<PhysicalCard> hand = gameState.getHand(playerId);
            if (hand != null) {
                for (PhysicalCard card : hand) {
                    if (card.getBlueprint() == null) {
                        continue;
                    }
                    CardCategory category = card.getBlueprint().getCardCategory();
                    if (category == CardCategory.CHARACTER
                            || category == CardCategory.STARSHIP
                            || category == CardCategory.VEHICLE) {
                        deployable = true;
                        Float deployCost = card.getBlueprint().getDeployCost();
                        if (deployCost != null && deployCost < cheapest) {
                            cheapest = deployCost.intValue();
                        }
                    }
                }
            }
        }
        return new ControlDrainAssessment.Economy(
            underBattleOrder, forceAvailable, deployable, cheapest, turnNumber);
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

    private static String title(PhysicalCard card) {
        return card != null && card.getTitle() != null ? card.getTitle() : "?";
    }
}
