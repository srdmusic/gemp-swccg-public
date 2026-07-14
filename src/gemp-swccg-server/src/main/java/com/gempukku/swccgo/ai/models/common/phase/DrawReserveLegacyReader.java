package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ForceReserveService;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Shared legacy read boundary feeding the pure DRAW reserve assessment. */
public final class DrawReserveLegacyReader {

    private DrawReserveLegacyReader() {
    }

    public static int calculate(GameState gameState,
                                String playerId,
                                int turnNumber,
                                Supplier<ForceReserveService.Facts> reserveFacts,
                                BooleanSupplier hiddenPathUnflipped,
                                Logger logger) {
        try {
            int contestedCount = 0;
            Collection<PhysicalCard> locations = gameState.getLocationsInOrder();
            for (PhysicalCard loc : locations) {
                if (loc == null) {
                    continue;
                }
                Collection<PhysicalCard> cardsAtLoc = gameState.getCardsAtLocation(loc);
                boolean weHavePresence = false;
                boolean theyHavePresence = false;
                for (PhysicalCard card : cardsAtLoc) {
                    if (card.getOwner() != null && card.getOwner().equals(playerId)) {
                        weHavePresence = true;
                    } else {
                        theyHavePresence = true;
                    }
                }
                if (weHavePresence && theyHavePresence) {
                    contestedCount++;
                }
            }

            ForceReserveService.Facts facts = reserveFacts.get();
            boolean opponentHasDTF = facts.dtfActive;
            boolean opponentHasFirstStrike = facts.firstStrikeActive;
            boolean opponentHasIAO = facts.iaoActive;
            int maintenanceCost = facts.maintenanceObligation;
            boolean ourVergeNeedsDeathStarMove = facts.vergeNeedsDeathStarMove;

            DrawReserveAssessment assessment = DrawReserveAssessment.base(
                    opponentHasDTF,
                    opponentHasFirstStrike,
                    contestedCount > 0,
                    turnNumber >= 4,
                    opponentHasIAO,
                    ourVergeNeedsDeathStarMove,
                    maintenanceCost);

            try {
                if (hiddenPathUnflipped.getAsBoolean()) {
                    int corridorCharacters = 0;
                    for (PhysicalCard loc : gameState.getLocationsInOrder()) {
                        if (loc == null || loc.getTitle() == null
                                || !loc.getTitle().toLowerCase(java.util.Locale.ROOT)
                                        .contains("underground corridor")) {
                            continue;
                        }
                        for (PhysicalCard card : gameState.getCardsAtLocation(loc)) {
                            if (card != null && playerId.equals(card.getOwner())
                                    && card.getBlueprint() != null
                                    && card.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                corridorCharacters++;
                            }
                        }
                    }
                    if (corridorCharacters > 0) {
                        assessment = assessment.plusCorridorCharacters(corridorCharacters);
                        logger.warn("V67z TRANSIT RESERVE: {} Jedi at Underground Corridor — reserve +{} Force for the move-phase transit off Mapuzo",
                                corridorCharacters, corridorCharacters);
                    }
                }
            } catch (Exception e) {
                logger.debug("V67z transit-reserve error: {}", e.getMessage());
            }

            logger.debug("V58 RESERVE: DTF={}, FirstStrike={}, IAO={}, contested={}, maint={}, turn={}, total={}",
                    opponentHasDTF, opponentHasFirstStrike, opponentHasIAO, contestedCount,
                    maintenanceCost, turnNumber, assessment.forceToReserve());
            return assessment.forceToReserve();
        } catch (Exception e) {
            logger.trace("V58 RESERVE: error calculating, using default 1: {}", e.getMessage());
            return 1;
        }
    }
}
