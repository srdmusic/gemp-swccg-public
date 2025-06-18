package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractCharacterWeapon;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;

/**
 * Set: Playtesting
 * Type: Weapon
 * Subtype: Character
 * Title: IG-88's Pulse Cannon (V)
 */

public class Card501_131 extends AbstractCharacterWeapon {
    public Card501_131() {
        super(Side.DARK, 1, "IG-88's Pulse Cannon (V)", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("IG-88's personal favorite for mass destruction. Rapid-fire fusion plasma bursts are extremely effective against multiple targets. Not widely used due to incidental damage.");
        setGameText("Deploy on your bounty hunter. May target a character. Draw destiny. If destiny +1 > defense value, target hit, its forfeit = 0 and, if IG-88 firing repeatedly, may add one destiny to power or attrition. May fire repeatedly for 1 Force each time.");
        addIcons(Icon.DAGOBAH, Icon.VIRTUAL_SET_25);
        setMatchingCharacterFilter(Filters.IG88);
        setVirtualSuffix(true);
        setTestingText("IG-88's Pulse Cannon (V)");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.your(self), Filters.bounty_hunter);
    }

    @Override
    protected Filter getGameTextValidToUseWeaponFilter(final SwccgGame game, final PhysicalCard self) {
        return Filters.bounty_hunter;
    }
}
