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
 * Title: A Good Friend
 */
public class Card501_173 extends AbstractNormalEffect {
    public Card501_173() {
        super(Side.LIGHT, 6, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "A Good Friend", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("Deploy on table if your [Sk] Epic Event on table. May [DOWNLOAD] Jedi Village or Be With Me. Ben Solo is immune to attrition < 4. Once per turn, Anakin's Lightsaber may relocate between Ben Solo and Rey. Chewie and Finn may move as a react. [A]");
        addIcons(Icon.SKYWALKER, Icon.VIRTUAL_SET_25);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("A Good Friend");
        hideFromDeckBuilder();
    }
}
