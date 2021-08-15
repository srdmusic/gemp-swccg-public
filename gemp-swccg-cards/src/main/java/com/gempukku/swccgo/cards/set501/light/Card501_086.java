package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.common.*;

/**
 * Set: Set 16
 * Type: Interrupt
 * Subtype: Used
 * Title: Wookiee Roar (V)
 */
public class Card501_086 extends AbstractUsedInterrupt {
    public Card501_086() {
        super(Side.LIGHT, 3, Title.Wookiee_Roar, Uniqueness.UNIQUE);
        setLore("'GHRRRRAARRRRHG!'");
        setGameText("If this is the top card of your Lost Pile, if you win a battle with two Wookiees, may retrieve this card. If a battle was just initiated involving your Wookiee, add one destiny to your power. OR Cancel an attempt to make a Wookiee lost or captured.");
        addIcons(Icon.A_NEW_HOPE, Icon.EPISODE_I, Icon.VIRTUAL_SET_16);
        setVirtualSuffix(true);
        setTestingText("Wookiee Roar (V)");
        hideFromDeckBuilder();
    }
}