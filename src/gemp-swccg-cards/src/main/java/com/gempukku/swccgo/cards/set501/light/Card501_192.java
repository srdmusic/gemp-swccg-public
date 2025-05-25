package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractLostOrStartingInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Lost Or Starting
 * Title: New Leadership Is Needed
 */
public class Card501_192 extends AbstractLostOrStartingInterrupt {
    public Card501_192() {
        super(Side.LIGHT, 5, "New Leadership Is Needed", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("LOST: [upload] a Coruscant Guard. STARTING: If Plead My Case To The Senate on table, deploy How Liberty Dies and 2 Effects that deploy on your side of the table, deploy for free, and are always immune to Alter. Place interrupt in hand");
        addIcons(Icon.CORUSCANT, Icon.VIRTUAL_SET_25);
        setTestingText("New Leadership Is Needed");
        hideFromDeckBuilder();
    }
}
