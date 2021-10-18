package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;

/**
 * Set: Set 17
 * Type: Interrupt
 * Subtype: Lost
 * Title: Wall Of Fire (V)
 */
public class Card501_024 extends AbstractUsedOrLostInterrupt {
    public Card501_024() {
        super(Side.DARK, 5, "Wall Of Fire");
        setVirtualSuffix(true);
        setLore("Walkers are capable of incinerating entire infantry units in seconds. Rebel troops refer to the deadly barrage as the 'wall of fire.'");
        setGameText("USED: If your AT-AT Cannon on table, use 2 Force to take an Effect with \"Imperial\" in title into hand from Reserve Deck; reshuffle. OR Add 1 to your just drawn AT-AT Cannon weapon destiny. LOST: During battle, If your AT-AT Cannon just fired, it may fire again.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_17);
        setTestingText("Wall Of Fire (V)");
        hideFromDeckBuilder();
    }
}