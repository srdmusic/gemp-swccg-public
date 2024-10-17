package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/*
 * Set: Playtesting
 * Type: Effect
 * Title: Blast Door Controls (V)
 */

public class Card501_035 extends AbstractNormalEffect {
    public Card501_035() {
        super(Side.DARK, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Blast_Door_Controls, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Panels control blast doors and key security lock-downs during alerts. Luke destroyed one, locking Imperial forces out of Hangar Bay 327.");
        setGameText("Deploy on table. Cancels Narrow Escape, Blast The Door, Kid!, and Rebel Barrier. Players may not search their Used Piles more than once each turn. Opponent must lose 1 Force in order to cancel a battle (or move a card with ability away from battle).");
        addIcons(Icon.VIRTUAL_SET_24);
        setTestingText("Blast Door Controls (V)");
        hideFromDeckBuilder();
    }
}
