package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDroid;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Droid
 * Title: Guri
 */
public class Card501_060 extends AbstractDroid {
    public Card501_060() {
        super(Side.DARK, 2, 6, 6, 6, Title.Guri, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setArmor(5);
        setLore("Human-replica droid. Programmed to function as Xizor's personal bodyguard and assassin. Black Sun agent. Cost 9 million credits. Worth every decicred.");
        setGameText("[Pilot] 2. Draws one battle destiny if unable to otherwise. While present at a site with Xizor, Force drain +1 here and he may not be targeted by weapons (unless Guri 'hit'). If your [Reflections II] objective on table, immune to attrition < 5.");
        addIcons(Icon.REFLECTIONS_II, Icon.PILOT, Icon.WARRIOR, Icon.PRESENCE, Icon.VIRTUAL_SET_26);
        addKeywords(Keyword.FEMALE, Keyword.BLACK_SUN_AGENT, Keyword.BODYGUARD, Keyword.ASSASSIN);
        addModelTypes(ModelType.ASSASSIN);
        setVirtualSuffix(true);
        setTestingText("Guri (V)");
    }

}
