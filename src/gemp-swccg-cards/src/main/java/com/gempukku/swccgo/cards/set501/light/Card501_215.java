package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Rebel
 * Title: Luthen Rael
 */
public class Card501_215 extends AbstractRebel {
    public Card501_215() {
        super(Side.LIGHT, 1, 1, 2, 4, 1, "Luthen Rael", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Spy. Leader. Massassi Group.");
        setGameText("While at a site opponent does not occupy, once per turn during a battle involving your spy, may add or subtract 1 from a just drawn destiny. While alone at a non-battleground, may lose 1 Force to place Luthen out of play; may activate up to 2 Force.");
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_27);
        setTestingText("Luthen Rael");
        hideFromDeckBuilder();
    }
}
