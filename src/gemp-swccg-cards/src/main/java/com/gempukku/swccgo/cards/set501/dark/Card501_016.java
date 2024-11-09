package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDroid;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Droid
 * Title: Vader's Probe Droid
 */
public class Card501_016 extends AbstractDroid {
    public Card501_016() {
        super(Side.DARK, 3, 1, 1, 2, "Vader's Probe Droid", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("Nabrun Leids may not 'transport' to here. During your move phase, may use 2 Force to relocate a Dark Jedi (with any captives they are escorting), your Mara, or an Inquistor from here to same site as a Jedi or Dark Jedi; place this droid in Used Pile.");
        addIcons(Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_24);
        addModelType(ModelType.PROBE);
        setTestingText("Vader's Probe Droid");
        hideFromDeckBuilder();
    }
}
