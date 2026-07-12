package com.gempukku.swccgo.logic.timing.results;

import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.timing.EffectResult;

/**
 * An effect result that is emitted after cards have been 'shielded' (3 added to destiny)
 */
public class CardsShieldedResult extends EffectResult {

    /**
     * Creates an effect result that is emitted when cards have been 'shielded' (3 added to destiny)
     * @param performingPlayerId the performing player
     */
    public CardsShieldedResult(String performingPlayerId) {
        super(Type.CARDS_SHIELDED, performingPlayerId);
    }

    /**
     * Gets the text to show to describe the effect result.
     * @param game the game
     * @return the text
     */
    @Override
    public String getText(SwccgGame game) {
        return "Just 'shielded' cards";
    }
}
