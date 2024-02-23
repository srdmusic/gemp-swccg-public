package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractCharacterWeapon;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Weapon
 * Subtype: Character
 * Title: Amban Sniper Rifle
 */
public class Card501_168 extends AbstractCharacterWeapon {
    public Card501_168() {
        super(Side.LIGHT, 4, "Amban Sniper Rifle", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        hideFromDeckBuilder();
    }
}
