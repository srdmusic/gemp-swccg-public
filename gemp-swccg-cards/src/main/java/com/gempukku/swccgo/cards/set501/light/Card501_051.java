package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.common.Side;

/**
 * Set: Set 20
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: Might of the Mandalorians
 */
public class Card501_051 extends AbstractUsedOrLostInterrupt {
    protected Card501_051() {
        super(Side.LIGHT, 4, "Might of the Mandaloriansn");
        setLore("");
        setGameText("USED: If your Mandalorian was just 'hit,' opponent chooses: make the character that fired that weapon 'hit’ or restore your character to normal.\n" +
                "LOST: Once per game, if your Mandalorian is in battle, add 2 to a just drawn destiny..");
        setTestingText("Might of the Mandalorians");
        hideFromDeckBuilder();
    }
}
