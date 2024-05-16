package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/*
 * Set: Set 23
 * Type: Interrupt
 * Subtype: Used
 * Title: Out Of Commission (V)
 */
public class Card501_162 extends AbstractUsedInterrupt {
    public Card501_162() {
        super(Side.LIGHT, 5, Title.Out_Of_Commission, Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("I hope that old man got that tractor beam out of commission or this is gonna be a real short trip.");
        setGameText("Choose an artwork card to be lost. OR For remainder of turn, forfeit values may not be increased and opponent may not target your 'hit' cards to be lost. OR Once per game, during your control phase, use 1 Force to relocate [Set 1] Obi-Wan to an adjacent site.");
        addIcons(Icon.A_NEW_HOPE, Icon.VIRTUAL_SET_23);
        setVirtualSuffix(true);
        setTestingText("Out Of Commission (V)");
        hideFromDeckBuilder();
    }
}