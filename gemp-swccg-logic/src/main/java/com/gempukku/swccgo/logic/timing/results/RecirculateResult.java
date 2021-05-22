package com.gempukku.swccgo.logic.timing.results;

import com.gempukku.swccgo.logic.timing.EffectResult;

/**
 * The effect result that is emitted when a player recirculates outside of the end of turn step.
 */
public class RecirculateResult extends EffectResult {

    /**
     * Creates an effect result that is emitted when a card becomes 'enslaved'.
     * @param performingPlayerId the performing player
     */
    public RecirculateResult(String performingPlayerId) {
        super(Type.RECIRCULATE_EXCEPT_AT_END_OF_TURN, performingPlayerId);
    }
}
