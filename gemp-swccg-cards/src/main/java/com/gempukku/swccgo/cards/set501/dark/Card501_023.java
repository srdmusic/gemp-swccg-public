package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;

/**
 * Set: Set 17
 * Type: Interrupt
 * Subtype: Lost
 * Title: Walker Barrage (V)
 */
public class Card501_023 extends AbstractUsedOrLostInterrupt {
    public Card501_023() {
        super(Side.DARK, 5, Title.Walker_Barrage);
        setVirtualSuffix(true);
        setLore("Before an AT-AT's troops can disembark to engage the enemy, the walker must first destroy the Rebel traitors' defensive emplacements.");
        setGameText("USED: During battle at a site, instead of firing one of your vehicle weapons, cause one opponent’s character present to be power -4 until end of turn. LOST: If you occupy a site with an AT-AT, cancel a Force drain at a related site.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_17);
        setTestingText("Wall Of Fire (V)");
        hideFromDeckBuilder();
    }
}