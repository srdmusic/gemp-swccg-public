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
import com.gempukku.swccgo.logic.effects.CaptureCharacterOnTableEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesing
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: There They Are! (V)
 */
public class Card501_048 extends AbstractUsedOrLostInterrupt {
    public Card501_048() {
        super(Side.DARK, 5, Title.There_They_Are, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("When unexplained droid losses are detected, a Droid Control Ship can call in reinforcements in seconds.");
        setGameText("USED: Take a non-unique battle droid, non-unique droid starfighter, or Battle Droid Reinforcements into hand from Reserve Deck; reshuffle." +
                "LOST: Break cover of and capture opponent's Undercover spy at same site as your battle droid. [Immune to Sense]");
        addIcons(Icon.THEED_PALACE, Icon.EPISODE_I, Icon.VIRTUAL_SET_25);
        setTestingText("There They Are! (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.THERE_THEY_ARE__UPLOAD_NON_UNIQUE_BATTLE_DROID_OR_DROID_STARFIGHTER;

        // Check condition(s)
        if (GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.USED);
            action.setText("Take card into hand from Reserve Deck");
            // Allow response(s)
            action.allowResponses("Take a non-unique battle droid, non-unique droid starfighter, or Battle Droid Reinforcements into hand from Reserve Deck",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.or(Filters.and(Filters.non_unique, Filters.or(Filters.battle_droid, Filters.droid_starfighter)), Filters.title("Battle Droid Reinforcements")), true));
                        }
                    }
            );
            actions.add(action);
        }


        Filter filter = Filters.and(Filters.opponents(self), Filters.undercover_spy, Filters.at(Filters.and(Filters.site, Filters.sameLocationAs(self, Filters.and(Filters.your(self), Filters.battle_droid)))));
        TargetingReason targetingReason = TargetingReason.TO_BE_CAPTURED;

        // Check condition(s)
        if (GameConditions.canTarget(game, self, SpotOverride.INCLUDE_UNDERCOVER, targetingReason, filter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Capture undercover spy");
            action.setImmuneTo(Title.Sense);

            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose undercover spy", SpotOverride.INCLUDE_UNDERCOVER, targetingReason, filter) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                            action.addAnimationGroup(targetedCard);
                            // Allow response(s)
                            action.allowResponses("Capture " + GameUtils.getCardLink(targetedCard),
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Get the targeted card(s) from the action using the targetGroupId.
                                            // This needs to be done in case the target(s) were changed during the responses.
                                            PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);

                                            // Perform result(s)
                                            action.appendEffect(
                                                    new CaptureCharacterOnTableEffect(action, finalTarget));
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