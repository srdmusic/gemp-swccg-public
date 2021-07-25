package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractCharacterWeapon;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;

/**
 * Set: Set 16
 * Type: Weapon
 * Subtype: Character
 * Title: Bowcaster (V)
 */
public class Card501_080 extends AbstractCharacterWeapon {
    public Card501_080() {
        super(Side.LIGHT, 2, "Bowcaster");
        setLore("Hand-crafted weapon of choice among Wookiees. Fires explosive 'quarrels' (which look like blaster bolts). Requires great strength to use. Extra ammo carried on bandoleers.");
        setGameText("Deploy on your Wookiee. Adds 1 to power of a non-unique Wookiee. May target a character or vehicle for free. Draw destiny. Target hit and power -3 if destiny + X > defense value, where X = number of Kashyyyk locations you control.");
        addIcons(Icon.A_NEW_HOPE, Icon.VIRTUAL_SET_16);
        addKeyword(Keyword.BOWCASTER);
        setVirtualSuffix(true);
        setTestingText("Bowcaster (V)");
        hideFromDeckBuilder();
    }


    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.your(self), Filters.Wookiee);
    }

    @Override
    protected Filter getGameTextValidToUseWeaponFilter(final SwccgGame game, final PhysicalCard self) {
        return Filters.Wookiee;
    }
}
