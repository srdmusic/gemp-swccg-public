package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien
 * Title: Wuta (V)
 */
public class Card501_161 extends AbstractAlien {
    public Card501_161() {
        super(Side.LIGHT, 3, 2, 2, 2, 3, Title.Wuta, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Ewok explorer. Scout. Searches for fallen trees to make tools. Tracks predators. First to notice the Imperial presence on Endor.");
        setGameText("Once per game, when deployed, may [UPLOAD] an Endor site. Game text of your other scouts may not be canceled here. During any deploy phase, if an Imperial at an adjacent site, Wuta may move to that site (using landspeed) as a regular move.");
        addIcons(Icon.ENDOR, Icon.VIRTUAL_SET_25);
        addKeywords(Keyword.SCOUT);
        setSpecies(Species.EWOK);
        setVirtualSuffix(true);
        setTestingText("Wuta (V)");
        hideFromDeckBuilder();
    }
}
