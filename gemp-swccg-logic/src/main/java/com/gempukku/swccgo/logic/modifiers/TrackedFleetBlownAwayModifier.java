package com.gempukku.swccgo.logic.modifiers;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;

/**
 * A modifier that indicates that the Shield Gate has been "blown away" this game
 */
public class TrackedFleetBlownAwayModifier extends AbstractModifier {
    /**
     * Creates modifier that indicates that the Shield Gate has been "blown away" this game
     * @param source the source of the modifier
     */
    public TrackedFleetBlownAwayModifier(PhysicalCard source) {
        super(source, null, null, null, ModifierType.TRACKED_FLEET_BLOWN_AWAY, true);
    }

    @Override
    public String getText(GameState gameState, ModifiersQuerying modifiersQuerying, PhysicalCard self) {
        return "Tracked Fleet is 'blown away'";
    }
}