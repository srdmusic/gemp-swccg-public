package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractJediMaster;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/*
 * Set: Set 23
 * Type: Character
 * Subtype: Alien
 * Title: Jedi Marshal Avar Kriss
 */
public class Card501_192 extends AbstractJediMaster {
    public Card501_192() {
        super(Side.LIGHT, 2, 5, 5, 7, 7, "Jedi Marshal Avar Kriss", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Female musician. High Republic.");
        setGameText("Adds 2 to power of anything she pilots (3 if piloting a capital starship). While piloting a capital starship, it is immune to attrition < 5. Once per game, may retrieve a musician (or take one into hand from Reserve Deck; reshuffle). Immune to attrition < 5.");
        addIcons(Icon.EPISODE_I, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_23);
        addKeywords(Keyword.FEMALE, Keyword.MUSICIAN);
        setTestingText("Jedi Marshal Avar Kriss");
        hideFromDeckBuilder();
    }
}