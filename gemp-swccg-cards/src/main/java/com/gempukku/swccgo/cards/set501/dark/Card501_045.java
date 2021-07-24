package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrStartingInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.PutCardFromVoidInReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardsFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collections;
import java.util.List;

/**
 * Set: Set 16
 * Type: Interrupt
 * Subtype: Starting
 * Title: The Sith Will Rule The Galaxy
 */
public class Card501_045 extends AbstractUsedOrStartingInterrupt {
    public Card501_045() {
        super(Side.DARK, 5, "The Sith Will Rule The Galaxy");
        setGameText("USED: Take a weapon into hand from Reserve Deck; reshuffle. STARTING: If your starting location was a battleground, deploy Rule Of Two and 2 Effects that are always Immune to Alter. Place Interrupt in Reserve Deck.");
        addIcons(Icon.VIRTUAL_SET_16);
        setTestingText("The Sith Will Rule The Galaxy");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        GameTextActionId gameTextActionId = GameTextActionId.THE_SITH_RULE_THE_GALAXY__UPLOAD_WEAPON;

        // Check condition(s)
        if (GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.USED);
            action.setText("Take a weapon into hand");
            // Allow response(s)
            action.allowResponses("Take a weapon into hand from Reserve Deck",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.weapon, true));
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
        if (startingLocation != null && Filters.battleground.accepts(game, startingLocation)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.STARTING);
            action.setText("Deploy a Rule of Two and Effects from Reserve Deck");
            // Allow response(s)
            action.allowResponses("Deploy Rule of Two and up to two Effects from Reserve Deck",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.title(Title.Rule_Of_Two), true, false));
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