package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSith;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Sith
 * Title: Baylan Skoll
 */
public class Card501_051 extends AbstractSith {
    public Card501_051() {
        super(Side.DARK, 1, 6, 6, 6, 7, "Baylan Skoll", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Mercenary");
        setGameText("Once per game, may [DOWNLOAD] a card with 'mercenary' in lore (or a lightsaber on Baylan). Unless Sidious on table, if in battle alone (or with Shin), opponent must have 2 characters (or Anakin or Yoda) in battle to draw battle destiny here. Immune to attrition < 5.");
        addIcons(Icon.WARRIOR, Icon.VIRTUAL_SET_25);
        addKeyword(Keyword.MERCENARY);
        setTestingText("Baylan Skoll");
        hideFromDeckBuilder();
    }
}
