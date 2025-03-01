package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractCharacterWeapon;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;

/**
 * Set: Playtesting
 * Type: Weapon
 * Subtype: Character
 * Title: The Grand Inquisitor's Lightsaber
 */
public class Card501_042 extends AbstractCharacterWeapon {
    public Card501_042() {
        super(Side.DARK, 2, "The Grand Inquisitor's Lightsaber", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("Deploy on an Inquisitor. May target a character or creature for free. Draw two destiny. Target hit, and may not be used to satisfy attrition, if total destiny > defense value (forfeit = 0 if hit by Grand Inquisitor). Opponent's characters may not move from here during your turn.");
        addIcons(Icon.VIRTUAL_SET_25);
        addKeywords(Keyword.LIGHTSABER);
        setMatchingCharacterFilter(Filters.inquisitor);
        setTestingText("The Grand Inquisitor's Lightsaber");
        hideFromDeckBuilder();
    }
}
