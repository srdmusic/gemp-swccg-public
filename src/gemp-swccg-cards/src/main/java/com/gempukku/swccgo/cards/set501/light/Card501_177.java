package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used
 * Title: My Sister Has It
 */
public class Card501_177 extends AbstractUsedInterrupt {
    public Card501_177() {
        super(Side.LIGHT, 5, "My Sister Has It", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("If a [Skywalker] Epic Event on table, [UPLOAD] Lars' Moisture Farm or Skywalker Hut. OR For remainder of turn, neither player may limit battle destiny draws at Leia's location. OR Subtract 1 from a Force Lightning or choke destiny draw (unless targeting an Undercover spy).");
        addIcons(Icon.SKYWALKER, Icon.VIRTUAL_SET_25);
        setTestingText("My Sister Has It");
        hideFromDeckBuilder();
    }
}
