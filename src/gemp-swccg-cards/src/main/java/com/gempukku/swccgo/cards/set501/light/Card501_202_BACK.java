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
public class Card501_202_BACK extends AbstractNormalEffect {
    public Card501_202_BACK() {
        super(Side.LIGHT, 7, PlayCardZoneOption.YOUR_SIDE_OF_LOCATION, Title.Sacrifice_For_Something_Bigger, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("Moves like a character at normal use of the Force. Where present, your Rebels gain Phoenix Squadron, your total power in battles is +3, and opponent's immunity to attrition is canceled. Effect canceled if two Imperials (or a Dark Jedi) control this site. (Immune to Alter.)");
        addIcons(Icon.VIRTUAL_SET_27);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Sacrifice For Something Bigger");
        hideFromDeckBuilder();
    }
}
