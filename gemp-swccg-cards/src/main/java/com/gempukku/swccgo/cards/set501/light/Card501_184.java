package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Lost
 * Title: A Jedi's Focus (V)
 */
public class Card501_184 extends AbstractLostInterrupt {
    public Card501_184() {
        super(Side.LIGHT, 4, "A Jedi's Focus", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        hideFromDeckBuilder();
    }
}
