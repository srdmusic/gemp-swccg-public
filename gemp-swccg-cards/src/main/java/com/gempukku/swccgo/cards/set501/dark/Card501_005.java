package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Set 20
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: A Fine Addition to my Collection
 */
public class Card501_005 extends AbstractUsedOrLostInterrupt {
    protected Card501_005() {
        super(Side.DARK, 5, "A Fine Addition to my Collection", Uniqueness.UNIQUE);
        setLore("");
        setGameText("USED: If Grievous just swung a stolen lightsaber, add one battle destiny.\n" +
                    "LOST: Cancel an attempt to target Grievous with a lightsaber. OR Deploy any lightsaber from your Lost Pile on Grievous (who may use that lightsaber).");
        setTestingText("A Fine Addition to my Collection");
        hideFromDeckBuilder();
    }
}
