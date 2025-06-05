package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractLostOrStartingInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Lost Or Starting
 * Title: No Civility, Only Politics (V)
 */
public class Card501_121 extends AbstractLostOrStartingInterrupt {
    public Card501_121(){
        super(Side.DARK, 4, "No Civility, Only Politics (V)", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("LOST: [upload] a Coruscant Guard. STARTING: If My Lord, Is That legal? on table, deploy With Thunderous Applause and 2 Effects that deploy on your side of the table, deploy for free, and are always immune to Alter. Place Interrupt in hand.");
        addIcons(Icon.CORUSCANT, Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("No Civility, Only Politics (V)");
        hideFromDeckBuilder();
    }
}
