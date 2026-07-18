package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Shared AI-only readers for facts consumed by the DRAW phase policy. */
public final class DrawPhaseFactsReader {

    private DrawPhaseFactsReader() {
    }

    public record ExpensiveCards(int handCardCount,
                                 int maxDeployableCost,
                                 int affordableCardsCount,
                                 boolean expensiveCardInHand) {
    }

    public record ForceStarved(int deployablePower, int minCostForThresholdPower) {
    }

    public static int calculateForceGeneration(SwccgGame game, GameState gameState,
                                               Side side, Logger logger) {
        if (game == null) {
            return 1;
        }

        int totalIcons = 1;
        try {
            ModifiersQuerying modifiers = game.getModifiersQuerying();
            if (modifiers != null) {
                Collection<PhysicalCard> locations = gameState.getLocationsInOrder();
                for (PhysicalCard loc : locations) {
                    if (loc == null) {
                        continue;
                    }
                    SwccgCardBlueprint blueprint = loc.getBlueprint();
                    if (blueprint != null) {
                        if (side == Side.DARK) {
                            Integer darkIcons = blueprint.getIconCount(Icon.DARK_FORCE);
                            if (darkIcons != null) {
                                totalIcons += darkIcons;
                            }
                        } else {
                            Integer lightIcons = blueprint.getIconCount(Icon.LIGHT_FORCE);
                            if (lightIcons != null) {
                                totalIcons += lightIcons;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.trace("Error calculating force generation: {}", e.getMessage());
        }
        return Math.max(1, totalIcons);
    }

    public static int computeOffensiveBank(SwccgGame game, GameState gameState,
                                           String playerId, List<PhysicalCard> hand,
                                           int forcePile, int forceGeneration,
                                           Logger logger) {
        if (game == null || gameState == null || playerId == null) {
            return 0;
        }
        if (hand == null || hand.isEmpty()) {
            return 0;
        }
        String opponentId = game.getOpponent(playerId);
        if (opponentId == null) {
            return 0;
        }
        ModifiersQuerying modifiers = game.getModifiersQuerying();

        List<float[]> handCharacters = new ArrayList<>();
        for (PhysicalCard card : hand) {
            if (card == null || card.getBlueprint() == null) {
                continue;
            }
            if (card.getBlueprint().getCardCategory() != CardCategory.CHARACTER
                    || !card.getBlueprint().hasPowerAttribute()) {
                continue;
            }
            Float power = card.getBlueprint().getPower();
            if (power == null) {
                continue;
            }
            Float cost = null;
            try {
                cost = card.getBlueprint().getDeployCost();
            } catch (Exception ignored) {
                // Preserve the legacy zero-cost fallback.
            }
            handCharacters.add(new float[]{power, cost == null ? 0f : cost});
        }
        if (handCharacters.isEmpty()) {
            return 0;
        }
        handCharacters.sort((a, b) -> Float.compare(b[0], a[0]));

        int bestBank = 0;
        try {
            for (PhysicalCard location : gameState.getLocationsInOrder()) {
                if (location == null) {
                    continue;
                }
                float opponentPower;
                try {
                    opponentPower = modifiers.getTotalPowerAtLocation(
                            gameState, location, opponentId, false, false);
                } catch (Exception e) {
                    continue;
                }
                if (opponentPower <= 0f) {
                    continue;
                }

                boolean worthIt = false;
                try {
                    worthIt = modifiers.isBattleground(gameState, location, null);
                } catch (Exception ignored) {
                    // Preserve the legacy false fallback.
                }
                if (!worthIt) {
                    try {
                        worthIt = modifiers.getForceDrainAmount(
                                gameState, location, opponentId) >= 1f;
                    } catch (Exception ignored) {
                        // Preserve the legacy false fallback.
                    }
                }
                if (!worthIt) {
                    continue;
                }

                float ourPower;
                try {
                    ourPower = modifiers.getTotalPowerAtLocation(
                            gameState, location, playerId, false, false);
                } catch (Exception e) {
                    ourPower = 0f;
                }
                float gap = opponentPower - ourPower;
                if (gap <= 0f) {
                    continue;
                }

                float coveredPower = 0f;
                float costToCover = 0f;
                for (float[] character : handCharacters) {
                    coveredPower += character[0];
                    costToCover += character[1];
                    if (coveredPower >= gap) {
                        break;
                    }
                }
                if (coveredPower < gap || costToCover <= forcePile
                        || forcePile + 2 * forceGeneration < costToCover) {
                    continue;
                }

                int needed = (int) Math.ceil(costToCover);
                if (bestBank == 0 || needed < bestBank) {
                    bestBank = needed;
                }
            }
        } catch (Exception e) {
            logger.debug("V182 offensive-bank scan error: {}", e.getMessage());
        }
        return bestBank;
    }

    public static ExpensiveCards inspectExpensiveCards(List<PhysicalCard> hand,
                                                       int forcePile) {
        if (hand == null || hand.isEmpty()) {
            return new ExpensiveCards(0, 0, 0, false);
        }

        int maxDeployableCost = 0;
        int affordableCardsCount = 0;
        boolean expensiveCardInHand = false;
        for (PhysicalCard card : hand) {
            if (card == null || card.getBlueprint() == null) {
                continue;
            }
            SwccgCardBlueprint blueprint = card.getBlueprint();
            if (blueprint.getCardCategory() == CardCategory.INTERRUPT) {
                continue;
            }
            try {
                Float deployCost = blueprint.getDeployCost();
                if (deployCost != null) {
                    int cost = deployCost.intValue();
                    maxDeployableCost = Math.max(maxDeployableCost, cost);
                    if (cost >= 8) {
                        expensiveCardInHand = true;
                    }
                    if (forcePile >= cost) {
                        affordableCardsCount++;
                    }
                }
            } catch (UnsupportedOperationException ignored) {
                // Preserve the legacy unsupported-card skip.
            }
        }
        return new ExpensiveCards(hand.size(), maxDeployableCost,
                affordableCardsCount, expensiveCardInHand);
    }

    public static ForceStarved inspectForceStarved(List<PhysicalCard> hand) {
        if (hand == null || hand.isEmpty()) {
            return new ForceStarved(0, 999);
        }

        int deployablePower = 0;
        int minCostForThresholdPower = 999;
        List<int[]> powerCostPairs = new ArrayList<>();
        for (PhysicalCard card : hand) {
            if (card == null || card.getBlueprint() == null) {
                continue;
            }
            SwccgCardBlueprint blueprint = card.getBlueprint();
            if (!blueprint.hasPowerAttribute()) {
                continue;
            }
            Float power = blueprint.getPower();
            Float cost;
            try {
                cost = blueprint.getDeployCost();
            } catch (UnsupportedOperationException e) {
                continue;
            }
            if (power != null && cost != null && power > 0 && cost > 0) {
                int integerPower = power.intValue();
                int integerCost = cost.intValue();
                deployablePower += integerPower;
                powerCostPairs.add(new int[]{integerPower, integerCost});
            }
        }

        powerCostPairs.sort((a, b) -> {
            float ratioA = a[1] > 0 ? (float) a[0] / a[1] : 0;
            float ratioB = b[1] > 0 ? (float) b[0] / b[1] : 0;
            return Float.compare(ratioB, ratioA);
        });

        int cumulativePower = 0;
        int cumulativeCost = 0;
        for (int[] pair : powerCostPairs) {
            cumulativePower += pair[0];
            cumulativeCost += pair[1];
            if (cumulativePower >= 6) {
                minCostForThresholdPower = cumulativeCost;
                break;
            }
        }
        return new ForceStarved(deployablePower, minCostForThresholdPower);
    }
}
