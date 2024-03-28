package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used Or Lost
 * Title: Physical Choke (V)
 */
public class Card501_063 extends AbstractUsedOrLostInterrupt {
    public Card501_063() {
        super(Side.DARK, 3, Title.Physical_Choke, Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity. V);
        setVirtualSuffix(true);
        hideFromDeckBuilder();
    }
}
