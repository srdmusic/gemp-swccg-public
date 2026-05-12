package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractStarfighter;
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
 * Title: Swarming Vulture Droid
 */
public class Card501_004 extends AbstractStarfighter {
    public Card501_004() {
        super(Side.DARK, 0, 1, 0, null, 3, null, 3, "Swarming Vulture Droid", Uniqueness.RESTRICTED_3, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("* = number of separatist systems on table. May not Force drain alone. May stack on Droid Racks.");
        addIcons(Icon.EPISODE_I, Icon.PRESENCE, Icon.SEPARATIST, Icon.TRADE_FEDERATION, Icon.VIRTUAL_SET_27);
        setTestingText("Swarming Vulture Droid");
        addModelType(ModelType.DROID_STARFIGHTER);
        hideFromDeckBuilder();
    }
}
