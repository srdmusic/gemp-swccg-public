package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.ControlsCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.cards.evaluators.OnTableEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.InBattleCondition;
import com.gempukku.swccgo.logic.conditions.NotCondition;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DrawCardIntoHandFromBottomOfLostPileEffect;
import com.gempukku.swccgo.logic.modifiers.AttritionModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifiersMayNotBeCanceledModifier;
import com.gempukku.swccgo.logic.modifiers.LostInterruptModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Objective
 * Title: The Shield Will Be Down In Moments / Imperial Troops Have Entered The Base!
 */
public class Card501_003_BACK extends AbstractObjective {
    public Card501_003_BACK() {
        super(Side.DARK, 7, Title.Imperial_Troops_Have_Entered_The_Base, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("While this side up, attrition against opponent is +1 for each Imperial leader in battle. Rebel Leadership and We're Doomed are Lost Interrupts. Your Force drains are +1 at opponent's sites (and Echo sites) where your snowtrooper or non-unique AT-AT is present. Unless opponent controls a Hoth location, your Force drain bonuses may not be canceled. Once per turn, if opponent just lost Force to You May Start Your Landing, may take bottom card of Lost Pile into hand. " +
                "Flip this card if opponent controls Hoth system.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_22);
        setTestingText("Imperial Troops Have Entered the Base!");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new LostInterruptModifier(self, Filters.or(Filters.title("Rebel Leadership"), Filters.Were_Doomed)));
        modifiers.add(new AttritionModifier(self, new InBattleCondition(self, Filters.Imperial_leader), new OnTableEvaluator(self, Filters.and(Filters.participatingInBattle, Filters.Imperial_leader)), opponent));
        modifiers.add(new ForceDrainModifier(self, Filters.and(Filters.or(Filters.and(Filters.opponents(self), Filters.site), Filters.Echo_site), Filters.wherePresent(self, Filters.and(Filters.your(self), Filters.or(Filters.snowtrooper, Filters.and(Filters.non_unique, Filters.AT_AT))))), 1, playerId));
        modifiers.add(new ForceDrainModifiersMayNotBeCanceledModifier(self, new NotCondition(new ControlsCondition(opponent, Filters.Hoth_location)), Filters.your(self)));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();

        final String opponent = game.getOpponent(playerId);
        GameTextActionId gameTextActionId = GameTextActionId.IMPERIAL_TROOPS_HAVE_ENTERED__TAKE_CARD_FROM_LOST_PILE;
        if (TriggerConditions.justLostForceFromCard(game, effectResult, opponent, Filters.and(Filters.your(self), Filters.You_May_Start_Your_Landing))
                && GameConditions.canTakeCardsIntoHandFromLostPile(game, playerId, self, gameTextActionId)
                && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Take bottom card of Lost Pile into hand");
            // Allow response(s)
            action.setSingletonTrigger(true);

            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                new DrawCardIntoHandFromBottomOfLostPileEffect(action, playerId));

            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canBeFlipped(game, self)
                && GameConditions.controls(game, opponent, SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE, Filters.Hoth_system)) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setSingletonTrigger(true);
            action.setText("Flip");
            action.setActionMsg(null);
            // Perform result(s)
            action.appendEffect(
                    new FlipCardEffect(action, self));
            return Collections.singletonList(action);
        }
        return null;
    }
}
