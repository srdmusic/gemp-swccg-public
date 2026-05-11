package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Imperial
 * Title: Warrant Officer Bachenkall
 */
public class Card501_017 extends AbstractImperial {
    public Card501_017() {
        super(Side.DARK, 2, 2, 2, 2, 4, "Warrant Officer Bachenkall", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Warrant Officer Bachenkall is typical of the many graduates of the Imperial Training academy on Raithal. The sector naval school trains pilots in capital starship help tactics.");
        setGameText("Adds 2 to power and 1 to hyperspeed of any capital starship he pilots. Opponent must first use 1 Force (if able) to draw a card for battle destiny here. Once per game, may deploy Imperial Pilot here from outside the game (for -2 Force).");
        addIcons(Icon.PILOT, Icon.VIRTUAL_SET_27);
        setTestingText("Warrant Officer Bachenkall");
        hideFromDeckBuilder();
    }
}
