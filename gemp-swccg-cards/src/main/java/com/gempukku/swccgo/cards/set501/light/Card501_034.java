package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractResistance;
import com.gempukku.swccgo.common.*;

/**
 * Set: Set 15
 * Type: Character
 * Subtype: Resistance
 * Title: Paige Tico
 */
public class Card501_034 extends AbstractResistance {
    public Card501_034() {
        super(Side.LIGHT, 3, 2, 2, 2, 5, Title.Paige, Uniqueness.UNIQUE);
        setLore("Female Gunner.");
        setGameText("While out of play, adds 1 to your total power where you have a resistance character of ability = 2. Adds 1 to weapon destiny and defense value of anything she is aboard as a passenger. When lost may place of out play.");
        addIcons(Icon.EPISODE_VII, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_9);
        addKeywords(Keyword.FEMALE);
    }
}
