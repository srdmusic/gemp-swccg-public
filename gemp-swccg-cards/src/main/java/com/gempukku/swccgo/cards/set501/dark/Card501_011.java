package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractStartingInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.PutCardFromVoidInLostPileEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardsFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;

/**
 * Set: Set 17
 * Type: Interrupt
 * Subtype: Lost or Starting
 * Title: That's It, The Rebels Are There! (V)
 */
public class Card501_011 extends AbstractStartingInterrupt {
    public Card501_011() {
        super(Side.DARK, 4, "That's It, The Rebels Are There!");
        setVirtualSuffix(true);
        setGameText("If you’ve deployed [Set 17] 4th marker, take [Set 6] Veers into hand from Reserve Deck. Deploy 1st marker, [Set 9] Prepare For A Surface Attack, and up to two Effects that deploy for free and are always immune to Alter. Place Interrupt in Lost Pile.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_17);
        setTestingText("That's It, The Rebels Are There! (V)");
    }

    @Override
    protected PlayInterruptAction getGameTextStartingAction(final String playerId, final SwccgGame game, final PhysicalCard self) {
        final Filter yourSiteEvenIfConverted = Filters.and(Icon.VIRTUAL_SET_17, Filters.Fourth_Marker, Filters.or(Filters.your(self), Filters.convertedLocationUnderTopLocation(Filters.your(self))));

        // Check condition(s)
        if (GameConditions.canSpotLocation(game, yourSiteEvenIfConverted)) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.STARTING);
            action.setText("Take Veers into hand and deploy 1st marker and Effects from Reserve Deck");
            // Allow response(s)
            action.allowResponses("Take Veers into hand. Deploy 1st marker, [Set 9] Prepare For A Surface Attack, and up to two Effects from Reserve Deck",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.and(Icon.VIRTUAL_SET_6, Filters.Veers), false));
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.First_Marker, true, false));
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.and(Icon.VIRTUAL_SET_9, Filters.Prepare_For_A_Surface_Attack), true, false));
                            action.appendEffect(
                                    new DeployCardsFromReserveDeckEffect(action, Filters.and(Filters.Effect, Filters.deploysForFree, Filters.always_immune_to_Alter), 1, 2, true, false));
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