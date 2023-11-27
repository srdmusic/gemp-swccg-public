package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractMobileSystem;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;

public class Card501_092 extends AbstractMobileSystem {
    public Card501_092() {
        super(Side.LIGHT, Title.Death_Star, 1, 0, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setTestingText("Death Star (V)");
        hideFromDeckBuilder();
    }
}
