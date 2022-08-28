package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Location
 * Subtype: Site
 * Title: Clone Command Center
 */
public class Card501_067 extends AbstractSite {
    public Card501_067() {
        super(Side.LIGHT, "Clone Command Center", Uniqueness.DIAMOND_1);
        setLocationDarkSideGameText("Deploys only if a [Clone Army] objective on table. Deploys only at start of game.");
        setLocationLightSideGameText("Once per turn, may deploy related system (or a [Clone Army] battleground) from Reserve Deck; reshuffle.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.EXTERIOR_SITE, Icon.PLANET, Icon.EPISODE_I, Icon.SCOMP_LINK, Icon.CLONE_ARMY, Icon.VIRTUAL_SET_20);
        setTestingText("Clone Command Center");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self) {
        return GameConditions.canSpot(game, self, Filters.and(Icon.CLONE_ARMY, Filters.Objective))
                && GameConditions.isDuringStartOfGame(game);
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextLightSideTopLevelActions(final String playerOnLightSideOfLocation, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.CLONE_COMMAND_CENTER__DEPLOY_LOCATION;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerOnLightSideOfLocation, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerOnLightSideOfLocation, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerOnLightSideOfLocation, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy a location");
            action.setActionMsg("Deploy related system (or a [Clone Army] battleground) from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));

            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.relatedSystem(self), Filters.and(Icon.CLONE_ARMY, Filters.location)), true));
            return Collections.singletonList(action);
        }
        return null;
    }
}