package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractCharacterWeapon;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Weapon
 * Subtype: Character
 * Title: IG-88's Pulse Cannon (V)
 */

public class Card501_131 extends AbstractCharacterWeapon {
    public Card501_131() {
        super(Side.DARK, 1, "IG-88's Pulse Cannon (V)", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setTestingText("IG-88's Pulse Cannon (V)");
        hideFromDeckBuilder();
    }    
}
