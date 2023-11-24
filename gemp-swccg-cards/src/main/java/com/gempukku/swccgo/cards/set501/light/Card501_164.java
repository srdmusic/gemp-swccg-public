package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used
 * Title: Scomp Link Access (V)
 */

public class Card501_164 extends AbstractUsedInterrupt {
    public Card501_164() {
        super(Side.LIGHT, 3, Title.Scomp_Link_Access, Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setTestingText("Scomp Link Access (V)");
        excludeFromDeckBuilder();
    }
}
