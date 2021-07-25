package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.common.*;

/**
 * Set: Set 16
 * Type: Character
 * Subtype: Alien
 * Title: Tarfful
 */
public class Card501_082 extends AbstractAlien {
    public Card501_082() {
        super(Side.LIGHT, 3, 4, 6, 2, 5, "Tarfful", Uniqueness.UNIQUE);
        setLore("Wookiee leader.");
        setGameText("If a battle was just initiated here, may name a non-[Immune to Sense] Interrupt; Interrupts with that title may not be played for remainder of battle. Once per game, if Yoda about to be lost, may take him into hand instead.");
        setSpecies(Species.WOOKIEE);
        addKeywords(Keyword.LEADER);
        addIcons(Icon.WARRIOR, Icon.EPISODE_I, Icon.VIRTUAL_SET_16);
        setTestingText("Tarfful");
        hideFromDeckBuilder();
    }

    @Override
    public final boolean hasSpecialDefenseValueAttribute() {
        return true;
    }

    @Override
    public final float getSpecialDefenseValue() {
        return 4;
    }
}
