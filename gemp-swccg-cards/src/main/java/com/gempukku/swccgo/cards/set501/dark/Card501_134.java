package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractVehicleWeapon;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Weapon
 * Subtype: Vehicle
 * Title: Electro-Rangefinder (V)
 */

public class Card501_134 extends AbstractVehicleWeapon {
    public Card501_134() {
        super(Side.DARK, 6, "Electro-Rangefinder (V)", Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setTestingText("Electro-Rangefinder (V)");
        hideFromDeckBuilder();
    }
}
