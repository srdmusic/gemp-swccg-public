package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: Sacrifice For Something Bigger
 */
public class Card501_202 extends AbstractNormalEffect {
    public Card501_202() {
        super(Side.LIGHT, 0, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Sacrifice_For_Something_Bigger, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setFrontOfDoubleSidedCard(true);
        setGameText("Deploy on table if Zero Hour on table. Once per game, may [DOWNLOAD] Kanan. If Kanan just lost, place him out of play, relocate this Effect to a Lothal site (if possible) and flip it. (Immune to Alter.)");
        addIcons(Icon.VIRTUAL_SET_27);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Sacrifice For Something Bigger");
        hideFromDeckBuilder();
    }
    
}
