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
 * Title: Endless Legions
 */
public class Card501_063 extends AbstractUsedInterrupt {
    public Card501_063() {
        super(Side.DARK, 3, "Endless Legions", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("If your stormtroopers control three battlegrounds and/or Rebel Base locations, choose: Draw 3 cards from Force Pile, then place 2 cards from hand on Force Pile. OR Once per game, your Force drains where you have a stormtrooper may not be canceled or reduced this turn.");
        addIcons(Icon.VIRTUAL_SET_26);
        setTestingText("Endless Legions");
        hideFromDeckBuilder();
    }
    
}
