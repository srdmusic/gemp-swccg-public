package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImmediateEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Effect
 * Subtype: Immediate
 * Title: Echo Base Destroyed
 */

public class Card501_133 extends AbstractImmediateEffect {
    public Card501_133() {
        super(Side.DARK, 6, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Echo Base Destroyed", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        hideFromDeckBuilder();
    }
}
