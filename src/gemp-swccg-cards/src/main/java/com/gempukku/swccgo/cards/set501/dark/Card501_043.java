package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: Stranded
 */
public class Card501_043 extends AbstractNormalEffect {
    public Card501_043() {
        super(Side.DARK, 6, PlayCardZoneOption.ATTACHED, "Stranded", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Imperial troopers use tactics to strand and cut off fugitives. Only daring and unpredictable actions gave Luke and Leia a chance to escape.");
        setGameText("Deploy on any character. Nabrun Leids is canceled. Characters here may not be Disarmed or fire weapons outside of battle. Opponent may not cancel battle destiny draws here.");
        addIcons(Icon.VIRTUAL_SET_25);
        setTestingText("Stranded");
        hideFromDeckBuilder();
    }
}
