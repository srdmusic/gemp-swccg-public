package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.LoseCardFromTableEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: Show No Mercy
 */
public class Card501_014 extends AbstractUsedOrLostInterrupt {
    public Card501_014() {
        super(Side.DARK, 4, "Show No Mercy", Uniqueness.UNIQUE, ExpansionSet.SET_20, Rarity.V);
        setGameText("USED: If Revenge Of The Sith on table, take 500 Republica, The Works, or Unlimited Power! into hand from Reserve Deck; reshuffle. LOST: Target an undercover spy with your Sidious or Neimoidian. Target is lost.");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_20);
        setTestingText("Show No Mercy");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.SHOW_NO_MERCY__UPLOAD_CORUSCANT_SITE;

        // Check condition(s)
        if (GameConditions.canSpot(game, self, Filters.Revenge_Of_The_Sith)
                && GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.USED);
            action.setText("Take a card into hand");
            action.setActionMsg("Take 500 Republica, The Works, or Unlimited Power! into hand from Reserve Deck");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.or(Filters.title("Coruscant: The Works"), Filters.title("Coruscant: 500 Republica"), Filters.title(Title.Unlimited_Power)), true));
                        }
                    }
            );
            actions.add(action);
        }

        Filter filter = Filters.and(Filters.opponents(self), Filters.undercover_spy, Filters.with(self, Filters.and(Filters.your(self), Filters.or(Filters.Sidious, Filters.Neimoidian))));
        TargetingReason targetingReason = TargetingReason.TO_BE_LOST;

        // Check condition(s)
        if (GameConditions.canTarget(game, self, SpotOverride.INCLUDE_UNDERCOVER, targetingReason, filter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Make opponent's undercover spy lost");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose opponent's undercover spy", SpotOverride.INCLUDE_UNDERCOVER, targetingReason, filter) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, final PhysicalCard opponentsSpy) {
                            action.addAnimationGroup(opponentsSpy);
                            // Allow response(s)
                            action.allowResponses("Make " + GameUtils.getCardLink(opponentsSpy) + " lost",
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Get the targeted card(s) from the action using the targetGroupId.
                                            // This needs to be done in case the target(s) were changed during the responses.
                                            final PhysicalCard finalOpponentsSpy = action.getPrimaryTargetCard(targetGroupId);

                                            // Perform result(s)
                                            action.appendEffect(
                                                    new LoseCardFromTableEffect(action, finalOpponentsSpy));
                                        }
                                    }
                            );
                        }
                    }
            );
            actions.add(action);
        }
        return actions;
    }
}