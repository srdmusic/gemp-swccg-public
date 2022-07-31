package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Set 20
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: Skywalkers (V)
 */
public class Card501_064 extends AbstractUsedOrLostInterrupt {
    public Card501_064() {
        super(Side.LIGHT, 5, Title.Skywalkers, Uniqueness.UNIQUE);
        setLore("Luke and Leia escaped to an unused portion of the Death Star, evading security checkpoints. At a retracted bridge, they swung across on a grappling line through enemy fire.");
        setGameText("USED: Reveal a character or starship from hand; for remainder of turn that card may not be excluded from battle. OR Take For Luck into hand from Reserve Deck; reshuffle. LOST: If you have two Skywalkers in battle, draw two battle destiny if unable to otherwise.");
        addIcon(Icon.VIRTUAL_SET_20);
        setTestingText("Skywalkers (V)");
        hideFromDeckBuilder();
    }
}
