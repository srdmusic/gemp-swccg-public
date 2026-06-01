package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien
 * Title: Run'heb Voend
 */
public class Card501_016 extends AbstractAlien {
    public Card501_016() {
        super(Side.DARK, 3, 2, 2, 2, 4, "Run'heb Voend", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Jawa mechanic.");
        setGameText("Once per game, may [DOWNLOAD] a Sandcrawler here. While driving a Sandcrawler it moves for free, may not be targeted by weapons and adds one battle destiny. Once during your turn, may select a player to activate 1 Force.");
        setSpecies(Species.JAWA);
        addIcons(Icon.VIRTUAL_SET_27);
        setTestingText("Run'heb Voend");
        hideFromDeckBuilder();
    }
}
