package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.SetWhileInPlayDataEffect;
import com.gempukku.swccgo.cards.effects.takeandputcards.StackCardsFromHandEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.WhileInPlayData;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.decisions.YesNoDecision;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.effects.PutStackedCardInLostPileEffect;
import com.gempukku.swccgo.logic.effects.PutStackedCardInUsedPileEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseStackedCardEffect;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Effect
 * Title: A Power Loss
 */
public class Card501_019 extends AbstractNormalEffect {
    public Card501_019() {
        super(Side.LIGHT, 5, PlayCardZoneOption.ATTACHED, Title.A_Power_Loss, Uniqueness.UNIQUE);
        setLore("");
        setGameText("Deploy on Central Core; opponent may stack up to 4 cards from their hand face-up here. If you just won a battle at a Death Star site, place a card stacked here in opponent's Used Pile. If no cards stacked here; power 'shut down.’ [Immune to Alter.]");
        addIcons(Icon.VIRTUAL_SET_15);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("A Power Loss");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Death_Star_Central_Core;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new ArrayList<>();
        final String playerId = self.getOwner();
        final String opponent = game.getOpponent(self.getOwner());

        if (TriggerConditions.justDeployed(game, effectResult, self)) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setActionMsg(opponent + " may stack up to 4 cards from hand");
            action.appendEffect(
                    new SetWhileInPlayDataEffect(action, self, new WhileInPlayData())
            );
            action.appendEffect(
                    new PlayoutDecisionEffect(action, opponent, new YesNoDecision("Stack cards on " + GameUtils.getCardLink(self) + "?") {
                        @Override
                        protected void yes() {
                            action.appendEffect(
                                    new StackCardsFromHandEffect(action, opponent, 1, 4, self, false)
                            );
                        }

                        @Override
                        protected void no() {
                            game.getGameState().sendMessage("Power is 'shut down'");
                            action.addAnimationGroup(self);
                            action.appendEffect(
                                    new SetWhileInPlayDataEffect(action, self, null)
                            );
                        }
                    })
            );
            actions.add(action);
        }

        if (TriggerConditions.wonBattleAt(game, effectResult, playerId, Filters.Death_Star_site)
                && GameConditions.hasStackedCards(game, self)) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Place a card stacked here in " + opponent + "'s Used Pile");
            action.appendTargeting(
                    new ChooseStackedCardEffect(action, playerId, self) {
                        @Override
                        protected void cardSelected(PhysicalCard selectedCard) {
                            if (GameConditions.hasGameTextModification(game, self, ModifyGameTextType.A_POWER_LOSS__CARDS_GO_LOST_INSTEAD_OF_USED)) {
                                action.appendEffect(
                                        new PutStackedCardInLostPileEffect(action, opponent, selectedCard, false)
                                );
                            } else {
                                action.appendEffect(
                                        new PutStackedCardInUsedPileEffect(action, opponent, selectedCard, false)
                                );
                            }
                        }
                    }
            );
            actions.add(action);
        }

        if (TriggerConditions.isTableChanged(game, effectResult)
                && !GameConditions.hasStackedCards(game, self)
                && GameConditions.cardHasWhileInPlayDataSet(self)) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            game.getGameState().sendMessage("Power is 'shut down'");
            action.addAnimationGroup(self);
            action.appendEffect(
                    new SetWhileInPlayDataEffect(action, self, null)
            );
            actions.add(action);
        }

        return actions;
    }

    @Override
    public String getDisplayableInformation(SwccgGame game, PhysicalCard self) {
        if (!GameConditions.cardHasWhileInPlayDataSet(self)) {
            return "Power is 'shut down'";
        } else {
            return "Power is on";
        }
    }
}
