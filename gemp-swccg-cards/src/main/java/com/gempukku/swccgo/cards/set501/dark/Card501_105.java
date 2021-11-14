package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDroid;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 17
 * Type: Character
 * Subtype: Droid
 * Title: BB-9E
 */
public class Card501_105 extends AbstractDroid {
    public Card501_105() {
        super(Side.DARK, 0.5, 2, 2, 4, "BB-9E", Uniqueness.UNIQUE);
        setAlternateDestiny(5.5);
        setGameText("If present with your First Order character or leader, may cancel game text of BB-8 or Rose at same or related location for remainder of turn. During your move phase, may use 1 Force to 'break cover' of opponent's Undercover spy at same site.");
        addIcons(Icon.EPISODE_VII, Icon.NAV_COMPUTER, Icon.VIRTUAL_SET_17);
        addModelType(ModelType.ASTROMECH);
        setTestingText("BB-9E");
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        String opponent = game.getOpponent(playerId);
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.isPresentWith(game, self, Filters.and(Filters.your(self), Filters.or(Filters.First_Order_character, Filters.leader)))
                && GameConditions.canTarget(game, self, Filters.and(Filters.atSameOrRelatedLocation(self), Filters.or(Filters.BB8, Filters.Rose), Filters.not(Filters.isGameTextCanceled)))) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Cancel game text of BB-8 or Rose");

            // Perform result(s)
            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target BB-8 or Rose to cancel their game text", Filters.and(Filters.atSameOrRelatedLocation(self), Filters.or(Filters.BB8, Filters.Rose), Filters.not(Filters.isGameTextCanceled))) {
                @Override
                protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                    action.allowResponses(new RespondableEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);
                            action.appendEffect(new CancelGameTextUntilEndOfTurnEffect(action, finalTarget));
                        }
                    });
                }
            });
            actions.add(action);
        }


        Filter targetFilter = Filters.and(Filters.opponents(self), Filters.undercover_spy, Filters.atSameSite(self));
        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.MOVE)
                && GameConditions.canUseForce(game, playerId, 1)
                && GameConditions.canTarget(game, self, SpotOverride.INCLUDE_UNDERCOVER, targetFilter)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("'Break cover' of opponent's spy");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose undercover spy", SpotOverride.INCLUDE_UNDERCOVER, targetFilter) {
                        @Override
                        protected void cardTargeted(int targetGroupId, final PhysicalCard targetedCard) {
                            action.addAnimationGroup(targetedCard);
                            // Pay cost(s)
                            action.appendCost(
                                    new UseForceEffect(action, playerId, 1));
                            // Allow response(s)
                            action.allowResponses("'Break cover' of " + GameUtils.getCardLink(targetedCard),
                                    new UnrespondableEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Perform result(s)
                                            action.appendEffect(
                                                    new BreakCoverEffect(action, targetedCard));
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
