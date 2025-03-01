package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractResistance;
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
 * Subtype: Resistance
 * Title: Finn, Resistance Leader
 */
public class Card501_174 extends AbstractResistance {
    public Card501_174() {
        super(Side.LIGHT, 1, 4, 4, 4, 6, "Finn, Resistance Hero", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Leader.");
        setGameText("During battle, if Luke out of play (or Rose or Jannah here), adds one destiny to total power. While alone, opponent may not cancel or reduce Force drains at same [E7] battleground. Jedi Lightsaber may deploy on Finn. Immune to attrition < 4.");
        addPersona(Persona.FINN);
        addIcons(Icon.EPISODE_VII, Icon.WARRIOR, Icon.VIRTUAL_SET_25);
        addKeywords(Keyword.LEADER);
        setTestingText("Finn, Resistance Leader");
        hideFromDeckBuilder();
    }
}
