package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDarkJediMaster;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Dark Jedi Master
 * Title: Master Sidious
 */
public class Card501_050 extends AbstractDarkJediMaster {
    public Card501_050() {
        super(Side.DARK, 1, 6, 5, 7, 8, "Master Sidious", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("While alone on Coruscant, your apprentice is immune to attrition. Disarmed canceled here. During your move phase, if you control this site, may use 1 Force to relocate between a Coruscant site and apprentice's site (or vice versa). Immune to attrition.");
        addIcons(Icon.WARRIOR, Icon.EPISODE_I, Icon.VIRTUAL_SET_25);
        addPersona(Persona.SIDIOUS);
        setTestingText("Master Sidious");
        hideFromDeckBuilder();
    }
}
