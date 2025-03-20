package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Effect
  * Title: Jabba's Influence (V)
 */
public class Card501_034 extends AbstractNormalEffect {
    public Card501_034() {
        super(Side.DARK, 3, PlayCardZoneOption.ATTACHED, Title.Jabbas_Influence, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Jabba makes offers one cannot refuse. Smugglers, thieves and competitors who do not acquiesce have been rumored to wake up with a bantha's head in their bed.");
        setGameText("Deploy on Audience Chamber. Opponent may not target this site with I Must Be Allowed To Speak. May raise your converted Audience Chamber to the top. If opponent just deployed a character here, may place a card from hand on Force Pile. [Immune to Alter.]");
        addKeywords(Keyword.DEPLOYS_ON_SITE);
        addIcons(Icon.SPECIAL_EDITION, Icon.JABBAS_PALACE, Icon.VIRTUAL_SET_25);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Jabba's Influence (V)");
        setVirtualSuffix(true);
        hideFromDeckBuilder();
    }
}