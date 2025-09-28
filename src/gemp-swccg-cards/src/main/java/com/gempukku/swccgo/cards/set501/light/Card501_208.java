package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractJediMaster;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Jedi Master
 * Title: Quinlan Vos
 */

public class Card501_208 extends AbstractJediMaster {
    public Card501_208() {
        super(Side.LIGHT, 1, 8, 7, 7, 8, "Quinlan Vos", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Jedi survivor.");
        setGameText("[Pilot] 2. Adds one battle destiny with Asajj, Dooku, or Grievous. Once per turn, may peek at the top card of any Reserve Deck or subtract 1 from a weapon destiny here. Dark Approach is a Used interrupt. Immune to Sniper and attrition < 6 (< 8 if alone).");
        addKeyword(Keyword.JEDI_SURVIVOR);
        addPersona(Persona.QUIGON);
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_26);
    }

}
