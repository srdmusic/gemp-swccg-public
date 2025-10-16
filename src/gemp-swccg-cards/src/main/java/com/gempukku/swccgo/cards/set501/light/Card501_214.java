package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractDroid;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Droid
 * Title: See-Threepio (V)
 */

public class Card501_214 extends AbstractDroid {
    public Card501_214() {
        super(Side.LIGHT, 2, 3, 1, 4, Title.See_Threepio, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("C-3PO was Jabba's 'khan chita,' or translator. Survived more battles than most members of the Alliance. Wasn't informed of R2-D2's role in the rescue of Han.");
        setGameText("While at a Jabba's Palace site: once per turn may place a card from hand in Used Pile to draw top card of Reserve Deck or activate 1 Force. If you control two Jabba's Palace sites, during your control phase retrieve 1 Force (into hand if [Jabba's Palace] R2-D2 here).");
        addIcons(Icon.PREMIUM, Icon.JABBAS_PALACE, Icon.VIRTUAL_SET_26);
        addModelType(ModelType.PROTOCOL);
        addPersona(Persona.C3PO);
        setVirtualSuffix(true);
        setTestingText("See-Threepio (V)");
        hideFromDeckBuilder();
    }
    
}
