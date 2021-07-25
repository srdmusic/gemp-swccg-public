package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRepublic;
import com.gempukku.swccgo.common.*;

/**
 * Set: Set 16
 * Type: Character
 * Subtype: Republic
 * Title: Wookiee Warrior
 */
public class Card501_084 extends AbstractRepublic {
    public Card501_084() {
        super(Side.LIGHT, 2, 4, 2, 2, 4, "Wookiee Warrior", Uniqueness.RESTRICTED_3);
        setLore("");
        setGameText("May add one destiny to total power. During battle, unless 'hit', may lose Wookiee Warrior to restore a 'hit' character to normal.");
        setSpecies(Species.WOOKIEE);
        addIcons(Icon.WARRIOR, Icon.EPISODE_I, Icon.VIRTUAL_SET_16);
        setTestingText("Wookiee Warrior");
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
