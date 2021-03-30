package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrStartingInterrupt;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

//•Let The Wookiee Win (v)
//LIGHT - USED OR STARTING INTERRUPT
//USED: Subtract 2 from a destiny draw targeting your Wookiee’s ability or defense value. STARTING: If your starting location had exactly 2 [LS], deploy a Kashyyyk location and up to three Effects that are always immune to Alter. Place Interrupt in Reserve Deck.

/**
 * Set: Set 15
 * Type: Interrupt
 * Subtype: Used Or Starting
 * Title: Let The Wookiee Win (V)
 */
public class Card501_030 extends AbstractUsedOrStartingInterrupt {
    public Card501_030() {
        super(Side.LIGHT, 5, Title.Let_The_Wookiee_Win, Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("'It's not wise to upset a Wookiee.' 'But sir, nobody worries about upsetting a droid.' 'That's cause a droid don't pull people's arms out of their sockets when they lose.'");
        setGameText("USED: Subtract 2 from a destiny draw targeting your Wookiee’s ability or defense value." +
                "STARTING: If your starting location had exactly 2 [LS], deploy a Kashyyyk location and up to three Effects that are always immune to Alter. Place Interrupt in Reserve Deck.");
        addIcons(Icon.A_NEW_HOPE, Icon.VIRTUAL_SET_15);
        setVirtualSuffix(true);
        setTestingText("Let The Wookiee Win (V)");
        hideFromDeckBuilder();
    }
}