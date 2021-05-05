package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrStartingInterrupt;
import com.gempukku.swccgo.common.*;

/**
 * Set: Set 15
 * Type: Interrupt
 * Subtype: Used Or Starting
 * Title: I Am Part Of The Living Force
 */
public class Card501_038 extends AbstractUsedOrStartingInterrupt {
    public Card501_038() {
        super(Side.LIGHT, 5, "I Am Part Of The Living Force", Uniqueness.UNIQUE);
        setGameText("USED: Activate 1 Force. STARTING: If your starting location had exactly 2 [Light Side], deploy Communing and stack a Jedi on it from Reserve Deck. Deploy up to three Effects that deploy on table and are always immune to Alter. Place Interrupt in Reserve Deck.");
        addIcons(Icon.VIRTUAL_SET_15, Icon.EPISODE_I);
        setTestingText("I Am Part Of The Living Force");
        hideFromDeckBuilder();
    }
}