package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractLostOrStartingInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.ConvertLocationByRaisingToTopEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.PutCardFromVoidInReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardsFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collections;
import java.util.List;

/**
 * Set: Set 16
 * Type: Interrupt
 * Subtype: Lost or Starting
 * Title: Rise Of The Sith
 */
public class Card501_045 extends AbstractLostOrStartingInterrupt {
    public Card501_045() {
        super(Side.DARK, 5, "Rise Of The Sith", Uniqueness.RESTRICTED_2);
        setGameText("LOST: Raise your converted location to the top. " +
                "STARTING: If your starting location was a battleground, deploy Rule Of Two and 2 Effects that deploy on table and are always Immune to Alter. Place Interrupt in hand.");
        addIcons(Icon.VIRTUAL_SET_16);
        setTestingText("Rise Of The Sith");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        Filter yourConvertedLocationFilter = Filters.and(Filters.location, Filters.canBeConvertedByRaisingYourLocationToTop(playerId));

        // Check condition(s)
        if (GameConditions.canTarget(game, self, yourConvertedLocationFilter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Raise a converted location");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose location to convert", yourConvertedLocationFilter) {
                        @Override
                        protected void cardTargeted(int targetGroupId, final PhysicalCard targetedCard) {
                            action.addAnimationGroup(targetedCard);
                            // Allow response(s)
                            action.allowResponses("Convert " + GameUtils.getCardLink(targetedCard),
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Perform result(s)
                                            action.appendEffect(
                                                    new ConvertLocationByRaisingToTopEffect(action, targetedCard, true));
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

    @Override
    protected PlayInterruptAction getGameTextStartingAction(final String playerId, final SwccgGame game, final PhysicalCard self) {
        // Check condition(s)
        final PhysicalCard startingLocation = game.getModifiersQuerying().getStartingLocation(playerId);
        if (startingLocation != null && Filters.battleground.accepts(game, startingLocation)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.STARTING);
            action.setText("Deploy a Rule of Two and Effects from Reserve Deck");
            // Allow response(s)
            action.allowResponses("Deploy Rule of Two and two Effects from Reserve Deck",
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