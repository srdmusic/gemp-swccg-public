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
 * Title: Dark Lightning
 */
public class Card501_036 extends AbstractUsedInterrupt {
    public Card501_036() {
        super(Side.DARK, 6, "Dark Lightning", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("Target a character present with your Dark Jedi Master. Draw destiny. If destiny + 2 > target's ability, targets game text is cancelled for remainder of turn (if target is Anakin, Luke, or Rey, opponent also loses 1 Force).");
        addIcons(Icon.VIRTUAL_SET_27);
        setTestingText("Dark Lightning");
        hideFromDeckBuilder();
    }
}
