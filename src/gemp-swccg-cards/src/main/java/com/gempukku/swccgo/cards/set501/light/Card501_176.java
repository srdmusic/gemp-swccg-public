package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

import java.util.Collections;
import java.util.List;


/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used Or Lost Interrupt
 * Title: Insertion Planning (V)
 */
public class Card501_176 extends AbstractUsedOrLostInterrupt {
    public Card501_176() {
        super(Side.LIGHT, 4, Title.Critical_Error_Revealed, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Hologram technology allows efficient communication of complex intelligence during war room briefings.");
        setGameText("USED: Suspend Ominous Rumors or There Are Many Hunting You Now for remainder of turn. OR [Upload] Lak Sivrak, Orrimaarko, Tala Durith, or [Endor] Chewie. LOST: Lose 1 Force to exclude a passenger from battle (then place this Interrupt out of play).");
        addIcons(Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_25);
        addKeyword(Keyword.HOLOGRAM);
        setVirtualSuffix(true);
        setTestingText("Critical Error Revealed (V)");
    }


}