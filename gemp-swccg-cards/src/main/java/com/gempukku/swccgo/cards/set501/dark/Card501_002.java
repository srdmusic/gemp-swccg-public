package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.common.*;

/**
 * Set: Set 16
 * Type: Character
 * Subtype: Alien
 * Title: Burg
 */
public class Card501_002 extends AbstractAlien {
    public Card501_002() {
        super(Side.DARK, 2, 3, 6, 1, 4, "Burg", Uniqueness.UNIQUE);
        setArmor(3);
        setLore("Devaronian mercenary.");
        setGameText("Once during battle, may use 1 Force to make Burg power +2 for remainder of turn. Once during battle, opponent may use 1 Force to make Burg power -2 for remainder of turn. At the end of each of your turns, use 1 Force or place Burg in Used Pile.");
        setSpecies(Species.DEVARONIAN);
        addIcons(Icon.WARRIOR, Icon.VIRTUAL_SET_16);
        setTestingText("Burg");
        hideFromDeckBuilder();
    }
}
