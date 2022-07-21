package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.common.*;

/**
 * Set: Set 20
 * Type: Character
 * Subtype: Alien
 * Title: Bib Fortuna, Heir to the Empire
 */
public class Card501_003 extends AbstractAlien {
    public Card501_003() {
        super(Side.DARK, 1, 4, 2, 1, 3, "Bib Fortuna, Heir to the Empire", Uniqueness.UNIQUE);
        setLore("Twi'lek leader. Gangster");
        setGameText("Jabba is lost. While with two other aliens, adds a destiny to attrition. While at the Audience Chamber, your Force Drains at other Tatooine battlegrounds are +1 and if opponent just deployed a character here, may activate 1 Force.");
        addPersona(Persona.BIB);
        setSpecies(Species.TWILEK);
        addKeywords(Keyword.LEADER, Keyword.GANGSTER);
        addIcons(Icon.VIRTUAL_SET_20);
        setTestingText("Bib Fortuna, Heir to the Empire");
        hideFromDeckBuilder();
    }
}
