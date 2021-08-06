package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrStartingInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.ActivateForceEffect;
import com.gempukku.swccgo.logic.effects.PutCardFromVoidInLostPileEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardsFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.StackCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.PassthruEffect;

import java.util.Collections;
import java.util.List;


/**
 * Set: Set 16
 * Type: Interrupt
 * Subtype: Used Or Starting
 * Title: I Am Part Of The Living Force
 */
public class Card501_038 extends AbstractUsedOrStartingInterrupt {
    public Card501_038() {
        super(Side.LIGHT, 5, "I Am Part Of The Living Force", Uniqueness.UNIQUE);
        setGameText("USED: Activate 1 Force. STARTING: If your starting location had exactly 2 [Light Side], deploy Communing and stack a Jedi on it from Reserve Deck. Deploy up to three Effects that deploy on table and are always immune to Alter. Place Interrupt in Lost Pile.");
        addIcons(Icon.VIRTUAL_SET_16, Icon.EPISODE_I);
        setTestingText("I Am Part Of The Living Force");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        // Check condition(s)
        if (GameConditions.canActivateForce(game, playerId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText("Activate 1 Force");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new ActivateForceEffect(action, playerId, 1));
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected PlayInterruptAction getGameTextStartingAction(final String playerId, final SwccgGame game, final PhysicalCard self) {
        final PhysicalCard startingLocation = game.getModifiersQuerying().getStartingLocation(playerId);
        if (startingLocation != null && Filters.iconCount(Icon.LIGHT_FORCE, 2).accepts(game, startingLocation)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.STARTING);
            action.setText("Deploy Communing and up to three Effects from Reserve Deck");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.Communing, true, false)
                            );

                            action.appendEffect(
                                    new PassthruEffect(action) {
                                        @Override
                                        protected void doPlayEffect(SwccgGame game) {
                                            PhysicalCard communing = Filters.findFirstActive(game, null, Filters.Communing);
                                            if (communing != null) {
                                                action.appendEffect(
                                                        new StackCardFromReserveDeckEffect(action, communing, Filters.Jedi, false)
                                                );
                                                action.appendEffect(
                                                        new DeployCardsFromReserveDeckEffect(action, Filters.and(Filters.Effect, Filters.always_immune_to_Alter,
                                                                Filters.or(Filters.gameTextContains("deploy on table"), Filters.gameTextContains("deploy on your side of table"))), 1, 3, true, false));
                                            }

                                            action.appendEffect(
                                                    new PutCardFromVoidInLostPileEffect(action, playerId, self));
                                        }
                                    }
                            );
                        }
                    }
            );
            return action;
        }
        return null;
    }
}