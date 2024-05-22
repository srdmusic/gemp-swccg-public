package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/*
 * Set: Set 23
 * Type: Interrupt
 * Subtype: Used Or Lost
 * Title: The Emperor's Hand
 */
public class Card501_038 extends AbstractUsedOrLostInterrupt {
    public Card501_038() {
        super(Side.DARK, 4, "The Emperor's Hand", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("USED: Take Mara Jade, The Emperor's Hand into hand from Reserve Deck; reshuffle. OR If a battle just ended that your Mara won, opponent loses 1 Force and, if Emperor and your Mara on table, may relocate your Mara to a battleground site. LOST: Retrieve Mara Jade's Lightsaber.");
        addIcon(Icon.VIRTUAL_SET_23);
        setTestingText("The Emperor's Hand");
        hideFromDeckBuilder();
    }
}