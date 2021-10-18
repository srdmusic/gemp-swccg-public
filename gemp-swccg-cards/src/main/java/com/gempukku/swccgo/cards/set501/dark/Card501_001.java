package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Set 17
 * Type: Character
 * Subtype: Imperial
 * Title: Deputy Director Harus Ison
 */
public class Card501_001 extends AbstractImperial {
    public Card501_001() {
        super(Side.DARK, 2, 3, 3, 3, 4, "Deputy Director Harus Ison", Uniqueness.UNIQUE);
        setLore("ISB leader.");
        setGameText("When deployed, your Imperial of ability < 5 may make a regular move from here. During your deploy phase, may send a unique ISB agent here to your Used Pile; search that pile for another non-leader ISB agent and deploy it here for free.");
        addKeywords(Keyword.LEADER);
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_17);
        setTestingText("Deputy Director Harus Ison");
        hideFromDeckBuilder();
    }
}
