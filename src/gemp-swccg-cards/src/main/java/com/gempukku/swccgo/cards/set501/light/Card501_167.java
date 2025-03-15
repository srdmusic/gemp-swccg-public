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
 * Title: Medal Ceremony
 */
public class Card501_167 extends AbstractNormalEffect {
    public Card501_167() {
        super(Side.LIGHT, 0, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Medal Ceremony", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("With the destruction of the Death Star, the Rebel Alliance received new-found support throughout the galaxy.");
        setGameText("f Massassi Throne Room on table, deploy on table. Non-Jedi Rebels are defense value +1. Once per game, may [download] a Yavin 4 battleground. Once per game, may retrieve a non-[Maintenance], non-[Permanent Weapon] Rebel of ability < 5. [Immune to Alter.]");
        addIcons(Icon.A_NEW_HOPE, Icon.VIRTUAL_SET_25);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Medal Ceremony");
    }    
}
