package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien
 * Title: Rystall (V)
 */
public class Card501_062 extends AbstractAlien {
    public Card501_062() {
        super(Side.DARK, 3, 2, 1, 2, 3, "Rystall", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Musician. Raised by Ortolans. Grew up on the streets of Coruscant. Rescued from the Black Sun crime cartel by Lando Calrissian.");
        setGameText("Power and forfeit +2 at a Coruscant site. Once per turn, if you just deployed a Black Sun agent to same site, may retrieve 1 Force. If opponent's [Maintenance] card just deployed here, it may not battle for remainder of turn.");
        addIcons(Icon.SPECIAL_EDITION, Icon.VIRTUAL_SET_26);
        addKeywords(Keyword.MUSICIAN, Keyword.FEMALE);
        setVirtualSuffix(true);
        setTestingText("Rystall (V)");
    }
}
