package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToLocationFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.List;

/**
 * Set: Set 17
 * Type: Location
 * Subtype: Site
 * Title: Ajan Kloss: Training Course
 */
public class Card501_062 extends AbstractSite {
    public Card501_062() {
        super(Side.LIGHT, "Ajan Kloss: Training Course", Title.Ajan_Kloss);
        setLocationDarkSideGameText("Deploys only as a starting location.");
        setLocationLightSideGameText("If you just deployed a [Skywalker] Epic Event, deploy My Parents Were Strong from Reserve Deck; reshuffle.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.SKYWALKER, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.EPISODE_VII, Icon.VIRTUAL_SET_17);
        setTestingText("Ajan Kloss: Training Course");
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
        GameTextActionId gameTextActionId = GameTextActionId.AJAN_KLOSS_TRAINING_GROUND__DEPLOY_EFFECT;

        if (TriggerConditions.justDeployed(game, effectResult, playerOnLightSideOfLocation, Filters.and(Icon.SKYWALKER, Filters.Epic_Event))
                && GameConditions.canDeployCardFromReserveDeck(game, playerOnLightSideOfLocation, self, gameTextActionId, true, false)) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setPerformingPlayer(playerOnLightSideOfLocation);
            action.setText("Deploy My Parents Were Strong");
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.title("My Parents Were Strong"), GameConditions.isDuringStartOfGame(game), !GameConditions.isDuringStartOfGame(game)));
            return Collections.singletonList(action);
        }
        return null;
    }
}