package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrStartingInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToLocationFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.ModifyManeuverUntilEndOfTurnEffect;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collections;
import java.util.List;


/**
 * Set: Set 19
 * Type: Interrupt
 * Subtype: Starting
 * Title: More Powerful Than Either Of Us
 */
public class Card501_046 extends AbstractUsedOrStartingInterrupt {
    public Card501_046() {
        super(Side.DARK, 5, "More Powerful Than Either Of Us", Uniqueness.UNIQUE);
        setGameText("USED: If Vader alone (or with Gunray), he is power +1 for remainder of turn. STARTING: Deploy [Episode I] Vader to Insidious Prisoner's site. Deploy Battle Order, Evil Is Everywhere, and Unlimited Power!. When drawing your starting hand, draw only 5 cards. Place Interrupt in Reserve Deck.");
        addIcons(Icon.VIRTUAL_SET_19);
        setTestingText("More Powerful Than Either Of Us");
    }

    @Override
    protected PlayInterruptAction getGameTextStartingAction(final String playerId, SwccgGame game, final PhysicalCard self) {
        final Filter sameSiteAsInsidiousPrisoner = Filters.and(Filters.site, Filters.hasAttached(Filters.Insidious_Prisoner));

        // Check condition(s)
        if (GameConditions.canSpotLocation(game, sameSiteAsInsidiousPrisoner)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.STARTING);
            action.setText("Deploy Vader and Effects from Reserve Deck");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new DeployCardToLocationFromReserveDeckEffect(action, Filters.and(Icon.EPISODE_I, Filters.Vader), sameSiteAsInsidiousPrisoner, true, false));
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.title(Title.Battle_Order), true, false));
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.title(Title.Evil_Is_Everywhere), true, false));
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.title(Title.Unlimited_Power), true, false));
                            action.appendEffect(
                                    new ModifyNumCardsDrawnInStartingHandEffect(action, playerId, 5));
                            action.appendEffect(
                                    new PutCardFromVoidInReserveDeckEffect(action, playerId, self));

                        }
                    }
            );
            return action;
        }
        return null;
    }


    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        Filter filter = Filters.and(Filters.Vader, Filters.or(Filters.alone, Filters.with(self, Filters.Gunray)));

        // Check condition(s)
        if (GameConditions.canTarget(game, self, filter)) {

            // Generate action using common method
            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText("Make Vader more powerful");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Target Vader", filter) {
                        @Override
                        protected boolean getUseShortcut() {
                            return true;
                        }

                        @Override
                        protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                            action.addAnimationGroup(targetedCard);
                            // Allow response(s)
                            action.allowResponses("Add 1 to power of " + GameUtils.getCardLink(targetedCard),
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Get the final targeted card(s)
                                            final PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);
                                            // Perform result(s)
                                            action.appendEffect(
                                                    new ModifyPowerUntilEndOfTurnEffect(action, finalTarget, 1));
                                        }
                                    }
                            );
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }
}