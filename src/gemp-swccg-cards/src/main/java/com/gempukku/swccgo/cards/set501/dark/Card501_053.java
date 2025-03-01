package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used
 * Title: Precise Attack (V)
 */
public class Card501_053 extends AbstractUsedInterrupt {
    public Card501_053() {
        super(Side.DARK, 4, "Precise Attack", Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Only Imperial stormtroopers are so precise.");
        setGameText("For remainder of turn, characters may not have their forfeit increased or targeted to be lost in battle. OR [UPLOAD] Blast Door Controls, Hyperwave Scan, or Pinned Down. OR Relocate Hyperwave Scan to battleground. OR Cancel Clash Of Sabers.");
        addIcons(Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("Precise Attack (V)");
        hideFromDeckBuilder();
    }
}