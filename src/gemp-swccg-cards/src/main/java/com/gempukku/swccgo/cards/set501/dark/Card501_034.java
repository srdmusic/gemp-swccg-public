package com.gempukku.swccgo.cards.set501.dark;

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
 * Title: Kintan Guard Beast
 */
public class Card501_034 extends AbstractNormalEffect {
    public Card501_034() {
        super(Side.DARK, 4, PlayCardZoneOption.ATTACHED, "Kintan Guard Beast", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("A dejarik of a ferocious creature with incredible healing abilities. Extinct on the homeworld of Kintan, but used as a guard beasts by many Hutt gangsters.");
        setGameText("Deploy on a location. Characters and starships here cannot be excluded from battle. If you just won a battle here, may place this card on top of any deck or pile. (Immune to Alter.)");
        addIcons(Icon.VIRTUAL_SET_27);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Kintan Guard Beast");
        hideFromDeckBuilder();
    }
}
