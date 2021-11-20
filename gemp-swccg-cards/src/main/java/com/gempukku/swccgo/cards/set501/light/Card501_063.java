package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToLocationFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.List;

/**
 * Set: Set 17
 * Type: Location
 * Subtype: Site
 * Title: Tatooine: Slave Quarters (V)
 */
public class Card501_063 extends AbstractSite {
    public Card501_063() {
        super(Side.LIGHT, Title.Slave_Quarters, Title.Tatooine);
        setVirtualSuffix(true);
        setLocationDarkSideGameText("Deploys only as a starting location.");
        setLocationLightSideGameText("If you just deployed a [Skywalker] Epic Event, deploy a [Skywalker] Effect here from Reserve Deck; reshuffle.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.SKYWALKER, Icon.TATOOINE, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.EPISODE_I, Icon.VIRTUAL_SET_17);
        setTestingText("Tatooine: Slave Quarters (V)");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self) {
        // Deploys only as a starting location.
        return GameConditions.isDuringStartOfGame(game)
                && game.getModifiersQuerying().getStartingLocation(playerId) == null
                && game.getGameState().getObjectivePlayed(playerId) == null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextLightSideRequiredAfterTriggers(String playerOnLightSideOfLocation, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.TATOOINE_SLAVE_QUARTERS_V__DEPLOY_EFFECT;

        if (TriggerConditions.justDeployed(game, effectResult, playerOnLightSideOfLocation, Filters.and(Icon.SKYWALKER, Filters.Epic_Event))
                && GameConditions.canDeployCardFromReserveDeck(game, playerOnLightSideOfLocation, self, gameTextActionId, true, false)) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setPerformingPlayer(playerOnLightSideOfLocation);
            action.setText("Deploy a [Skywalker] Effect here");
            action.setActionMsg("Deploy a [Skywalker] Effect here from Reserve Deck");
            action.appendEffect(
                    new DeployCardToLocationFromReserveDeckEffect(action, Filters.and(Icon.SKYWALKER, Filters.Effect), Filters.here(self), !GameConditions.isDuringStartOfGame(game)));
            return Collections.singletonList(action);
        }
        return null;
    }
}