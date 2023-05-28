package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.CancelTargetingEffect;
import com.gempukku.swccgo.cards.effects.PeekAtBottomCardOfCardPileEffect;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.decisions.YesNoDecision;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.effects.PutCardFromForcePileOnTopOfCardPileEffect;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.SendMessageEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.TargetingActionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Interrupt
 * Subtype: Lost
 * Title: A Jedi's Fury
 */
public class Card501_063 extends AbstractUsedOrLostInterrupt {
    public Card501_063() {
        super(Side.LIGHT, 4, "A Jedi's Fury", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("It had been decades since Vader had felt the sting of an enemy's blade.");
        setGameText("USED: If a character of ability > 4 was just 'hit' during battle, opponent loses 1 Force. OR Peek at bottom card of your Force Pile; may relocate it to the top of that Pile. LOST: Cancel an attempt to ‘choke’ (or target with Force Lightning) a character of ability > 4.");
        addIcons(Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_21);
        setTestingText("A Jedi's Fury");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new ArrayList<>();

        if (GameConditions.isDuringBattle(game)
                && TriggerConditions.justHit(game, effectResult, Filters.and(Filters.character, Filters.abilityMoreThan(4)))) {

            final String opponent = game.getOpponent(playerId);

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText("Opponent loses 1 Force");
            action.allowResponses(new RespondablePlayCardEffect(action) {
                @Override
                protected void performActionResults(Action targetingAction) {
                    action.appendEffect(
                            new LoseForceEffect(action, opponent, 1));
                }
            });

            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        // Check condition(s)
        if (GameConditions.hasForcePile(game, playerId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText("Peek at bottom card of Force Pile");
            // Allow response(s)
            action.allowResponses(new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new PeekAtBottomCardOfCardPileEffect(action, playerId, playerId, Zone.FORCE_PILE) {
                                        @Override
                                        protected void cardsPeekedAt(List<PhysicalCard> peekedAtCards) {
                                            final PhysicalCard card = peekedAtCards.iterator().next();
                                            if (card != null) {
                                                action.appendEffect(new PlayoutDecisionEffect(action, playerId, new YesNoDecision("Move card to top of Force Pile?") {
                                                    @Override
                                                    protected void yes() {
                                                        action.appendEffect(new PutCardFromForcePileOnTopOfCardPileEffect(action, playerId, card, Zone.FORCE_PILE, true));
                                                    }

                                                    @Override
                                                    protected void no() {
                                                        action.appendEffect(new SendMessageEffect(action, playerId + " chooses not to move card to top of Force Pile"));
                                                    }
                                                }));
                                            }
                                        }
                                    });
                        }
                    }
            );
            actions.add(action);
        }
        return actions;
    }


    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(final String playerId, SwccgGame game, final Effect effect, final PhysicalCard self) {
        String opponent = game.getOpponent(playerId);
        Filter filter = Filters.and(Filters.character, Filters.abilityMoreThan(4));
        Collection<TargetingReason> targetingReasons = Arrays.asList(TargetingReason.TO_BE_CHOKED);

        // Check condition(s)
        if (TriggerConditions.isTargetedForReason(game, effect, opponent, filter, targetingReasons)
                || TriggerConditions.isPlayingCardTargeting(game, effect, Filters.Force_Lightning, filter)) {
            final RespondableEffect respondableEffect = (RespondableEffect) effect;
            final List<PhysicalCard> cardsTargeted = (TriggerConditions.isPlayingCardTargeting(game, effect, Filters.Force_Lightning, filter)?
                    TargetingActionUtils.getCardsTargeted(game, respondableEffect.getTargetingAction(), filter) :
                    TargetingActionUtils.getCardsTargetedForReason(game, respondableEffect.getTargetingAction(), targetingReasons, filter));

            if (!cardsTargeted.isEmpty()) {

                final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
                action.setText("Cancel targeting");
                // Choose target(s)
                action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Choose character", Filters.in(cardsTargeted)) {
                            @Override
                            protected void cardTargeted(final int targetGroupId1, final PhysicalCard droidTargeted) {
                                action.addAnimationGroup(droidTargeted);
                                // Allow response(s)
                                action.allowResponses("Cancel targeting of " + GameUtils.getCardLink(droidTargeted),
                                        new RespondablePlayCardEffect(action) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {
                                                // Get the final targeted card(s)
                                                PhysicalCard finalDroid = action.getPrimaryTargetCard(targetGroupId1);
                                                // Perform result(s)
                                                action.appendEffect(
                                                        new CancelTargetingEffect(action, respondableEffect));
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
