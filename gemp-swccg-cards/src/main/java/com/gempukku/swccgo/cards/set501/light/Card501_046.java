package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractLostOrStartingInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.LookAtForcePileEffect;
import com.gempukku.swccgo.logic.effects.PutCardFromVoidInReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collections;
import java.util.List;

/**
 * Set: Set 17
 * Type: Interrupt
 * Subtype: Lost or Starting
 * Title: The Rise Of Skywalker
 */
public class Card501_046 extends AbstractLostOrStartingInterrupt {
    public Card501_046() {
        super(Side.LIGHT, 5, "The Rise Of Skywalker", Uniqueness.UNIQUE);
        setGameText("LOST: Peek at Force Pile. " +
                "STARTING: If Shmi's Hut, Anakin's Funeral Pyre, or Rey's Encampment on table, " +
                "deploy The Force Is Strong In My Family, Battle Plan, Insurrection, and one Effect that is always immune to Alter. Place Interrupt in Reserve Deck.");
        addIcons(Icon.VIRTUAL_SET_17);
        setTestingText("The Rise Of Skywalker");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        // Check condition(s)
        if (GameConditions.hasForcePile(game, playerId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText("Peek at Force Pile");
            // Allow response(s)
            action.allowResponses("Peek at Force Pile",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new LookAtForcePileEffect(action, playerId, playerId));
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

        if (GameConditions.canSpot(game, self, Filters.or(Filters.title(Title.Slave_Quarters), Filters.title(Title.Anakins_Funeral_Pyre), Filters.title(Title.Reys_Encampment)))) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.STARTING);
            action.setText("Deploy The Force is Strong In My Family and Effects from Reserve Deck");
            // Allow response(s)
            action.allowResponses("Deploy The Force Is Strong In My Family, Battle Plan, Insurrection, and one Effect that is always immune to Alter from Reserve Deck",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.title(Title.The_Force_Is_Strong_In_My_Family), true, false));
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.title(Title.Battle_Plan), true, false));
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.title(Title.Insurrection), true, false));
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.and(Filters.Effect, Filters.always_immune_to_Alter), true, false));
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