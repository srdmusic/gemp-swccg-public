package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDefensiveShield;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;

/**
 * Set: Playtesting
 * Type: Defensive Shield
 * Title: Come Here You Big Coward (V)
 */
public class Card501_047 extends AbstractDefensiveShield {
    public Card501_047() {
        super(Side.DARK, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Come_Here_You_Big_Coward, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Chewie! Come here!");
        setGameText("Plays on table. Sectors are not battlegrounds. While you occupy a battleground and opponent occupies less than two battlegrounds, cancels Asteroid Sanctuary, opponent's Force drains at non-battlegrounds, and opponent's Force retrieval.");
        addIcons(Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("Come Here You Big Coward (V)");
        hideFromDeckBuilder();
    }
}
