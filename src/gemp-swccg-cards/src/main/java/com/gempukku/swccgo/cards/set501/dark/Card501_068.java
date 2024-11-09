package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSystem;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: System
 * Title: Ord Mantell (V)
 */
public class Card501_068 extends AbstractSystem {
    public Card501_068() {
        super(Side.DARK, Title.Ord_Mantell, 7, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("Your Bounty Hunters are deploy -2 here.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.HOTH, Icon.PLANET, Icon.VIRTUAL_SET_24);
        setVirtualSuffix(true);
        setTestingText("Ord Mantell (DS) (ERRATA)");
        hideFromDeckBuilder();
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.bounty_hunter), -2, self));
        return modifiers;
    }
}