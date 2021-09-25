package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;


/**
 * Set: Set 17
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: I'd Just As Soon Kiss A Wookiee (V)
 */
public class Card501_005 extends AbstractUsedOrLostInterrupt {
    public Card501_005() {
        super(Side.DARK, 2, Title.Id_Just_As_Soon_Kiss_A_Wookiee, Uniqueness.UNIQUE);
        setLore("'I can arrange that. You could USE a good kiss!'");
        setGameText("USED: Use 1 Force to target a just-deployed Rebel or Resistance character (free if Leia while a Wookiee on table). Opponent must move the character away or return character to hand. LOST: Perform the Used function of this Interrupt for free.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_17);
        setVirtualSuffix(true);
        setTestingText("I'd Just As Soon Kiss A Wookiee (V)");
        hideFromDeckBuilder();
    }
}