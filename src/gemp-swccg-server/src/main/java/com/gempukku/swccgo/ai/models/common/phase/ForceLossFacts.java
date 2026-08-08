package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.common.AiPriorityCards;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;

import java.util.List;
import java.util.Locale;

/** Immutable board reads shared by both FORCE-LOSS adapters. */
public final class ForceLossFacts {

    public enum ZoneBand {
        HAND,
        USED,
        RESERVE,
        FORCE_PILE,
        UNRESOLVED_DESTINY,
        SABACC,
        OTHER
    }

    public record DecisionFacts(int handSize,
                                int reserveDeckSize,
                                int lifeForce,
                                int forcePileSize,
                                int turnNumber,
                                boolean drawTheirFireActive) {
    }

    public record CandidateFacts(String title,
                                 String zoneName,
                                 ZoneBand zoneBand,
                                 CardCategory category,
                                 boolean duplicate,
                                 boolean senator,
                                 boolean battleInterrupt,
                                 boolean hasWielder,
                                 boolean priorityCard,
                                 // WMAOP 2026-08-08 (Steve directive): in-hand We
                                 // Must Accelerate Our Plans with its Blockade
                                 // Flagship site already on table — dead by
                                 // directive, PREFERRED force-loss fodder.
                                 boolean wmaopFodderHold) {
        // WMAOP 2026-08-08 (Steve directive): compatibility constructor —
        // pre-directive callers default wmaopFodderHold=false.
        public CandidateFacts(String title,
                              String zoneName,
                              ZoneBand zoneBand,
                              CardCategory category,
                              boolean duplicate,
                              boolean senator,
                              boolean battleInterrupt,
                              boolean hasWielder,
                              boolean priorityCard) {
            this(title, zoneName, zoneBand, category, duplicate, senator,
                    battleInterrupt, hasWielder, priorityCard, false);
        }

        public boolean fromHand() {
            return zoneBand == ZoneBand.HAND;
        }

        public boolean fromUsedPile() {
            return zoneBand == ZoneBand.USED;
        }

        public boolean fromReserve() {
            return zoneBand == ZoneBand.RESERVE;
        }

        public boolean fromForcePile() {
            return zoneBand == ZoneBand.FORCE_PILE;
        }

        public boolean handCharacter() {
            return fromHand() && category == CardCategory.CHARACTER;
        }

        public boolean handShipOrVehicle() {
            return fromHand()
                    && (category == CardCategory.STARSHIP
                        || category == CardCategory.VEHICLE);
        }
    }

    private ForceLossFacts() {
    }

    public static DecisionFacts readDecision(GameState gameState,
                                             String playerId,
                                             int turnNumber) {
        int handSize = 0;
        int reserveDeckSize = 0;
        int forcePileSize = 0;
        int lifeForce = 0;
        if (gameState != null && playerId != null) {
            try {
                handSize = gameState.getHand(playerId).size();
                reserveDeckSize = gameState.getReserveDeckSize(playerId);
                lifeForce = reserveDeckSize
                        + gameState.getUsedPile(playerId).size()
                        + gameState.getForcePileSize(playerId);
            } catch (Exception ignored) {
                // Preserve the legacy zero/default fallback.
            }
        }
        boolean drawTheirFireActive = drawTheirFireActive(gameState, playerId);
        if (drawTheirFireActive && gameState != null && playerId != null) {
            try {
                forcePileSize = gameState.getForcePileSize(playerId);
            } catch (Exception ignored) {
                // V28 historically read this in its own exception scope.
            }
        }
        return new DecisionFacts(handSize, reserveDeckSize, lifeForce,
                forcePileSize, turnNumber,
                drawTheirFireActive);
    }

    public static DecisionFacts readCombinedDecision(GameState gameState,
                                                     String playerId,
                                                     int turnNumber) {
        int reserveDeckSize = 0;
        int forcePileSize = 0;
        int lifeForce = 0;
        if (gameState != null && playerId != null) {
            try {
                reserveDeckSize = gameState.getReserveDeckSize(playerId);
                int usedPileSize = gameState.getUsedPile(playerId).size();
                int readForcePileSize = gameState.getForcePileSize(playerId);
                lifeForce = reserveDeckSize
                        + usedPileSize
                        + readForcePileSize;
                forcePileSize = readForcePileSize;
            } catch (Exception ignored) {
                // Preserve the combined route's legacy life-force fallback.
            }
        }

        int handSize = 0;
        if (gameState != null && playerId != null) {
            try {
                handSize = gameState.getHand(playerId).size();
            } catch (Exception ignored) {
                // The combined route historically read its hand floor separately.
            }
        }
        return new DecisionFacts(handSize, reserveDeckSize, lifeForce,
                forcePileSize, turnNumber, false);
    }

    public static CandidateFacts readCandidate(GameState gameState,
                                               String playerId,
                                               PhysicalCard card) {
        if (card == null) {
            return new CandidateFacts(null, "", ZoneBand.OTHER, null,
                    false, false, false, false, false);
        }

        String title = card.getTitle();
        SwccgCardBlueprint blueprint = card.getBlueprint();
        String zoneName = card.getZone() != null ? card.getZone().name() : "";
        ZoneBand zoneBand = zoneBand(zoneName);
        CardCategory category = blueprint != null ? blueprint.getCardCategory() : null;
        boolean duplicate = zoneBand == ZoneBand.HAND
                && duplicateInHand(gameState, playerId, title);
        boolean senator = isSenator(blueprint);
        boolean battleInterrupt = category == CardCategory.INTERRUPT
                && isBattleInterrupt(blueprint);
        boolean hasWielder = category == CardCategory.WEAPON
                && hasAnyWielder(gameState, playerId);
        boolean priorityCard = title != null
                && AiPriorityCards.isPriorityCardByTitle(title);
        // WMAOP 2026-08-08 (Steve directive): once the Blockade Flagship site is
        // on table, the in-hand WMAOP is dead — mark it as the preferred fodder
        // (V95 dead-interrupt-save precedent; shared engine-typed table probe).
        boolean wmaopFodderHold = zoneBand == ZoneBand.HAND
                && title != null
                && title.toLowerCase(Locale.ROOT).contains("accelerate our plans")
                && PullActionFactsReader.blockadeFlagshipSiteOnTable(
                        gameState != null ? gameState.getGame() : null, gameState);
        // return new CandidateFacts(title, zoneName, zoneBand, category,
        //         duplicate, senator, battleInterrupt, hasWielder, priorityCard);
        return new CandidateFacts(title, zoneName, zoneBand, category,
                duplicate, senator, battleInterrupt, hasWielder, priorityCard,
                wmaopFodderHold);
    }

    public static boolean isForceLossZone(PhysicalCard card) {
        return card != null && zoneBand(card.getZone() != null
                ? card.getZone().name() : "") != ZoneBand.OTHER;
    }

    private static ZoneBand zoneBand(String zoneName) {
        if (zoneName.contains("SABACC")) {
            return ZoneBand.SABACC;
        }
        if (zoneName.contains("HAND")) {
            return ZoneBand.HAND;
        }
        if (zoneName.contains("USED")) {
            return ZoneBand.USED;
        }
        if (zoneName.contains("RESERVE")) {
            return ZoneBand.RESERVE;
        }
        if (zoneName.contains("FORCE_PILE")) {
            return ZoneBand.FORCE_PILE;
        }
        if (zoneName.contains("UNRESOLVED_DESTINY")) {
            return ZoneBand.UNRESOLVED_DESTINY;
        }
        return ZoneBand.OTHER;
    }

    private static boolean duplicateInHand(GameState gameState,
                                           String playerId,
                                           String title) {
        if (gameState == null || playerId == null || title == null) {
            return false;
        }
        try {
            int copiesInHand = 0;
            List<PhysicalCard> hand = gameState.getHand(playerId);
            if (hand != null) {
                for (PhysicalCard handCard : hand) {
                    if (handCard != null && title.equals(handCard.getTitle())) {
                        copiesInHand++;
                    }
                }
            }
            boolean copyOnTable = false;
            for (PhysicalCard tableCard : gameState.getAllPermanentCards()) {
                if (tableCard != null
                        && playerId.equals(tableCard.getOwner())
                        && title.equals(tableCard.getTitle())
                        && tableCard.getZone() != null
                        && tableCard.getZone().isInPlay()) {
                    copyOnTable = true;
                    break;
                }
            }
            return copiesInHand >= 2 || copyOnTable;
        } catch (Exception ignored) {
            // Preserve the legacy non-duplicate fallback.
        }
        return false;
    }

    private static boolean isSenator(SwccgCardBlueprint blueprint) {
        if (blueprint == null) {
            return false;
        }
        if (blueprint.hasKeyword(Keyword.SENATOR)) {
            return true;
        }
        // SUPERSEDED 2026-08-07 (phantom-senator fix, m01676): lore widening retired — the only
        // character it admitted was Mas Amedda 12_15, a non-senator by card law (Card12_061:87
        // lists him separately from Filters.senator); V109 was protecting a phantom. Keyword only.
        // String lore = blueprint.getLore();
        // return lore != null && lore.toLowerCase(Locale.ROOT).contains("senator");
        return false;
    }

    private static boolean isBattleInterrupt(SwccgCardBlueprint blueprint) {
        if (blueprint == null) {
            return false;
        }
        try {
            String gameText = blueprint.getGameText();
            String lower = gameText != null ? gameText.toLowerCase(Locale.ROOT) : "";
            return lower.contains("battle destiny")
                    || lower.contains("during battle")
                    || lower.contains("during a battle")
                    || lower.contains("'hit'")
                    || lower.contains("substitute")
                    || lower.contains("power +");
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean hasAnyWielder(GameState gameState, String playerId) {
        if (gameState == null || playerId == null) {
            return false;
        }
        try {
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card != null
                        && playerId.equals(card.getOwner())
                        && !card.isUndercover()
                        && card.getBlueprint() != null
                        && card.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                    return true;
                }
            }
            for (PhysicalCard card : gameState.getHand(playerId)) {
                if (card != null && card.getBlueprint() != null
                        && card.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            // Preserve the legacy no-wielder fallback.
        }
        return false;
    }

    private static boolean drawTheirFireActive(GameState gameState, String playerId) {
        if (gameState == null || playerId == null) {
            return false;
        }
        try {
            String opponentId = gameState.getOpponent(playerId);
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card != null
                        && opponentId != null
                        && opponentId.equals(card.getOwner())
                        && card.getBlueprint() != null
                        && card.getBlueprint().getTitle() != null
                        && card.getBlueprint().getTitle().toLowerCase(Locale.ROOT)
                                .contains("draw their fire")
                        && card.getZone() != null
                        && card.getZone().isInPlay()) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            // Preserve the legacy inactive fallback.
        }
        return false;
    }
}
