package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;

/**
 * Set: Set 17
 * Type: Character
 * Subtype: Imperial
 * Title: Captain Needa (V)
 */
public class Card501_026 extends AbstractImperial {
    public Card501_026() {
        super(Side.DARK, 1, 3, 3, 3, 5, "Captain Needa", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Able leader and captain of the Avenger. Was given his command by Admiral Ozzel. Treated with suspicion by Darth Vader and the Emperor, who distrust Ozzel's close advisors.");
        setGameText("Adds 2 to power of anything he pilots (3 if Avenger). When targeting Needa, Apology Accepted is a Used Interrupt and is [immune to Sense]. If just lost, may place out of play. While out of play, your total battle destiny is +1 where you have an Imperial captain.");
        addKeywords(Keyword.LEADER, Keyword.CAPTAIN);
        addIcons(Icon.DAGOBAH, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_17);
        setMatchingStarshipFilter(Filters.Avenger);
        setTestingText("Captain Needa (V)");
        hideFromDeckBuilder();
    }
}
