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
 * Title: The Greatest Of All The Jedi
 */
public class Card501_209_BACK extends AbstractNormalEffect {
    public Card501_209_BACK() {
        super(Side.LIGHT, 7, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.The_Greatest_Of_All_The_Jedi, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("You total battle destiny is +1 for each character in battle involving Anakin. If opponent has 3 or more characters in battle, Anakin may fire a weapon twice. Flip this card at the end of turn (if Anakin on table, opponent loses 1 Force). (Immune to Alter.)");
        addIcons(Icon.SKYWALKER, Icon.EPISODE_I, Icon.VIRTUAL_SET_27);
        setTestingText("The Greatest Of All The Jedi");
        hideFromDeckBuilder();
    }
}
