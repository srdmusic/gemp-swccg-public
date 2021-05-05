package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.common.*;

/**
 * Set: Set 15
 * Type: Character
 * Subtype: Rebel
 * Title: Han Solo, Optimistic General
 */
public class Card501_042 extends AbstractRebel {
    public Card501_042() {
        super(Side.LIGHT, 1, 4, 4, 3, 6, "Han Solo, Optimistic General", Uniqueness.UNIQUE);
        setLore("Leader. Scout.");
        setGameText("May be targeted instead of a Resistance character by I Want That Map. Adds 3 to power of anything he pilots. Rebels of printed destiny < 4 are destiny +1. Adds one battle destiny with Chewie or [Endor] Leia. Draws one battle destiny if unable to otherwise.");
        addIcons(Icon.WARRIOR, Icon.PILOT, Icon.ENDOR, Icon.VIRTUAL_SET_15);
        addPersona(Persona.HAN);
        addKeywords(Keyword.LEADER, Keyword.SCOUT, Keyword.GENERAL);
        setTestingText("Han Solo, Optimistic General");
        hideFromDeckBuilder();
    }
}