package com.gempukku.swccgo.cards.set501.light;

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
 * Title: Balanced Attack & Darklighter Spin
 */
public class Card501_218 extends AbstractUsedOrLostInterrupt {
    public Card501_218() {
        super(Side.LIGHT, 5, "Balanced Attack & Darklighter Spin", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        addComboCardTitles("Balanced Attack", Title.Darklighter_Spin);
        setGameText("USED: ▲ a unique (•) unpiloted starfighter. LOST: During battle at a system or sector, if you are about to draw a card for battle destiny, instead use the ability number of one of your characters piloting in the battle. OR Once per game, if Alderaan 'blown away' (or if opponent has deployed two battleground systems and no battleground sites), deploy a non-unique Corellian Corvette from outside your deck.");
        addIcon(Icon.VIRTUAL_SET_24);
        setTestingText("Balanced Attack & Darklighter Spin");
        hideFromDeckBuilder();
    }
}
