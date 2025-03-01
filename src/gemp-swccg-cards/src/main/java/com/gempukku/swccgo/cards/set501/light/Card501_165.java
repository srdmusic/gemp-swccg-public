package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Lost
 * Title: Courage Of A Skywalker (V)
 */
public class Card501_165 extends AbstractLostInterrupt {
    public Card501_165() {
        super(Side.LIGHT, 2, Title.Courage_Of_A_Skywalker, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Despite being alone, trapped and desperately outmatched, Luke continued his battle with the Dark Lord of the Sith.");
        setGameText("When drawn for destiny, destiny +1 for each [Sk] Effect on table. During battle where your Skywalker is alone, add one destiny to total power. OR Cancel You Are Beaten OR Once per game, if a Skywalker in a battle or duel with a Dark Jedi, make a just drawn destiny = 2.");
        addIcons(Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("Courage Of A Skywalker (V)");
        hideFromDeckBuilder();
    }
}