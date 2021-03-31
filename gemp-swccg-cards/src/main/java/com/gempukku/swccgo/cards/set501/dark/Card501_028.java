package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;


/**
 * Set: Set 15
 * Type: Character
 * Subtype: Imperial
 * Title: Admiral Piett
 */
public class Card501_028 extends AbstractImperial {
    public Card501_028() {
        super(Side.DARK, 1, 4, 4, 3, 6, "Admiral Piett", Uniqueness.UNIQUE);
        setLore("Veteran of the Imperial military machine. Leader of the Imperial fleet at Endor. Skilled at political maneuvering and appeasing his powerful superiors.");
        setGameText("While piloting Executor, adds 3 to power. Deploy -1 for each of your starship sites on table. If piloting Executor, once per game may take Emperor's Order into hand from Reserve Deck; reshuffle.");
        addPersona(Persona.PIETT);
        addIcons(Icon.VIRTUAL_SET_15, Icon.PILOT, Icon.WARRIOR);
        addKeywords(Keyword.ADMIRAL, Keyword.LEADER);
        setMatchingStarshipFilter(Filters.Executor);
        setTestingText("Admiral Piett");
        hideFromDeckBuilder();
    }
}
