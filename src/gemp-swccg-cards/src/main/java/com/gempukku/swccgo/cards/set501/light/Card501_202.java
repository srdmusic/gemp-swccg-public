package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: A Cunning Warrior
 */
public class Card501_202 extends AbstractNormalEffect {
    public Card501_202() {
        super(Side.LIGHT, 3, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "A Cunning Warrior", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Despite being alone, trapped and desperately outmatched, Luke continued his battle with the Dark Lord of the Sith.");
        setGameText("Deploy on table if your [Sk] Epic Event on table. Skywalkers may initiate battles for free. Once per turn, may / Anakin’s Lightsaber, Polis Masa, or a Cloud City Corridor. If a Skywalker warrior in battle, may cause a player to activate 1 Force. [Immune to Alter.]");
        addIcons(Icon.SKYWALKER, Icon.VIRTUAL_SET_24);
        setTestingText("A Cunning Warrior");
        hideFromDeckBuilder();
    }
}
