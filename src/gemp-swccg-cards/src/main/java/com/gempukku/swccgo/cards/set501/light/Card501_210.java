package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractJediMasterRepublic;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Jedi Master/Republic
 * Title: Kelleran Beq
 */

public class Card501_210 extends AbstractJediMasterRepublic {
    public Card501_210() {
        super(Side.LIGHT, 1, 8, 6, 7, 8, "Kelleran Beq", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Jedi survivor.");
        setGameText("[Pilot] 2. Any lightsaber may deploy on Beq. Once per game, may deploy up to two lightsabers on Beq from Lost Pile. Your other characters here are defense value +1 for each lightsaber on Beq. Adds one battle destiny with Grogu or a Padawan. Immune to attrition < 6.");
        addKeyword(Keyword.JEDI_SURVIVOR);
        addIcons(Icon.EPISODE_I, Icon.PILOT, Icon.VIRTUAL_SET_26);
        addIcon(Icon.WARRIOR, 2);
        setTestingText("Kelleran Beq");
    }
    
}
