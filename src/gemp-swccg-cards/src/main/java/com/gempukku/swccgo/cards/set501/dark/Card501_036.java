package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToLocationFromReserveDeckEffect;

import java.util.Collections;
import java.util.List;

/*
 * Set: Playtesting
 * Type: Location
 * Subtype: Site
 * Title: Scarif: Ocean Shallows
 */

public class Card501_036 extends AbstractSite {
    public Card501_036() {
        super(Side.DARK, Title.OceanShallows, Title.Scarif, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("Once per game, may download An Inkling Of Its Destructive Potential here.");
        setLocationLightSideGameText("");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.PLANET, Icon.EXTERIOR_SITE, Icon.VIRTUAL_SET_24);
        setTestingText(Title.OceanShallows);
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextDarkSideTopLevelActions(final String playerOnDarkSideOfLocation, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.SCARIF_OCEAN__DOWNLOAD_INKLING;

        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerOnDarkSideOfLocation, self, gameTextActionId)
                && !GameConditions.canSpot(game, self, Filters.An_Inkling_Of_Its_Destructive_Potential)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerOnDarkSideOfLocation, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy An Inkling Of Its Destructive Potential from Reserve Deck");
            action.setActionMsg("Deploy An Inkling Of Its Destructive Potential from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardToLocationFromReserveDeckEffect(action, Filters.An_Inkling_Of_Its_Destructive_Potential, Filters.here(self), true));
            return Collections.singletonList(action);
        }
        return null;
    }


}
