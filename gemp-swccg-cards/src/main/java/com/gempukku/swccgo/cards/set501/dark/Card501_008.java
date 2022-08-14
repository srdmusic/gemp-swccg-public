package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.common.*;

/**
 * Set: Set 20
 * Type: Character
 * Subtype: Alien
 * Title: Zuckuss (V)
 */
public class Card501_008 extends AbstractAlien {
    public Card501_008() {
        super(Side.DARK, 1, 4, 3, 4, 4, "Zuckuss", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Male Gand. Practitioner of ancient religious findsman vocation. Bounty hunter and scout. Gains surprisingly accurate information through mystical visions during meditation.");
        setGameText("Adds 2 power to anything he pilots. Once per battle, if opponent just drew weapon or battle destiny, " +
                    "may draw destiny and reset opponent's destiny number with your drawn destiny number. " +
                    "Power and defense value +2 with 4-LOM. Immune to attrition < 4.");
        addPersona(Persona.ZUCKUSS);
        addIcons(Icon.DAGOBAH, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_20);
        addKeywords(Keyword.BOUNTY_HUNTER, Keyword.SCOUT);
        setSpecies(Species.GAND);
        setTestingText("Zuckuss (V)");
        hideFromDeckBuilder();
    }
}