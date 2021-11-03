package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.ActivateForceEffect;

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
        setLocationDarkSideGameText("Deploys only as a starting location (or by He Is The Chosen One instead of Jedi Council Chamber).");
        setLocationLightSideGameText("Once during opponent's turn, unless Anakin on table, may activate 1 Force.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.EXTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_17);
        setTestingText("Endor: Anakin's Funeral Pyre");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self) {
        return game.getGameState().getCurrentPhase() == Phase.PLAY_STARTING_CARDS
                && ((game.getModifiersQuerying().getStartingLocation(playerId) == null
                    && game.getGameState().getObjectivePlayed(playerId) == null) //as starting location
                || (game.getGameState().getObjectivePlayed(playerId) != null
                    && Filters.He_Is_The_Chosen_One.accepts(game, game.getGameState().getObjectivePlayed(playerId))
                    && !GameConditions.canSpot(game, self, Filters.Jedi_Council_Chamber)) // this is definitely not the best way to do this
            );
    }

    // NOTE: The "May be deployed instead of Jedi Council Chamber by He Is The Chosen One." portion of the text has been
    // implemented on the HITCO objective.

    @Override
    protected List<TopLevelGameTextAction> getGameTextLightSideTopLevelActions(String playerOnLightSideOfLocation, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Once during opponent's turn, unless Anakin on table, may activate 1 Force.

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerOnLightSideOfLocation, gameTextSourceCardId, gameTextActionId)
                && GameConditions.isOpponentsTurn(game, playerOnLightSideOfLocation)
                && GameConditions.canActivateForce(game, playerOnLightSideOfLocation)
                && !GameConditions.canSpot(game, self, Filters.Anakin)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerOnLightSideOfLocation, gameTextSourceCardId, gameTextActionId);
            action.setText("Activate 1 Force");

            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));

            // Perform result(s)
            action.appendEffect(
                    new ActivateForceEffect(action, playerOnLightSideOfLocation, 1));

            actions.add(action);
        }

        return actions;
    }
}