package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Effect
 * Subtype: Normal
 * Title: Battle Station Plans
 */

public class Card501_090 extends AbstractNormalEffect {
    public Card501_090() {
        super(Side.LIGHT, 0, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Battle Station Plans", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        hideFromDeckBuilder();
    }
}
