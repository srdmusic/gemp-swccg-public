package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.evaluators.HereEvaluator;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Location
 * Subtype: Site
 * Title: Scarif: Data Vault Control Room
 */
public class Card501_057 extends AbstractSite {
    public Card501_057() {
        super(Side.LIGHT, "Scarif: Data Vault Control Room", Title.Scarif);
        setLocationLightSideGameText("K-2SO deploys -2 here and is power +1 here for each opponent's character here.");
        setLocationDarkSideGameText("Your characters are defense value -1 here. May not be separated from Data Vault.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcon(Icon.DARK_FORCE, 2);
        addIcons(Icon.INTERIOR_SITE, Icon.PLANET, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_18);
        setTestingText("Scarif: Data Vault Control Room");
    }

    @Override
    public List<Modifier> getAlwaysOnModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeploysAdjacentToLocationModifier(self, self, Filters.DataVault, true));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DeployCostToLocationModifier(self, Filters.K2SO, -2, self));
        modifiers.add(new PowerModifier(self, Filters.and(Filters.K2SO, Filters.here(self)), new HereEvaluator(self, Filters.and(Filters.opponents(playerOnLightSideOfLocation), Filters.character))));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DefenseValueModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.character, Filters.here(self)), -1));

        modifiers.add(new DeploysAdjacentToLocationModifier(self, Filters.DataVault, self, true));
        modifiers.add(new MayNotDeploySitesBetweenSitesModifier(self, self, Filters.DataVault));
        return modifiers;
    }
}
