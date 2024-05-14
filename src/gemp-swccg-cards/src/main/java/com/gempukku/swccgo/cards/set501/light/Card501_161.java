package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/*
 * Set: Set 23
 * Type: Interrupt
 * Subtype: Used
 * Title: Out Of Commission (V)
 */
public class Card501_161 extends AbstractUsedInterrupt {
    public Card501_161() {
        super(Side.LIGHT, 4, "Choke", Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Reaching out with the Force, Luke rendered Ortugg unconscious without doing the Gamorrean any actual harm.");
        setGameText("Cancel None Shall Pass. OR Cancel game text of a character of ability < 3 at a Jabba's Palace site until end of turn. OR During battle, target opponent's alien present with your character of ability = 5. Draw destiny. If destiny > target's ability, target excluded from battle.");
        addIcons(Icon.JABBAS_PALACE, Icon.VIRTUAL_SET_23);
        setVirtualSuffix(true);
        setTestingText("Choke (V)");
        hideFromDeckBuilder();
    }
}
