package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImmediateEffect;
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
 * Subtype: Immediate
 * Title: The Client's Bounty
 */
public class Card501_037 extends AbstractImmediateEffect {
    public Card501_037() {
        super(Side.DARK, 4, PlayCardZoneOption.ATTACHED, "The Client's Bounty", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("Deploy on opponent's just deployed character. If that character captured, may take this card into hand; retrieve 2 Force (3 if The Client on table). If a bounty hunter here, once per turn, may reveal top card of every Reserve Deck. (Immune to Control.)");
        addIcons(Icon.VIRTUAL_SET_25);
        addImmuneToCardTitle(Title.Control);
        setTestingText("The Client's Bounty");
        hideFromDeckBuilder();
    }
}
