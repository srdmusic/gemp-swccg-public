package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractJediMaster;
import com.gempukku.swccgo.common.*;

/**
 * Set: Set 15
 * Type: Character
 * Subtype: Jedi Master
 * Title: Master Qui-Gon Jinn, An Old Friend
 */
public class Card501_039 extends AbstractJediMaster {
    public Card501_039() {
        super(Side.LIGHT, 1, 7, 6, 7, 8, "Master Qui-Gon Jinn, An Old Friend", Uniqueness.UNIQUE);
        setLore("");
        setGameText("While 'communing': Your total power in battles is +1 for each Jedi 'communing.' Anakin and Obi-Wan may deploy -2 as a 'react.' Once per turn, may place a card from hand on Used Pile to draw top card of Force Pile. You may not deploy Rebels.");
        addIcons(Icon.WARRIOR, Icon.VIRTUAL_SET_15, Icon.EPISODE_I);
        addPersona(Persona.QUIGON);
        setTestingText("Master Qui-Gon Jinn, An Old Friend");
        hideFromDeckBuilder();
    }
}