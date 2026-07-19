package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Read-only SETUP facts shared by the Rando and ChosenOne adapters. */
public final class SetupFactsReader {
    private SetupFactsReader() {
    }

    public static String allLocationText(SwccgCardBlueprint blueprint) {
        String baseText = blueprint.getGameText();
        String lightText = null;
        String darkText = null;
        try {
            lightText = blueprint.getLocationLightSideGameText();
        } catch (Exception ignored) {
        }
        try {
            darkText = blueprint.getLocationDarkSideGameText();
        } catch (Exception ignored) {
        }

        StringBuilder allText = new StringBuilder();
        if (baseText != null) {
            allText.append(baseText).append(' ');
        }
        if (lightText != null) {
            allText.append(lightText).append(' ');
        }
        if (darkText != null) {
            allText.append(darkText).append(' ');
        }
        return allText.toString();
    }

    public static boolean hasOwnedSithStartingEffect(
            GameState gameState, String playerId) {
        if (gameState == null || playerId == null) {
            return false;
        }

        try {
            List<PhysicalCard> cards = new ArrayList<>();
            try {
                cards.addAll(gameState.getHand(playerId));
            } catch (Exception ignored) {
            }
            try {
                cards.addAll(gameState.getReserveDeck(playerId));
            } catch (Exception ignored) {
            }
            try {
                cards.addAll(gameState.getForcePile(playerId));
            } catch (Exception ignored) {
            }
            try {
                cards.addAll(gameState.getUsedPile(playerId));
            } catch (Exception ignored) {
            }
            try {
                cards.addAll(gameState.getLostPile(playerId));
            } catch (Exception ignored) {
            }
            try {
                cards.addAll(gameState.getOutOfPlayPile(playerId));
            } catch (Exception ignored) {
            }
            try {
                cards.addAll(gameState.getOutsideOfDeck(playerId));
            } catch (Exception ignored) {
            }
            try {
                cards.addAll(gameState.getSideOfTableFaceDown(playerId));
            } catch (Exception ignored) {
            }
            try {
                cards.addAll(gameState.getVoid(playerId));
            } catch (Exception ignored) {
            }
            try {
                cards.addAll(gameState.getCardsRevealedAfterStartingEffect(playerId));
            } catch (Exception ignored) {
            }
            try {
                for (PhysicalCard card : gameState.getAllPermanentCards()) {
                    if (card != null && playerId.equals(card.getOwner())) {
                        cards.add(card);
                    }
                }
            } catch (Exception ignored) {
            }

            for (PhysicalCard card : cards) {
                if (card == null) {
                    continue;
                }
                if (card.getOwner() != null
                        && !playerId.equals(card.getOwner())) {
                    continue;
                }
                String title = card.getTitle();
                if (title == null) {
                    continue;
                }
                String lower = title.toLowerCase(Locale.ROOT);
                if (lower.contains("rise of the sith")
                        || lower.contains("revenge of the sith")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean hasOwnedInPlayRevengeOfTheSith(
            GameState gameState, String playerId) {
        if (gameState == null || playerId == null) {
            return false;
        }
        try {
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card == null || card.getBlueprint() == null) {
                    continue;
                }
                if (!playerId.equals(card.getOwner())) {
                    continue;
                }
                if (card.getZone() == null || !card.getZone().isInPlay()) {
                    continue;
                }
                String title = card.getTitle();
                if (title != null
                        && title.toLowerCase(Locale.ROOT)
                        .contains("revenge of the sith")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }
}
