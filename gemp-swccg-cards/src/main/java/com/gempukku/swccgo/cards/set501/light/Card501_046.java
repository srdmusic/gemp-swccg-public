package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrStartingInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.ActivateForceEffect;
import com.gempukku.swccgo.logic.effects.PutCardFromVoidInReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardsFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collections;
import java.util.List;

/**
 * Set: Set 16
 * Type: Interrupt
 * Subtype: Starting
 * Title: What Gives A Jedi His Power
 */
public class Card501_046 extends AbstractUsedOrStartingInterrupt {
    public Card501_046() {
        super(Side.LIGHT, 5, "What Gives A Jedi His Power", Uniqueness.UNIQUE);
        setGameText("USED: If a Jedi on table, activate 1 Force. " +
                "STARTING: If your starting location was Lars' Homestead, deploy a site, " +
                "The Force Is Strong In My Family, and up to two Effects that are always immune to Alter. " +
                "Place Interrupt in Reserve Deck.");
        addIcons(Icon.VIRTUAL_SET_16);
        setTestingText("[Set 17] What Gives A Jedi His Power");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        // Check condition(s)
        if (GameConditions.canActivateForce(game, playerId)
                && GameConditions.canSpot(game, self, Filters.Jedi)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText(" Activate 1 Force");
            // Allow response(s)
            action.allowResponses(" Activate 1 Force",
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
        // Check condition(s)
        final PhysicalCard startingLocation = game.getModifiersQuerying().getStartingLocation(playerId);
        if (startingLocation != null && Filters.title(Title.Lars_Homestead).accepts(game, startingLocation)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.STARTING);
            action.setText("Deploy a site, The Force is Strong In My Family, and up to two Effects from Reserve Deck");
            // Allow response(s)
            action.allowResponses("Deploy a site, The Force is Strong In My Family, and up to two Effects from Reserve Deck",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.site, true, false));
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.title(Title.The_Force_Is_Strong_In_My_Family), true, false));
                            action.appendEffect(
                                    new DeployCardsFromReserveDeckEffect(action, Filters.and(Filters.Effect, Filters.always_immune_to_Alter), 1, 2, true, false));
                            action.appendEffect(
                                    new PutCardFromVoidInReserveDeckEffect(action, playerId, self));
                        }
                    }
            );
            return action;
        }

        return null;
    }
}