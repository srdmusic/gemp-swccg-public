package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/*
 * Set: Set 23
 * Type: Effect
 * Subtype: Normal
 * Title: Jabba's Last Chance
 */
public class Card501_193 extends AbstractNormalEffect {
    public Card501_193() {
        super(Side.LIGHT, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Jabba's Last Chance", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Jabba! This is your last chance. Free us or die.");
        setGameText("If Han is frozen, deploy on table. Chewie, Lando, and Leia are immune to attrition < 4. Once per turn, may deploy a character weapon from Reserve Deck; reshuffle. Once per game, if opponent just initiated a battle, may take the first weapons segment action. [Immune to Alter.]");
        addIcon(Icon.VIRTUAL_SET_23);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Jabba's Last Chance");
        hideFromDeckBuilder();
    }
}
