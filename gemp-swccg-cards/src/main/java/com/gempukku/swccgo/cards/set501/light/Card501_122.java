package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used
 * Title: Life Day
 */

public class Card501_122 extends AbstractUsedInterrupt {
    public Card501_122() {
        super(Side.LIGHT, 4, "Life Day", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        excludeFromDeckBuilder();
    }
}
