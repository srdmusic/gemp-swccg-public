package com.gempukku.swccgo.cards.set501.light;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.IgnoresLocationDeploymentRestrictionsWhenDeployingToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: Site
 * Title: Coruscant: Jedi Council Chamber (V)
 */
public class Card501_164 extends AbstractSite {
    public Card501_164() {
        super(Side.LIGHT, Title.Jedi_Council_Chamber, Title.Coruscant, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("[Episode I] Vader may deploy here regardless of presence or Force icons.");
        setLocationLightSideGameText("Deploys only if you have deployed a battleground or if a Jedi 'communing.'");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.A_NEW_HOPE, Icon.INTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("Coruscant: Jedi Council Chamber (V)");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self) {
        Filter yourBattleground = Filters.and(Filters.your(self), Filters.battleground);
        Filter jediCommuning = Filters.and(Filters.Communing, Filters.hasStacked(Filters.Jedi));

        return (GameConditions.canSpotLocation(game, yourBattleground)
                || GameConditions.canSpotConvertedLocation(game, yourBattleground)
                || GameConditions.canSpot(game, self, jediCommuning));
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new IgnoresLocationDeploymentRestrictionsWhenDeployingToLocationModifier(self, Filters.and(Icon.EPISODE_I, Filters.Vader), self));
        return modifiers;
    }
}