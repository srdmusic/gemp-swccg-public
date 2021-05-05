package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.common.*;

/**
 * Set: Set 15
 * Type: Character
 * Subtype: Rebel
 * Title: Master Kenobi
 */
public class Card501_040 extends AbstractRebel {
    public Card501_040() {
        super(Side.LIGHT, 1, 5, 5, 6, 9, "Master Kenobi", Uniqueness.UNIQUE);
        setLore("");
        setGameText("While 'communing': Once per turn, may deploy a battleground from Reserve Deck that is related to a location on table; reshuffle. If you just initiated battle, opponent loses 1 Force (2 if Luke in battle). You may not deploy Jedi (except Yoda).");
        addIcons(Icon.WARRIOR, Icon.VIRTUAL_SET_15);
        addPersona(Persona.OBIWAN);
        setTestingText("Master Kenobi");
        hideFromDeckBuilder();
    }
}