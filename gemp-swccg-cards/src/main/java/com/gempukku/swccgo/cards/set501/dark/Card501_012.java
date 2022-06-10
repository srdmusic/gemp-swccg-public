package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Effect
 * Title: You May Start Your Landing (V)
 */
public class Card501_012 extends AbstractNormalEffect {
    public Card501_012() {
        super(Side.DARK, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.You_May_Start_Your_Landing, Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Echo Base was no match for the Imperial war machine.");
        setGameText("If The Shield Will Be Down In Moments on table, deploy on table. Once per turn, may deploy a Hoth site from Reserve Deck; reshuffle. During your control phase, opponent loses 1 Force for each marker site you control with a piloted AT-AT. [Immune to Alter.]");
        addIcons(Icon.TATOOINE, Icon.HOTH, Icon.VIRTUAL_SET_19);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("You May Start Your Landing (V)");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.The_Shield_Will_Be_Down_In_Moments);
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        String opponent = game.getOpponent(playerId);
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)
                && GameConditions.controlsWith(game, self, playerId, Filters.marker_site, Filters.and(Filters.piloted, Filters.AT_AT))) {
            
            int numForce = Filters.countTopLocationsOnTable(game, Filters.and(Filters.marker_site, Filters.controlsWith(playerId, self, Filters.and(Filters.piloted, Filters.AT_AT))));
            if (numForce > 0) {
                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Make opponent lose " + numForce + " Force");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerPhaseEffect(action));
                // Perform result(s)
                action.appendEffect(
                        new LoseForceEffect(action, opponent, numForce));
                actions.add(action);
            }
        }

        gameTextActionId = GameTextActionId.YOU_MAY_START_YOUR_LANDING_V__DOWNLOAD_CARD;
        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy a Hoth site from Reserve Deck");
            action.setActionMsg("Deploy a Hoth site from Reserve Deck");

            action.appendUsage(
                    new OncePerTurnEffect(action));
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.Hoth_site, true));

            actions.add(action);
        }
        return actions;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        // Check if reached end of each control phase and action was not performed yet.
        if (TriggerConditions.isEndOfYourPhase(game, effectResult, Phase.CONTROL, playerId)
                && GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)
                && GameConditions.controlsWith(game, self, playerId, Filters.marker_site, Filters.and(Filters.piloted, Filters.AT_AT))) {

            int numForce = Filters.countTopLocationsOnTable(game, Filters.and(Filters.marker_site, Filters.controlsWith(playerId, self, Filters.and(Filters.piloted, Filters.AT_AT))));

            if (numForce > 0) {
                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Make opponent lose " + numForce + " Force");
                // Perform result(s)
                action.appendEffect(
                        new LoseForceEffect(action, opponent, numForce));
                actions.add(action);
            }
        }
        return actions;
    }
}