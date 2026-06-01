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
 * Title: Molator Guardian
 */
public class Card501_208 extends AbstractNormalEffect {
    public Card501_208() {
        super(Side.LIGHT, 4, PlayCardZoneOption.ATTACHED, "Molator Guardian", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Dejarik representation of mythical Molator guardian. The spirit of Grimtaash is said to protect Alderaanian royalty from corruption and betrayal.");
        setGameText("Deploy on a location. Characters and starships here cannot be excluded from battle or targeted by Set For Stun. If you just won a battle here, may place this card on top of any deck or pile. (Immune to Alter.)");
        addIcons(Icon.VIRTUAL_SET_27);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Molator Guardian");
        hideFromDeckBuilder();
    }
}
