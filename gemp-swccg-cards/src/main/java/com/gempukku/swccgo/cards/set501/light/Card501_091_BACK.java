package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;

/**
 * Set: Playtesting
 * Type: Objective
 * Title: Massassi Base Operations (V) / One In A Million (V)
 */

public class Card501_091_BACK extends AbstractObjective {
    public Card501_091_BACK() {
        super(Side.LIGHT, 7, Title.One_In_A_Million, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setTestingText("One In A Million (V)");
        excludeFromDeckBuilder();
    }
}
