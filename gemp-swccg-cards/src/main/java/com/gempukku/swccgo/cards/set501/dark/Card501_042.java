package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddBattleDestinyEffect;
import com.gempukku.swccgo.cards.effects.SatisfyAllBattleDamageAndAttritionEffect;
import com.gempukku.swccgo.cards.effects.choose.ChooseAndLoseCardFromHandEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.ArtworkCardRevealedResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 18
 * Type: Interrupt
 * Subtype: Lost
 * Title: Not Within Sight Or Reach
 */
public class Card501_042 extends AbstractLostInterrupt {
    public Card501_042() {
        super(Side.DARK, 3, "Not Within Sight Or Reach", Uniqueness.UNIQUE);
        setGameText("If Thrawn and Vanto are participating in a battle, place Vanto in Used Pile to cancel all battle damage and attrition against you. OR Take your [Grabber] card on table into hand; place all cards stacked on it in owner's Used Pile.");
        addIcons(Icon.VIRTUAL_SET_18);
        setTestingText("Not Within Sight Or Reach");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        // If Thrawn and Vanto are participating in a battle, place Vanto in Used Pile to cancel all battle damage and attrition against you.

        Filter vantoFilter = Filters.and(Filters.title("Ensign Eli Vanto"), Filters.participatingInBattle, Filters.canBeTargetedBy(self));
        // Check condition(s)
        if (TriggerConditions.isResolvingBattleDamageAndAttrition(game, effectResult, playerId)
                && (GameConditions.isBattleDamageRemaining(game, playerId) || GameConditions.isAttritionRemaining(game, playerId))
                && GameConditions.isDuringBattleWithParticipant(game, Filters.Thrawn)
                && GameConditions.isDuringBattleWithParticipant(game, vantoFilter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Cancel all remaining battle damage and attrition");

            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Choose Vanto to place in Used Pile", vantoFilter) {
                @Override
                protected void cardTargeted(int targetGroupId, PhysicalCard targetedCard) {
                    action.appendCost(
                            new PlaceCardInUsedPileFromTableEffect(action, targetedCard));
                    // Allow response(s)
                    action.allowResponses("Cancel all remaining battle damage and attrition",
                            new RespondablePlayCardEffect(action) {
                                @Override
                                protected void performActionResults(Action targetingAction) {
                                    // Perform result(s)
                                    action.appendEffect(
                                            new SatisfyAllBattleDamageAndAttritionEffect(action, playerId));
                                }
                            }
                    );
                }
            });

            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        // Take your [Grabber] card on table into hand, place all cards stacked upon it in owner’s Used Pile.
        if (GameConditions.canTarget(game, self, Filters.and(Filters.your(self), Icon.GRABBER))) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Return your grabber to hand");

            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target your [Grabber] card to take into hand", Filters.and(Filters.your(self), Icon.GRABBER)) {
                @Override
                protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                    // Allow response(s)
                    action.allowResponses("Take "+GameUtils.getCardLink(targetedCard)+" to hand and put cards on it in owner's Used Pile",
                            new RespondablePlayCardEffect(action) {
                                @Override
                                protected void performActionResults(Action targetingAction) {
                                    PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);
                                    // Perform result(s)
                                    action.appendEffect(
                                            new ReturnCardToHandFromTableEffect(action, finalTarget, Zone.USED_PILE));
                                }
                            }
                    );
                }
            });

            return Collections.singletonList(action);
        }
        return null;
    }
}