package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Rebel
 * Title: Anakin Skywalker, Redeemed
 */
public class Card501_211 extends AbstractRebel {
    public Card501_211() {
        super(Side.LIGHT, 0, 6, 3, 6, 8, "Anakin Skywalker, Redeemed", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("While 'communing': You may not deploy Jedi Masters or any characters of ability < 4 (except for droids); your Force drains are -1; while Return Of The Jedi on table, may [DOWNLOAD] a Death Star II site.");
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_27);
        setTestingText("Anakin Skywalker, Redeemed");
        hideFromDeckBuilder();
    }
}
