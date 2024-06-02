package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/*
 * Set: Set 23
 * Type: Character
 * Subtype: Alien
 * Title: Lobot, Lando's Broker
 */
public class Card501_039 extends AbstractAlien {
    public Card501_039() {
        super(Side.DARK, 1, 2, 2, 2, 4, "Lobot, Lando's Broker", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Son of a traveling slaver. Helps run Cloud City with Administrator Lando Calrissian. Speech capability worn away by constant cyborg neural connection.");
        setGameText("Deploys free to Cloud City. Your characters of ability < 5 at same Cloud City site are immune to Clash Of Sabers, may not be targeted to be lost during the weapons segment of a battle, and during your move phase, may use 1 Force to relocate to a related site.");
        addIcons(Icon.SPECIAL_EDITION, Icon.VIRTUAL_SET_23);
        addPersona(Persona.LOBOT);
        setTestingText("Lobot, Lando's Broker");
        hideFromDeckBuilder();
    }
}
