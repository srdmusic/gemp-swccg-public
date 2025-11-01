package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractStartingInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.LightSideGoesFirstEffect;
import com.gempukku.swccgo.logic.effects.ModifyNumCardsDrawnInStartingHandEffect;
import com.gempukku.swccgo.logic.effects.PutCardFromVoidInLostPileEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToTargetFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;

/**
 * Set: Set 26
 * Type: Interrupt
 * Subtype: Starting
 * Title: Something About This Boy
 */

public class Card501_216 extends AbstractStartingInterrupt {
    public Card501_216() {
        super(Side.LIGHT, 3, "Something About This Boy", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("'What'd he mean by that?' 'I'll tell you later.'");
        setGameText("If your starting location was City Outskirts, deploy Slave Quarters (with Prophecy Of The Force there), Jedi Business and Your Thoughts Dwell On Your Mother. Light Side goes first. When you draw your starting hand, draw only 6 cards. Place Interrupt in Lost Pile.");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_26);
        setTestingText("Something About This Boy");
    }

    @Override
    protected PlayInterruptAction getGameTextStartingAction(final String playerId, final SwccgGame game, final PhysicalCard self) {
        // Check condition(s)
        final PhysicalCard startingLocation = game.getModifiersQuerying().getStartingLocation(playerId);
        if (startingLocation != null && Filters.City_Outskirts.accepts(game, startingLocation)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Deploy Slave Quarters and other cards from Reserve Deck");
            // Allow response(s)
            action.allowResponses("Deploy Slave Quarters (with Prophecy Of The Force there), Jedi Business and Your Thoughts Dwell On Your Mother from Reserve Deck.",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.Slave_Quarters, true, false));
                            action.appendEffect(
                                    new DeployCardToTargetFromReserveDeckEffect(action, Filters.Prophecy_Of_The_Force, Filters.Slave_Quarters, true, false));
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.Jedi_Business, true, false));
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.Your_Thoughts_Dwell_On_Your_Mother, true, false));
                            action.appendEffect(
                                    new LightSideGoesFirstEffect(action));
                            action.appendEffect(
                                new ModifyNumCardsDrawnInStartingHandEffect(action, playerId, 6));
                            action.appendEffect(
                                    new PutCardFromVoidInLostPileEffect(action, playerId, self));
                        }
                    }
            );
            return action;
        }
        return null;
    }
}
