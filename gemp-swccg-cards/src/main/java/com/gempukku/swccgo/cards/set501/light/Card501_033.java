package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebelResistance;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;

/**
 * Set: Set 15
 * Type: Character
 * Subtype: Rebel
 * Title: Lando, Hero of the Rebellion
 */
public class Card501_033 extends AbstractRebelResistance {
    public Card501_033() {
        super(Side.LIGHT, 3, 3, 2, 3, 5, "Lando, Hero of the Rebellion", Uniqueness.UNIQUE);
        setLore("Leader. Resistance Agent.");
        setGameText("During your turn, may reveal the top three cards of your Reserve Deck, take one starship into hand with a deploy cost < 6 (if possible), and shuffle your Reserve Deck. Adds one destiny to total power, while piloting or with Chewie or Jannah.");
        addPersona(Persona.LANDO);
        addIcons(Icon.VIRTUAL_SET_15, Icon.PILOT, Icon.WARRIOR);
        addKeyword(Keyword.RESISTANCE_AGENT);
        setMatchingStarshipFilter(Filters.Falcon);
        setTestingText("Lando, Hero of the Rebellion");
        hideFromDeckBuilder();
    }
}
