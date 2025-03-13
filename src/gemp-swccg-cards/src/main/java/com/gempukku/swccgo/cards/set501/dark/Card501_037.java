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
        setLore("One of the most profitable occupations in the galaxy is hunting down and capturing wanted beings. The more notable the quarry, the more profitable the venture.");
        setGameText("Deploy on opponent's just deployed character. Once per turn, if a bounty hunter here, may reveal the top card of each player's Reserve Deck. If this character captured and seized, retrieve 2 Force (3 if The Client on table) and return this card to your hand. [Immune to Control.]");
        addIcons(Icon.VIRTUAL_SET_25);
        addImmuneToCardTitle(Title.Control);
        setTestingText("The Client's Bounty");
    }
}
