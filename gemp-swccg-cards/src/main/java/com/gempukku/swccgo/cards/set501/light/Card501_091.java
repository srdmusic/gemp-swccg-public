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

public class Card501_091 extends AbstractObjective {
    public Card501_091() {
        super(Side.LIGHT, 0, Title.Massassi_Base_Operations, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setTestingText("Massassi Base Operations (V)");
        excludeFromDeckBuilder();
    }
}
