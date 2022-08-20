package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.common.*;

/**
 * Set: Set 20
 * Type: Character
 * Subtype: Alien
 * Title: Ahsoka, Friend Of The Family
 */
public class Card501_055 extends AbstractAlien {
    public Card501_055() {
        super(Side.LIGHT, 1, 5, 5, 6, 7, "Ahsoka, Friend Of The Family", Uniqueness.UNIQUE);
        setLore("Female Togruta.");
        setGameText("Ignores [Sk] Epic Event deployment restrictions. Once per turn, may subtract 1 from a non-weapon destiny targeting your character's ability or defense value. Characters she hits are forfeit = 0. Immune to non-lightsaber weapon’s and attrition < 5.");
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_20);
        addKeywords(Keyword.FEMALE);
        setSpecies(Species.TOGRUTA);
        addPersona(Persona.AHSOKA);
        setTestingText("Ahsoka, Friend Of The Family");
        hideFromDeckBuilder();
    }
}
