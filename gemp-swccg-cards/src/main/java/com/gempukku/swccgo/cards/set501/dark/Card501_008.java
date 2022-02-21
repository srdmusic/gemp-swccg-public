package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.UseForceEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 18
 * Type: Interrupt
 * Subtype: Used Or Starting
 * Title: Neimoidian Advisor (V)
 */
public class Card501_008 extends AbstractUsedInterrupt {
    public Card501_008() {
        super(Side.DARK, 5, "Neimoidian Advisor", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("'We must move quickly to disrupt all communication down there.'");
        setGameText("Use 3 Force to take a Neimoidian or an Effect of any kind into hand from Reserve Deck; reshuffle. OR Deploy Blockade Flagship: Bridge from Reserve Deck; reshuffle.");
        addIcons(Icon.CORUSCANT, Icon.EPISODE_I, Icon.VIRTUAL_SET_18);
        setTestingText("Neimoidian Advisor (V)");
    }


    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.NEIMOIDIAN_ADVISOR_V__UPLOAD_CARD;

        // Check condition(s)
        if (GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)
                && GameConditions.canUseForceToPlayInterrupt(game, playerId, self, 3)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);
            action.setText("Take a Neimoidian or Effect into hand");
            // Pay cost(s)
            action.appendCost(
                    new UseForceEffect(action, playerId, 3));
            // Allow response(s)
            action.allowResponses("Take a Neimoidian or an Effect of any kind into hand from Reserve Deck",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.or(Filters.Neimoidian, Filters.Effect_of_any_Kind), true));
                        }
                    }
            );
            actions.add(action);
        }

        gameTextActionId = GameTextActionId.NEIMOIDIAN_ADVISOR_V__DOWNLOAD_BRIDGE;

        // Check condition(s)
        if (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Title.BlockadeFlagshipBridge)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);
            action.setText("Deploy Blockade Flagship: Bridge");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.BlockadeFlagshipBridge, true));
                        }
                    }
            );
            actions.add(action);
        }

        return actions;
    }
}