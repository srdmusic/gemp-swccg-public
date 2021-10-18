package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Set 17
 * Type: Interrupt
 * Subtype: Used Or Lost
 * Title: Dark Approach (V)
 */
public class Card501_020 extends AbstractUsedInterrupt {
    public Card501_020() {
        super(Side.LIGHT, 4, "Dark Approach", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("'The Force is with you, young Skywalker. But you are not a Jedi yet.'");
        setGameText("If opponent just retrieved Force, opponent must lose 1 Force. OR Cancel Dark Strike, Stunning Leader or You Are Beaten. OR If your Skywalker defending a battle alone, add one destiny to your total power (if [CC] Luke, he is also immune to attrition).");
        addIcons(Icon.CLOUD_CITY, Icon.VIRTUAL_SET_17);
        setTestingText("Dark Approach (V)");
        hideFromDeckBuilder();
    }
}