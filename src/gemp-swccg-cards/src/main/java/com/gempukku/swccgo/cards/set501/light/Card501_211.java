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
        setLore("'You were right about me. Tell your sister ... you were right.'");
        setGameText("While 'communing': You may not deploy Jedi Masters or characters of ability < 4 (except droids); your Force drains are -1; once per turn, if Return Of The Jedi on table, may [download] a Death Star II site.");
        addIcons(Icon.DEATH_STAR_II, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_27);
        setTestingText("Anakin Skywalker, Redeemed");
    }
}
