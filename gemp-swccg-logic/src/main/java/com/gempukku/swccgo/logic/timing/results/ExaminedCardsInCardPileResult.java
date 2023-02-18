package com.gempukku.swccgo.logic.timing.results;

import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.timing.EffectResult;

/**
 * This effect result is triggered when any cards in a card pile are examined (not just using the word examined).
 */
public class ExaminedCardsInCardPileResult extends EffectResult {
    private String _zoneOwner;
    private Zone _cardPile;

    /**
     * Creates an effect result that is triggered when any cards in a card pile are examined.
     * @param playerId the player examining the card pile
     * @param zoneOwner the card pile owner
     * @param cardPile the card pile
     */
    public ExaminedCardsInCardPileResult(String playerId, String zoneOwner, Zone cardPile) {
        super(Type.EXAMINED_CARDS_IN_CARD_PILE, playerId);
        _zoneOwner = zoneOwner;
        _cardPile = cardPile;
    }

    /**
     * Gets the owner of the card pile.
     * @return the owner
     */
    public String getZoneOwner() {
        return _zoneOwner;
    }

    /**
     * Gets the card pile.
     * @return the card pile
     */
    public Zone getCardPile() {
        return _cardPile;
    }

    /**
     * Gets the text to show to describe the effect result.
     * @param game the game
     * @return the text
     */
    @Override
    public String getText(SwccgGame game) {
        return "Just examined one or more cards in " + _zoneOwner + "'s " + _cardPile.getHumanReadable();
    }
}
