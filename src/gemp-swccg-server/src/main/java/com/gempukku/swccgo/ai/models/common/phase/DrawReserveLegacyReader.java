package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ForceReserveService;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** V194: shared AI read boundary feeding the pure DRAW reserve assessment. */
public final class DrawReserveLegacyReader {

    private DrawReserveLegacyReader() {
    }

    public static int calculate(GameState gameState,
                                String playerId,
                                int turnNumber,
                                Supplier<ForceReserveService.Facts> reserveFacts,
                                IntSupplier hiddenPathTransitReserve,
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
                int transitReserve = Math.max(
                        0, hiddenPathTransitReserve.getAsInt());
                if (transitReserve > 0) {
                    assessment = assessment
                            .plusHiddenPathTransitReserve(
                                transitReserve);
                    logger.warn("V67z TRANSIT RESERVE: reserve +{} Force for legal Underground Corridor exits still needed to flip Hidden Path",
                            transitReserve);
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
