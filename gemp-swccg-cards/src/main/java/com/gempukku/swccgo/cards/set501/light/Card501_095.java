package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.ActivateForceEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Location
 * Subtype: Site
 * Title: Endor: Anakin's Funeral Pyre
 */
public class Card501_095 extends AbstractSite {
    public Card501_095() {
        super(Side.LIGHT, Title.Anakins_Funeral_Pyre, Title.Endor);
        setLocationDarkSideGameText("Deploys only as a starting location.");
        setLocationLightSideGameText("If you just deployed a [Skywalker] Epic Event, deploy Like My Father Before Me from Reserve Deck; reshuffle.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.SKYWALKER, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_17);
        setTestingText("Endor: Anakin's Funeral Pyre");
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
        GameTextActionId gameTextActionId = GameTextActionId.ANAKINS_FUNERAL_PYRE__DEPLOY_EFFECT;

        if (TriggerConditions.justDeployed(game, effectResult, playerOnLightSideOfLocation, Filters.and(Icon.SKYWALKER, Filters.Epic_Event))
                && GameConditions.canDeployCardFromReserveDeck(game, playerOnLightSideOfLocation, self, gameTextActionId, true, false)) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setPerformingPlayer(playerOnLightSideOfLocation);
            action.setText("Deploy Like My Father Before Me");
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.title(Title.Like_My_Father_Before_Me), GameConditions.isDuringStartOfGame(game), !GameConditions.isDuringStartOfGame(game)));
            return Collections.singletonList(action);
        }
        return null;
    }
}