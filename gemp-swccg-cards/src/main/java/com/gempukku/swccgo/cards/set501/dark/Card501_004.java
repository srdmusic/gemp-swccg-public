package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrStartingInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.MoveAsReactEffect;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.PutCardFromVoidInLostPileEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardsFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Lost or Starting
 * Title: That's It, The Rebels Are There! (V)
 */
public class Card501_004 extends AbstractUsedOrStartingInterrupt {
    public Card501_004() {
        super(Side.DARK, 4, "That's It, The Rebels Are There!", Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setGameText("USED: Move your AT-AT as a 'react.' STARTING: If 1st Marker on table, take [Set 6] Veers into hand. Deploy [Set 9] Prepare For A Surface Attack and up to two Effects that deploy for free and are always [Immune to Alter]. Place Interrupt in Lost Pile.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_22);
        setTestingText("That's It, The Rebels Are There! (V)");
    }

    @Override
    protected PlayInterruptAction getGameTextStartingAction(final String playerId, final SwccgGame game, final PhysicalCard self) {

        // Check condition(s)
        if (GameConditions.canSpotLocation(game, Filters.First_Marker)) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.STARTING);
            action.setText("Take Veers into hand and deploy Effects");
            // Allow response(s)
            action.allowResponses("Take [Set 6] Veers into hand and deploy [Set 9] Prepare For A Surface Attack and up to two Effects that deploy for free and are always [Immune to Alter]",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.and(Icon.VIRTUAL_SET_6, Filters.Veers), false));
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.and(Icon.VIRTUAL_SET_9, Filters.Prepare_For_A_Surface_Attack), true, false));
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

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, SwccgGame game, final EffectResult effectResult, final PhysicalCard self) {
        String opponent = game.getOpponent(playerId);
        // Check condition(s)

        // Check condition(s)
        if (TriggerConditions.forceDrainInitiatedBy(game, effectResult, opponent, Filters.and(Filters.canBeTargetedBy(self)))
            || TriggerConditions.battleInitiatedAt(game, effectResult, opponent, Filters.and(Filters.canBeTargetedBy(self)))) {
            final Filter atatFilter = Filters.and(Filters.your(playerId), Filters.AT_AT,
                Filters.canMoveAsReactAsActionFromOtherCard(self, false, GameConditions.additionalForceUseRequiredToPlayInterrupt(game, playerId, self), false));

                if (GameConditions.canTarget(game, self, atatFilter)) {

                final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
                action.setText("Move your AT-AT as a 'react'");
                // Choose target(s)
                action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Choose AT-AT", atatFilter) {
                            @Override
                            protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                                action.addAnimationGroup(targetedCard);
                                // Allow response(s)
                                action.allowResponses("Move " + GameUtils.getCardLink(targetedCard) + " as a 'react'",
                                        new RespondablePlayCardEffect(action) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {
                                                // Get the final targeted card(s)
                                                PhysicalCard finalAtat = action.getPrimaryTargetCard(targetGroupId);
                                                // Perform result(s)
                                                action.appendEffect(
                                                        new MoveAsReactEffect(action, finalAtat, false));
                                            }
                                        }
                                );
                            }
                        }
                );
                return Collections.singletonList(action);
            }
        }

        return null;
    }
}