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
 * Set: Reflections II
 * Type: Effect
 * Title: The Greatest Of All The Jedi
 */
public class Card501_209 extends AbstractNormalEffect {
    public Card501_209() {
        super(Side.LIGHT, 0, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.The_Greatest_Of_All_The_Jedi, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setFrontOfDoubleSidedCard(true);
        setGameText("If your [SKYWALKER] Epic Event on table, deploy on table. Your personal Force generation = 2. Courage Of A Skywalker is destiny +2. Force drains initiated by Skywalkers at battlegrounds may not be canceled. If Anakin in battle alone, may flip this Effect.");
        addIcons(Icon.SKYWALKER, Icon.EPISODE_I, Icon.VIRTUAL_SET_27);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("The Greatest Of All The Jedi");
        hideFromDeckBuilder();
    }
}
