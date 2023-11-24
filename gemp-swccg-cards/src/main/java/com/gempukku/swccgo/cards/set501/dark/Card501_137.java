package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Effect
 * Subtype: Normal
 * Title: Ominous Rumors (V)
 */

public class Card501_137 extends AbstractNormalEffect {
    public Card501_137() {
        super(Side.DARK, 5, PlayCardZoneOption.ATTACHED, Title.Ominous_Rumors, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setTestingText("Ominous Rumors (V)");
        hideFromDeckBuilder();
    }
}
