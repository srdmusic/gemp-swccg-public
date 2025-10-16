package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used Or Lost
 * Title: Ommni Box & It's Worse (V)
 */

public class Card501_071 extends AbstractUsedOrLostInterrupt {
    public Card501_071() {
        super(Side.DARK, 5, Title.Ommni_Box_Its_Worse, Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        addComboCardTitles(Title.Ommni_Box, Title.Its_Worse);
        setGameText("USED: For remainder of turn, you lose no Force to the following cards on table (if any): A Good Blaster At Your Side, No Disintegrations!, Stardust, and They Will Be Lost And Confused. OR Cancel It Could Be Worse, It Can Wait or It's a Trap! (Immune to It's A Hit!) OR Shuffle any player's Reserve Deck or Lost Pile. OR ▲ a character with 'Cantina' in lore or game text. LOST: If opponent just lost a battle, they lose 2 Force.");
        addIcons(Icon.REFLECTIONS_II, Icon.VIRTUAL_SET_26);
        setVirtualSuffix(true);
        setTestingText("Ommni Box & It's Worse (V)");
        hideFromDeckBuilder();
    }
    
}
