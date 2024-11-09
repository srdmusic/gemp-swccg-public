package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: All Wrapped Up (V)
 */
public class Card501_066 extends AbstractNormalEffect {
    public Card501_066() {
        super(Side.DARK, 2, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.All_Wrapped_Up, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("A capture cable is a quick and effective way for bounty hunters to suddenly snare their target.");
        setGameText("Deploy on table. Unless Court Of The Vile Gangster on table, [Dag] and [CC] bounty hunters are forfeit +2. May / [JP] Ord Mantell. If opponent's character about to be forfeited, your bounty hunter present may capture that character (character first restored to normal) [A]");
        addIcons(Icon.JABBAS_PALACE, Icon.VIRTUAL_SET_24);
        addImmuneToCardTitle(Title.Alter);
        setVirtualSuffix(true);
        setTestingText("All Wrapped Up (V)");
        hideFromDeckBuilder();
    }
}
