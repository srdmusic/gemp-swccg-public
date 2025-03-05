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
 * Title: Blast Points (V)
 */
public class Card501_053 extends AbstractUsedInterrupt {
    public Card501_053() {
        super(Side.DARK, 5, "Blast Points", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Only Imperial stormtroopers are so precise.");
        setGameText("[Upload] Ghhhk or Hyperwave Scan. OR If you just won a battle, cancel Tatooine Celebration. OR If opponent just looked at cards in their Force Pile or Used Pile, peek at the top 2 cards of your Reserve Deck; take one into hand.");
        addIcons(Icon.SPECIAL_EDITION, Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("Blast Points (V)");
    }
}