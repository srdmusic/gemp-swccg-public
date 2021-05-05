package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractJediMaster;
import com.gempukku.swccgo.common.*;

/**
 * Set: Set 15
 * Type: Character
 * Subtype: Jedi Master
 * Title: Master Yoda
 */
public class Card501_041 extends AbstractJediMaster {
    public Card501_041() {
        super(Side.LIGHT, 1, 5, 2, 7, 9, "Master Yoda", Uniqueness.UNIQUE);
        setLore("");
        setGameText("While 'communing': During your control phase, if you control more battlegrounds than opponent, retrieve 1 Force. Once per turn, may deploy a battleground with two [Dark Side] from Reserve Deck; reshuffle. Attrition against you is -2. You may not deploy [Permanent Weapon] cards.");
        addIcons(Icon.WARRIOR, Icon.VIRTUAL_SET_15);
        addPersona(Persona.YODA);
        setTestingText("Master Yoda");
        hideFromDeckBuilder();
    }
}