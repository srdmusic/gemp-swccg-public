package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractDroid;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;


/**
 * Set: Set 16
 * Type: Character
 * Subtype: Droid
 * Title: R3-A2 (Arthree-Aytoo) (V)
 */
public class Card501_099 extends AbstractDroid {
    public Card501_099() {
        super(Side.LIGHT, 2, 2, 1, 3, "R3-A2 (Arthree-Aytoo)", Uniqueness.UNIQUE);
        setLore("Special-purpose astromech capable of coordinating piloting coordinates and approach angles during combat.");
        setGameText("Your starships here with an astromech character aboard are power +1 (+2 at Hoth), immune to Lateral Damage, and may move to systems or sectors as a 'react.'");
        addIcons(Icon.SPECIAL_EDITION, Icon.NAV_COMPUTER, Icon.VIRTUAL_SET_16);
        addModelType(ModelType.ASTROMECH);
        setVirtualSuffix(true);
        setTestingText("[Set 17] R3-A2 (Arthree-Aytoo) (V)");
        hideFromDeckBuilder();
    }
}
