package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used Or Lost
 * Title: Welcome Home, Lord Tyranus
 */
public class Card501_064 extends AbstractUsedOrLostInterrupt {
    public Card501_064() {
        super(Side.DARK, 4, "Welcome Home, Lord Tyranus", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("USED: If Dooku is your apprentice, [upload] The Works or Petranaki Arena. LOST: Once per game, if Darth Tyranus in battle at a site and you are about to draw a card for battle destiny, instead use his ability number.");
        addIcons(Icon.VIRTUAL_SET_26);
        setTestingText("Welcome Home, Lord Tyranus");
        hideFromDeckBuilder();
    }
    
}
