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
 * Set: Set 18
 * Type: Interrupt
 * Subtype: Lost or Starting
 * Title: That's It, The Rebels Are There! (V)
 */
public class Card501_011 extends AbstractStartingInterrupt {
    public Card501_011() {
        super(Side.DARK, 4, "That's It, The Rebels Are There!");
        setVirtualSuffix(true);
        setGameText("If 1st Marker on table, take [Set 6] Veers into hand and deploy Make Ready To Land Our Troops and up to two Effects that deploy for free and are always [Immune to Alter]. Place Interrupt in Lost Pile.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_18);
        setTestingText("[Set 19] That's It, The Rebels Are There! (V)");
    }

    @Override
    protected PlayInterruptAction getGameTextStartingAction(final String playerId, final SwccgGame game, final PhysicalCard self) {

        // Check condition(s)
        if (GameConditions.canSpotLocation(game, Filters.First_Marker)) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.STARTING);
            action.setText("Take Veers into hand and deploy Effects");
            // Allow response(s)
            action.allowResponses("Take [Set 6] Veers into hand and deploy Make Ready To Land Our Troops and up to two Effects that deploy for free and are always [Immune to Alter]",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.and(Icon.VIRTUAL_SET_6, Filters.Veers), false));
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.Make_Ready_To_Land_Our_Troops, true, false));
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