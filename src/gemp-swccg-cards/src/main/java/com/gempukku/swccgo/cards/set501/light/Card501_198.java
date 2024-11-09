package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractResistance;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Resistance
 * Title: Ben Solo
 */
public class Card501_198 extends AbstractResistance {
    public Card501_198() {
        super(Side.LIGHT, 1, 5, 6, 5, 8, "Ben Solo", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("P3. If drawn for destiny, may ▲ Rey or a lightsaber. Only deploys if a [E7] Epic Event on table. Your total battle destiny here is +1 for each interrupt you have out of play (limit +3). Once per game, may deploy a lightsaber on Ben from lost pile.");
        addIcons(Icon.EPISODE_VII, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_24);
        addPersona(Persona.KYLO);
        setTestingText("Ben Solo");
        hideFromDeckBuilder();
    }
}
