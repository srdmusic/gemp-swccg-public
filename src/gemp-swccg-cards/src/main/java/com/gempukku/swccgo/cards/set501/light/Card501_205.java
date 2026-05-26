package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien
 * Title: The Mandalorian & Grogu
 */
public class Card501_205 extends AbstractAlien {
    public Card501_205() {
        super(Side.LIGHT, 1, 5, 6, 4, 7, "The Mandalorian & Grogu", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("[P]3. During your control phase, may [UPLOAD] an Interrupt with 'Mandalorian' in game text or title. Fires Stun Blaster for free. Once per game, if opponent's character lost in battle here, may return it to owner's hand to make them lose 3 Force. Immune to attrition < 4.");
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_27);
        setTestingText("The Mandalorian & Grogu");
        hideFromDeckBuilder();
    }
}
